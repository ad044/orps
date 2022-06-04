package ad044.orps.filter;

import ad044.orps.auth.OrpsAuthenticationToken;
import ad044.orps.model.user.OrpsUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

public class AnonymousIdentityFilter extends AbstractAuthenticationProcessingFilter {
    Logger logger = LoggerFactory.getLogger(AnonymousIdentityFilter.class);

    public AnonymousIdentityFilter(AuthenticationManager authenticationManager) {
        super("**", authenticationManager);
    }

    @Override
    protected boolean requiresAuthentication(HttpServletRequest request, HttpServletResponse response) {
        return SecurityContextHolder.getContext().getAuthentication() == null &&
                super.requiresAuthentication(request, response);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        OrpsUserDetails userDetails = new OrpsUserDetails("anonuser", UUID.randomUUID().toString());
        OrpsAuthenticationToken token = new OrpsAuthenticationToken(userDetails);

        logger.info(String.format("Authenticated new anonymous user with UUID: %s", userDetails.getUuid()));

        return this.getAuthenticationManager().authenticate(token);
    }
}
