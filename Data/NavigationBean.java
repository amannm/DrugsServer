/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.drugs;

import com.amann.model.Concept;
import com.amann.service.Navigator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.sql.DataSource;

/**
 *
 * @author Admin
 */
@Stateless
public class NavigationBean implements Navigator {

    @Resource(name = "drugdata")
    private DataSource drugdata;
    private static final String predecessorsQuery = "select rela, rxcui2 from rxnorm.relationships where rxcui1 = ?";
    private static final String successorsQuery = "select r.rela, c.rxcui, c.str, c.tty from rxnorm.relationships as r inner join rxnorm.concepts as c on r.rxcui1 = c.rxcui where c.tty != 'TMSY' and c.tty != 'SY' and r.rxcui2 = ?";
    private static final String attributesQuery = "select atn, atv from rxnorm.attributes where rxcui = ?";
    private static final String conceptQuery = "select str, tty from rxnorm.concepts as c where c.tty != 'TMSY' and c.tty != 'SY' and rxcui = ?";

    @Override
    public Concept getConceptById(int conceptId) {
        Concept concept = new Concept();
        try (Connection conn = drugdata.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(conceptQuery)) {
                st.setInt(1, conceptId);
                try (ResultSet rs = st.executeQuery()) {
                    if (rs.next()) {
                        concept.setId(conceptId);
                        concept.setName(rs.getString(1));
                        concept.setType(rs.getString(2));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new EJBException(ex);
        }
        return concept;
    }

    
    @Override
    public Map<String, List<Concept>> getSuccessors(int conceptId) {
        try (Connection conn = drugdata.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(successorsQuery)) {
                st.setInt(1, conceptId);
                try (ResultSet rs = st.executeQuery()) {
                    Map<String, List<Concept>> successorMap = new HashMap<>();
                    while (rs.next()) {
                        String relation = rs.getString(1);
                            Concept conc = new Concept();
                            conc.setId(rs.getInt(2));
                            conc.setName(rs.getString(3));
                            conc.setType(rs.getString(4));
                        if (successorMap.containsKey(relation)) {
                            successorMap.get(relation).add(conc);
                        } else {
                            List<Concept> successors = new ArrayList<>();
                            successors.add(conc);
                            successorMap.put(relation, successors);
                        }
                    }
                    return successorMap;
                }
            }
        } catch (SQLException ex) {
            throw new EJBException(ex);
        }
    }

    @Override
    public Map<String, List<String>> getAttributes(int conceptId) {
        try (Connection conn = drugdata.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(attributesQuery)) {
                st.setInt(1, conceptId);
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
        } catch (SQLException ex) {
            throw new EJBException(ex);
        }
    }
}
