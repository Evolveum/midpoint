/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;

import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SubscriptionComplianceException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.testng.Assert.*;

/**
 * Integration tests for {@link ConnectorSignatureVerifier}.
 *
 * Verifies connector signature validation and grace period for connectors in production mode.
 */
@ContextConfiguration(locations = "classpath:ctx-connector-signature-test.xml")
@DirtiesContext(classMode = AFTER_CLASS)
public class TestConnectorSignatureVerifier extends AbstractIntegrationTest {

    @Autowired
    MockConnectorSignatureVerifier mockConnectorSignatureVerifier;

    @Autowired
    private ConnectorManager connectorManager;

    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired
    protected ProvisioningService provisioningService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        provisioningService.postInit(initResult);
    }

    /**
     * Verifies that a connector with a valid signature can be loaded successfully after the grace period has expired.
     */
    @Test
    public void test100SuccessWithValidSignatureAfterGrace() throws Exception {
        basicTest(
                (connector, lastConnectorOidRef) -> {
                    lastConnectorOidRef.set(connector.getOid());
                    setDiscoveryTimestampToDistantPast(connector);
                    return true; // will be added to the allowed list
                },
                useConnectorAssertSuccess(),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector without an entry in the list is rejected after the grace period has expired.
     */
    @Test
    public void test110FailureDueToMissingEntry() throws Exception {
        basicTest(
                (connector, lastConnectorOidRef) -> {
                    lastConnectorOidRef.set(connector.getOid());
                    setDiscoveryTimestampToDistantPast(connector);
                    return false; // won't be added to the allowed list
                },
                useConnectorAssertExceptionWithStandardSuffix(),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector signed with an unknown key is rejected after the grace period has expired.
     */
    @Test
    public void test120FailureDueToWrongKeyId() throws Exception {
        basicTest(
                (connector, lastConnectorOidRef) -> {
                    lastConnectorOidRef.set(connector.getOid());
                    setDiscoveryTimestampToDistantPast(connector);
                    return true; // will be added to allowed list
                },
                useConnectorAssertExceptionWithStandardSuffix(),
                "wrongKeyId"
        );
    }

    /**
     * Verifies that a connector with an invalid signature is rejected after the grace period has expired.
     */
    @Test
    public void test130FailureDueToWrongSignature() throws Exception {
        basicTest(
                (connector, lastConnectorOidRef) -> {
                    if (lastConnectorOidRef.get() == null) {
                        // First run
                        setDiscoveryTimestampToDistantPast(connector);
                        lastConnectorOidRef.set(connector.getOid());
                        return true;
                    } else {
                        // Second run - we just invalidate the key and exit (not updating lastConnectorOidRef)
                        mockConnectorSignatureVerifier.refreshKeyPair();
                        return false;
                    }
                },
                useConnectorAssertExceptionWithStandardSuffix(),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that connector verification fails when the discovery timestamp is missing.
     */
    @Test
    public void test140FailureDueToMissingDiscoveryTimestamp() throws Exception {
        basicTest(
                (connector, lastConnectorOidRef) -> {
                    if (lastConnectorOidRef.get() == null) {
                        // first run
                        lastConnectorOidRef.set(connector.getOid());
                        try {
                            repositoryService.modifyObject(
                                    ConnectorType.class,
                                    connector.getOid(),
                                    prismContext.deltaFor(ConnectorType.class)
                                            .item(ConnectorType.F_DISCOVERY_TIMESTAMP)
                                            .delete(connector.getDiscoveryTimestamp())
                                            .asItemDeltas(),
                                    createOperationResult());
                        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
                            throw new RuntimeException(e);
                        }
                        return true; // add to allowed list
                    } else {
                        // second run - just exit
                        return false;
                    }
                },
                useConnectorAssertExceptionWithPrefix("Discovery timestamp for the connector "),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector with a discovery timestamp in the future is rejected.
     */
    @Test
    public void test150FailureDueToDiscoveryTimestampInTheFuture() throws Exception {
        basicTest(
                (connector, lastConnectorOidRef) -> {
                    if (lastConnectorOidRef.get() == null) {
                        // first run
                        lastConnectorOidRef.set(connector.getOid());
                        replaceDiscoveryTimestamp(Instant.now().plus(10, ChronoUnit.DAYS), connector);
                        return true; // add to allowed list
                    } else {
                        // second run - just exit
                        return false;
                    }
                },
                useConnectorAssertExceptionWithPrefix("A discovery timestamp of the connector "),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector can be loaded while it is still within the grace period (even if not in the list).
     */
    @Test
    public void test160SuccessWithinGracePeriod() throws Exception {
        basicTest(
                (connector, lastConnectorOidRef) -> {
                    lastConnectorOidRef.set(connector.getOid());
                    setDiscoveryTimestampToNearPast(connector);
                    return false;
                },
                useConnectorAssertSuccess(),
                "just-any-key"
        );
    }

    /** We set discovery timestamp so that now we're in the grace period. */
    private void setDiscoveryTimestampToNearPast(ConnectorType connector) {
        replaceDiscoveryTimestamp(
                Instant.now().minus(ConnectorSignatureVerifier.GRACE_PERIOD_FOR_CONNECTOR_IN_DAYS - 1, ChronoUnit.DAYS),
                connector);
    }

    /** We set discovery timestamp so that now we're after grace period. */
    private void setDiscoveryTimestampToDistantPast(ConnectorType connector) {
        replaceDiscoveryTimestamp(
                Instant.now().minus(ConnectorSignatureVerifier.GRACE_PERIOD_FOR_CONNECTOR_IN_DAYS + 10, ChronoUnit.DAYS),
                connector);
    }

    private void replaceDiscoveryTimestamp(Instant discoveryInstant, ConnectorType connector) {
        try {
            long discoveryTimestamp = discoveryInstant.toEpochMilli();
            ProtectedStringType discoveryTimestampBean = new ProtectedStringType()
                    .clearValue(
                            String.valueOf(discoveryTimestamp));
            protector.encrypt(discoveryTimestampBean);

            repositoryService.modifyObject(ConnectorType.class, connector.getOid(),
                    prismContext.deltaFor(ConnectorType.class)
                            .item(ConnectorType.F_DISCOVERY_TIMESTAMP)
                            .replace(discoveryTimestampBean)
                            .asItemDeltas(), createOperationResult());
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException | EncryptionException e) {
            throw new RuntimeException(e);
        }
    }

    /** Use connector, then assert success. */
    private @NotNull TestSpecificMethod useConnectorAssertSuccess() {
        return (lastConnectorOid, result) -> {
            try {
                connectorManager.getUnconfiguredConnectorInstance(lastConnectorOid, result);
            } catch (ObjectNotFoundException | SchemaException | SubscriptionComplianceException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /** Use connector, then assert exception (using standard suffix). */
    private @NotNull TestSpecificMethod useConnectorAssertExceptionWithStandardSuffix() {
        return (lastConnectorOid, result) -> {
            SubscriptionComplianceException exception = useConnectorAssertException(lastConnectorOid, result);
            assertExpectedException(exception)
                    .hasMessageEndingWith(" is not present in the current public list of open source connectors.");
        };
    }

    /** Use connector, then assert exception (using a prefix). */
    private @NotNull TestSpecificMethod useConnectorAssertExceptionWithPrefix(String prefix) {
        return (lastConnectorOid, result) -> {
            SubscriptionComplianceException exception = useConnectorAssertException(lastConnectorOid, result);
            assertExpectedException(exception)
                    .hasMessageStartingWith(prefix);
        };
    }

    /** Use connector, assert {@link SubscriptionComplianceException} is thrown. */
    private SubscriptionComplianceException useConnectorAssertException(String lastConnectorOid, OperationResult result) {
        return expectThrows(
                SubscriptionComplianceException.class,
                () -> connectorManager.getUnconfiguredConnectorInstance(lastConnectorOid, result));
    }

    /**
     * Executes a test.
     *
     * @param connectorFoundProcessor Code that is called for each ConnId connector in the repository.
     * @param testMethod Code that is called after connectors are processed
     * @param keyId The key ID to use for the test
     */
    private void basicTest(
            ConnectorFoundProcessor connectorFoundProcessor,
            TestSpecificMethod testMethod,
            String keyId)
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        String nameOfAllowedConnectorList = "test100Success-AllowedConnectorList";

        when("processing connectors from the repository");
        AllowedConnectorsListType allowedConnectorsListBean = new AllowedConnectorsListType();
        allowedConnectorsListBean.name(nameOfAllowedConnectorList);
        AtomicReference<String> lastConnectorOidRef = new AtomicReference<>();
        repositoryService.searchObjectsIterative(
                ConnectorType.class,
                null,
                (connector, handlerResult) -> {
                    ConnectorType connectorBean = connector.asObjectable();
                    if (!SchemaConstants.ICF_FRAMEWORK_URI.equals(connectorBean.getFramework())) {
                        return true;
                    }

                    boolean addToAllowedAndContinue =
                            connectorFoundProcessor.processConnectorFound(connectorBean, lastConnectorOidRef);
                    if (addToAllowedAndContinue) {
                        addConnectorToAllowedConnectorList(
                                connectorBean, allowedConnectorsListBean, keyId);
                        return true;
                    } else {
                        return false;
                    }
                },
                null,
                true,
                result);
        repoAddObject(allowedConnectorsListBean.asPrismObject(), result);

        then("calling test-specific method");
        try {
            testMethod.call(lastConnectorOidRef.get(), result);
        } finally {
            connectorManager.invalidate(null, null, null);
            clearAllowedConnectorsList(result);
        }
    }

    interface ConnectorFoundProcessor {
        /**
         * Test-specific processing of a ICF connector found in the repo.
         *
         * @param lastConnectorOidRef reference to store last connector OID seen; caller should set this
         * @return {@code true} if the connector should be added to "allowed list", {@code false} if not, and also to stop
         * iterating (this is mixing of two meanings of the boolean return value)
         */
        boolean processConnectorFound(ConnectorType connectorBean, AtomicReference<String> lastConnectorOidRef);
    }

    interface TestSpecificMethod {
        /**
         * Test-specific method to be called after connectors are processed.
         */
        void call(String lastConnectorOid, OperationResult result);
    }

    private void addConnectorToAllowedConnectorList(
            ConnectorType connectorBean, AllowedConnectorsListType allowedConnectorsListBean, String keyId) {
        ConnectorIdentifierType connectorIdentifier = new ConnectorIdentifierType()
                .className(connectorBean.getConnectorType())
                .bundle(connectorBean.getConnectorBundle());
        try {
            allowedConnectorsListBean.signedConnector(
                    new SignedConnectorType()
                            .connector(connectorIdentifier)
                            .signature(new ConnectorSignatureType()
                                    .keyId(keyId)
                                    .value(mockConnectorSignatureVerifier.sign(connectorIdentifier))));
        } catch (JsonProcessingException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    private void clearAllowedConnectorsList(OperationResult result) throws SchemaException {
        @NotNull SearchResultList<PrismObject<AllowedConnectorsListType>> searchResult =
                repositoryService.searchObjects(AllowedConnectorsListType.class, null, null, result);
        searchResult.forEach(
                connectorList -> {
                    try {
                        repositoryService.deleteObject(
                                AllowedConnectorsListType.class, connectorList.getOid(), result);
                    } catch (ObjectNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
