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
import com.evolveum.midpoint.util.exception.RestrictedObjectException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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
     * Verifies that a connector with a valid signature can be loaded successfully.
     */
    @Test
    public void test100Success() throws Exception {
        basicTest(
                (connector, lastConnectorOid) -> {
                    lastConnectorOid.set(connector.getOid());
                    return true;
                },
                processSuccess(),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector without a signature is rejected after the grace
     * period has expired.
     */
    @Test
    public void test110FailVerification() throws Exception {
        basicTest(
                (connector, lastConnectorOid) -> {
                    lastConnectorOid.set(connector.getOid());
                    finishGracePeriod(connector);
                    return false;
                },
                checkExceptionWithPrefix("No signature found for connector"),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector signed with an unknown key is rejected after the
     * grace period has expired.
     */
    @Test
    public void test120WrongKeyId() throws Exception {
        basicTest(
                (connector, lastConnectorOid) -> {
                    lastConnectorOid.set(connector.getOid());
                    finishGracePeriod(connector);
                    return true;
                },
                checkExceptionWithPrefix("Unable to verify the connector signature for connector"),
                "wrongKeyId"
        );
    }

    /**
     * Verifies that a connector with an invalid signature is rejected after the
     * grace period has expired.
     */
    @Test
    public void test100WrongSignature() throws Exception {
        basicTest(
                (connector, lastConnectorOid) -> {
                    if (!StringUtils.isEmpty(lastConnectorOid.get())) {
                        mockConnectorSignatureVerifier.refreshKeyPair();
                        return false;
                    }
                    finishGracePeriod(connector);
                    lastConnectorOid.set(connector.getOid());
                    return true;
                },
                checkExceptionWithSuffix(" is not present in the current list of allowed signed connectors."),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that connector verification fails when the discovery timestamp is
     * missing.
     */
    @Test
    public void test100UndefinedGracePeriod() throws Exception {
        basicTest(
                (connector, lastConnectorOid) -> {
                    if (StringUtils.isEmpty(lastConnectorOid.get())) {
                        lastConnectorOid.set(connector.getOid());
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
                    }
                    return true;
                },
                checkExceptionWithPrefix("Discovery timestamp for the connector "),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector with a discovery timestamp in the future is
     * rejected.
     */
    @Test
    public void test100GracePeriodInFuture() throws Exception {
        basicTest(
                (connector, lastConnectorOid) -> {
                    if (StringUtils.isEmpty(lastConnectorOid.get())) {
                        lastConnectorOid.set(connector.getOid());
                        replaceGracePeriod(Instant.now().plus(10, ChronoUnit.DAYS), connector);
                    }
                    return true;
                },
                checkExceptionWithPrefix("A discovery timestamp of the connector "),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    /**
     * Verifies that a connector can be loaded while it is still within the grace..
     */
    @Test
    public void test100InGracePeriod() throws Exception {
        basicTest(
                (connector, lastConnectorOid) -> {
                    if (StringUtils.isEmpty(lastConnectorOid.get())) {
                        lastConnectorOid.set(connector.getOid());
                        replaceGracePeriod(
                                Instant.now().minus(ConnectorSignatureVerifier.GRACE_PERIOD_FOR_CONNECTOR - 1, ChronoUnit.DAYS),
                                connector);
                        return false;
                    }
                    return true;
                },
                processSuccess(),
                mockConnectorSignatureVerifier.getKeyId()
        );
    }

    private void finishGracePeriod(ConnectorType connector) {
        replaceGracePeriod(
                Instant.now().minus(ConnectorSignatureVerifier.GRACE_PERIOD_FOR_CONNECTOR + 10, ChronoUnit.DAYS),
                connector);
    }

    private void replaceGracePeriod(Instant gracePeriod, ConnectorType connector) {
        try {
            long discoverTimestamp = gracePeriod.toEpochMilli();
            ProtectedStringType discoverTimestampBean = new ProtectedStringType()
                    .clearValue(
                            String.valueOf(discoverTimestamp));
            protector.encrypt(discoverTimestampBean);

            repositoryService.modifyObject(ConnectorType.class, connector.getOid(),
                    prismContext.deltaFor(ConnectorType.class)
                            .item(ConnectorType.F_DISCOVERY_TIMESTAMP)
                            .replace(discoverTimestampBean)
                            .asItemDeltas(), createOperationResult());
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException | EncryptionException e) {
            throw new RuntimeException(e);
        }
    }

    private @NonNull BiConsumer<AtomicReference<String>, OperationResult> processSuccess() {
        return (lastConnectorOid, result) -> {
            try {
                connectorManager.getUnconfiguredConnectorInstance(lastConnectorOid.get(), result);
            } catch (ObjectNotFoundException | SchemaException | RestrictedObjectException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private @NonNull BiConsumer<AtomicReference<String>, OperationResult> checkExceptionWithSuffix(String prefix) {
        return (lastConnectorOid, result) -> {
            RestrictedObjectException exception = checkException(lastConnectorOid, result);

            assertTrue(exception.getMessage().endsWith(prefix));
        };
    }

    private @NonNull BiConsumer<AtomicReference<String>, OperationResult> checkExceptionWithPrefix(String prefix) {
        return (lastConnectorOid, result) -> {
            RestrictedObjectException exception = checkException(lastConnectorOid, result);

            assertTrue(exception.getMessage().startsWith(prefix));
        };
    }

    private RestrictedObjectException checkException(AtomicReference<String> lastConnectorOid, OperationResult result) {
        return expectThrows(
                RestrictedObjectException.class,
                () -> connectorManager.getUnconfiguredConnectorInstance(lastConnectorOid.get(), result));
    }

    private void basicTest(
            BiFunction<ConnectorType, AtomicReference<String>, Boolean> searchFunction,
            BiConsumer<AtomicReference<String>, OperationResult> operationConsumer,
            String keyId)
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        OperationResult result = createOperationResult();
        String nameOfAllowedConnectorList = "test100Success-AllowedConnectorList";

        when();
        AllowedConnectorsListType allowedConnectorsListType = new AllowedConnectorsListType();
        allowedConnectorsListType.name(nameOfAllowedConnectorList);
        AtomicReference<String> lastConnectorOid = new AtomicReference<>();
        repositoryService.searchObjectsIterative(
                ConnectorType.class,
                null,
                (connector, handlerResult) -> {
                    ConnectorType connectorBean = connector.asObjectable();
                    if (!SchemaConstants.ICF_FRAMEWORK_URI.equals(connectorBean.getFramework())) {
                        return true;
                    }

                    boolean canContinue = searchFunction.apply(connectorBean, lastConnectorOid);
                    if (!canContinue) {
                        return false;
                    }

                    return createAllowedConnectorList(
                            connectorBean, allowedConnectorsListType, keyId);
                }
                ,
                null,
                true,
                result);
        repoAddObject(allowedConnectorsListType.asPrismObject(), result);

        then();
        try {
            operationConsumer.accept(lastConnectorOid, result);
        } finally {
            connectorManager.invalidate(null, null, null);
            clearAllowedConnectorsList(result);
        }
    }

    private boolean createAllowedConnectorList(ConnectorType connectorBean, AllowedConnectorsListType allowedConnectorsListType, String keyId) {
        ConnectorIdentifierType connectorIdentifier = new ConnectorIdentifierType()
                .className(connectorBean.getConnectorType())
                .bundle(connectorBean.getConnectorBundle());
        try {
            allowedConnectorsListType.signedConnector(
                    new SignedConnectorType()
                            .connector(connectorIdentifier)
                            .signature(new ConnectorSignatureType()
                                    .keyId(keyId)
                                    .value(mockConnectorSignatureVerifier.sign(connectorIdentifier))));
        } catch (JsonProcessingException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        return true;
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
