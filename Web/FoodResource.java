/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.amann.web;

import com.amann.service.SearchBean;
import com.amann.data.ConceptTerms;
import com.amann.data.FoodItem;
import com.amann.service.DataBean;
import com.amann.service.FoodSearchBean;
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

@Path("/food")
@Stateless
public class FoodResource {

    @Inject
    private FoodSearchBean foodSearchBean;

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public List<FoodItem> findFood(@QueryParam("name") String name) {
        if (name == null) {
            throw new WebApplicationException(400);
        }
        try {
            return foodSearchBean.findFoodItem(name);
        } catch (EJBException ex) {
            throw new WebApplicationException(500);
        }
    }

}
