package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.model.api.authentication.MidPointUserProfilePrincipal;
import org.springframework.security.saml.saml2.authentication.Assertion;
import org.springframework.security.saml.spi.DefaultSamlAuthentication;

public class MidpointSamlAuthentication extends DefaultSamlAuthentication {

    private MidPointUserProfilePrincipal midPointUserProfilePrincipal;

    public MidpointSamlAuthentication(boolean authenticated, Assertion assertion, String assertingEntityId, String holdingEntityId, String relayState, MidPointUserProfilePrincipal midPointUserProfilePrincipal) {
        super(authenticated, assertion, assertingEntityId, holdingEntityId, relayState);
        this.midPointUserProfilePrincipal = midPointUserProfilePrincipal;
    }

    @Override
    public Object getPrincipal() {
        return midPointUserProfilePrincipal;
    }
}
