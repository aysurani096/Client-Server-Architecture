package com.smartcampus;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private static Map<String, List<SensorReading>> readingDB = new HashMap<>();

    private String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public List<SensorReading> getReadings() {
        return readingDB.getOrDefault(sensorId, new ArrayList<>());
    }

    @POST
public Response addReading(SensorReading reading) {

    Sensor sensor = SensorResource.sensorDB.get(sensorId);

    if (sensor == null) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity("Sensor not found")
                .build();
    }

    if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
        throw new SensorUnavailableException("Sensor is unavailable while in maintenance");
    }

    readingDB.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);

    sensor.setCurrentValue(reading.getValue());

    return Response.status(Response.Status.CREATED)
            .entity(reading)
            .build();
}
}
