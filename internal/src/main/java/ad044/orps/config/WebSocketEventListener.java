package ad044.orps.config;

import ad044.orps.model.action.Action;
import ad044.orps.model.Category;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashMap;

@Component
public class WebSocketEventListener {
    Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    @Autowired
    ActionHandlerService actionHandlerService;

    @EventListener
    public void handleSessionConnect(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal principal = headers.getUser();

        if (principal != null) {
            OrpsUserDetails user = (OrpsUserDetails) ((Authentication) principal).getPrincipal();
            logger.info(String.format("User %s connected.", user.getUuid()));
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal principal = headers.getUser();

        if (principal != null) {
            OrpsUserDetails userThatLeft = (OrpsUserDetails) ((Authentication) principal).getPrincipal();

            logger.info(String.format("User %s disconnected.", userThatLeft.getUuid()));

            Action disconnectAction = new Action("USER_DISCONNECT",
                    Category.GENERAL,
                    new HashMap<>(),
                    userThatLeft);

            actionHandlerService.putAction(disconnectAction);
        }
    }
}
