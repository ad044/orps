package ad044.orps.service;

import ad044.orps.model.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserMessagingService {
    @Autowired
    SimpMessagingTemplate messagingTemplate;

    public void sendEvent(Event<?> event) {
        String topic = event.getCategory().getTextValue().toLowerCase();
        event.getRecipientUuids().forEach(recipient -> {
            sendSocketMessageToUser(recipient, topic, event);
        });
    }

    public void sendEvent(List<Event<?>> events) {
        events.forEach(this::sendEvent);
    }

    private void sendSocketMessageToUser(String uuid, String topic, Object message) {
        messagingTemplate.convertAndSendToUser(uuid, String.format("/queue/reply/%s", topic), message);
    }
}
