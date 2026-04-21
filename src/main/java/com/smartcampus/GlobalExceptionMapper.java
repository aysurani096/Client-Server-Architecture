package com.smartcampus;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            WebApplicationException webApplicationException = (WebApplicationException) ex;
            return webApplicationException.getResponse();
        }

        ErrorResponse error =
                new ErrorResponse(500, "Internal Server Error");

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(error)
                .build();
    }
}
