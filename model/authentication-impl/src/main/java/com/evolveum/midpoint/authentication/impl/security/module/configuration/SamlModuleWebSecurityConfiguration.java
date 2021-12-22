/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.module.configuration;

import static com.evolveum.midpoint.authentication.impl.security.util.AuthSequenceUtil.getBasePath;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

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
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import com.evolveum.midpoint.authentication.impl.security.saml.MidpointAssertingPartyMetadataConverter;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.saml2.Saml2Exception;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.web.util.UriComponentsBuilder;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfiguration extends ModuleWebSecurityConfigurationImpl {

    private static final Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfiguration.class);

    public static final String SSO_LOCATION_URL_SUFFIX = "/SSO/alias/{registrationId}";
    public static final String LOGOUT_LOCATION_URL_SUFFIX = "/logout/alias/{registrationId}";

    private static Protector protector;
    private static final ResourceLoader RESOURCE_LOADER = new DefaultResourceLoader();
    private static final MidpointAssertingPartyMetadataConverter ASSERTING_PARTY_METADATA_CONVERTER = new MidpointAssertingPartyMetadataConverter();

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private final Map<String, SamlMidpointAdditionalConfiguration> additionalConfiguration = new HashMap<>();

    private SamlModuleWebSecurityConfiguration() {
    }

    public static void setProtector(Protector protector) {
        SamlModuleWebSecurityConfiguration.protector = protector;
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

            if (serviceProviderType.getIdentityProvider() == null) {
                List<Saml2ProviderAuthenticationModuleType> providersType = serviceProviderType.getProvider();
                providersType.forEach(providerType -> {

                });
            } else {
                Saml2ProviderAuthenticationModuleType providerType = serviceProviderType.getIdentityProvider();
                RelyingPartyRegistration.Builder registrationBuilder = getRelyingPartyFromMetadata(providerType.getMetadata());
                SamlMidpointAdditionalConfiguration.Builder additionalConfigBuilder = SamlMidpointAdditionalConfiguration.builder();
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
            }
        });

        InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository = new InMemoryRelyingPartyRegistrationRepository(registrations);
        configuration.setRelyingPartyRegistrationRepository(relyingPartyRegistrationRepository);
        return configuration;
    }

    private static void createRelyingPartyRegistration(RelyingPartyRegistration.Builder registrationBuilder,
            SamlMidpointAdditionalConfiguration.Builder additionalConfigBuilder, Saml2ProviderAuthenticationModuleType providerType,
            String publicHttpUrlPattern, SamlModuleWebSecurityConfiguration configuration, Saml2KeyAuthenticationModuleType keysType,
            Saml2ServiceProviderAuthenticationModuleType serviceProviderType, ServletRequest request) {

        String linkText = providerType.getLinkText() == null ?
                (providerType.getAlias() == null ? providerType.getEntityId() : providerType.getAlias())
                : providerType.getLinkText();
        additionalConfigBuilder.nameOfUsernameAttribute(providerType.getNameOfUsernameAttribute())
                .linkText(linkText);
        String registrationId;
        if (serviceProviderType.getIdentityProvider() != null || serviceProviderType.getProvider().size() == 1) {
            registrationId = StringUtils.isNotEmpty(serviceProviderType.getAliasForPath()) ? serviceProviderType.getAliasForPath() :
                    (StringUtils.isNotEmpty(serviceProviderType.getAlias()) ? serviceProviderType.getAlias() : serviceProviderType.getEntityId());
        } else {
            registrationId = StringUtils.isNotEmpty(providerType.getAlias()) ? providerType.getAlias() : providerType.getEntityId();
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                StringUtils.isNotBlank(publicHttpUrlPattern) ? publicHttpUrlPattern : getBasePath((HttpServletRequest) request));
        UriComponentsBuilder ssoBuilder = builder.cloneBuilder();
        ssoBuilder.pathSegment(AuthUtil.stripSlashes(configuration.getPrefix()) + SSO_LOCATION_URL_SUFFIX);
        UriComponentsBuilder logoutBuilder = builder.cloneBuilder();
        logoutBuilder.pathSegment(AuthUtil.stripSlashes(configuration.getPrefix()) + LOGOUT_LOCATION_URL_SUFFIX);
        registrationBuilder
                .registrationId(registrationId)
                .entityId(serviceProviderType.getEntityId())
                .assertionConsumerServiceLocation(ssoBuilder.build().toUriString())
                .singleLogoutServiceLocation(logoutBuilder.build().toUriString())
                .assertingPartyDetails(party -> {
                    party.entityId(providerType.getEntityId())
                            .singleSignOnServiceBinding(Saml2MessageBinding.from(providerType.getAuthenticationRequestBinding()));
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

    private static RelyingPartyRegistration.Builder getRelyingPartyFromMetadata(Saml2ProviderMetadataAuthenticationModuleType metadata) {
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
                    builder = ASSERTING_PARTY_METADATA_CONVERTER.convert(new ByteArrayInputStream(metadataAsString.getBytes()));
                }
            }
            if (metadata.getMetadataUrl() != null) {
                try (InputStream source = RESOURCE_LOADER.getResource(metadata.getMetadataUrl()).getInputStream()) {
                    builder = ASSERTING_PARTY_METADATA_CONVERTER.convert(source);
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

    public Map<String, SamlMidpointAdditionalConfiguration> getAdditionalConfiguration() {
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
        try {
            Saml2X509Credential saml2credential = null;
//            byte[] certbytes = X509Utilities.getDER(protector.decryptString(key.getCertificate()));
//            X509Certificate certificate = X509Utilities.getCertificate(certbytes);

            byte[] certbytes = protector.decryptString(key.getCertificate()).getBytes();
            X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certbytes));

            String stringPrivateKey = protector.decryptString(key.getPrivateKey());
            String stringPassphrase = protector.decryptString(key.getPassphrase());
            if (hasText(stringPrivateKey)) {
                PrivateKey pkey;
                Object obj = null;
                try {
                    PEMParser parser = new PEMParser(new CharArrayReader(stringPrivateKey.toCharArray()));
                    obj = parser.readObject();
                    parser.close();
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                    if (obj == null) {
                        throw new Saml2Exception("Unable to decode PEM key:" + key.getPrivateKey());
                    } else if (obj instanceof PEMEncryptedKeyPair) {

                        // Encrypted key - we will use provided password
                        PEMEncryptedKeyPair ckp = (PEMEncryptedKeyPair) obj;
                        char[] passarray = (ofNullable(stringPassphrase).orElse("")).toCharArray();
                        PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(passarray);
                        KeyPair kp = converter.getKeyPair(ckp.decryptKeyPair(decProv));
                        pkey = kp.getPrivate();
                    } else if (obj instanceof PEMKeyPair) {
                        // Unencrypted key - no password needed
                        PEMKeyPair ukp = (PEMKeyPair) obj;
                        KeyPair kp = converter.getKeyPair(ukp);
                        pkey = kp.getPrivate();
                    } else if (obj instanceof PrivateKeyInfo) {
                        // Encrypted key - we will use provided password
                        PrivateKeyInfo pk = (PrivateKeyInfo) obj;
                        pkey = converter.getPrivateKey(pk);
                    } else if (obj instanceof PKCS8EncryptedPrivateKeyInfo) {
                        // Encrypted key - we will use provided password
                        PKCS8EncryptedPrivateKeyInfo cpk = (PKCS8EncryptedPrivateKeyInfo) obj;
                        char[] passarray = (ofNullable(stringPassphrase).orElse("")).toCharArray();
                        final InputDecryptorProvider provider = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(passarray);
                        pkey = converter.getPrivateKey(cpk.decryptPrivateKeyInfo(provider));
                    } else {
                        throw new Saml2Exception("Unable get private key from " + obj);
                    }
                } catch (IOException e) {
                    throw new Saml2Exception("Unable get private key", e);
                } catch (OperatorCreationException | PKCSException e) {
                    throw new Saml2Exception("Unable get private key from " + obj, e);
                }
                List<Saml2X509Credential.Saml2X509CredentialType> types = getTypesForKey(isActive, key.getType());
                saml2credential = new Saml2X509Credential(pkey, certificate, types.toArray(new Saml2X509Credential.Saml2X509CredentialType[0]));
            }

            return saml2credential;
        } catch (EncryptionException | CertificateException e) {
            throw new Saml2Exception("Unable get key from " + key);
        }
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
        try {
            KeyStore ks = KeyStore.getInstance("JKS");
            FileInputStream is = new FileInputStream(key.getKeyStorePath());
            ks.load(is, protector.decryptString(key.getKeyStorePassword()).toCharArray());

            Key pkey = ks.getKey(key.getKeyAlias(), protector.decryptString(key.getKeyPassword()).toCharArray());
            if (!(pkey instanceof PrivateKey)) {
                throw new Saml2Exception("Alias " + key.getKeyAlias() + " don't return key of PrivateKey type.");
            }
            Certificate certificate = ks.getCertificate(key.getKeyAlias());
            if (!(certificate instanceof X509Certificate)) {
                throw new Saml2Exception("Alias " + key.getKeyAlias() + " don't return certificate of X509Certificate type.");
            }
            List<Saml2X509Credential.Saml2X509CredentialType> types = getTypesForKey(isActive, key.getType());
            return new Saml2X509Credential((PrivateKey) pkey, (X509Certificate) certificate, types.toArray(new Saml2X509Credential.Saml2X509CredentialType[0]));
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | EncryptionException | UnrecoverableKeyException e) {
            throw new Saml2Exception("Unable get key from " + key);
        }
    }
}
