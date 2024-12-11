/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ScriptCapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter.OPERATION_MODIFY_ENTITLEMENT;

/**
 * Base class for the implementation of add, modify, "ucf modify", and delete operations.
 *
 * Contains selected commonly-used methods, e.g. for entitlement handling.
 */
abstract class ResourceObjectProvisioningOperation {

    @NotNull final ProvisioningContext ctx;
    @Nullable final OperationProvisioningScriptsType scripts;
    @Nullable final ConnectorOperationOptions connOptions;

    ResourceObjectsBeans b = ResourceObjectsBeans.get();

    ResourceObjectProvisioningOperation(
            @NotNull ProvisioningContext ctx,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ConnectorOperationOptions connOptions) {
        this.ctx = ctx;
        this.scripts = scripts;
        this.connOptions = connOptions;
    }

    void executeProvisioningScripts(
            ProvisioningOperationTypeType provisioningOperationType,
            BeforeAfterType beforeAfter,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, GenericConnectorException {
        var operations = determineExecuteScriptOperations(provisioningOperationType, beforeAfter, scripts, ctx.getResource());
        if (operations.isEmpty()) {
            return;
        }
        ctx.checkExecutionFullyPersistent();
        ConnectorInstance connector = ctx.getConnector(ScriptCapabilityType.class, result);
        for (var operation : operations) {
            var ucfCtx = new UcfExecutionContext(b.lightweightIdentifierGenerator, ctx.getResource(), ctx.getTask());

            try {
                getLogger().debug("PROVISIONING SCRIPT EXECUTION {} {} operation on resource {}",
                        beforeAfter.value(), provisioningOperationType.value(),
                        ctx.getResource());

                Object returnedValue = connector.executeScript(operation, ucfCtx, result);

                getLogger().debug("PROVISIONING SCRIPT EXECUTION {} {} successful, returned value: {}",
                        beforeAfter.value(), provisioningOperationType.value(), returnedValue);

            } catch (CommunicationException ex) {
                String message = String.format(
                        "Could not execute provisioning script. Error communicating with the connector (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage());
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, ex);
                    result.markExceptionRecorded();
                    throw new CommunicationException(message, ex);
                } else {
                    getLogger().warn("{}", message);
                }
            } catch (GenericFrameworkException ex) {
                String message = String.format(
                        "Could not execute provisioning script. Generic error in connector (%s): %s",
                        ctx.getExceptionDescription(connector), ex.getMessage());
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, ex);
                    result.markExceptionRecorded();
                    throw new GenericConnectorException(message, ex);
                } else {
                    getLogger().warn("{}", message);
                }
            } catch (Throwable t) {
                String message = String.format(
                        "Could not execute provisioning script. Unexpected error in connector (%s): %s: %s",
                        ctx.getExceptionDescription(connector), t.getClass().getSimpleName(), t.getMessage());
                if (ExceptionUtil.isFatalCriticality(operation.getCriticality(), CriticalityType.FATAL)) {
                    result.recordFatalError(message, t);
                    result.markExceptionRecorded();
                    throw t;
                } else {
                    getLogger().warn("{}", message);
                }
            }
        }
    }

    private @NotNull Collection<ExecuteProvisioningScriptOperation> determineExecuteScriptOperations(
            ProvisioningOperationTypeType provisioningOperationType,
            BeforeAfterType beforeAfter,
            OperationProvisioningScriptsType scripts,
            ResourceType resource) throws SchemaException {
        if (scripts == null) {
            // No warning needed, this is quite normal
            getLogger().trace("Skipping creating script operation to execute. No scripts were defined.");
            return List.of();
        }

        Collection<ExecuteProvisioningScriptOperation> operations = new ArrayList<>();
        for (var script : scripts.getScript()) {
            for (var operationType : script.getOperation()) {
                if (operationType == provisioningOperationType && beforeAfter == script.getOrder()) {
                    var scriptOperation = ProvisioningUtil.convertToScriptOperation(
                            script, "script value for " + operationType + " in " + resource);
                    getLogger().trace("Created script operation: {}", SchemaDebugUtil.prettyPrintLazily(scriptOperation));
                    operations.add(scriptOperation);
                }
            }
        }
        return operations;
    }

    /** Executes the induced operations on entitlement objects during object ADD, MODIFY, or DELETE operation. */
    void executeEntitlementObjectsOperations(
            EntitlementConverter.EntitlementObjectsOperations objectsOperations,
            OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ObjectAlreadyExistsException {

        getLogger().trace("Executing entitlement changes, roMap:\n{}", objectsOperations.debugDumpLazily(1));

        for (var operationsEntry : objectsOperations.roMap.entrySet()) {
            var entitlementSpec = operationsEntry.getKey();
            var entitlementCtx = operationsEntry.getValue().getResourceObjectContext();
            var entitlementIdentifiers = entitlementSpec.identifiers();
            var entitlementOperations = operationsEntry.getValue().getUcfOperations();

            getLogger().trace("Executing entitlement change with identifiers={}:\n{}",
                    entitlementIdentifiers, DebugUtil.debugDumpLazily(entitlementOperations, 1));

            var result = parentResult.createMinorSubresult(OPERATION_MODIFY_ENTITLEMENT);
            try {

                ResourceObjectUcfModifyOperation.execute(
                        entitlementCtx,
                        null,
                        operationsEntry.getValue().getCurrentResourceObject(),
                        ResourceObjectIdentification.of(entitlementCtx.getObjectDefinitionRequired(), entitlementIdentifiers),
                        entitlementOperations,
                        null,
                        result,
                        connOptions);

                result.recordSuccess();

            } catch (ObjectNotFoundException | CommunicationException | SchemaException | SecurityViolationException |
                    PolicyViolationException | ConfigurationException | ObjectAlreadyExistsException |
                    ExpressionEvaluationException e) {
                // We need to handle this specially.
                // E.g. ObjectNotFoundException means that the entitlement object was not found,
                // not that the subject was not found. It we throw ObjectNotFoundException here it may be
                // interpreted by the consistency code to mean that the subject is missing. Which is not
                // true. And that may cause really strange reactions. In fact we do not want to throw the
                // exception at all, because the primary operation was obviously successful. So just
                // properly record the operation in the result.
                getLogger().error("Error while modifying entitlement {} of {}: {}", entitlementCtx, ctx, e.getMessage(), e);
                result.recordException(e);
            } catch (RuntimeException | Error e) {
                getLogger().error("Error while modifying entitlement {} of {}: {}", entitlementCtx, ctx, e.getMessage(), e);
                result.recordException(e);
                throw e;
            } finally {
                result.close();
            }
        }
    }

    @Nullable
    ExistingResourceObjectShadow preOrPostRead(
            ProvisioningContext ctx,
            ResourceObjectIdentification.WithPrimary identification,
            Collection<Operation> operations,
            boolean fetchEntitlements,
            RepoShadow repoShadow,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        List<ShadowSimpleAttributeDefinition<?>> neededExtraAttributes = new ArrayList<>();
        for (Operation operation : operations) {
            ShadowSimpleAttributeDefinition<?> rad = operation.getAttributeDefinitionIfApplicable(ctx.getObjectDefinitionRequired());
            if (rad != null && (!rad.isReturnedByDefault() || rad.getFetchStrategy() == AttributeFetchStrategyType.EXPLICIT)) {
                neededExtraAttributes.add(rad);
            }
        }

        ShadowItemsToReturn shadowItemsToReturn = new ShadowItemsToReturn();
        shadowItemsToReturn.setItemsToReturn(neededExtraAttributes);
        CompleteResourceObject resourceObject;
        try {
            if (ctx.isReadingCachingOnly() && repoShadow != null) {
                resourceObject = b.resourceObjectConverter.completeResourceObject(
                        ctx, ExistingResourceObjectShadow.fromRepoShadow(repoShadow.clone()), fetchEntitlements, result);
            } else {
                resourceObject = b.resourceObjectConverter.fetchResourceObject(
                        ctx, identification, shadowItemsToReturn, fetchEntitlements, result);
            }
        } catch (ObjectNotFoundException e) {
            // This may happen for semi-manual connectors that are not yet up to date.
            // No big deal. We will have to work without it.
            getLogger().warn("Cannot read shadow {}, it is probably not present on {}. Skipping pre/post read.",
                    identification, ctx.getResource());
            return null;
        }
        ShadowType resourceObjectBean = resourceObject.getBean();
        resourceObjectBean.setName( // TODO why this?
                ShadowUtil.determineShadowNameRequired(resourceObject.resourceObject()));

        return resourceObject.resourceObject();
    }

    abstract Trace getLogger();
}
