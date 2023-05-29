/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import static com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil.getBasePath;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.impl.saml.MidpointAssertingPartyMetadataConverter;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import com.evolveum.midpoint.prism.PrismContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.common.util.Base64Exception;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.web.util.UriComponentsBuilder;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfiguration extends RemoteModuleWebSecurityConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfiguration.class);

    public static final String SSO_LOCATION_URL_SUFFIX = "/SSO/alias/{registrationId}";
    public static final String LOGOUT_LOCATION_URL_SUFFIX = "/logout/alias/{registrationId}";

    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
    private static final MidpointAssertingPartyMetadataConverter ASSERTING_PARTY_METADATA_CONVERTER = new MidpointAssertingPartyMetadataConverter();

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private final Map<String, SamlAdditionalConfiguration> additionalConfiguration = new HashMap<>();

    private SamlModuleWebSecurityConfiguration() {
    }

    public static SamlModuleWebSecurityConfiguration build(Saml2AuthenticationModuleType modelType, String prefixOfSequence,
            String publicHttpUrlPattern, ServletRequest request) {
        SamlModuleWebSecurityConfiguration configuration = buildInternal(modelType, prefixOfSequence, publicHttpUrlPattern, request);
        configuration.validate();
        return configuration;
    }

    private static SamlModuleWebSecurityConfiguration buildInternal(Saml2AuthenticationModuleType modelType, String prefixOfSequence,
            String publicHttpUrlPattern, ServletRequest request) {
        SamlModuleWebSecurityConfiguration configuration = new SamlModuleWebSecurityConfiguration();
        build(configuration, modelType, prefixOfSequence);

        List<Saml2ServiceProviderAuthenticationModuleType> serviceProviders = modelType.getServiceProvider();
        List<RelyingPartyRegistration> registrations = new ArrayList<>();
        serviceProviders.forEach(serviceProviderType -> {
            Saml2KeyAuthenticationModuleType keysType = serviceProviderType.getKeys();

            Saml2ProviderAuthenticationModuleType providerType = serviceProviderType.getIdentityProvider();
            RelyingPartyRegistration.Builder registrationBuilder = getRelyingPartyFromMetadata(providerType.getMetadata(), providerType);
            SamlAdditionalConfiguration.Builder additionalConfigBuilder = SamlAdditionalConfiguration.builder();
            createRelyingPartyRegistration(registrationBuilder,
                    additionalConfigBuilder,
                    providerType,
                    publicHttpUrlPattern,
                    configuration,
                    keysType,
                    serviceProviderType,
                    request);
            RelyingPartyRegistration registration = registrationBuilder.build();
            registrations.add(registration);
            configuration.additionalConfiguration.put(registration.getRegistrationId(), additionalConfigBuilder.build());
        });

        InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository = new InMemoryRelyingPartyRegistrationRepository(registrations);
        configuration.setRelyingPartyRegistrationRepository(relyingPartyRegistrationRepository);
        return configuration;
    }

    private static void createRelyingPartyRegistration(RelyingPartyRegistration.Builder registrationBuilder,
            SamlAdditionalConfiguration.Builder additionalConfigBuilder, Saml2ProviderAuthenticationModuleType providerType,
            String publicHttpUrlPattern, SamlModuleWebSecurityConfiguration configuration, Saml2KeyAuthenticationModuleType keysType,
            Saml2ServiceProviderAuthenticationModuleType serviceProviderType, ServletRequest request) {

        String linkText = providerType.getLinkText() == null ? providerType.getEntityId() : providerType.getLinkText();
        additionalConfigBuilder.nameOfUsernameAttribute(providerType.getNameOfUsernameAttribute())
                .linkText(linkText);

        String registrationId = StringUtils.isNotEmpty(serviceProviderType.getAliasForPath()) ? serviceProviderType.getAliasForPath() :
                    (StringUtils.isNotEmpty(serviceProviderType.getAlias()) ? serviceProviderType.getAlias() : serviceProviderType.getEntityId());

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                StringUtils.isNotBlank(publicHttpUrlPattern) ? publicHttpUrlPattern : getBasePath((HttpServletRequest) request));

        UriComponentsBuilder ssoBuilder = builder.cloneBuilder();
        ssoBuilder.pathSegment(AuthUtil.stripSlashes(configuration.getPrefixOfModule()) + SSO_LOCATION_URL_SUFFIX);

        UriComponentsBuilder logoutBuilder = builder.cloneBuilder();
        logoutBuilder.pathSegment(AuthUtil.stripSlashes(configuration.getPrefixOfModule()) + LOGOUT_LOCATION_URL_SUFFIX);

        registrationBuilder
                .registrationId(registrationId)
                .entityId(serviceProviderType.getEntityId())
                .assertionConsumerServiceLocation(ssoBuilder.build().toUriString())
                .singleLogoutServiceLocation(logoutBuilder.build().toUriString())
                .assertingPartyDetails(party -> {
                    party.entityId(providerType.getEntityId());

                    if (serviceProviderType.isSignRequests() != null) {
                        party.wantAuthnRequestsSigned(Boolean.TRUE.equals(serviceProviderType.isSignRequests()));
                    }

                    if (providerType.getVerificationKeys() != null && !providerType.getVerificationKeys().isEmpty()) {
                        party.verificationX509Credentials(c -> providerType.getVerificationKeys().forEach(verKey -> {
                            byte[] certbytes = new byte[0];
                            try {
                                certbytes = protector.decryptString(verKey).getBytes();
                            } catch (EncryptionException e) {
                                LOGGER.error("Couldn't obtain clear string for provider verification key");
                            }
                            try {
                                X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certbytes));
                                c.add(new Saml2X509Credential(certificate, Saml2X509Credential.Saml2X509CredentialType.VERIFICATION));
                            } catch (CertificateException e) {
                                LOGGER.error("Couldn't obtain certificate from " + verKey);
                            }
                        }));
                    }
                });
        Saml2X509Credential activeCredential = null;
        if (keysType != null) {
            ModuleSaml2SimpleKeyType simpleKeyType = keysType.getActiveSimpleKey();
            if (simpleKeyType != null) {
                activeCredential = getSaml2Credential(simpleKeyType, true);
            }
            ModuleSaml2KeyStoreKeyType storeKeyType = keysType.getActiveKeyStoreKey();
            if (storeKeyType != null) {
                activeCredential = getSaml2Credential(storeKeyType, true);
            }

            List<Saml2X509Credential> credentials = new ArrayList<>();
            if (activeCredential != null) {
                credentials.add(activeCredential);
            }

            if (keysType.getStandBySimpleKey() != null && !keysType.getStandBySimpleKey().isEmpty()) {
                for (ModuleSaml2SimpleKeyType standByKey : keysType.getStandBySimpleKey()) {
                    Saml2X509Credential credential = getSaml2Credential(standByKey, false);
                    if (credential != null) {
                        credentials.add(credential);
                    }
                }
            }
            if (keysType.getStandByKeyStoreKey() != null && !keysType.getStandByKeyStoreKey().isEmpty()) {
                for (ModuleSaml2KeyStoreKeyType standByKey : keysType.getStandByKeyStoreKey()) {
                    Saml2X509Credential credential = getSaml2Credential(standByKey, false);
                    if (credential != null) {
                        credentials.add(credential);
                    }
                }
            }

            if (!credentials.isEmpty()) {
                registrationBuilder.decryptionX509Credentials(c -> credentials.forEach(cred -> {
                    if (cred.getCredentialTypes().contains(Saml2X509Credential.Saml2X509CredentialType.DECRYPTION)) {
                        c.add(cred);
                    }
                }));
                registrationBuilder.signingX509Credentials(c -> credentials.forEach(cred -> {
                    if (cred.getCredentialTypes().contains(Saml2X509Credential.Saml2X509CredentialType.SIGNING)) {
                        c.add(cred);
                    }
                }));
            }
        }
    }

    private static RelyingPartyRegistration.Builder getRelyingPartyFromMetadata(Saml2ProviderMetadataAuthenticationModuleType metadata, Saml2ProviderAuthenticationModuleType providerConfig) {
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId("builder");
        if (metadata != null) {
            if (metadata.getXml() != null || metadata.getPathToFile() != null) {
                String metadataAsString = null;
                try {
                    metadataAsString = createMetadata(metadata);
                } catch (IOException e) {
                    LOGGER.error("Couldn't obtain metadata as string from " + metadata);
                }
                if (StringUtils.isNotEmpty(metadataAsString)) {
                    builder = ASSERTING_PARTY_METADATA_CONVERTER.convert(new ByteArrayInputStream(metadataAsString.getBytes()), providerConfig);
                }
            }
            if (metadata.getMetadataUrl() != null) {
                try (InputStream source = RESOURCE_LOADER.getResource(metadata.getMetadataUrl()).getInputStream()) {
                    builder = ASSERTING_PARTY_METADATA_CONVERTER.convert(source, providerConfig);
                } catch (IOException ex) {
                    if (ex.getCause() instanceof Saml2Exception) {
                        throw (Saml2Exception) ex.getCause();
                    }
                    throw new Saml2Exception(ex);
                }
            }
        }
        return builder;
    }

    private static String createMetadata(Saml2ProviderMetadataAuthenticationModuleType metadata) throws IOException {
        if (metadata != null) {
            String metadataUrl = metadata.getMetadataUrl();
            if (StringUtils.isNotBlank(metadataUrl)) {
                return metadataUrl;
            }
            String pathToFile = metadata.getPathToFile();
            if (StringUtils.isNotBlank(pathToFile)) {
                return readFile(pathToFile);
            }
            byte[] xml = metadata.getXml();
            if (xml != null && xml.length != 0) {
                return new String(xml);
            }
        }
        throw new IllegalArgumentException("Metadata is not present");
    }

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

    public InMemoryRelyingPartyRegistrationRepository getRelyingPartyRegistrationRepository() {
        return relyingPartyRegistrationRepository;
    }

    public void setRelyingPartyRegistrationRepository(InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
    }

    public Map<String, SamlAdditionalConfiguration> getAdditionalConfiguration() {
        return additionalConfiguration;
    }

    @Override
    protected void validate() {
        super.validate();
        if (getRelyingPartyRegistrationRepository() == null) {
            throw new IllegalArgumentException("Saml configuration is null");
        }
    }

    public static Saml2X509Credential getSaml2Credential(ModuleSaml2SimpleKeyType key, boolean isActive) {
        if (key == null) {
            return null;
        }
        PrivateKey pkey;
        try {
            pkey = getPrivateKey(key, protector);
        } catch (IOException | OperatorCreationException | PKCSException | EncryptionException e) {
            throw new Saml2Exception("Unable get key from " + key, e);
        }

        Certificate certificate;
        try {
            certificate = getCertificate(key, protector);
        } catch (Base64Exception | EncryptionException | CertificateException e) {
            throw new Saml2Exception("Unable get certificate from " + key, e);
        }
        List<Saml2X509Credential.Saml2X509CredentialType> types = getTypesForKey(isActive, key.getType());
        return new Saml2X509Credential(
                pkey, (X509Certificate) certificate, types.toArray(new Saml2X509Credential.Saml2X509CredentialType[0]));
    }

    private static List<Saml2X509Credential.Saml2X509CredentialType> getTypesForKey(boolean isActive, ModuleSaml2KeyTypeType type) {
        List<Saml2X509Credential.Saml2X509CredentialType> types = new ArrayList<>();
        if (isActive) {
            types.add(Saml2X509Credential.Saml2X509CredentialType.SIGNING);
            types.add(Saml2X509Credential.Saml2X509CredentialType.DECRYPTION);
        } else if (type != null) {
            if (ModuleSaml2KeyTypeType.UNSPECIFIED.equals(type)) {
                types.add(Saml2X509Credential.Saml2X509CredentialType.SIGNING);
                types.add(Saml2X509Credential.Saml2X509CredentialType.DECRYPTION);
            } else {
                Saml2X509Credential.Saml2X509CredentialType samlType = Saml2X509Credential.Saml2X509CredentialType.valueOf(type.name());
                if (samlType.equals(Saml2X509Credential.Saml2X509CredentialType.SIGNING)
                        || samlType.equals(Saml2X509Credential.Saml2X509CredentialType.DECRYPTION)) {
                    types.add(samlType);
                }
            }
        } else {
            types.add(Saml2X509Credential.Saml2X509CredentialType.SIGNING);
            types.add(Saml2X509Credential.Saml2X509CredentialType.DECRYPTION);
        }
        return types;
    }

    public static Saml2X509Credential getSaml2Credential(ModuleSaml2KeyStoreKeyType key, boolean isActive) {
        if (key == null) {
            return null;
        }
        PrivateKey pkey;
        try {
            pkey = getPrivateKey(key, protector);
        } catch (KeyStoreException | IOException | EncryptionException | CertificateException |
                NoSuchAlgorithmException | UnrecoverableKeyException e) {
            throw new Saml2Exception("Unable get key from " + key, e);
        }

        Certificate certificate;
        try {
            certificate = getCertificate(key, protector);
        } catch (EncryptionException | CertificateException | KeyStoreException | IOException | NoSuchAlgorithmException e) {
            throw new Saml2Exception("Unable get certificate from " + key, e);
        }
        if (!(certificate instanceof X509Certificate)) {
            throw new Saml2Exception("Alias " + key.getKeyAlias() + " don't return certificate of X509Certificate type.");
        }
        List<Saml2X509Credential.Saml2X509CredentialType> types = getTypesForKey(isActive, key.getType());
        return new Saml2X509Credential(pkey, (X509Certificate) certificate, types.toArray(new Saml2X509Credential.Saml2X509CredentialType[0]));
    }
}
