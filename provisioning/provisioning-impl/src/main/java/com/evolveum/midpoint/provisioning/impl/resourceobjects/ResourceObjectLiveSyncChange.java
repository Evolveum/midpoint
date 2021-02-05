/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectConverter;
import com.evolveum.midpoint.provisioning.impl.sync.SkipProcessingException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfLiveSyncChange;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * A live sync change at the level of ResourceObjectConverter, i.e. completely processed except
 * for repository (shadow) connection.
 */
public class ResourceObjectLiveSyncChange extends ResourceObjectChange {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectLiveSyncChange.class);

    /**
     * Sync token.
     */
    @NotNull private final PrismProperty<?> token;

    public ResourceObjectLiveSyncChange(UcfLiveSyncChange ucfLiveSyncChange) {
        super(ucfLiveSyncChange);
        this.token = ucfLiveSyncChange.getToken();
    }

    /**
     * Initializes the change into fully operational "resource object live sync change" state.
     *
     * @param originalCtx Provisioning context determined from the parameters of the synchronize method. It can be wildcard.
     * @param originalAttributesToReturn Attributes to return determined from the parameters of the synchronize method. It can be null.
     */
    public void preprocess(ResourceObjectConverter converter, ProvisioningContext originalCtx,
            AttributesToReturn originalAttributesToReturn, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException, GenericFrameworkException, SkipProcessingException {

        processingState.checkSkipProcessing();

        LOGGER.trace("Change before pre-processing:\n{}", debugDumpLazily());

        determineProvisioningContext(originalCtx, null);

        if (!isDelete()) {
            postProcessOrFetchResourceObject(converter, originalCtx, originalAttributesToReturn, result);
        }

        LOGGER.trace("Pre-processed change:\n{}", debugDumpLazily());
    }

    private void postProcessOrFetchResourceObject(ResourceObjectConverter converter, ProvisioningContext originalCtx,
            AttributesToReturn originalAttributesToReturn, OperationResult result) throws CommunicationException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException,
            ObjectNotFoundException, SkipProcessingException {
        AttributesToReturn actualAttributesToReturn = determineAttributesToReturn(originalCtx, originalAttributesToReturn);
        if (resourceObject == null) {
            // TODO maybe we can postpone this fetch to ShadowCache.preProcessChange where it is implemented anyway
            LOGGER.trace("Fetching object {} because it is not in the change", identifiers);
            fetchResourceObject(converter, actualAttributesToReturn, result);
        } else if (originalCtx.isWildcard() && !MiscUtil.equals(actualAttributesToReturn, originalAttributesToReturn)) {
            LOGGER.trace("Re-fetching object {} because mismatching attributesToReturn", identifiers);
            fetchResourceObject(converter, actualAttributesToReturn, result);
        } else {
            converter.postProcessResourceObjectRead(context, resourceObject, true, result);
        }
    }

    private void fetchResourceObject(ResourceObjectConverter converter, AttributesToReturn attributesToReturn,
            OperationResult result) throws CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, SkipProcessingException {
        try {
            // todo consider whether it is always necessary to fetch the entitlements
            resourceObject = converter.fetchResourceObject(context, identifiers, attributesToReturn, true, result);
        } catch (ObjectNotFoundException ex) {
            result.recordHandledError(
                    "Object detected in change log no longer exist on the resource. Skipping processing this object.", ex);
            LOGGER.warn("Object detected in change log no longer exist on the resource. Skipping processing this object "
                    + ex.getMessage());
            throw new SkipProcessingException();
        }
    }

    private AttributesToReturn determineAttributesToReturn(ProvisioningContext originalCtx, AttributesToReturn originalAttrsToReturn)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {
        if (context == originalCtx) {
            return originalAttrsToReturn;
        } else {
            return ProvisioningUtil.createAttributesToReturn(context);
        }
    }

    public @NotNull PrismProperty<?> getToken() {
        return token;
    }

    @Override
    protected String toStringExtra() {
        return ", token=" + token;
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "token", token, indent + 1);
    }
}
