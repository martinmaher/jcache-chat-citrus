package de.consol.chat.citrus;

/**
 * @author Martin Maher
 */
public class StompFrameBuilder {
    StompFrame stompFrame = new StompFrame();

    public StompFrameBuilder withCommand(String command) {
        stompFrame.setCommand(command);
        return this;
    }

    public StompFrameBuilder withHeader(String key, String value) {
        stompFrame.addHeader(key, value);
        return this;
    }

    public StompFrameBuilder withBody(String body) {
        stompFrame.setBody(body);
        return this;
    }

    public StompFrame build() {
        return stompFrame;
    }
}
