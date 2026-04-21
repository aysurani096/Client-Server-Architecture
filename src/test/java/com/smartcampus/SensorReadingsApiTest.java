package com.smartcampus;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class SensorReadingsApiTest {

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
    public void resetState() throws Exception {
        RoomResource.roomDB.clear();
        SensorResource.sensorDB.clear();
        clearReadingDb();
    }

    @Test
    public void getReadings_subResourceLocatorReturnsEmptyHistoryInitially() {
        createRoom("room-r1", "Engineering Lab", 40);
        createSensor("sensor-r1", "TEMPERATURE", "ACTIVE", 21.0, "room-r1");

        given()
        .when()
                .get("/sensors/{id}/readings", "sensor-r1")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(0));
    }

    @Test
    public void postReading_returns201AndCreatedReadingPayload() {
        createRoom("room-r2", "Library", 15);
        createSensor("sensor-r2", "TEMPERATURE", "ACTIVE", 20.0, "room-r2");

        Map<String, Object> reading = Map.of(
                "id", "reading-r1",
                "timestamp", 1711015200000L,
                "value", 24.1
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "sensor-r2")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo("reading-r1"))
                .body("timestamp", equalTo(1711015200000L))
                .body("value", equalTo(24.1f));
    }

    @Test
    public void getReadings_afterPostingReturnsFullHistory() {
        createRoom("room-r3", "Seminar Hall", 50);
        createSensor("sensor-r3", "HUMIDITY", "ACTIVE", 45.0, "room-r3");

        postReading("sensor-r3", "reading-r2", 1711015300000L, 46.5);
        postReading("sensor-r3", "reading-r3", 1711015400000L, 47.2);

        given()
        .when()
                .get("/sensors/{id}/readings", "sensor-r3")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(2))
                .body("id", hasItem("reading-r2"))
                .body("id", hasItem("reading-r3"));
    }

    @Test
    public void postReading_updatesParentSensorCurrentValue() {
        createRoom("room-r4", "Control Room", 8);
        createSensor("sensor-r4", "TEMPERATURE", "ACTIVE", 18.0, "room-r4");

        Map<String, Object> reading = Map.of(
                "id", "reading-r4",
                "timestamp", 1711015500000L,
                "value", 26.4
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "sensor-r4")
        .then()
                .statusCode(201);

        given()
                .queryParam("type", "TEMPERATURE")
        .when()
                .get("/sensors")
        .then()
                .statusCode(200)
                .body("[0].id", equalTo("sensor-r4"))
                .body("[0].currentValue", equalTo(26.4f));
    }

    @Test
    public void postReading_forMissingSensorReturns404() {
        Map<String, Object> reading = Map.of(
                "id", "reading-r5",
                "timestamp", 1711015600000L,
                "value", 22.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "missing-sensor")
        .then()
                .statusCode(404)
                .body(equalTo("Sensor not found"));
    }

    @Test
    public void postReading_forMaintenanceSensorReturns403JsonError() {
        createRoom("room-r5", "Service Bay", 5);
        createSensor("sensor-r5", "TEMPERATURE", "MAINTENANCE", 18.0, "room-r5");

        Map<String, Object> reading = Map.of(
                "id", "reading-r6",
                "timestamp", 1711015700000L,
                "value", 30.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "sensor-r5")
        .then()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .body("status", equalTo(403))
                .body("message", notNullValue());
    }

    @Test
    public void postReading_forMaintenanceSensorDoesNotPersistReading() {
        createRoom("room-r6", "Workshop", 10);
        createSensor("sensor-r6", "TEMPERATURE", "MAINTENANCE", 19.0, "room-r6");

        Map<String, Object> reading = Map.of(
                "id", "reading-r7",
                "timestamp", 1711015800000L,
                "value", 31.2
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "sensor-r6")
        .then()
                .statusCode(403);

        given()
        .when()
                .get("/sensors/{id}/readings", "sensor-r6")
        .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    public void postReading_withWrongContentTypeReturns415() {
        createRoom("room-r7", "Media Room", 12);
        createSensor("sensor-r7", "TEMPERATURE", "ACTIVE", 20.0, "room-r7");

        given()
                .contentType("text/plain")
                .body("not-json")
        .when()
                .post("/sensors/{id}/readings", "sensor-r7")
        .then()
                .statusCode(415);
    }

    @Test
    public void postReading_withMalformedJsonReturns400() {
        createRoom("room-r8", "Innovation Hub", 22);
        createSensor("sensor-r8", "TEMPERATURE", "ACTIVE", 20.0, "room-r8");

        given()
                .contentType(ContentType.JSON)
                .body("{\"id\":\"reading-r8\",")
        .when()
                .post("/sensors/{id}/readings", "sensor-r8")
        .then()
                .statusCode(400);
    }

    private void postReading(String sensorId, String readingId, long timestamp, double value) {
        Map<String, Object> reading = Map.of(
                "id", readingId,
                "timestamp", timestamp,
                "value", value
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", sensorId)
        .then()
                .statusCode(201);
    }

    private void createRoom(String id, String name, int capacity) {
        Room room = new Room(id, name, capacity);
        RoomResource.roomDB.put(id, room);
    }

    private void createSensor(String id, String type, String status, double currentValue, String roomId) {
        Sensor sensor = new Sensor();
        sensor.setId(id);
        sensor.setType(type);
        sensor.setStatus(status);
        sensor.setCurrentValue(currentValue);
        sensor.setRoomId(roomId);
        SensorResource.sensorDB.put(id, sensor);

        Room room = RoomResource.roomDB.get(roomId);
        if (room != null) {
            room.getSensorIds().add(id);
        }
    }

    @SuppressWarnings("unchecked")
    private void clearReadingDb() throws Exception {
        Field field = SensorReadingResource.class.getDeclaredField("readingDB");
        field.setAccessible(true);
        ((Map<String, List<SensorReading>>) field.get(null)).clear();
    }
}
