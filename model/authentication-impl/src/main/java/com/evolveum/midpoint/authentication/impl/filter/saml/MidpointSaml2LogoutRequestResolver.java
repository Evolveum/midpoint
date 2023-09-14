/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter.saml;

import jakarta.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;

import com.evolveum.midpoint.authentication.impl.module.authentication.Saml2ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.provider.Saml2Provider;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.logout.Saml2LogoutRequest;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.OpenSaml4LogoutRequestResolver;
import org.springframework.security.saml2.provider.service.web.authentication.logout.Saml2LogoutRequestResolver;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MidpointSaml2LogoutRequestResolver implements Saml2LogoutRequestResolver {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointSaml2LogoutRequestResolver.class);

    private final OpenSaml4LogoutRequestResolver resolver;
    private final RelyingPartyRegistrationRepository relyingPartyRegistrations;

    public MidpointSaml2LogoutRequestResolver(InMemoryRelyingPartyRegistrationRepository inMemoryRelyingPartyRegistrations) {
        this.relyingPartyRegistrations = new InMemoryRelyingPartyRegistrationRepository(
                createRegistrationsWithFakeKeys(inMemoryRelyingPartyRegistrations));
        this.resolver = new OpenSaml4LogoutRequestResolver(new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrations));
        this.resolver.setParametersConsumer(this::resolveParameters);
    }

    private List<RelyingPartyRegistration> createRegistrationsWithFakeKeys(InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrations) {
        List<RelyingPartyRegistration> registrations = new ArrayList<>();
        relyingPartyRegistrations.forEach(registration -> {
            if (registration.getSigningX509Credentials().isEmpty()) {
                LOGGER.debug("Relying party registration with id: " + registration.getRegistrationId()
                        + " doesn't contain any singing key. Logout request need sign key for signing of logout request, "
                        + "so we try creating fake key and certificate.");
                RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRelyingPartyRegistration(registration);
                try {
                    createFakeKeyForSign(builder);
                    registrations.add(builder.build());
                    LOGGER.debug(
                            "We add fake key and certificate to relying party registration with id: "
                            + registration.getRegistrationId());
                    return;
                } catch (NoSuchAlgorithmException | CertificateException | OperatorCreationException | IOException e) {
                    LOGGER.debug("Couldn't generate fake key pair for logout.", e);
                }
            }

            LOGGER.debug("We use original saml relying party registration without fake key and certificate");
            registrations.add(registration);
        });
        return registrations;
    }

    private void resolveParameters(OpenSaml4LogoutRequestResolver.LogoutRequestParameters logoutRequestParameters) {
        if (logoutRequestParameters.getLogoutRequest() == null) {
            return;
        }
        if (logoutRequestParameters.getAuthentication() != null
                && logoutRequestParameters.getAuthentication().getPrincipal() instanceof Saml2Provider.MidpointSaml2AuthenticatedPrincipal) {
            LogoutRequest logoutRequest = logoutRequestParameters.getLogoutRequest();
            Saml2Provider.MidpointSaml2AuthenticatedPrincipal principal =
                    (Saml2Provider.MidpointSaml2AuthenticatedPrincipal) logoutRequestParameters.getAuthentication().getPrincipal();
            logoutRequest.getNameID().setSPNameQualifier(principal.getSpNameQualifier());
            logoutRequest.getNameID().setFormat(principal.getNameIdFormat());
        }
    }

    @Override
    public Saml2LogoutRequest resolve(HttpServletRequest httpServletRequest, Authentication authentication) {
        try {
            Saml2AuthenticationToken token = null;
            if (authentication instanceof MidpointAuthentication) {
                ModuleAuthentication authModule = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
                if (authModule instanceof Saml2ModuleAuthenticationImpl) {
                    if (authModule.getAuthentication() instanceof Saml2AuthenticationToken) {
                        token = (Saml2AuthenticationToken) authModule.getAuthentication();
                    } else if ((authModule.getAuthentication() instanceof PreAuthenticatedAuthenticationToken
                            || authModule.getAuthentication() instanceof AnonymousAuthenticationToken)
                            && authModule.getAuthentication().getDetails() instanceof Saml2AuthenticationToken) {
                        token = (Saml2AuthenticationToken) authModule.getAuthentication().getDetails();
                    }
                }
            } else if (authentication instanceof AnonymousAuthenticationToken
                    && authentication.getDetails() instanceof Saml2AuthenticationToken) {
                token = (Saml2AuthenticationToken) authentication.getDetails();
            }
            if (token != null) {
                AuthenticatedPrincipal principal = token.getDetails() instanceof AuthenticatedPrincipal ?
                        (AuthenticatedPrincipal) token.getDetails() : null;
                if (!(principal instanceof Saml2AuthenticatedPrincipal)) {
                    String name = token.getRelyingPartyRegistration().getEntityId();
                    String relyingPartyRegistrationId = token.getRelyingPartyRegistration().getRegistrationId();
                    principal = new Saml2AuthenticatedPrincipal() {
                        @Override
                        public String getName() {
                            return name;
                        }

                        @Override
                        public String getRelyingPartyRegistrationId() {
                            return relyingPartyRegistrationId;
                        }
                    };
                }

                return resolver.resolve(httpServletRequest, new Saml2Authentication(principal, token.getSaml2Response(), null));
            }
            return resolver.resolve(httpServletRequest, authentication);
        } catch (Saml2Exception e) {
            LOGGER.debug("Couldn't create logout request.", e);
        }
        return null;
    }

    private void createFakeKeyForSign(RelyingPartyRegistration.Builder relyingPartyRegistrationBuilder) throws NoSuchAlgorithmException, CertificateException, OperatorCreationException, IOException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        KeyPair keyPair = kpg.generateKeyPair();
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();

        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        X509v3CertificateBuilder certBuilder = getX509v3CertificateBuilder(privateKey, subPubKeyInfo);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider(new BouncyCastleProvider())
                .build(keyPair.getPrivate());
        X509CertificateHolder certificateHolder = certBuilder.build(signer);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(certificateHolder.getEncoded());
        X509Certificate cert = (X509Certificate) certFactory.generateCertificate(in);

        relyingPartyRegistrationBuilder.signingX509Credentials(
                signingX509Credentials -> signingX509Credentials.add(
                        Saml2X509Credential.signing(
                                privateKey,
                                cert)));
    }

    @NotNull
    private X509v3CertificateBuilder getX509v3CertificateBuilder(RSAPrivateCrtKey privateKey, SubjectPublicKeyInfo subPubKeyInfo) {
        Calendar cal = Calendar.getInstance();
        Date from = cal.getTime();
        cal.add(Calendar.HOUR, 1);
        Date to = cal.getTime();
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                new X500Name("CN=MidPoint,O=Evolveum,L=,C="),
                privateKey.getCrtCoefficient(),
                from,
                to,
                new X500Name("CN=MidPoint,O=Evolveum,L=,C="),
                subPubKeyInfo);
        return certBuilder;
    }
}
