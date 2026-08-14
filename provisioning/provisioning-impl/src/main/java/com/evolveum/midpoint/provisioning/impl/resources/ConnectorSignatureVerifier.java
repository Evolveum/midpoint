/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.subscription.SubscriptionState;
import com.evolveum.midpoint.repo.common.subscription.SubscriptionStateCache;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Verifies whether currently used connector is in the list of (allowed) open source connectors.
 * This check is applied in production environments without an active subscription.
 *
 * The check is done according to objects of type {@link AllowedConnectorsListType} containing signed records
 * of allowed connectors, including checking that the signature is valid with regard to at least one trusted public key.
 *
 * Newly discovered connectors are temporarily exempt from verification during the configured grace period.
 *
 * See (internal) document "Import and Verification of Allowed Connectors in midPoint".
 */
@Component
public class ConnectorSignatureVerifier {

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorSignatureVerifier.class);

    static final long GRACE_PERIOD_FOR_CONNECTOR_IN_DAYS = 90;

    /** Clocks within the cluster can be a little off - by this value. */
    private static final long MARGIN_FOR_CLUSTERWIDE_TIME_SYNC_MILLIS = 3000L;

    private static final Map<String, String> TRUSTED_CERTIFICATES_PATHS = new HashMap<>();

    static {
        TRUSTED_CERTIFICATES_PATHS.put("key-2026-01", "integration-catalog-2026-01.crt");
    }

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
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
                LoggingUtils.logUnexpectedException(
                        LOGGER, "Couldn't load public key from certificate '{}' from resources with path '/certs/{}'",
                        e, keyId.getKey(), keyId.getValue());
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
     * @param result operation result
     *
     * @throws SubscriptionComplianceException if the connector cannot be used because
     * of a failed signature verification or an invalid discovery timestamp
     * @throws SchemaException if repository access fails
     */
    void verifyConnectorInProduction(ConnectorType connectorBean, OperationResult result)
            throws SubscriptionComplianceException, SchemaException {
        String connectorTypeName = connectorBean.getConnectorType();
        String connectorBundleName = connectorBean.getConnectorBundle();
        String connectorVersion = connectorBean.getConnectorVersion();
        LOGGER.debug(
                "Production environment detected. Verifying connector '{}' version '{}' from bundle '{}' against "
                        + "the list of allowed open source connectors.",
                connectorTypeName, connectorVersion, connectorBundleName);

        var publicKeys = getPublicKeys();
        if (publicKeys.isEmpty()) {
            throw createSubscriptionComplianceException(
                    "Couldn't check the connector '%s' against the public list of open source connectors: "
                            + "Unable to find any public key for verification of signatures.",
                    "ConnectorSignatureVerifier.unableToFindAnyPublicKey",
                    connectorTypeName);
        }

        if (isConnectorInGracePeriod(connectorBean)) {
            LOGGER.trace("Connector is in grace period, not checking the signature");
            return;
        }

        @NotNull SearchResultList<PrismObject<AllowedConnectorsListType>> allowedConnectorsListObjects =
                repositoryService.searchObjects(AllowedConnectorsListType.class, null, null, result);

        if (allowedConnectorsListObjects.isEmpty()) {
            LOGGER.error("Couldn't check connector against the public list of open source connectors, as no list"
                    + " is present in the repository. Please import one from the Integration catalog.");
        }

        // There may be more "allowed connector list" objects. We are OK if the connector is present in at least one of them.
        boolean found = false;
        main: for (PrismObject<AllowedConnectorsListType> allowedConnectorsListObject : allowedConnectorsListObjects) {
            List<SignedConnectorType> matchingSignedConnectorBeans =
                    allowedConnectorsListObject.asObjectable().getSignedConnector().stream()
                            .filter(signedAllowedConnectorBean -> {
                                ConnectorIdentifierType allowedConnectorBean = signedAllowedConnectorBean.getConnector();
                                // We assume the list is well-formed i.e. className and bundle are both present
                                return allowedConnectorBean.getClassName().equals(connectorTypeName)
                                        && allowedConnectorBean.getBundle().equals(connectorBundleName);
                            }).toList();

            LOGGER.trace("Found {} matching signed allowed connector records in {}",
                    matchingSignedConnectorBeans.size(), allowedConnectorsListObject);

            for (SignedConnectorType signedAllowedConnectorBean : matchingSignedConnectorBeans) {

                ConnectorIdentifierType allowedConnectorBean = signedAllowedConnectorBean.getConnector();

                byte[] payload;
                try {
                    payload = toJson(allowedConnectorBean);
                } catch (JsonProcessingException e) {
                    throw createSubscriptionComplianceException(
                            "Unable to create JSON payload for verifying the connector signature for connector '%s' from bundle '%s'.",
                            "ConnectorSignatureVerifier.unableToCreateJson",
                            allowedConnectorBean.getClassName(), allowedConnectorBean.getBundle());
                }

                // We have potentially multiple signatures and potentially multiple public keys in the system.
                // We try to find at least one match.

                for (ConnectorSignatureType signatureBean : signedAllowedConnectorBean.getSignature()) {

                    var keyIdInSignature = signatureBean.getKeyId();
                    LOGGER.trace("Trying to check the signature using key ID '{}'", keyIdInSignature);
                    PublicKey publicKey = publicKeys.get(keyIdInSignature);
                    if (publicKey == null) {
                        // This can happen e.g. if Integration Catalog gets a new key but existing midPoint deployment does
                        // not know about it. Hence, DEBUG level is OK to avoid cluttering the logs.
                        LOGGER.debug("Ignoring unknown public key '{}' in the signature for connector record: {}",
                                keyIdInSignature, allowedConnectorBean);
                        continue;
                    }

                    try {
                        Signature verifier = Signature.getInstance("Ed25519");
                        verifier.initVerify(publicKey);
                        verifier.update(payload);

                        if (verifier.verify(signatureBean.getValue())) {
                            LOGGER.trace("Successfully verified the signature for {}: {}", allowedConnectorBean, signatureBean);
                            found = true;
                            break main;
                        } else {
                            LOGGER.error(
                                    "Couldn't verify the signature in 'open source connector record' using public key ID '{}' "
                                            + "(verifier returned 'false'), will try other signatures for the connector, "
                                            + "if there are any.\n{}",
                                    keyIdInSignature, signedAllowedConnectorBean);
                            // Do not update the operation result. If the operation succeeds, we're OK. If not, the (overall)
                            // "not found" exception will be reported.
                        }
                    } catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException e) {
                        LoggingUtils.logUnexpectedException(
                                LOGGER,
                                "Couldn't verify the signature in 'open source connector record' using public key ID '{}', "
                                        + "will try other signatures for the connector, if there are any.\n{}",
                                e, keyIdInSignature, signedAllowedConnectorBean);
                        // Also here we don't update the operation result.
                    }
                }
            }
        }

        if (!found) {
            // Details are to be found in the log.
            throw createSubscriptionComplianceException(
                    "Connector '%s' version '%s' from bundle '%s' is not present in the current public list of open source connectors.",
                    "ConnectorSignatureVerifier.isNotPresent",
                    connectorTypeName, connectorVersion, connectorBundleName);
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
        long discoveryTimestamp;
        if (connectorBean.getDiscoveryTimestamp() == null || connectorBean.getDiscoveryTimestamp().isEmpty()) {
            throw createSubscriptionComplianceException(
                    "Discovery timestamp for the connector '%s' is empty.",
                    "ConnectorSignatureVerifier.discoverTimestampIsEmpty",
                    connectorBean.getName());
        } else {
            ProtectedStringType discoveryTimestampPS = connectorBean.getDiscoveryTimestamp();
            try {
                discoveryTimestamp = Long.parseLong(protector.decryptString(discoveryTimestampPS));
            } catch (EncryptionException e) {
                throw createSubscriptionComplianceException(
                        "Couldn't decrypt discovery timestamp of the connector '%s'.",
                        "ConnectorSignatureVerifier.couldntDecryptDiscoveryTimestamp",
                        connectorBean.getName());
            }
        }

        if (discoveryTimestamp > Instant.now().toEpochMilli() + MARGIN_FOR_CLUSTERWIDE_TIME_SYNC_MILLIS) {
            throw createSubscriptionComplianceException(
                    "A discovery timestamp of the connector '%s' is in the future.",
                    "ConnectorSignatureVerifier.discoverTimestampIsInFuture",
                    connectorBean.getName());
        }

        long gracePeriodEndTimestamp = Instant.ofEpochMilli(discoveryTimestamp)
                .plus(GRACE_PERIOD_FOR_CONNECTOR_IN_DAYS, ChronoUnit.DAYS)
                .toEpochMilli();

        return gracePeriodEndTimestamp >= Instant.now().toEpochMilli();
    }

    private SubscriptionState getSubscriptionState() {
        return subscriptionStateCache.getSubscriptionState();
    }

    protected byte[] toJson(ConnectorIdentifierType connectorIdentifierType) throws JsonProcessingException {
        ActiveConnectorDto dto = new ActiveConnectorDto(
                connectorIdentifierType.getClassName(),
                connectorIdentifierType.getBundle());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsBytes(dto);
    }

    private SubscriptionComplianceException createSubscriptionComplianceException(String baseOfTechMessage, String key, Object... objects) {
        String technicalMessage = baseOfTechMessage.formatted(objects);
        return new SubscriptionComplianceException(new SingleLocalizableMessage(key, objects, technicalMessage));
    }

    private record ActiveConnectorDto(
            String className,
            String bundle
    ) {
    }
}
