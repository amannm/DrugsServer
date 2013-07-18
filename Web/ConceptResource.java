/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.web;

import com.amann.service.SearchBean;
import com.amann.data.ConceptTerms;
import com.amann.service.DataBean;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

@Path("/drugs")
@Stateless
public class ConceptResource {

    @Inject
    private SearchBean searchBean;
    @Inject
    private DataBean dataBean;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Set<String> users() {
        try {
            return null;
        } catch (EJBException ex) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("/terms")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> terms(@QueryParam("completion") String completion, @QueryParam("similar") String similar) {
        if (completion != null && similar == null) {
            try {
                return searchBean.getCompletionSuggestions(completion);
            } catch (EJBException ex) {
                throw new WebApplicationException(500);
            }
        } else {
            if (similar != null && completion == null) {
                try {
                    return searchBean.getSpellingSuggestions(similar);
                } catch (EJBException ex) {
                    throw new WebApplicationException(500);
                }
            } else {
                throw new WebApplicationException(400);
            }
        }
    }

    @GET
    @Path("/concepts")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<Integer, Map<String, Set<String>>> concepts(@QueryParam("term") String query) {
        if (query == null) {
            throw new WebApplicationException(400);
        }
        try {
            return searchBean.findConceptsByTerm(query);
        } catch (EJBException ex) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("concept/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public ConceptTerms concept(@PathParam("id") Integer id) {
        if (id == null) {
            throw new WebApplicationException(400);
        }
        try {
            //return dataBean.getTermsByConceptId(id);
            return null;
        } catch (EJBException ex) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("concept/{id}/attributes")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, List<String>> attributes(@PathParam("id") Integer id) {
        if (id == null) {
            throw new WebApplicationException(400);
        }
        try {
            //return dataBean.getConceptAttributes(id);
            return null;
        } catch (EJBException ex) {
            throw new WebApplicationException(500);
        }
    }

    @GET
    @Path("concept/{id}/relations")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, List<ConceptTerms>> relations(@PathParam("user") String voc, @PathParam("id") Integer id) {
        if (voc == null || id == null) {
            throw new WebApplicationException(400);
        }
        try {
            //return dataBean.getConceptRelations(voc, id);
            return null;
        } catch (EJBException ex) {
            throw new WebApplicationException(500);
        }
    }
}
