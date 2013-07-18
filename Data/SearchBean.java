/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.drugs;

import com.amann.model.Concept;
import com.amann.service.Searcher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.sql.DataSource;

/**
 *
 * @author Admin
 */
@Stateless
public class SearchBean implements Searcher {

    @Resource(name = "drugdata")
    DataSource drugdata;
    private static final String conceptSearchTermQuery = "select rxcui, str, tty from rxnorm.concepts where (tty = 'BN' or tty = 'IN') and str ilike ?";

    @Override
    public List<Concept> findConceptsByTerm(String conceptTerm) {
        List<Concept> results = new ArrayList<>();
        try (Connection conn = drugdata.getConnection()) {
            try (PreparedStatement st = conn.prepareStatement(conceptSearchTermQuery)) {
                st.setString(1, "%" + conceptTerm + "%");
                try (ResultSet rs = st.executeQuery()) {
                    while (rs.next()) {
                        Concept c = new Concept();
                        c.setId(rs.getInt(1));
                        c.setName(rs.getString(2));
                        c.setType(rs.getString(3));
                        results.add(c);
                    }
                }
            }
        } catch (SQLException ex) {
            throw new EJBException(ex);
        }
        return results;
    }
}
