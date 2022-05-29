package ad044.orps.config;

import ad044.orps.filter.AnonymousIdentityFilter;
import ad044.orps.filter.FilterChainExceptionHandleFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    FilterChainExceptionHandleFilter filterChainExceptionHandler;

    @Autowired
    AnonymousIdentityFilter anonymousIdentityFilter;

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
        http
                .csrf().csrfTokenRepository(csrfTokenRepository())
                .and()
                .anonymous().authenticationFilter(anonymousIdentityFilter)
                .and()
                .exceptionHandling().accessDeniedHandler(accessDeniedHandler())
                .and()
                .addFilterAfter(filterChainExceptionHandler, SecurityContextPersistenceFilter.class)
                .authorizeRequests()
                .antMatchers("/**").authenticated();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> { throw ex; };
    }
}
