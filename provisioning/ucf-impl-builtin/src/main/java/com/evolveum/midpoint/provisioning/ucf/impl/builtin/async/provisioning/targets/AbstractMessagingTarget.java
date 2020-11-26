/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.targets;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningTarget;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning.AsyncProvisioningConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncProvisioningTargetType;

/**
 * Abstract superclass for various messaging-based targets.
 *
 * Takes care mainly of graceful closing of the broker connection.
 * (Does also other mundane tasks like handling the operation result and exceptions.)
 *
 * @param <C> type of the connector configuration
 */
public abstract class AbstractMessagingTarget<C extends AsyncProvisioningTargetType> implements AsyncProvisioningTarget {

    /** Configuration of this target */
    @NotNull final C configuration;

    /** Reference to the owning connector instance */
    @NotNull final AsyncProvisioningConnectorInstance connectorInstance;

    protected AbstractMessagingTarget(@NotNull C configuration, @NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        this.configuration = configuration;
        this.connectorInstance = connectorInstance;
        validate();
    }

    /**
     * Validates the configuration. Because in some cases the connection is created lazily, it is advisable to validate
     * the config as soon as possible.
     */
    protected abstract void validate();

    /**
     * How many operations are in progress? We can close the broker connection only if there's none.
     * Guarded by "this".
     */
    private int operationsInProgress;

    /**
     * True if we are closing (disconnecting) i.e. the {@link #disconnect()} method has been called.
     * Once true, never returns to false.
     * Guarded by "this".
     */
    private boolean closing;

    /**
     * True if we are closed.
     * Once true, never returns to false.
     * Guarded by "this".
     */
    private boolean closed;

    @Override
    public void connect() {
        // nothing here - we create connection lazily
    }

    @Override
    public void disconnect() {
        synchronized (this) {
            closing = true;
            closeBrokerConnectionIfPossible();
        }
    }

    @Override
    public void test(OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(getClass().getName() + ".test");
        result.addParam("targetName", configuration.getName());
        onOperationStart();
        try {
            executeTest();
        } catch (RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        } catch (Exception e) {
            result.recordFatalError(e);
            throw new SystemException("Couldn't test the target connection: " + e.getMessage(), e);
        } finally {
            onOperationEnd();
            result.computeStatusIfUnknown();
        }
    }

    protected abstract void executeTest() throws Exception;

    @Override
    public String send(AsyncProvisioningRequest request, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(getClass().getName() + ".send");
        result.addParam("targetName", configuration.getName());
        onOperationStart();
        try {
            return executeSend(request);
        } catch (RuntimeException | Error e) {
            result.recordFatalError(e);
            throw e;
        } catch (Exception e) {
            result.recordFatalError(e);
            throw new SystemException("Couldn't send the message: " + e.getMessage(), e);
        } finally {
            onOperationEnd();
            result.computeStatusIfUnknown();
        }
    }

    protected abstract String executeSend(AsyncProvisioningRequest request) throws Exception;

    private synchronized void onOperationStart() {
        if (closing || closed) {
            throw new IllegalStateException("The target is closing or closed");
        }
        operationsInProgress++;
    }

    private synchronized void onOperationEnd() {
        operationsInProgress--;
        if (closing) {
            closeBrokerConnectionIfPossible();
        }
    }

    private synchronized void closeBrokerConnectionIfPossible() {
        if (operationsInProgress == 0) {
            closeBrokerConnection();
            closed = true;
        }
    }

    /**
     * Closes broker connection. We assume there are no operations in progress when called.
     */
    protected abstract void closeBrokerConnection();

    String decrypt(ProtectedStringType protectedString) throws EncryptionException {
        if (protectedString != null) {
            Protector protector = connectorInstance.getPrismContext().getDefaultProtector();
            return protector.decryptString(protectedString);
        } else {
            return null;
        }
    }
}
