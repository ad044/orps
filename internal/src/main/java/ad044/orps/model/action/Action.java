package ad044.orps.model.action;

import ad044.orps.model.Category;
import ad044.orps.model.user.OrpsUserDetails;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

public class Action {
    private final String idString;
    private final Category category;
    private final Map<String, String> data;
    private OrpsUserDetails author;

    public Action(String idString, Category category, Map<String, String> data, OrpsUserDetails author) {
        this.idString = idString;
        this.category = category;
        this.data = data;
        this.author = author;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public Action(@JsonProperty("idString") String idString, @JsonProperty("category") Category category,
                  @JsonProperty("data") Map<String, String> data) {
        this.idString = idString;
        this.category = category;
        this.data = data;
        this.author = null;
    }

    public String getIdString() {
        return idString;
    }

    public Map<String, String> getData() {
        return data;
    }

    public Category getCategory() {
        return category;
    }

    public OrpsUserDetails getAuthor() {
        return author;
    }

    public Optional<String> getDataByKey(String key) {
        return Optional.ofNullable(data.get(key));
    }

    public void setAuthor(OrpsUserDetails author) {
        this.author = author;
    }
}
