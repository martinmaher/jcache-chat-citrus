package de.consol.chat.citrus;

import com.consol.citrus.exceptions.CitrusRuntimeException;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Martin Maher
 */
public class StompFrame {
    private String command;
    private Map<String, String> headers = new LinkedHashMap<>();
    private String body;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public String getBody() {
        return body;
    }

    public JSONObject getBodyAsJSONObject() {
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        try {
            return (JSONObject) parser.parse(body);
        } catch (ParseException e) {
            throw new CitrusRuntimeException("Error parsing body: " + body, e);
        }
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean hasBody() {
        return StringUtils.hasText(this.body);
    }

    public static String toWireFormat(StompFrame stompFrame) {
        StringBuilder builder = new StringBuilder();

        builder.append(stompFrame.getCommand());
        builder.append("\n");

        for (Map.Entry<String, String> header : stompFrame.getHeaders().entrySet()) {
            builder.append(header.getKey());
            builder.append(":");
            builder.append(header.getValue());
            builder.append("\n");
        }

        if (stompFrame.hasBody()) {
            builder.append(stompFrame.getBody());
            builder.append("\n");
        }

        builder.append("\n");
        builder.append((char) 0);

        return builder.toString();
    }

    public static StompFrame fromWireFormat(String payload) {
        StompFrame stompFrame = new StompFrame();

        int separateAt = payload.indexOf("\n\n");
        String header = payload.substring(0, separateAt);
        String body = payload.substring(separateAt + 1);

        StringTokenizer stringTokenizer = new StringTokenizer(header, "\n");

        // set command
        stompFrame.setCommand(stringTokenizer.nextToken());

        // add headers
        while (stringTokenizer.hasMoreTokens()) {
            String token = stringTokenizer.nextToken();
            if (StringUtils.hasText(token) && token.contains(Character.toString(':'))) {
                String[] keyValue = token.split(":");
                stompFrame.addHeader(keyValue[0], keyValue[1]);
            }
        }

        // add body
        stompFrame.setBody(stripNullCharacter(body));

        return stompFrame;
    }

    private static String stripNullCharacter(String body) {
        if (StringUtils.hasText(body) && body.endsWith(Character.toString('\u0000'))) {
            return StringUtils.trimWhitespace(body.substring(0, body.length() - 1));
        } else {
            return StringUtils.trimWhitespace(body);
        }
    }

}
