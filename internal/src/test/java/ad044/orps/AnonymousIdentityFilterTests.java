package ad044.orps;

import ad044.orps.filter.AnonymousIdentityFilter;
import ad044.orps.service.OrpsAuthenticationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AnonymousIdentityFilterTests {
    @Autowired
    OrpsAuthenticationProvider authProvider;

    @Test
    public void testDoFilter() throws IOException, ServletException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        AnonymousIdentityFilter anonIdentityFilter = new AnonymousIdentityFilter(new ProviderManager(List.of(authProvider)));
        anonIdentityFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(SecurityContextHolder.getContext().getAuthentication().getName(), "anonuser");
    }
}
