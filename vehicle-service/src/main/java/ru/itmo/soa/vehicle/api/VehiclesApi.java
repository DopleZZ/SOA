package ru.itmo.soa.vehicle.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import ru.itmo.soa.vehicle.model.IdGroupCount;
import ru.itmo.soa.vehicle.model.SumResult;
import ru.itmo.soa.vehicle.model.Vehicle;
import ru.itmo.soa.vehicle.model.VehicleInput;
import ru.itmo.soa.vehicle.model.VehiclePage;
import ru.itmo.soa.vehicle.model.VehiclePatch;

import java.util.List;

@Path("/api/vehicles")
public interface VehiclesApi {

    @POST
    @Consumes({"application/xml"})
    @Produces({"application/xml"})
    Response createVehicle(VehicleInput vehicleInput);

    @GET
    @Produces({"application/xml"})
    VehiclePage getVehicles(@QueryParam("pageNumber") @DefaultValue("1") String pageNumber,
                             @QueryParam("pageSize") @DefaultValue("10") String pageSize,
                             @QueryParam("sort") List<String> sort,
                             @QueryParam("filter") List<String> filter);

    @GET
    @Path("/{id}")
    @Produces({"application/xml"})
    Vehicle getVehicleById(@PathParam("id") Long id);

    @PUT
    @Path("/{id}")
    @Consumes({"application/xml"})
    @Produces({"application/xml"})
    Vehicle updateVehicle(@PathParam("id") Long id, VehicleInput vehicleInput);

    @PATCH
    @Path("/{id}")
    @Consumes({"application/xml"})
    @Produces({"application/xml"})
    Vehicle partialUpdateVehicle(@PathParam("id") Long id, VehiclePatch vehiclePatch);

    @DELETE
    @Path("/{id}")
    void deleteVehicle(@PathParam("id") Long id);

    @GET
    @Path("/sum-engine-power")
    @Produces({"application/xml"})
    SumResult sumEnginePower();

    @GET
    @Path("/group-count-by-id")
    @Produces({"application/xml"})
    IdGroupCount groupCountById();

    @GET
    @Path("/by-type-greater/{type}")
    @Produces({"application/xml"})
    List<Vehicle> vehiclesByTypeGreaterThan(@PathParam("type") String type);
}
