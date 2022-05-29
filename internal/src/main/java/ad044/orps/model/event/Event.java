package ad044.orps.model.event;

import java.util.HashMap;
import java.util.Map;

public abstract class Event<T extends Enum<T>> {
    private final T id;
    private final Map<String, Object> data;

    protected Event(T id) {
        this.id = id;
        this.data = new HashMap<>();
    }

    public void putData(String k, Object v) {
        data.put(k, v);
    }

    public Map<String, Object> getData() {
        return data;
    }

    public T getId() {
        return id;
    }
}
