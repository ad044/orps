package ad044.orps.filter;

import ad044.orps.model.event.ErrorEvent;
import ad044.orps.model.user.OrpsUserDetails;
import ad044.orps.service.UserMessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class FilterChainExceptionHandleFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(FilterChainExceptionHandleFilter.class);

    @Autowired
    UserMessagingService userMessagingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Caught Filter Chain Exception:", e);
            OrpsUserDetails userDetails = (OrpsUserDetails) ((Authentication)(request.getUserPrincipal())).getPrincipal();

            ErrorEvent errorEvent = ErrorEvent.somethingWentWrong(userDetails.getUuid());
            userMessagingService.sendEvent(errorEvent);
        }
    }
}