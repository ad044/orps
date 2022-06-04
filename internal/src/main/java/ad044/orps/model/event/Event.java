package ad044.orps.model.event;

import ad044.orps.model.Category;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Event<T extends Enum<T>> {
    private final T id;
    private final Category category;
    private final List<String> recipientUuids;
    private final Map<String, Object> data;

    protected Event(T id, Category category, List<String> recipientUuids) {
        this.id = id;
        this.category = category;
        this.recipientUuids = recipientUuids;
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

    public Category getCategory() {
        return category;
    }

    public List<String> getRecipientUuids() {
        return recipientUuids;
    }
}
