package com.smartcampus;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        ErrorResponse error = new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), ex.getMessage());
        return Response.status(Response.Status.CONFLICT)
                .entity(error)
                .build();
    }
}
