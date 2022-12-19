package horse.wtf.nzyme.rest.authentication;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import horse.wtf.nzyme.NzymeLeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

@PrometheusBasicAuthSecured
@Provider
@Priority(Priorities.AUTHENTICATION)
public class PrometheusBasicAuthFilter implements ContainerRequestFilter  {

    private static final Logger LOG = LogManager.getLogger(PrometheusBasicAuthFilter.class);

    private final NzymeLeader nzyme;

    public PrometheusBasicAuthFilter(NzymeLeader nzyme) {
        this.nzyme = nzyme;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try {
            String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

            // No Authorization header supplied.
            if (Strings.isNullOrEmpty(authorizationHeader) || authorizationHeader.trim().isEmpty()) {
                abortWithUnauthorized(requestContext);
                return;
            }

            HTTPBasicAuthParser.Credentials creds = HTTPBasicAuthParser.parse(authorizationHeader);

            // abort if no creds configured in registry

            if (creds.getUsername().equals("lennart") && creds.getPassword().equals("123123123")) {
                // Set new security context for later use in resources.
                final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
                requestContext.setSecurityContext(new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return () -> creds.getUsername();
                    }

                    @Override
                    public boolean isUserInRole(String role) {
                        return true;
                    }

                    @Override
                    public boolean isSecure() {
                        return currentSecurityContext.isSecure();
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return SecurityContext.BASIC_AUTH;
                    }

                });
            } else {
                LOG.warn("Rejected attempted authentication using invalid Prometheus basic auth.");
                abortWithUnauthorized(requestContext);
            }
        } catch (Exception e) {
            LOG.info("Could not authenticate using Prometheus HTTP basic authentication.", e);
            abortWithUnauthorized(requestContext);
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
    }

}