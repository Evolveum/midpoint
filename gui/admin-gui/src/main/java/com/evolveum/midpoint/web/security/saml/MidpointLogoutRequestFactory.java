/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.saml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.IssuerBuilder;

import org.opensaml.saml.saml2.core.impl.LogoutRequestBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDImpl;
import org.opensaml.saml.security.impl.SAMLMetadataSignatureSigningParametersResolver;
import org.opensaml.security.SecurityException;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.CredentialSupport;
import org.opensaml.security.credential.UsageType;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.SignatureSigningParametersResolver;
import org.opensaml.xmlsec.criterion.SignatureSigningConfigurationCriterion;
import org.opensaml.xmlsec.crypto.XMLSigningUtil;
import org.opensaml.xmlsec.impl.BasicSignatureSigningConfiguration;
import org.opensaml.xmlsec.keyinfo.KeyInfoGeneratorManager;
import org.opensaml.xmlsec.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.SignableXMLObject;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureSupport;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.OpenSamlInitializationService;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.*;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.w3c.dom.Element;

/**
 * @author lskublik
 */
public final class MidpointLogoutRequestFactory implements Saml2AuthenticationRequestFactory {

	static {
		OpenSamlInitializationService.initialize();
	}

	private final LogoutRequestBuilder logoutRequestBuilder;
	private final IssuerBuilder issuerBuilder;

	public MidpointLogoutRequestFactory() {
		XMLObjectProviderRegistry registry = ConfigurationService.get(XMLObjectProviderRegistry.class);
		this.logoutRequestBuilder = (LogoutRequestBuilder) registry.getBuilderFactory()
				.getBuilder(LogoutRequest.DEFAULT_ELEMENT_NAME);
		this.issuerBuilder = (IssuerBuilder) registry.getBuilderFactory().getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
	}

	@Override
	@Deprecated
	public String createAuthenticationRequest(Saml2AuthenticationRequest request) {
		RelyingPartyRegistration registration = RelyingPartyRegistration.withRegistrationId("noId")
				.assertionConsumerServiceBinding(Saml2MessageBinding.POST)
				.assertionConsumerServiceLocation(request.getAssertionConsumerServiceUrl())
				.entityId(request.getIssuer()).remoteIdpEntityId("noIssuer").idpWebSsoUrl("noUrl")
				.credentials((credentials) -> credentials.addAll(request.getCredentials())).build();
		Saml2AuthenticationRequestContext context = Saml2AuthenticationRequestContext.builder()
				.relyingPartyRegistration(registration).issuer(request.getIssuer())
				.assertionConsumerServiceUrl(request.getAssertionConsumerServiceUrl()).build();
		LogoutRequest logoutRequest = createLogoutRequest(context);
		return serialize(sign(logoutRequest, registration));
	}

	@Override
	public Saml2PostAuthenticationRequest createPostAuthenticationRequest(Saml2AuthenticationRequestContext context) {
        LogoutRequest logoutRequest = createLogoutRequest(context);
		RelyingPartyRegistration registration = context.getRelyingPartyRegistration();
		if (registration.getAssertingPartyDetails().getWantAuthnRequestsSigned()) {
			sign(logoutRequest, registration);
		}
		String xml = serialize(logoutRequest);
		return Saml2PostAuthenticationRequest.withAuthenticationRequestContext(context)
				.samlRequest(Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8))).build();
	}

	@Override
	public Saml2RedirectAuthenticationRequest createRedirectAuthenticationRequest(
			Saml2AuthenticationRequestContext context) {
        LogoutRequest logoutRequest = createLogoutRequest(context);
		RelyingPartyRegistration registration = context.getRelyingPartyRegistration();
		String xml = serialize(logoutRequest);
		Saml2RedirectAuthenticationRequest.Builder result = Saml2RedirectAuthenticationRequest
				.withAuthenticationRequestContext(context);
		String deflatedAndEncoded = Base64.getEncoder().encodeToString(deflate(xml));
		result.samlRequest(deflatedAndEncoded).relayState(context.getRelayState());
		if (registration.getAssertingPartyDetails().getWantAuthnRequestsSigned()) {
            SignatureSigningParameters parameters = resolveSigningParameters(registration);
            Credential credential = parameters.getSigningCredential();
            String algorithmUri = parameters.getSignatureAlgorithm();
            Map<String, String> queryParams = new LinkedHashMap<>();
            queryParams.put("SAMLRequest", deflatedAndEncoded);
            if (StringUtils.hasText(context.getRelayState())) {
                queryParams.put("RelayState", context.getRelayState());
            }
            queryParams.put("SigAlg", algorithmUri);
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            for (Map.Entry<String, String> component : queryParams.entrySet()) {
                builder.queryParam(component.getKey(),
                        UriUtils.encode(component.getValue(), StandardCharsets.ISO_8859_1));
            }
            String queryString = builder.build(true).toString().substring(1);
            try {
                byte[] rawSignature = XMLSigningUtil.signWithURI(credential, algorithmUri,
                        queryString.getBytes(StandardCharsets.UTF_8));
                String b64Signature = Base64.getEncoder().encodeToString(rawSignature);
                queryParams.put("Signature", b64Signature);
            }
            catch (SecurityException ex) {
                throw new Saml2Exception(ex);
            }
			return result.sigAlg(queryParams.get("SigAlg")).signature(queryParams.get("Signature")).build();
		}
		return result.build();
	}

	private LogoutRequest createLogoutRequest(Saml2AuthenticationRequestContext context) {
		String issuer = context.getIssuer();
		String destination = context.getDestination();
        LogoutRequest logoutRequest = this.logoutRequestBuilder.buildObject();
		if (logoutRequest.getID() == null) {
            logoutRequest.setID("LRQ" + UUID.randomUUID().toString().substring(1));
		}
		if (logoutRequest.getIssueInstant() == null) {
            logoutRequest.setIssueInstant(Instant.now(Clock.systemUTC()));
		}
		Issuer iss = this.issuerBuilder.buildObject();
		iss.setValue(issuer);
        logoutRequest.setIssuer(iss);
        logoutRequest.setDestination(destination);
        NameID nameID = new SamlNameId();
        nameID.setValue(logoutRequest.getID());
        nameID.setSPNameQualifier(issuer);
        logoutRequest.setNameID(nameID);
		return logoutRequest;
	}

    private String serialize(XMLObject object) {
        try {
            Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(object);
            Element element = marshaller.marshall(object);
            return SerializeSupport.nodeToString(element);
        }
        catch (MarshallingException ex) {
            throw new Saml2Exception(ex);
        }
    }

    private static <O extends SignableXMLObject> O sign(O object, RelyingPartyRegistration relyingPartyRegistration) {
        try {
            SignatureSigningParameters parameters = resolveSigningParameters(relyingPartyRegistration);
            Assert.notNull(parameters, "Failed to resolve any signing credential");
            SignatureSupport.signObject(object, parameters);
            return object;
        }
        catch (Exception ex) {
            throw new Saml2Exception(ex);
        }
    }

    private byte[] deflate(String s) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DeflaterOutputStream deflater = new DeflaterOutputStream(b, new Deflater(Deflater.DEFLATED, true));
            deflater.write(s.getBytes(StandardCharsets.UTF_8));
            deflater.finish();
            return b.toByteArray();
        }
        catch (IOException ex) {
            throw new Saml2Exception("Unable to deflate string", ex);
        }
    }

    private static SignatureSigningParameters resolveSigningParameters(
            RelyingPartyRegistration relyingPartyRegistration) {
        List<Credential> credentials = new ArrayList<>();
        for (Saml2X509Credential x509Credential : relyingPartyRegistration.getSigningX509Credentials()) {
            X509Certificate certificate = x509Credential.getCertificate();
            PrivateKey privateKey = x509Credential.getPrivateKey();
            BasicCredential credential = CredentialSupport.getSimpleCredential(certificate, privateKey);
            credential.setEntityId(relyingPartyRegistration.getEntityId());
            credential.setUsageType(UsageType.SIGNING);
            credentials.add(credential);
        }
        List<String> algorithms = relyingPartyRegistration.getAssertingPartyDetails().getSigningAlgorithms();
        List<String> digests = Collections.singletonList(SignatureConstants.ALGO_ID_DIGEST_SHA256);
        String canonicalization = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;
        SignatureSigningParametersResolver resolver = new SAMLMetadataSignatureSigningParametersResolver();
        CriteriaSet criteria = new CriteriaSet();
        BasicSignatureSigningConfiguration signingConfiguration = new BasicSignatureSigningConfiguration();
        signingConfiguration.setSigningCredentials(credentials);
        signingConfiguration.setSignatureAlgorithms(algorithms);
        signingConfiguration.setSignatureReferenceDigestMethods(digests);
        signingConfiguration.setSignatureCanonicalizationAlgorithm(canonicalization);
        final NamedKeyInfoGeneratorManager namedManager = new NamedKeyInfoGeneratorManager();

        namedManager.setUseDefaultManager(true);
        final KeyInfoGeneratorManager defaultManager = namedManager.getDefaultManager();
        // Generator for X509Credentials
        final X509KeyInfoGeneratorFactory x509Factory = new X509KeyInfoGeneratorFactory();
        x509Factory.setEmitEntityCertificate(true);
        x509Factory.setEmitEntityCertificateChain(true);

        defaultManager.registerFactory(x509Factory);
        signingConfiguration.setKeyInfoGeneratorManager(namedManager);
        criteria.add(new SignatureSigningConfigurationCriterion(signingConfiguration));
        try {
            SignatureSigningParameters parameters = resolver.resolveSingle(criteria);
            Assert.notNull(parameters, "Failed to resolve any signing credential");
            return parameters;
        }
        catch (Exception ex) {
            throw new Saml2Exception(ex);
        }
    }

    private class SamlNameId extends NameIDImpl{

        protected SamlNameId() {
            super(NameID.DEFAULT_ELEMENT_NAME.getNamespaceURI(), NameID.DEFAULT_ELEMENT_NAME.getLocalPart(), NameID.DEFAULT_ELEMENT_NAME.getPrefix());
        }
    }
}
