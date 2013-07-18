/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;

/**
 *
 * @author Admin
 */
@JsonInclude(Include.NON_NULL)
public class FoodItem {
    private String id;
    private List<String> tags;
    private List<String> commonNames;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getCommonNames() {
        return commonNames;
    }

    public void setCommonNames(List<String> commonNames) {
        this.commonNames = commonNames;
    }
    
}
