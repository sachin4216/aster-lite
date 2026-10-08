package com.asterlite.appointment_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// WHAT IS A FILTER?
// A servlet filter sits in front of the DispatcherServlet. Every HTTP request passes
// through the filter chain BEFORE it reaches any controller, and the response passes
// back through it on the way out:
//
//   Postman -> Tomcat -> [filter 1] -> [filter 2] -> DispatcherServlet -> Controller
//
// That makes a filter the right place for something that must happen on EVERY response
// (controllers, /actuator/health, 404s, error responses) without touching each controller.

// WHY OncePerRequestFilter AND NOT PLAIN jakarta.servlet.Filter?
// One HTTP request can travel through the filter chain more than once inside the server,
// for example when it is forwarded internally to /error after an exception.
// OncePerRequestFilter is Spring's base class that guarantees doFilterInternal runs
// exactly once per request. It also hands you HttpServletRequest/HttpServletResponse
// directly, so you do not have to cast from ServletRequest/ServletResponse.

// WHY @Component?
// It makes this class a Spring bean. Spring Boot automatically registers every bean
// of type Filter with the embedded Tomcat for all URLs ("/*").
// No extra configuration class or FilterRegistrationBean is needed.
@Component
public class InstancePortFilter extends OncePerRequestFilter {

    // The header name from APT-1 criterion 5, kept in one constant so there is no typo risk.
    private static final String HEADER = "X-Instance-Port";

    // final + constructor injection: the value is set once at startup and never changes.
    // One filter instance serves all requests on many threads, so immutable state is safe.
    private final String port;

    // @Value reads a property from the Spring Environment. server.port is not in this
    // module's application.yaml; it arrives from Config Server (appointment-service.yml -> 8082).
    // If you start a second instance with a --server.port program argument, command-line
    // arguments override Config Server, so that instance injects its own port here.
    // ":8080" is a fallback used only if server.port is not defined anywhere. 8080 is
    // Spring Boot's own default port, so the header stays truthful. Without the fallback
    // the application fails to start with "Could not resolve placeholder 'server.port'",
    // which would hit you in tests that run without Config Server
    public InstancePortFilter(@Value("${server.port:8080}") String port){
        this.port = port;
    }

    // This is the one method you must implement. Spring calls it once for each request.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // Add the header BEFORE passing the request on. Once the controller writes the
        // body, the response is "committed" (the headers are already sent to the client)
        // and a header set after that point is silently ignored.
        // setHeader replaces any existing value; addHeader would append a second one.
        response.setHeader(HEADER, port);

        // Pass the request to the next filter and finally to the controller.
        // If you forget this line the request stops here: the controller never runs
        // and the client gets an empty 200 response
        filterChain.doFilter(request, response);
    }
}
