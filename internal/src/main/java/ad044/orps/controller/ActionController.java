package ad044.orps.controller;

import ad044.orps.model.action.Action;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.ActionHandlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ActionController {
    @Autowired
    ActionHandlerService actionHandlerService;

    @MessageMapping("/user-action")
    public void handleAction(Action action, Principal principal) {
        OrpsUserDetails user = (OrpsUserDetails) ((Authentication) principal).getPrincipal();

        action.setAuthor(user);

        actionHandlerService.putAction(action);
    }
}
