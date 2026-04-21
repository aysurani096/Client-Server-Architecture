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

public class SensorsApiTest {

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
    public void createSensor_withValidRoom_returns201_andPersistsSensor() {
        createRoom("room-s1", "Engineering Lab", 40);

        Map<String, Object> sensor = Map.of(
                "id", "sensor-1",
                "type", "TEMPERATURE",
                "status", "ACTIVE",
                "currentValue", 23.5,
                "roomId", "room-s1"
        );

        given()
                .contentType(ContentType.JSON)
                .body(sensor)
        .when()
                .post("/sensors")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo("sensor-1"))
                .body("type", equalTo("TEMPERATURE"))
                .body("status", equalTo("ACTIVE"))
                .body("roomId", equalTo("room-s1"))
                .body("currentValue", equalTo(23.5f));

        given()
        .when()
                .get("/rooms/{id}", "room-s1")
        .then()
                .statusCode(200)
                .body("sensorIds", hasItem("sensor-1"));
    }

    @Test
    public void createSensor_withMissingRoom_returns422() {
        Map<String, Object> sensor = Map.of(
                "id", "sensor-2",
                "type", "TEMPERATURE",
                "status", "ACTIVE",
                "currentValue", 20.0,
                "roomId", "missing-room"
        );

        given()
                .contentType(ContentType.JSON)
                .body(sensor)
        .when()
                .post("/sensors")
        .then()
                .statusCode(422)
                .contentType(ContentType.JSON)
                .body("status", equalTo(422))
                .body("message", equalTo("Room does not exist for sensor assignment"));
    }

    @Test
    public void createSensor_withWrongContentType_returns415() {
        createRoom("room-s2", "Library", 20);

        given()
                .contentType("text/plain")
                .body("plain text payload")
        .when()
                .post("/sensors")
        .then()
                .statusCode(415);
    }

    @Test
    public void createSensor_withMalformedJson_returns400() {
        createRoom("room-s3", "Lecture Hall", 100);

        given()
                .contentType(ContentType.JSON)
                .body("{\"id\":\"sensor-3\",")
        .when()
                .post("/sensors")
        .then()
                .statusCode(400);
    }

    @Test
    public void getSensors_withoutFilter_returnsAllSensors() {
        createRoom("room-s4", "North Lab", 30);
        createSensor("sensor-4", "TEMPERATURE", "ACTIVE", 22.0, "room-s4");
        createSensor("sensor-5", "HUMIDITY", "ACTIVE", 61.0, "room-s4");

        given()
        .when()
                .get("/sensors")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", hasItem("sensor-4"))
                .body("id", hasItem("sensor-5"));
    }

    @Test
    public void getSensors_withTypeFilter_returnsMatchingSensorsOnly() {
        createRoom("room-s5", "South Lab", 25);
        createSensor("sensor-6", "TEMPERATURE", "ACTIVE", 21.0, "room-s5");
        createSensor("sensor-7", "HUMIDITY", "ACTIVE", 58.0, "room-s5");
        createSensor("sensor-8", "TEMPERATURE", "ACTIVE", 24.0, "room-s5");

        given()
                .queryParam("type", "TEMPERATURE")
        .when()
                .get("/sensors")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", hasSize(2))
                .body("id", hasItem("sensor-6"))
                .body("id", hasItem("sensor-8"));
    }

    @Test
    public void getSensors_withCaseInsensitiveTypeFilter_returnsMatches() {
        createRoom("room-s6", "Innovation Hub", 18);
        createSensor("sensor-9", "TEMPERATURE", "ACTIVE", 20.5, "room-s6");

        given()
                .queryParam("type", "temperature")
        .when()
                .get("/sensors")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", hasSize(1))
                .body("id", hasItem("sensor-9"));
    }

    @Test
    public void getSensors_withUnknownType_returnsEmptyCollection() {
        createRoom("room-s7", "Media Room", 12);
        createSensor("sensor-10", "TEMPERATURE", "ACTIVE", 19.0, "room-s7");

        given()
                .queryParam("type", "PRESSURE")
        .when()
                .get("/sensors")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(0));
    }

    @Test
    public void getReadings_forExistingSensor_returnsEmptyHistoryInitially() {
        createRoom("room-s8", "Archive", 10);
        createSensor("sensor-11", "TEMPERATURE", "ACTIVE", 18.0, "room-s8");

        given()
        .when()
                .get("/sensors/{id}/readings", "sensor-11")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(0));
    }

    @Test
    public void addReading_forExistingSensor_returns201_andStoresReading() {
        createRoom("room-s9", "Clean Room", 8);
        createSensor("sensor-12", "TEMPERATURE", "ACTIVE", 18.0, "room-s9");

        Map<String, Object> reading = Map.of(
                "id", "reading-1",
                "timestamp", 1711015200000L,
                "value", 24.1
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "sensor-12")
        .then()
                .statusCode(201)
                .contentType(ContentType.JSON)
                .body("id", equalTo("reading-1"))
                .body("timestamp", equalTo(1711015200000L))
                .body("value", equalTo(24.1f));

        given()
        .when()
                .get("/sensors/{id}/readings", "sensor-12")
        .then()
                .statusCode(200)
                .body("id", hasItem("reading-1"));
    }

    @Test
    public void addReading_updatesParentSensorCurrentValue() {
        createRoom("room-s10", "Control Room", 6);
        createSensor("sensor-13", "TEMPERATURE", "ACTIVE", 18.0, "room-s10");

        Map<String, Object> reading = Map.of(
                "id", "reading-2",
                "timestamp", 1711015300000L,
                "value", 26.4
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "sensor-13")
        .then()
                .statusCode(201);

        given()
                .queryParam("type", "TEMPERATURE")
        .when()
                .get("/sensors")
        .then()
                .statusCode(200)
                .body("[0].id", equalTo("sensor-13"))
                .body("[0].currentValue", equalTo(26.4f));
    }

    @Test
    public void addReading_forMissingSensor_returns404() {
        Map<String, Object> reading = Map.of(
                "id", "reading-3",
                "timestamp", 1711015400000L,
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
    public void addReading_forMaintenanceSensor_returns403_perRubric() {
        createRoom("room-s11", "Service Bay", 5);
        createSensor("sensor-14", "TEMPERATURE", "MAINTENANCE", 18.0, "room-s11");

        Map<String, Object> reading = Map.of(
                "id", "reading-4",
                "timestamp", 1711015500000L,
                "value", 30.0
        );

        given()
                .contentType(ContentType.JSON)
                .body(reading)
        .when()
                .post("/sensors/{id}/readings", "sensor-14")
        .then()
                .statusCode(403)
                .contentType(ContentType.JSON)
                .body("message", notNullValue());
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
