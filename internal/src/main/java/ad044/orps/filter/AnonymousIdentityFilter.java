package ad044.orps.filter;

import ad044.orps.model.user.OrpsAuthenticationToken;
import ad044.orps.model.user.OrpsUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

@Component
public class AnonymousIdentityFilter extends AnonymousAuthenticationFilter {
    Logger logger = LoggerFactory.getLogger(AnonymousIdentityFilter.class);

    private static final String ANONYMOUS_KEY = "ANON_CRPS_KEY";

    public AnonymousIdentityFilter() {
        super(ANONYMOUS_KEY);
    }

    @Override
    protected Authentication createAuthentication(HttpServletRequest request) {
        OrpsUserDetails anonOrpsUserDetails =
                new OrpsUserDetails("anonuser", UUID.randomUUID().toString());
        logger.info(String.format("Authenticated new anonymous user with UUID: %s", anonOrpsUserDetails.getUuid()));

        return new OrpsAuthenticationToken(anonOrpsUserDetails);
    }
}
