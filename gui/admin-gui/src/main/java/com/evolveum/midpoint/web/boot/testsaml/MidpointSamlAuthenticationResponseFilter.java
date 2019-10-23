package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import com.evolveum.midpoint.model.api.authentication.UserProfileService;
import com.evolveum.midpoint.util.exception.*;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml.provider.provisioning.SamlProviderProvisioning;
import org.springframework.security.saml.provider.service.ServiceProviderService;
import org.springframework.security.saml.provider.service.authentication.SamlAuthenticationResponseFilter;
import org.springframework.security.saml.saml2.attribute.Attribute;
import org.springframework.security.saml.saml2.authentication.Response;
import org.springframework.security.saml.saml2.metadata.IdentityProviderMetadata;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;
import org.springframework.security.saml.validation.ValidationResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.util.StringUtils.hasText;

public class MidpointSamlAuthenticationResponseFilter extends SamlAuthenticationResponseFilter {

    private UserProfileService userProfileService;

    public MidpointSamlAuthenticationResponseFilter(SamlProviderProvisioning<ServiceProviderService> provisioning) {
        super(provisioning);
    }

    public void setUserProfileService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        DefaultSamlAuthentication authentication = (DefaultSamlAuthentication) super.attemptAuthentication(request, response);

        MidPointUserProfilePrincipal midPointUserProfilePrincipal = null;

        if (authentication.getAssertion() != null
        && authentication.getAssertion().getAttributes() != null
        && !authentication.getAssertion().getAttributes().isEmpty()) {
            List<Attribute> attributes = ((DefaultSamlAuthentication) authentication).getAssertion().getAttributes();
            for (Attribute attribute : attributes){
                if (attribute.getFriendlyName().equals("uid")) {
                    try {
                        midPointUserProfilePrincipal = userProfileService.getPrincipal((String)attribute.getValues().get(0));
                    } catch (ObjectNotFoundException e) {
                        e.printStackTrace();
                    } catch (SchemaException e) {
                        e.printStackTrace();
                    } catch (CommunicationException e) {
                        e.printStackTrace();
                    } catch (ConfigurationException e) {
                        e.printStackTrace();
                    } catch (SecurityViolationException e) {
                        e.printStackTrace();
                    } catch (ExpressionEvaluationException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

        MidpointSamlAuthentication mpAuthentication = new MidpointSamlAuthentication(
                true,
                authentication.getAssertion(),
                authentication.getAssertingEntityId(),
                authentication.getHoldingEntityId(),
                authentication.getRelayState(),
                midPointUserProfilePrincipal
        );
        mpAuthentication.setResponseXml(authentication.getResponseXml());

        return getAuthenticationManager().authenticate(mpAuthentication);
    }

}
