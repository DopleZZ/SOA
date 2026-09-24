package ru.itmo.soa.shop.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import ru.itmo.soa.shop.model.Vehicle;

import java.util.List;

@Path("/shop/search")
public interface ShopApi {

    @GET
    @Path("/by-type/{type}")
    @Produces({"application/xml"})
    List<Vehicle> searchByType(@PathParam("type") String type);

    @GET
    @Path("/by-engine-power/{from}/{to}")
    @Produces({"application/xml"})
    List<Vehicle> searchByEnginePowerRange(@PathParam("from") String from, @PathParam("to") String to);
}
