package de.consol.chat.citrus;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestDesigner;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.validation.callback.AbstractValidationCallback;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author Martin Maher
 */
@Test
public class Test_03_StompIT extends TestNGCitrusTestDesigner {

    @CitrusTest
    public void testJoiningAndLeaving() {
        variable("username", "citrus:randomString(10, UPPERCASE)");

        echo("Connecting to server with STOMP");
        send("chatWebSocketClient")
                .payload(StompFrame.toWireFormat(new StompFrameBuilder()
                        .withCommand("CONNECT")
                        .withHeader("accept-version", "1.1")
                        .withHeader("heart-beat", "0,0")
                        .build()));

        echo("Verify connection is successful");
        receive("chatWebSocketClient")
                .messageType(MessageType.PLAINTEXT)
                .payload(StompFrame.toWireFormat(new StompFrameBuilder()
                        .withCommand("CONNECTED")
                        .withHeader("version", "1.1")
                        .withHeader("heart-beat", "0,0")
                        .build()));

        echo("Subscribe to destination: /topic/users/joined");
        send("chatWebSocketClient")
                .payload(StompFrame.toWireFormat(new StompFrameBuilder()
                        .withCommand("SUBSCRIBE")
                        .withHeader("id", "sub-0")
                        .withHeader("destination", "/topic/users/joined")
                        .build()));

        echo("Joining with user: ${username}");
        send("chatRestClient")
                .payload("")
                .http()
                .method(HttpMethod.POST)
                .path("/users/${username}");
        receive("chatRestClient")
                .messageType(MessageType.JSON)
                .http()
                .status(HttpStatus.OK);

        echo("Verify user-joined notification is received");
        receive("chatWebSocketClient")
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
    }
}
