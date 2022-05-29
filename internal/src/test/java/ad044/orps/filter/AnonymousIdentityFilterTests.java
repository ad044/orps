package ad044.orps.filter;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

public class AnonymousIdentityFilterTests {
    @Test
    public void testDoFilter() throws IOException, ServletException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        AnonymousIdentityFilter anonIdentityFilter = new AnonymousIdentityFilter();
        anonIdentityFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(SecurityContextHolder.getContext().getAuthentication().getName(), "anonuser");
    }
}
