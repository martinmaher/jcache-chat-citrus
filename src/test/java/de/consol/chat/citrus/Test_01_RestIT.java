package de.consol.chat.citrus;

import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.dsl.testng.TestNGCitrusTestDesigner;
import com.consol.citrus.message.MessageType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * @author Martin Maher
 */
@Test
public class Test_01_RestIT extends TestNGCitrusTestDesigner {
    @CitrusTest
    public void testJoiningAndLeaving() {
        variable("username", "citrus:randomString(10, UPPERCASE)");

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

        echo("Leaving chat: ${username}");
        send("chatRestClient")
                .payload("")
                .http()
                .method(HttpMethod.DELETE)
                .path("/users/${username}");
        receive("chatRestClient")
                .messageType(MessageType.JSON)
                .http()
                .status(HttpStatus.OK);


    }

}
