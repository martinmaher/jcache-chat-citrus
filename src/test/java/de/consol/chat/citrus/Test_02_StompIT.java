package de.consol.chat.citrus;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestDesigner;
import com.consol.citrus.message.MessageType;
import com.consol.citrus.validation.callback.AbstractValidationCallback;
import com.consol.citrus.validation.callback.ValidationCallback;
import junit.framework.Assert;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * @author Martin Maher
 */
@Test
public class Test_02_StompIT extends TestNGCitrusTestDesigner {

    @CitrusTest
    public void testJoiningAndLeaving() {
        variable("username", "citrus:randomString(10, UPPERCASE)");

        echo("Connecting to server with STOMP");
        send("chatWebSocketClient")
                .payload("CONNECT\n" +
                        "accept-version:1.1\n" +
                        "heart-beat:0,0\n" +
                        "\n" + (char) 0
                );

        echo("Verify connection is successful");
        receive("chatWebSocketClient")
                .messageType(MessageType.PLAINTEXT)
                .payload("CONNECTED\n" +
                        "version:1.1\n" +
                        "heart-beat:0,0\n" +
                        "\n" + (char) 0
                );

        echo("Subscribe to destination: /topic/users/joined");
        send("chatWebSocketClient")
                .payload("SUBSCRIBE\n" +
                        "id:sub-0\n" +
                        "destination:/topic/users/joined\n" +
                        "\n" + (char) 0
                );

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
                        Assert.assertTrue(payload.contains("MESSAGE\n"));
                        Assert.assertTrue(payload.contains("destination:/topic/users/joined\n"));
                        Assert.assertTrue(payload.contains("subscription:sub-0\n"));
                        Assert.assertTrue(payload.contains(context.getVariable("username")));
                    }
                });
    }
}
