package com.smartcampus;

import io.restassured.RestAssured;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertTrue;

public class ApiLoggingFilterTest {

    private static HttpServer server;
    private static Logger logger;
    private static CapturingHandler handler;

    @BeforeClass
    public static void setUpClass() {
        server = MainServer.startServer();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
        RestAssured.basePath = "/api/v1";

        logger = Logger.getLogger(ApiLoggingFilter.LOGGER_NAME);
        handler = new CapturingHandler();
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);
    }

    @AfterClass
    public static void tearDownClass() {
        if (logger != null && handler != null) {
            logger.removeHandler(handler);
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Before
    public void resetState() {
        RoomResource.roomDB.clear();
        SensorResource.sensorDB.clear();
        handler.clear();
    }

    @Test
    public void requestFilter_logsMethodAndUri() {
        given()
        .when()
                .get("/rooms")
        .then()
                .statusCode(200);

        List<String> messages = handler.messages();
        assertTrue(messages.stream().anyMatch(message ->
                message.contains("Request method=GET") &&
                message.contains("/api/v1/rooms")));
    }

    @Test
    public void responseFilter_logsFinalStatusCodeForSuccess() {
        given()
        .when()
                .get("/rooms")
        .then()
                .statusCode(200);

        List<String> messages = handler.messages();
        assertTrue(messages.stream().anyMatch(message ->
                message.contains("Response method=GET") &&
                message.contains("/api/v1/rooms") &&
                message.contains("status=200")));
    }

    @Test
    public void responseFilter_logsFinalStatusCodeForConflict() {
        Room room = new Room("room-log-1", "Server Room", 4);
        room.getSensorIds().add("sensor-log-1");
        RoomResource.roomDB.put(room.getId(), room);

        given()
        .when()
                .delete("/rooms/{id}", room.getId())
        .then()
                .statusCode(409);

        List<String> messages = handler.messages();
        assertTrue(messages.stream().anyMatch(message ->
                message.contains("Request method=DELETE") &&
                message.contains("/api/v1/rooms/" + room.getId())));
        assertTrue(messages.stream().anyMatch(message ->
                message.contains("Response method=DELETE") &&
                message.contains("/api/v1/rooms/" + room.getId()) &&
                message.contains("status=409")));
    }

    private static class CapturingHandler extends Handler {
        private final List<String> messages = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record != null) {
                messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }

        void clear() {
            messages.clear();
        }

        List<String> messages() {
            return new ArrayList<>(messages);
        }
    }
}
