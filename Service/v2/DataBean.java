/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.service;

import com.amann.data.ConceptTerms;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.sql.DataSource;
import javax.ws.rs.WebApplicationException;

/**
 *
 * @author Admin
 */
public class DataBean {

    @Resource(lookup = "jdbc/postgres")
    private DataSource ds;
    
    private static final String attributesQuery = "select a.sab, replace(atn, '_', ' '), atv from rxnorm.attributes where rxcui = ?";
    private static final String relationshipsQuery = "select r.sab, replace(initcap(r.rela), '_', ' '), c.rxcui, array_agg(c.str) from rxnorm.relationships as r inner join rxnorm.concepts as c on r.rxcui1 = c.rxcui where r.rxcui2 = ? group by r.sab, r.rela, c.rxcui";
    private static final String conceptQuery = "select sab, str, tty from rxnorm.concepts where rxcui = ?";

    public ConceptTerms getTermsByConceptId(String voc, Integer conceptId) {
        ConceptTerms term = new ConceptTerms();
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(conceptQuery)) {
                st.setInt(1, conceptId);
                try (ResultSet rs = st.executeQuery()) {
                    term.setId(conceptId);
                    if (rs.next()) {
                        term.setPrimaryTerm(rs.getString(1));
                        List list = new ArrayList<>();
                        while (rs.next()) {
                            list.add(rs.getString(1));
                        }
                        term.setAlternateTerms(list);
                    }
                }
            }
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
        return term;
    }

    public Map<String, List<ConceptTerms>> getConceptRelations(String voc, Integer conceptId) {
        if (voc == null || conceptId == null) {
            throw new WebApplicationException(400);
        }
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(relationshipsQuery)) {
                st.setString(1, voc);
                st.setInt(2, conceptId);
                try (ResultSet rs = st.executeQuery()) {
                    Map<String, List<ConceptTerms>> relationsMap = new HashMap<>();
                    while (rs.next()) {
                        ConceptTerms term = new ConceptTerms();
                        String relation = rs.getString(1);
                        term.setId(rs.getInt(2));
                        Array arr = rs.getArray(3);
                        ResultSet rs2 = arr.getResultSet();
                        if (rs2.next()) {
                            term.setPrimaryTerm(rs2.getString(1));
                            List<String> list = new ArrayList<>();
                            while (rs2.next()) {
                                list.add(rs2.getString(1));
                            }
                            term.setAlternateTerms(list);
                        }
                        if (relationsMap.containsKey(relation)) {
                            relationsMap.get(relation).add(term);
                        } else {
                            List<ConceptTerms> relations = new ArrayList<>();
                            relations.add(term);
                            relationsMap.put(relation, relations);
                        }
                    }
                    return relationsMap;
                }
            }
        } catch (Exception ex) {
            System.err.println(ex);
            throw new WebApplicationException(500);
        }
    }

    public Map<String, List<String>> getConceptAttributes(String voc, Integer conceptId) {
        try (Connection conn = ds.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(attributesQuery)) {
                st.setString(1, voc);
                st.setInt(2, conceptId);
                try (ResultSet rs = st.executeQuery()) {
                    Map<String, List<String>> attributeMap = new HashMap<>();
                    while (rs.next()) {
                        String rela = rs.getString(1);
                        if (attributeMap.containsKey(rela)) {
                            attributeMap.get(rela).add(rs.getString(2));
                        } else {
                            List<String> attributes = new ArrayList<>(1);
                            attributes.add(rs.getString(2));
                            attributeMap.put(rela, attributes);
                        }
                    }
                    return attributeMap;
                }
            }
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }
}
