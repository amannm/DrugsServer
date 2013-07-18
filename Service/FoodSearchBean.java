/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.service;

import com.amann.data.FoodItem;
import java.io.File;
import java.io.IOException;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         .import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.sql.DataSource;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * @author Admin
 */
@Singleton
public class FoodSearchBean {

    @Resource(lookup = "jdbc/postgres")
    private DataSource ds;
    private Directory directory;
    private IndexSearcher searcher;
    private static final String termIndexPath = "C:/data/lucene/food";
    private static final String updateQuery = "select \"NDB_No\", \"Long_Desc\", \"ComName\" from sr25.\"FOOD_DES\"";
    private static final int numSearchResults = 200;
    private static final int numSpellingSuggestions = 5;
    private static final int numCompletionSuggestions = 5;

    @PostConstruct
    public void init() {
        System.out.println("Starting initialization");
        try {
            directory = new MMapDirectory(new File(termIndexPath));
        } catch (IOException ex) {
            Logger.getLogger(FoodSearchBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        rebuildSearchIndex();
        loadIndices();
        System.out.println("Finished initialization");
    }

    public void rebuildSearchIndex() {
        System.out.println("Rebuilding search index");
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(updateQuery)) {
                try (ResultSet rs = st.executeQuery()) {
                    IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_43, new StandardAnalyzer(Version.LUCENE_43));
                    try (IndexWriter writer = new IndexWriter(directory, iwc)) {
                        while (rs.next()) {
                            Document doc = new Document();
                            doc.add(new StoredField("ndb", rs.getString(1)));
                            String descriptionString = rs.getString(2);
                            TextField f = new TextField("tag", descriptionString, Field.Store.YES);
                            doc.add(f);
                            String commonNameString = rs.getString(3);
                            if (commonNameString != null) {
                                doc.add(new TextField("common", commonNameString, Field.Store.YES));
                            }
                            writer.addDocument(doc);
                        }
                    }
                } catch (IOException ex) {
                    Logger.getLogger(FoodSearchBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(FoodSearchBean.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Future<Void> loadIndices() {
        System.out.println("Loading indices");
        try {
            IndexReader reader = DirectoryReader.open(directory);

            searcher = new IndexSearcher(reader);
        } catch (IOException ex) {
            Logger.getLogger(SearchBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public List<FoodItem> findFoodItem(String queryString) {
        List<FoodItem> results = new ArrayList<>(numSearchResults);
        System.out.println("Searching for term \"" + queryString + "\"");
        try {
            String[] queryTokens = queryString.split(" ");
            BooleanQuery booleanQuery = new BooleanQuery();
            for (String token : queryTokens) {
                booleanQuery.add(new TermQuery(new Term("tag", token.trim())), Occur.SHOULD);
            }
            TopDocs td = searcher.search(booleanQuery, numSearchResults);
            for (ScoreDoc scoreDoc : td.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
//                Iterator<IndexableField> l = doc.iterator();
//                List<String> taglist = new ArrayList<>();
//                List<String> commonlist = new ArrayList<>();
//                while (l.hasNext()) {
//                    IndexableField inf = l.next();
//                    if ("common".equals(inf.name())) {
//                        System.out.println(inf.name() + " " + inf.boost());
//                        commonlist.add(inf.stringValue());
//                    } else {
//                        if ("id".equals(inf.name())) {
//                        } else {
//                            System.out.println(inf.name() + " " + inf.boost());
//                            taglist.add(inf.stringValue());
//                        }
//                    }
//
//                }
                
                FoodItem fi = new FoodItem();
                fi.setId(doc.get("id"));
                fi.setTags(Arrays.asList(doc.getValues("tag")));
                String[] r = doc.getValues("common");
                if(r.length > 0) {
                    fi.setCommonNames(Arrays.asList(r));
                }
                results.add(fi);
            }
        } catch (IOException ex) {
            throw new EJBException(ex);
        }
        return results;
    }
}
