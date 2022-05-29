package ad044.orps.model.message;

import ad044.orps.model.Category;
import ad044.orps.model.event.*;
import ad044.orps.model.game.Player;
import ad044.orps.model.user.OrpsUserDetails;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EventMessage {
    private final List<String> recipientUuids;
    private final Category category;
    private final Event<?> event;


    private EventMessage(List<String> recipientUuids, Category category, Event<?> event) {
        this.recipientUuids = recipientUuids;
        this.category = category;
        this.event = event;
    }

    public List<String> getRecipientUuids() {
        return recipientUuids;
    }

    public Category getCategory() {
        return category;
    }

    public Event<?> getEvent() {
        return event;
    }

    public static EventMessage lobby(String recipient, LobbyEvent event) {
        return new EventMessage(Collections.singletonList(recipient), Category.LOBBY, event);
    }

    public static EventMessage lobby(List<OrpsUserDetails> members, LobbyEvent event) {
        List<String> memberUuids = members.stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList());
        return new EventMessage(memberUuids, Category.LOBBY, event);
    }

    public static EventMessage error(String recipient, ErrorEvent event) {
        return new EventMessage(Collections.singletonList(recipient), Category.ERROR, event);
    }

    public static EventMessage game(String recipient, GameEvent event) {
        return new EventMessage(Collections.singletonList(recipient), Category.GAME, event);
    }

    public static EventMessage game(List<Player> players, GameEvent event) {
        List<String> playerUuids = players.stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList());
        return new EventMessage(playerUuids, Category.GAME, event);
    }

    public static EventMessage general(String recipient, GeneralEvent event) {
        return new EventMessage(Collections.singletonList(recipient), Category.GENERAL, event);
    }

    public static EventMessage general(List<OrpsUserDetails> recipients, GeneralEvent event) {
        List<String> uuids = recipients.stream().map(OrpsUserDetails::getUuid).collect(Collectors.toList());
        return new EventMessage(uuids, Category.GENERAL, event);
    }


}
