/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.configuration;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.saml.MidpointAssertingPartyMetadataConverter;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
import org.springframework.security.saml2.provider.service.registration.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.util.Optional.ofNullable;
import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class SamlModuleWebSecurityConfiguration extends ModuleWebSecurityConfigurationImpl {

    private static final Trace LOGGER = TraceManager.getTrace(SamlModuleWebSecurityConfiguration.class);

    public static final String RESPONSE_PROCESSING_URL_SUFFIX = "/SSO/alias/{registrationId}";
    public static final String REQUEST_PROCESSING_URL_SUFFIX = "/authenticate/{registrationId}";

    private static Protector protector;
    private static final ResourceLoader resourceLoader = new DefaultResourceLoader();
    private static final MidpointAssertingPartyMetadataConverter assertingPartyMetadataConverter = new MidpointAssertingPartyMetadataConverter();

    private InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
    private Map<String, SamlMidpointAdditionalConfiguration> additionalConfiguration = new HashMap<String, SamlMidpointAdditionalConfiguration>();

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

    private static SamlModuleWebSecurityConfiguration buildInternal(Saml2AuthenticationModuleType modelType, String prefixOfSequence, String publicHttpUrlPattern,
            ServletRequest request) {
        SamlModuleWebSecurityConfiguration configuration = new SamlModuleWebSecurityConfiguration();
        build(configuration, modelType, prefixOfSequence);

        Saml2ServiceProviderAuthenticationModuleType serviceProviderType = modelType.getServiceProvider();
        Saml2KeyAuthenticationModuleType keysType = serviceProviderType.getKeys();

        List<Saml2ProviderAuthenticationModuleType> providersType = serviceProviderType.getProvider();
        List<RelyingPartyRegistration> registrations = new ArrayList<>();
        for (Saml2ProviderAuthenticationModuleType providerType : providersType) {
            String linkText = providerType.getLinkText() == null ?
                    (providerType.getAlias() == null ? providerType.getEntityId() : providerType.getAlias())
                    : providerType.getLinkText();
            SamlMidpointAdditionalConfiguration.Builder additionalConfigBuilder =
                    SamlMidpointAdditionalConfiguration.builder()
                    .nameOfUsernameAttribute(providerType.getNameOfUsernameAttribute())
                    .linkText(linkText);
            String registrationId = StringUtils.isNotEmpty(providerType.getAliasForPath()) ? providerType.getAliasForPath() :
                    (StringUtils.isNotEmpty(providerType.getAlias()) ? providerType.getAlias() : providerType.getEntityId());
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(
                    StringUtils.isNotBlank(publicHttpUrlPattern) ? publicHttpUrlPattern : getBasePath((HttpServletRequest) request));
            builder.pathSegment(SecurityUtils.stripSlashes(configuration.getPrefix()) + RESPONSE_PROCESSING_URL_SUFFIX);
            RelyingPartyRegistration.Builder registrationBuilder = getRelyingPartyFromMetadata(providerType.getMetadata(), additionalConfigBuilder)
                    .registrationId(registrationId)
                    .entityId(serviceProviderType.getEntityId())
                    .assertionConsumerServiceLocation(builder.build().toUriString())
                    .assertingPartyDetails(party -> {
                        party.entityId(providerType.getEntityId())
                                .singleSignOnServiceBinding(Saml2MessageBinding.from(providerType.getAuthenticationRequestBinding()));
                        if (serviceProviderType.isSignRequests() != null) {
                            party.wantAuthnRequestsSigned(Boolean.TRUE.equals(serviceProviderType.isSignRequests()));
                        }
                        if (providerType.getVerificationKeys() != null && !providerType.getVerificationKeys().isEmpty()) {
                            party.verificationX509Credentials(c -> {
                                providerType.getVerificationKeys().forEach(verKey -> {
                                    byte[] certbytes = new byte[0];
                                    try {
                                        certbytes = protector.decryptString(verKey).getBytes();;
//                                        certbytes = X509Certificate. X509Utilities.getDER(protector.decryptString(verKey));
                                    } catch (EncryptionException e) {
                                        LOGGER.error("Couldn't obtain clear string for provider verification key");
                                    }
                                    try {
//                                        X509Certificate certificate = X509Utilities.getCertificate(certbytes);
                                        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certbytes));
                                        c.add(new Saml2X509Credential(certificate, Saml2X509Credential.Saml2X509CredentialType.VERIFICATION));
                                    } catch (CertificateException e) {
                                        LOGGER.error("Couldn't obtain certificate from " + verKey);
                                    }
                                });
                            });
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
                registrationBuilder.decryptionX509Credentials(c -> {
                    credentials.forEach(cred -> {
                        if (cred.getCredentialTypes().contains(Saml2X509Credential.Saml2X509CredentialType.DECRYPTION)) {
                            c.add(cred);
                        }
                    });
                });
                registrationBuilder.signingX509Credentials(c -> {
                    credentials.forEach(cred -> {
                        if (cred.getCredentialTypes().contains(Saml2X509Credential.Saml2X509CredentialType.SIGNING)) {
                            c.add(cred);
                        }
                    });
                });
            }
            registrations.add(registrationBuilder.build());

//            ExternalIdentityProviderConfiguration provider = new ExternalIdentityProviderConfiguration();
//            provider.setAlias(providerType.getAlias())
//                    .setSkipSslValidation(Boolean.TRUE.equals(providerType.isSkipSslValidation()))
//                    .setMetadataTrustCheck(Boolean.TRUE.equals(providerType.isMetadataTrustCheck()))
//                    .setAuthenticationRequestBinding(URI.create(providerType.getAuthenticationRequestBinding()));
//            if (StringUtils.isNotBlank(providerType.getLinkText())) {
//                provider.setLinktext(providerType.getLinkText());
//            }
//            List<String> verificationKeys = new ArrayList<String>();
//            for (ProtectedStringType verificationKeyProtected : providerType.getVerificationKeys()) {
//                try {
//                    String verificationKey = protector.decryptString(verificationKeyProtected);
//                    verificationKeys.add(verificationKey);
//                } catch (EncryptionException e) {
//                    LOGGER.error("Couldn't obtain clear string for provider verification key");
//                }
//            }
//            if (verificationKeys != null && !verificationKeys.isEmpty()) {
//                provider.setVerificationKeys(verificationKeys);
//            }
//            try {
//                provider.setMetadata(createMetadata(providerType.getMetadata(), true));
//            } catch (Exception e) {
//                LOGGER.error("Couldn't obtain metadata as string from " + providerType.getMetadata());
//            }
//            providers.add(provider);

            configuration.additionalConfiguration.put(providerType.getEntityId(), additionalConfigBuilder.build());
        }

        InMemoryRelyingPartyRegistrationRepository relyingPartyRegistrationRepository = new InMemoryRelyingPartyRegistrationRepository(registrations);
        configuration.setRelyingPartyRegistrationRepository(relyingPartyRegistrationRepository);
        return configuration;
    }

    private static RelyingPartyRegistration.Builder getRelyingPartyFromMetadata(
            Saml2ProviderMetadataAuthenticationModuleType metadata, SamlMidpointAdditionalConfiguration.Builder additionalConfigurationBuilder) {
        RelyingPartyRegistration.Builder builder = RelyingPartyRegistration.withRegistrationId("builder");
        if (metadata != null) {
            if (metadata.getXml() != null || metadata.getPathToFile() != null) {
                String metadataAsString = null;
                try {
                    metadataAsString = createMetadata(metadata, true);
                } catch (IOException e) {
                    LOGGER.error("Couldn't obtain metadata as string from " + metadata);
                }
                builder = assertingPartyMetadataConverter.convert(new ByteArrayInputStream(metadataAsString.getBytes()), additionalConfigurationBuilder);
            }
            if (metadata.getMetadataUrl() != null) {
                try (InputStream source = resourceLoader.getResource(metadata.getMetadataUrl()).getInputStream()) {
                    builder = assertingPartyMetadataConverter.convert(source, additionalConfigurationBuilder);
                }
                catch (IOException ex) {
                    if (ex.getCause() instanceof Saml2Exception) {
                        throw (Saml2Exception) ex.getCause();
                    }
                    throw new Saml2Exception(ex);
                }
            }
        }
        return builder;
    }

    private static String createMetadata(Saml2ProviderMetadataAuthenticationModuleType metadata, boolean required) throws IOException {
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
        if (required) {
            throw new IllegalArgumentException("Metadata is not present");
        }
        return null;
    }

    private static String readFile(String path) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded);
    }

    private static String getBasePath(HttpServletRequest request) {
        boolean includePort = true;
        if (443 == request.getServerPort() && "https".equals(request.getScheme())) {
            includePort = false;
        } else if (80 == request.getServerPort() && "http".equals(request.getScheme())) {
            includePort = false;
        }
        return request.getScheme() +
                "://" +
                request.getServerName() +
                (includePort ? (":" + request.getServerPort()) : "") +
                request.getContextPath();
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
