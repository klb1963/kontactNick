package kontactNick.security.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;

@Component
public class SecurityLoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(SecurityLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        logger.debug("🔍 Incoming request: {} {}", req.getMethod(), req.getRequestURI());
        chain.doFilter(request, response);
    }
}
