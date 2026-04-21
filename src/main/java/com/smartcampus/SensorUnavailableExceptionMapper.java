package com.smartcampus;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        ErrorResponse error = new ErrorResponse(Response.Status.FORBIDDEN.getStatusCode(), ex.getMessage());
        return Response.status(Response.Status.FORBIDDEN)
                .entity(error)
                .build();
    }
}
