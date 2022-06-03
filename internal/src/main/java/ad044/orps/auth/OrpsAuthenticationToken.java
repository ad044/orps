package ad044.orps.auth;

import ad044.orps.model.user.OrpsUserDetails;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.simp.user.DestinationUserNameProvider;
import org.springframework.security.authentication.AbstractAuthenticationToken;

public class OrpsAuthenticationToken extends AbstractAuthenticationToken implements DestinationUserNameProvider {
    private final OrpsUserDetails orpsUserDetails;

    public OrpsAuthenticationToken(final OrpsUserDetails orpsUserDetails) {
        super(orpsUserDetails.getAuthorities());
        super.setAuthenticated(true);
        setDetails(orpsUserDetails);
        this.orpsUserDetails = orpsUserDetails;
    }

    @Override
    public Object getCredentials() {
        return orpsUserDetails.getPassword();
    }

    @Override
    public Object getPrincipal() {
        return orpsUserDetails;
    }

    @Override
    public final void setAuthenticated(boolean authenticated) {
        throw new IllegalArgumentException("Cannot change the authenticated state.");
    }

    @Override
    public String toString() {
        return orpsUserDetails.getUsername();
    }

    @Override
    public @NotNull String getDestinationUserName() {
        return orpsUserDetails.getUuid();
    }
}
