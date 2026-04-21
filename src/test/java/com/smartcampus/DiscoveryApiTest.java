package com.smartcampus;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class DiscoveryApiTest {

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

    @Test
    public void discoveryEndpoint_returns200AndJson() {
        given()
        .when()
                .get()
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    public void discoveryEndpoint_returnsExpectedApiMetadata() {
        given()
        .when()
                .get()
        .then()
                .statusCode(200)
                .body("api", equalTo("Smart Campus API"))
                .body("version", equalTo("v1"))
                .body("contact", equalTo("admin@university.com"));
    }

    @Test
    public void discoveryEndpoint_returnsRoomsAndSensorsResourceMap() {
        given()
        .when()
                .get()
        .then()
                .statusCode(200)
                .body("resources.rooms", equalTo("/api/v1/rooms"))
                .body("resources.sensors", equalTo("/api/v1/sensors"));
    }

    @Test
    public void discoveryEndpoint_returnsNonEmptyMetadataObject() {
        given()
        .when()
                .get()
        .then()
                .statusCode(200)
                .body("api", notNullValue())
                .body("version", notNullValue())
                .body("contact", notNullValue())
                .body("resources", notNullValue());
    }

    @Test
    public void unsupportedPathUnderApiVersion_returns404() {
        given()
        .when()
                .get("/missing")
        .then()
                .statusCode(404);
    }
}
