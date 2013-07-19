/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.service;

import com.amann.data.ConceptTerms;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.sql.DataSource;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Admin
 */
@Singleton
public class SearchBean {

    @Resource(lookup = "jdbc/postgres")
    private DataSource ds;
    private Directory directory;
    private IndexSearcher searcher;
    private AnalyzingSuggester suggester;
    private SpellChecker checker;
    private static final String termIndexPath = "C:/data/lucene";
    private static final String updateQuery = "select sab, rxcui, str from rxnorm.concepts";
    private static final int numSearchResults = 200;
    private static final int numSpellingSuggestions = 5;
    private static final int numCompletionSuggestions = 5;

    @PostConstruct
    public void init() {
        try {
            directory = new MMapDirectory(new File(termIndexPath));
            loadIndices();
        } catch (IOException ex) {
            Logger.getLogger(SearchBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        //rebuildSearchIndex();
    }

    public void rebuildSearchIndex() {
        System.out.println("Rebuilding search index");
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(updateQuery)) {
                try (ResultSet rs = st.executeQuery()) {
                    IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(Version.LUCENE_42));
                    try (IndexWriter writer = new IndexWriter(directory, iwc)) {
                        while (rs.next()) {
                            Document doc = new Document();
                            doc.add(new StoredField("src", rs.getString(1)));
                            doc.add(new StoredField("id", rs.getInt(2)));
                            doc.add(new TextField("term", rs.getString(3), Field.Store.YES));
                            writer.addDocument(doc);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SearchBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(SearchBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Asynchronous
    public Future<Void> loadIndices() {
        System.out.println("Loading indices");
        try {
            IndexReader reader = DirectoryReader.open(directory);
            searcher = new IndexSearcher(reader);
            checker = new SpellChecker(directory);
            LuceneDictionary dict = new LuceneDictionary(reader, "term");
            checker.indexDictionary(dict, new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(Version.LUCENE_42)), true);
            suggester = new AnalyzingSuggester(new Analyzer() {
                @Override
                protected Analyzer.TokenStreamComponents createComponents(String string, Reader reader) {
                    Tokenizer source = new StandardTokenizer(Version.LUCENE_43, reader);
                    TokenFilter sink =
                            new EdgeNGramTokenFilter(
                            new LowerCaseFilter(Version.LUCENE_43,
                            new StandardFilter(Version.LUCENE_43, source)), EdgeNGramTokenFilter.Side.FRONT, 1, 20);
                    return new Analyzer.TokenStreamComponents(source, sink);
                }
            });
            suggester.build(dict);
        } catch (IOException ex) {
            Logger.getLogger(SearchBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Finished loading indices");
        return null;
    }

    public Map<Integer, Map<String, Set<String>>> findConceptsByTerm(String queryString) {
        Map<Integer, Map<String, Set<String>>> results = new HashMap<>(numSearchResults);
        System.out.println("Searching for term \"" + queryString + "\"");
        try {
            TermQuery tq = new TermQuery(new Term("term", queryString.toLowerCase()));
            TopDocs td = searcher.search(tq, numSearchResults);
            for (ScoreDoc scoreDoc : td.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                int x = doc.getField("id").numericValue().intValue();
                String z = doc.get("term");
                String y = doc.get("src");
                if (results.containsKey(x)) {
                    Map<String, Set<String>> asdf = results.get(x);
                    if (asdf.containsKey(y)) {
                        asdf.get(y).add(z);
                    } else {
                        Set<String> list = new HashSet<>(1);
                        list.add(z);
                        asdf.put(y, list);
                    }
                } else {
                    Map<String, Set<String>> xMap = new HashMap<>(1);
                    Set<String> list = new HashSet<>(1);
                    list.add(z);
                    xMap.put(y, list);
                    results.put(x, xMap);
                }
            }
        } catch (IOException ex) {
            throw new EJBException(ex);
        }
        return results;
    }

    public List<String> getSpellingSuggestions(String s) {
        try {
            return Arrays.asList(checker.suggestSimilar(s, numSpellingSuggestions));
        } catch (IOException ex) {
            throw new EJBException(ex);
        }
    }

    public List<String> getCompletionSuggestions(String s) {
        List<String> stringResults = new ArrayList<>(numCompletionSuggestions);
        List<Lookup.LookupResult> results = suggester.lookup(s, false, numCompletionSuggestions);
        for (Lookup.LookupResult r : results) {
            stringResults.add(r.key.toString());
        }
        return stringResults;
    }
}
