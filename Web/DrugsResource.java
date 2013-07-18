/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.drugs;

import com.amann.data.Concept;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.PathParam;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

@Path("/")
@Stateless
public class DrugsResource {

    @EJB
    private SuggestionBean suggest;
    @EJB
    private SearchBean search;
    @EJB
    private NavigationBean nav;

    @GET
    @Path("completions/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> complete(@PathParam("query") String query) {
        if (query != null) {
            return suggest.getCompletionSuggestions(query);
        } else {
            throw new WebApplicationException(400);
        }
    }

    @GET
    @Path("spellings/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<String> spell(@PathParam("query") String query) {
        if (query != null) {
            return suggest.getSpellingSuggestions(query);
        } else {
            throw new WebApplicationException(400);
        }
    }

    @GET
    @Path("search/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Concept> search(@PathParam("query") String query) {
        if (query != null) {
            return search.findConceptsByTerm(query);
        } else {
            throw new WebApplicationException(400);
        }
    }

  
    @GET
    @Path("{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public Concept concepts(@PathParam("query") Integer query) {
        if (query != null) {
            return nav.getConceptById(query);
        } else {
            throw new WebApplicationException(400);
        }
    }
    @GET
    @Path("attributes/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, List<String>> attributes(@PathParam("query") Integer query) {
        if (query != null) {
            return nav.getAttributes(query);
        } else {
            throw new WebApplicationException(400);
        }
    }
    
      @GET
    @Path("successors/{query}")
    @Produces({MediaType.APPLICATION_JSON})
    public Map<String, List<Concept>> successors(@PathParam("query") Integer query) {
        if (query != null) {
            return nav.getSuccessors(query);
        } else {
            throw new WebApplicationException(400);
        }
    }
}
