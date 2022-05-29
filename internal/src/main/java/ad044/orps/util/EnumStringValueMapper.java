package ad044.orps.util;

import java.util.Arrays;
import java.util.Optional;

public class EnumStringValueMapper {
    public static <T extends Enum<T>> Optional<T> stringValueToEnum(String value, Class<T> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .filter(e -> e.name().equals(value))
                .findFirst();
    }
}
