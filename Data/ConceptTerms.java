/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Admin
 */
@JsonInclude(Include.NON_NULL)
public class ConceptTerms implements Serializable {
    
    private Integer id;
    private String primaryTerm;
    private List<String> alternateTerms;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPrimaryTerm() {
        return primaryTerm;
    }

    public void setPrimaryTerm(String primaryTerm) {
        this.primaryTerm = primaryTerm;
    }

    public List<String> getAlternateTerms() {
        return alternateTerms;
    }

    public void setAlternateTerms(List<String> alternateTerms) {
        this.alternateTerms = alternateTerms;
    }

    

}
