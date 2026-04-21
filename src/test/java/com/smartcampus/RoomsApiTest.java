package com.smartcampus;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

public class RoomsApiTest {

    private static HttpServer server;

    @BeforeClass
    public static void setUpClass() {
        server = MainServer.startServer();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
        RestAssured.basePath = "/api/v1";
    }

    @AfterClass
    public static void tearDownClass() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Before
    public void resetState() {
        RoomResource.roomDB.clear();
        SensorResource.sensorDB.clear();
    }

    @Test
    public void createRoom_returns201_andCreatedRoomPayload() {
        Map<String, Object> room = Map.of(
                "id", "room-101",
                "name", "Engineering Lab",
                "capacity", 40
        );

        given()
                .contentType(ContentType.JSON)
                .body(room)
        .when()
                .post("/rooms")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo("room-101"))
                .body("name", equalTo("Engineering Lab"))
                .body("capacity", equalTo(40));
    }

    @Test
    public void createRoom_includesLocationHeader_perRubric() {
        Map<String, Object> room = Map.of(
                "id", "room-102",
                "name", "Library Study Room",
                "capacity", 12
        );

        given()
                .contentType(ContentType.JSON)
                .body(room)
        .when()
                .post("/rooms")
        .then()
                .statusCode(201)
                .header("Location", notNullValue());
    }

    @Test
    public void createRoom_withoutId_returns400() {
        Map<String, Object> room = Map.of(
                "name", "Unnamed Room",
                "capacity", 20
        );

        given()
                .contentType(ContentType.JSON)
                .body(room)
        .when()
                .post("/rooms")
        .then()
                .statusCode(400)
                .body(equalTo("Room ID is required"));
    }

    @Test
    public void getRoomById_returnsRoomDetails() {
        Room room = new Room("room-103", "Seminar Hall", 80);
        RoomResource.roomDB.put(room.getId(), room);

        given()
        .when()
                .get("/rooms/{id}", room.getId())
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo("room-103"))
                .body("name", equalTo("Seminar Hall"))
                .body("capacity", equalTo(80));
    }

    @Test
    public void getRoomById_whenMissing_returns404() {
        given()
        .when()
                .get("/rooms/{id}", "missing-room")
        .then()
                .statusCode(404)
                .body(equalTo("Room not found"));
    }

    @Test
    public void listRooms_returnsAllStoredRooms() {
        RoomResource.roomDB.put("room-104", new Room("room-104", "Physics Lab", 30));
        RoomResource.roomDB.put("room-105", new Room("room-105", "Chemistry Lab", 25));

        given()
        .when()
                .get("/rooms")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", hasItem("room-104"))
                .body("id", hasItem("room-105"));
    }

    @Test
    public void updateRoom_updatesMutableFields() {
        RoomResource.roomDB.put("room-106", new Room("room-106", "Old Name", 10));
        Map<String, Object> updatedRoom = Map.of(
                "id", "room-106",
                "name", "Updated Name",
                "capacity", 60
        );

        given()
                .contentType(ContentType.JSON)
                .body(updatedRoom)
        .when()
                .put("/rooms/{id}", "room-106")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo("room-106"))
                .body("name", equalTo("Updated Name"))
                .body("capacity", equalTo(60));
    }

    @Test
    public void updateRoom_whenMissing_returns404() {
        Map<String, Object> updatedRoom = Map.of(
                "id", "missing-room",
                "name", "Updated Name",
                "capacity", 60
        );

        given()
                .contentType(ContentType.JSON)
                .body(updatedRoom)
        .when()
                .put("/rooms/{id}", "missing-room")
        .then()
                .statusCode(404)
                .body(equalTo("Room not found"));
    }

    @Test
    public void deleteRoom_withoutAssignedSensors_returnsSuccess() {
        RoomResource.roomDB.put("room-107", new Room("room-107", "Tutorial Room", 18));

        given()
        .when()
                .delete("/rooms/{id}", "room-107")
        .then()
                .statusCode(200)
                .body(equalTo("Room deleted successfully"));

        given()
        .when()
                .get("/rooms/{id}", "room-107")
        .then()
                .statusCode(404);
    }

    @Test
    public void deleteRoom_withAssignedSensors_returns409Conflict() {
        Room occupiedRoom = new Room("room-108", "Server Room", 6);
        occupiedRoom.setSensorIds(List.of("sensor-1"));
        RoomResource.roomDB.put(occupiedRoom.getId(), occupiedRoom);

        given()
        .when()
                .delete("/rooms/{id}", occupiedRoom.getId())
        .then()
                .statusCode(409)
                .contentType(ContentType.JSON)
                .body("status", equalTo(409))
                .body("message", equalTo("Room cannot be deleted. Sensors still assigned."));
    }

    @Test
    public void deleteRoom_whenMissing_returns404() {
        given()
        .when()
                .delete("/rooms/{id}", "missing-room")
        .then()
                .statusCode(404)
                .body(equalTo("Room not found"));
    }
}
