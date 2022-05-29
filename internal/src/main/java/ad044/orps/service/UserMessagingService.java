package ad044.orps.service;

import ad044.orps.model.message.EventMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserMessagingService {
    @Autowired
    SimpMessagingTemplate messagingTemplate;

    public void sendEventMessage(EventMessage message) {
        String topic = message.getCategory().getTextValue().toLowerCase();
        message.getRecipientUuids().forEach(recipient -> {
            sendSocketMessageToUser(recipient, topic, message.getEvent());
        });
    }

    public void sendEventMessage(List<EventMessage> messages) {
        messages.forEach(this::sendEventMessage);
    }

    private void sendSocketMessageToUser(String uuid, String topic, Object message) {
        messagingTemplate.convertAndSendToUser(uuid, String.format("/queue/reply/%s", topic), message);
    }
}
