/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.subscription.SubscriptionState;
import com.evolveum.midpoint.repo.common.subscription.SubscriptionStateCache;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Strings;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Verifies connector signatures in production environments without an active
 * subscription.
 *
 * The verifier validates that a connector is present in the configured list of
 * allowed signed connectors and that its signature matches one of the trusted
 * public keys. Newly discovered connectors are temporarily exempt from
 * verification during the configured grace period.
 */
@Component
public class ConnectorSignatureVerifier {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorSignatureVerifier.class);

    //grace period for connector in days
    static final long GRACE_PERIOD_FOR_CONNECTOR = 90;

    private static final Map<String, String> TRUSTED_CERTIFICATES_PATHS = new HashMap<>();

    static {
        TRUSTED_CERTIFICATES_PATHS.put("key-2026-01", "integration-catalog-2026-01.crt");
    }

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private SubscriptionStateCache subscriptionStateCache;
    @Autowired private Protector protector;

    private final Map<String, PublicKey> publicKeys;

    public ConnectorSignatureVerifier() {
        this.publicKeys = loadPublicKeys();
    }

    private Map<String, PublicKey> loadPublicKeys() {
        Map<String, PublicKey> publicKeys = new HashMap<>();
        for (Map.Entry<String, String> keyId : TRUSTED_CERTIFICATES_PATHS.entrySet()) {

            ClassPathResource resource =
                    new ClassPathResource("certs/" + keyId.getValue());

            try (InputStream is = resource.getInputStream()) {

                CertificateFactory factory = CertificateFactory.getInstance("X.509");

                X509Certificate certificate =
                        (X509Certificate) factory.generateCertificate(is);

                PublicKey publicKey = certificate.getPublicKey();
                publicKeys.put(keyId.getKey(), publicKey);
            } catch (CertificateException | IOException e) {
                LOGGER.error("Couldn't load public key from certificate '%s' from resources with path '/certs/%s'".formatted(keyId.getKey(), keyId.getValue()), e);
            }
        }
        return publicKeys;
    }

    /**
     * Verifies that the specified connector is allowed to be used in a production
     * environment without an active subscription.
     *
     * The connector must either be within the grace period after discovery or have
     * a valid signature matching one of the configured trusted public keys.
     *
     * @param connectorBean connector to verify
     * @param connOid connector OID
     * @param result operation result
     * @throws SubscriptionComplianceException if the connector cannot be used because
     * of a failed signature verification or an invalid discovery timestamp
     * @throws SchemaException if repository access fails
     */
    public void verifyConnectorInProduction(ConnectorType connectorBean, String connOid, OperationResult result)
            throws SubscriptionComplianceException, SchemaException {
        LOGGER.debug(
                "Production environment detected. Verifying connector '{}' version '{}' from bundle '{}' against the list of allowed signed connectors.",
                connectorBean.getNamespace(),
                connectorBean.getVersion(),
                connectorBean.getConnectorBundle());

        if (publicKeys.isEmpty()) {
            throw new SubscriptionComplianceException("Unable to find any public key for verification connector signature.");
        }

        if (isConnectorInGracePeriod(connectorBean)) {
            return;
        }

        boolean found = false;
        @NotNull SearchResultList<PrismObject<AllowedConnectorsListType>> allowedConnectorsLists =
                repositoryService.searchObjects(AllowedConnectorsListType.class, null, null, result);
        for (PrismObject<AllowedConnectorsListType> allowedConnectorsList : allowedConnectorsLists) {
            if (found) {
                break;
            }
            List<SignedConnectorType> matchedContainers = allowedConnectorsList.asObjectable().getSignedConnector().stream()
                    .filter(signedConnectorBean -> {
                        ConnectorIdentifierType allowedConnectorBean = signedConnectorBean.getConnector();
                        return allowedConnectorBean.getClassName().equals(connectorBean.getConnectorType())
                                && allowedConnectorBean.getBundle().equals(connectorBean.getConnectorBundle());
                    }).toList();

            if (matchedContainers.isEmpty()) {
                throw new SubscriptionComplianceException("No signature found for connector '%s' version '%s' from bundle '%s'."
                        .formatted(connectorBean.getConnectorType(), connectorBean.getVersion(), connectorBean.getConnectorBundle()));
            }

            for (SignedConnectorType signedConnectorBean : matchedContainers) {

                if (found) {
                    break;
                }

                ConnectorIdentifierType allowedConnectorBean = signedConnectorBean.getConnector();

                byte[] payload;
                try {
                    payload = toJson(allowedConnectorBean);
                } catch (JsonProcessingException e) {
                    throw new SubscriptionComplianceException("Unable to create json payload for verifying the connector signature for connector '%s' version '%s' from bundle '%s'."
                            .formatted(allowedConnectorBean.getClassName(), allowedConnectorBean.getVersion(), allowedConnectorBean.getBundle()), e);
                }

                for (Map.Entry<String, PublicKey> publicKey : getPublicKeys().entrySet()) {

                    try {
                        Signature verifier = Signature.getInstance("Ed25519");
                        verifier.initVerify(publicKey.getValue());
                        verifier.update(payload);

                        ConnectorSignatureType signatureBean = signedConnectorBean.getSignature().stream()
                                .filter(signature -> Strings.CS.equals(signature.getKeyId(), publicKey.getKey()))
                                .findFirst()
                                .orElse(null);

                        if (signatureBean == null) {
                            throw new InvalidKeyException("No signature found for keyId '%s'".formatted(publicKey.getKey()));
                        }

                        found = verifier.verify(Base64.getUrlDecoder().decode(signatureBean.getValue()));
                    } catch (InvalidKeyException | NoSuchAlgorithmException |
                            SignatureException e) {
                        throw new SubscriptionComplianceException("Unable to verify the connector signature for connector '%s' version '%s' from bundle '%s'."
                                .formatted(connectorBean.getConnectorType(), connectorBean.getVersion(), connectorBean.getConnectorBundle()), e);
                    }
                }
            }
        }

        if (!found) {
            throw new SubscriptionComplianceException("Connector '%s' version '%s' from bundle '%s' is not present in the current list of allowed signed connectors."
                    .formatted(connectorBean.getConnectorType(), connectorBean.getVersion(), connectorBean.getConnectorBundle()));
        }
    }

    protected Map<String, PublicKey> getPublicKeys() {
        return publicKeys;
    }

    /**
     * Determines whether signature verification should be performed for the
     * specified connector.
     *
     * Verification is required only for ConnId connectors running in a production
     * environment without an active subscription.
     *
     * @param connectorBean connector to evaluate
     * @return {@code true} if signature verification is required
     */
    public boolean isVerificationNeeded(ConnectorType connectorBean) {
        return isConnIdConnector(connectorBean) && isInProductionWithoutSubscription();
    }

    private boolean isInProductionWithoutSubscription() {
        return getSubscriptionState().isProductionEnvironment() && !getSubscriptionState().isActive();
    }

    protected boolean isConnIdConnector(ConnectorType connectorBean) {
        return SchemaConstants.ICF_FRAMEWORK_URI.equals(connectorBean.getFramework());
    }

    private boolean isConnectorInGracePeriod(ConnectorType connectorBean) throws SubscriptionComplianceException {
        Long discoverTimestamp;
        if (connectorBean.getDiscoveryTimestamp() == null || connectorBean.getDiscoveryTimestamp().isEmpty()) {
            throw new SubscriptionComplianceException("Discovery timestamp for the connector '%s' is empty."
                    .formatted(connectorBean.getName()));
        } else {
            ProtectedStringType discoverTimestampBean = connectorBean.getDiscoveryTimestamp();
            try {
                discoverTimestamp = Long.valueOf(protector.decryptString(discoverTimestampBean));
            } catch (EncryptionException e) {
                throw new SubscriptionComplianceException("Couldn't encrypt discovery timestamp of the connector '%s'."
                        .formatted(connectorBean.getName()));
            }
        }

        if (discoverTimestamp > Instant.now().toEpochMilli()) {
            throw new SubscriptionComplianceException("A discovery timestamp of the connector '%s' is in the future."
                    .formatted(connectorBean.getName()));
        }

        long gracePeriodTimestamp = Instant.ofEpochMilli(discoverTimestamp)
                .plus(GRACE_PERIOD_FOR_CONNECTOR, ChronoUnit.DAYS)
                .toEpochMilli();

        return gracePeriodTimestamp >= Instant.now().toEpochMilli();
    }

    private SubscriptionState getSubscriptionState() {
        return subscriptionStateCache.getSubscriptionState();
    }

    protected byte[] toJson(ConnectorIdentifierType connectorIdentifierType) throws JsonProcessingException {
        ActiveConnectorDto dto = new ActiveConnectorDto(
                connectorIdentifierType.getClassName(),
                connectorIdentifierType.getVersion(),
                connectorIdentifierType.getBundle());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsBytes(dto);
    }

    private record ActiveConnectorDto(
            String className,
            String version,
            String bundle
    ) {
    }
}
