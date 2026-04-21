package com.smartcampus;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;
import java.util.*;

@Path("/api/v1/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    public static Map<String, Room> roomDB = new HashMap<>();

    @GET
    public Collection<Room> getAllRooms() {
        return roomDB.values();
    }

    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Room ID is required")
                    .build();
        }

        roomDB.put(room.getId(), room);

        URI location = UriBuilder.fromPath("/api/v1/rooms/{id}")
                .build(room.getId());

        return Response.status(Response.Status.CREATED)
                .location(location)
                .entity(room)
                .build();
    }

    @GET
    @Path("/{id}")
    public Response getRoom(@PathParam("id") String id) {

        Room room = roomDB.get(id);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found")
                    .build();
        }

        return Response.ok(room).build();
    }

    // ✅ DELETE must be inside class
    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") String id) {

        Room room = roomDB.get(id);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found")
                    .build();
        }

        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Room cannot be deleted. Sensors still assigned.");
        }

        roomDB.remove(id);

        return Response.ok("Room deleted successfully").build();
    }

    @PUT
    @Path("/{id}")
    public Response updateRoom(@PathParam("id") String id, Room updatedRoom) {

        Room existing = roomDB.get(id);

        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Room not found")
                    .build();
        }

        existing.setName(updatedRoom.getName());
        existing.setCapacity(updatedRoom.getCapacity());

        return Response.ok(existing).build();
    }

}
