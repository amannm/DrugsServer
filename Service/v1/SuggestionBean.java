/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.drugs;

import com.amann.util.DrugDictionary;
import com.amann.service.Suggester;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.BytesRefArray;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Counter;
import org.apache.lucene.util.Version;

/**
 *
 * @author Admin
 */
@Singleton
public class SuggestionBean implements Suggester {
    @Resource(name = "drugdata")
    private DataSource drugdata;
    
    private static final String queryString = "select distinct str from rxnorm.concepts where (tty = 'BN' or tty = 'IN')";
    private AnalyzingSuggester look;
    private SpellChecker checker;

    @PostConstruct
    public void init() {
        look = new AnalyzingSuggester(new Analyzer() {
            @Override
            protected Analyzer.TokenStreamComponents createComponents(String string, Reader reader) {
                Tokenizer source = new StandardTokenizer(Version.LUCENE_42, reader);
                TokenFilter sink =
                        new EdgeNGramTokenFilter(
                        new LowerCaseFilter(Version.LUCENE_42,
                        new StandardFilter(Version.LUCENE_42, source)), EdgeNGramTokenFilter.Side.FRONT, 1, 20);
                return new Analyzer.TokenStreamComponents(source, sink);
            }
        });
        try {
            checker = new SpellChecker(new RAMDirectory());
        } catch (IOException ex) {
            throw new EJBException(ex);
        }
        update();
    }

    @Asynchronous
    public Future<Void> update() {
        BytesRefArray arr = new BytesRefArray(Counter.newCounter());
        try (Connection conn = drugdata.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(queryString)) {
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        arr.append(new BytesRef(rs.getBytes(1)));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new EJBException(ex);
        }
        DrugDictionary dictionary = new DrugDictionary(arr);
        try {
            look.build(dictionary);
            checker.indexDictionary(dictionary, new IndexWriterConfig(Version.LUCENE_42, new StandardAnalyzer(Version.LUCENE_42)), true);
        } catch (IOException ex) {
            throw new EJBException(ex);
        }

        return null;
    }
    @Override
    public List<String> getSpellingSuggestions(String s) {
        try {
            String[] results = checker.suggestSimilar(s, 5);
            return Arrays.asList(results);
        } catch (IOException ex) {
            throw new EJBException(ex);
        }
    }
    @Override
    public List<String> getCompletionSuggestions(String s) {
        List<Lookup.LookupResult> results = look.lookup(s, false, 5);
        List<String> stringResults = new ArrayList<>(5);
        for (Lookup.LookupResult r : results) {
            stringResults.add(r.key.toString());
        }
        return stringResults;
    }
}
