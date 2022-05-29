package ad044.orps.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum Category {
    LOBBY("LOBBY"), GAME("GAME"), GENERAL("GENERAL"), ERROR("ERROR");

    private final String textValue;

    Category(String textValue) {
        this.textValue = textValue;
    }

    @JsonValue
    public String getTextValue() {
        return textValue;
    }

    @JsonCreator
    public static Category forValue(String value) {
        return Arrays.stream(Category.values())
                .filter(op -> op.getTextValue().equals(value))
                .findFirst()
                .orElseThrow();
    }
}
