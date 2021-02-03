package com.example.steps.public_api;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Integration tests for the public API")
public class UnitTest {
  private RequestSpecification requestSpecification;

  @BeforeAll
  void prepareSpec(Vertx vertx, VertxTestContext testContext) {
    requestSpecification = new RequestSpecBuilder()
      .addFilters(asList(new ResponseLoggingFilter(), new RequestLoggingFilter()))
      .setBaseUri("http://localhost:8080/")
      .setBasePath("/api/v1")
      .build();

    Server spy = spy(new Server());

    // Change visibility to some methods to avoid using powermock
    doAnswer(new Answer<Void>() {
      public Void answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        RoutingContext ctx = (RoutingContext) args[0];
        // ok
        ctx.response().setStatusCode(200).end();
        return null;
      }
    }).when(spy).register(any());

    doAnswer(new Answer<Void>() {
      public Void answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        RoutingContext ctx = (RoutingContext) args[0];
        // ok
        ctx.response().putHeader("Content-Type", "application/jwt").end("token");
        return null;
      }
    }).when(spy).token(any());

    vertx.rxDeployVerticle(spy)
      .ignoreElement()
      .subscribe(testContext::completeNow, testContext::failNow);
  }

  private final HashMap<String, JsonObject> registrations = new HashMap<String, JsonObject>() {
    {
      put("Foo", new JsonObject()
        .put("username", "Foo")
        .put("password", "foo-123")
        .put("email", "foo@email.me")
        .put("city", "Lyon")
        .put("deviceId", "a1b2c3")
        .put("makePublic", true));

      put("Bar", new JsonObject()
        .put("username", "Bar")
        .put("password", "bar-#$69")
        .put("email", "bar@email.me")
        .put("city", "Tassin-La-Demi-Lune")
        .put("deviceId", "def1234")
        .put("makePublic", false));
    }
  };

  // TODO
  @Test
  @Order(1)
  @DisplayName("Register some users")
  void registerUsers() {
    registrations.forEach((key, registration) -> {
      given(requestSpecification)
        .contentType(ContentType.JSON)
        .body(registration.encode())
        .post("/register")
        .then()
        .assertThat()
        .statusCode(200);
    });
  }

  private final HashMap<String, String> tokens = new HashMap<>();

  @Test
  @Order(2)
  @DisplayName("Get JWT tokens to access the API")
  void obtainToken() {
    registrations.forEach((key, registration) -> {

      JsonObject login = new JsonObject()
        .put("username", key)
        .put("password", registration.getString("password"));

      String token = given(requestSpecification)
        .contentType(ContentType.JSON)
        .body(login.encode())
        .post("/token")
        .then()
        .assertThat()
        .statusCode(200)
        .contentType("application/jwt")
        .extract()
        .asString();

      assertThat(token)
        .isNotNull()
        .isNotBlank();

      tokens.put(key, token);
    });
  }
}
