package de.consol.chat.citrus;

import com.consol.citrus.annotations.CitrusEndpoint;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestDesigner;
import com.consol.citrus.endpoint.Endpoint;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.validation.callback.AbstractValidationCallback;
import com.consol.citrus.websocket.endpoint.WebSocketEndpoint;
import net.minidev.json.JSONObject;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author Martin Maher
 */
@Test
public class Test_04_CompleteIT extends TestNGCitrusTestDesigner {

    @CitrusEndpoint(name = "chatRestClient")
    Endpoint chatRestClient;

    @CitrusEndpoint(name = "chatWebSocketClient")
    WebSocketEndpoint chatWebSocketClient;

    @CitrusTest
    public void testComplete() {
        variable("username", "citrus:randomString(10, UPPERCASE)");
        variable("room", "citrus:randomString(10, UPPERCASE)");

        echo("Connecting to server with STOMP");
        send(chatWebSocketClient)
                .payload(getPayloadConnect());

        echo("Verify connection is successful");
        receive(chatWebSocketClient)
                .messageType(MessageType.PLAINTEXT)
                .payload(getPayloadConnected());

        echo("Subscribe to destination: /topic/users/joined");
        send(chatWebSocketClient)
                .payload(getPayloadSubscribe("sub-0", "/topic/users/joined"));

        echo("Subscribe to destination: /topic/users/left");
        send(chatWebSocketClient)
                .payload(getPayloadSubscribe("sub-1", "/topic/users/left"));

        echo("Subscribe to destination: /topic/rooms/created");
        send(chatWebSocketClient)
                .payload(getPayloadSubscribe("sub-2", "/topic/rooms/created"));

        echo("Subscribe to destination: /topic/rooms/removed");
        send(chatWebSocketClient)
                .payload(getPayloadSubscribe("sub-3", "/topic/rooms/removed"));

        echo("Subscribe to destination: /topic/messages");
        send(chatWebSocketClient)
                .payload(getPayloadSubscribe("sub-4", "/topic/messages"));

        echo("Joining with user: ${username}");
        send(chatRestClient)
                .payload("")
                .http()
                .method(HttpMethod.POST)
                .path("/users/${username}");
        receive("chatRestClient")
                .messageType(MessageType.JSON)
                .http()
                .status(HttpStatus.OK);

        echo("Verify user-joined notification is received");
        receive(chatWebSocketClient)
                .messageType(MessageType.PLAINTEXT)
                .validationCallback(new AbstractValidationCallback<String>() {
                    @Override
                    public void validate(String payload, Map<String, Object> headers, TestContext context) {
                        StompFrame stompFrame = StompFrame.fromWireFormat(payload);
                        Assert.assertEquals(stompFrame.getCommand(), "MESSAGE");
                        Assert.assertEquals(stompFrame.getHeader("destination"), "/topic/users/joined");
                        Assert.assertEquals(stompFrame.getHeader("subscription"), "sub-0");
                        String expectedUsername = String.format("\"%s\"", context.getVariable("username"));
                        Assert.assertEquals(stompFrame.getBody(), expectedUsername);
                    }
                });

        echo("Getting list of logged in users");
        send(chatRestClient)
                .payload("")
                .http()
                .method(HttpMethod.GET)
                .path("/users");
        receive(chatRestClient)
                .messageType(MessageType.JSON)
                .validationCallback(new AbstractValidationCallback<String>() {
                    @Override
                    public void validate(String payload, Map headers, TestContext context) {
                        Assert.assertTrue(payload.indexOf(context.getVariable("username")) > -1);
                    }
                })
                .http()
                .status(HttpStatus.OK);

        echo("Creating a new room: ${room}");
        send(chatRestClient)
                .messageType(MessageType.JSON)
                .payload("{\"name\":\"${room}\",\"user\":\"${username}\"}")
                .http()
                .method(HttpMethod.POST)
                .path("/room");
        receive(chatRestClient)
                .messageType(MessageType.JSON)
                .http()
                .status(HttpStatus.OK);

        echo("Verifying notification received for room: ${room}");
        receive(chatWebSocketClient)
                .validationCallback(new AbstractValidationCallback<String>() {
                    @Override
                    public void validate(String payload, Map<String, Object> headers, TestContext context) {
                        StompFrame stompFrame = StompFrame.fromWireFormat(payload);
                        Assert.assertEquals(stompFrame.getCommand(), "MESSAGE");
                        Assert.assertEquals(stompFrame.getHeaders().get("destination"), "/topic/rooms/created");
                        Assert.assertEquals(stompFrame.getHeaders().get("subscription"), "sub-2");

                        JSONObject receivedJson = stompFrame.getBodyAsJSONObject();
                        Assert.assertEquals(receivedJson.get("user"), context.getVariable("username"));
                        Assert.assertEquals(receivedJson.get("name"), context.getVariable("room"));

                        // save the room Id so we can use it later on
                        context.setVariable("roomId", receivedJson.get("id"));
                    }
                });

        echo("Creating a message in room: ${room}");
        send(chatRestClient)
                .payload("{" +
                        "\"text\":\"Sample message for room\"," +
                        "\"roomId\":${roomId}," +
                        "\"user\":\"${username}\"" +
                        "}")
                .http()
                .method(HttpMethod.POST)
                .path("/room/${roomId}/message");
        receive(chatRestClient)
                .messageType(MessageType.JSON)
                .http()
                .status(HttpStatus.OK);

        echo("Verifying notification received for message");
        receive(chatWebSocketClient)
                .validationCallback(new AbstractValidationCallback<String>() {
                    @Override
                    public void validate(String payload, Map<String, Object> headers, TestContext context) {
                        StompFrame stompFrame = StompFrame.fromWireFormat(payload);
                        Assert.assertEquals(stompFrame.getCommand(), "MESSAGE");
                        Assert.assertEquals(stompFrame.getHeaders().get("destination"), "/topic/messages");
                        Assert.assertEquals(stompFrame.getHeaders().get("subscription"), "sub-4");

                        JSONObject receivedJson = stompFrame.getBodyAsJSONObject();
                        Assert.assertEquals(receivedJson.get("text"), "Sample message for room");
                        Assert.assertEquals(receivedJson.get("roomId").toString(), context.getVariable("roomId"));
                        Assert.assertEquals(receivedJson.get("user"), context.getVariable("username"));
                    }
                });

        echo("Deleting the room: ${room}");
        send(chatRestClient)
                .payload("")
                .http()
                .method(HttpMethod.DELETE)
                .path("/room/${roomId}");
        receive(chatRestClient)
                .messageType(MessageType.JSON)
                .http()
                .status(HttpStatus.OK);

        echo("Verifying notification received for deleted room");
        receive(chatWebSocketClient)
                .validationCallback(new AbstractValidationCallback<String>() {
                    @Override
                    public void validate(String payload, Map<String, Object> headers, TestContext context) {
                        StompFrame stompFrame = StompFrame.fromWireFormat(payload);
                        Assert.assertEquals(stompFrame.getCommand(), "MESSAGE");
                        Assert.assertEquals(stompFrame.getHeaders().get("destination"), "/topic/rooms/removed");
                        Assert.assertEquals(stompFrame.getHeaders().get("subscription"), "sub-3");

                        JSONObject receivedJson = stompFrame.getBodyAsJSONObject();
                        Assert.assertEquals(receivedJson.get("id").toString(), context.getVariable("roomId"));
                        Assert.assertEquals(receivedJson.get("name"), context.getVariable("room"));
                    }
                });

        echo("Leaving chat: ${username}");
        send(chatRestClient)
                .payload("")
                .http()
                .method(HttpMethod.DELETE)
                .path("/users/${username}");
        receive(chatRestClient)
                .messageType(MessageType.JSON)
                .http()
                .status(HttpStatus.OK);

        echo("Verify user-left notification is received");
        receive("chatWebSocketClient")
                .messageType(MessageType.PLAINTEXT)
                .validationCallback(new AbstractValidationCallback<String>() {
                    @Override
                    public void validate(String payload, Map<String, Object> headers, TestContext context) {
                        StompFrame stompFrame = StompFrame.fromWireFormat(payload);
                        Assert.assertEquals(stompFrame.getCommand(), "MESSAGE");
                        Assert.assertEquals(stompFrame.getHeader("destination"), "/topic/users/left");
                        Assert.assertEquals(stompFrame.getHeader("subscription"), "sub-1");
                        String expectedUsername = String.format("\"%s\"", context.getVariable("username"));
                        Assert.assertEquals(stompFrame.getBody(), expectedUsername);
                    }
                });
    }


    private String getPayloadConnect() {
        return StompFrame.toWireFormat(new StompFrameBuilder()
                .withCommand("CONNECT")
                .withHeader("accept-version", "1.1")
                .withHeader("heart-beat", "0,0")
                .build());
    }

    private String getPayloadConnected() {
        return StompFrame.toWireFormat(new StompFrameBuilder()
                .withCommand("CONNECTED")
                .withHeader("version", "1.1")
                .withHeader("heart-beat", "0,0")
                .build());
    }

    private String getPayloadSubscribe(String id, String destination) {
        return StompFrame.toWireFormat(new StompFrameBuilder()
                .withCommand("SUBSCRIBE")
                .withHeader("id", id)
                .withHeader("destination", destination)
                .build());
    }
}
