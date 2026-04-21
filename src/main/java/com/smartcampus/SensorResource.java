package com.smartcampus;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;

@Path("/api/v1/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    public static Map<String, Sensor> sensorDB = new HashMap<>();

    // POST register sensor
    @POST
    public Response createSensor(Sensor sensor) {

        // validate room existence
        Room room = RoomResource.roomDB.get(sensor.getRoomId());

        if (room == null) {
            throw new LinkedResourceNotFoundException("Room does not exist for sensor assignment");
        }

        sensorDB.put(sensor.getId(), sensor);

        // link sensor to room
        room.getSensorIds().add(sensor.getId());

        return Response.status(Response.Status.CREATED)
                .entity(sensor)
                .build();
    }

    // GET sensors (with optional filtering)
    @GET
    public Collection<Sensor> getSensors(@QueryParam("type") String type) {

        if (type == null) {
            return sensorDB.values();
        }

        List<Sensor> filtered = new ArrayList<>();

        for (Sensor s : sensorDB.values()) {
            if (s.getType().equalsIgnoreCase(type)) {
                filtered.add(s);
            }
        }

        return filtered;
    }
    @Path("/{id}/readings")
    public SensorReadingResource getReadingResource(@PathParam("id") String id) {
        return new SensorReadingResource(id);
    }
    @PUT
    @Path("/{id}/status")
    public Response updateSensorStatus(@PathParam("id") String id,
                                    @QueryParam("value") String status) {

        Sensor sensor = sensorDB.get(id);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Sensor not found")
                    .build();
        }

        sensor.setStatus(status);

        return Response.ok(sensor).build();
    }
}
