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
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.UcfLiveSyncChange;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

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

    /** The context known at creation time. Used for initialization. */
    @NotNull private final InitializationContext ictx;

    /**
     * @param originalContext Provisioning context determined from the parameters of the synchronize method. It can be wildcard.
     * @param originalAttributesToReturn Attributes to return determined from the parameters of the synchronize method. It can be null.
     */
    public ResourceObjectLiveSyncChange(UcfLiveSyncChange ucfLiveSyncChange, Exception preInitializationException,
            ResourceObjectConverter converter, ProvisioningContext originalContext, AttributesToReturn originalAttributesToReturn) {
        super(ucfLiveSyncChange, preInitializationException, originalContext, converter.getLocalBeans());
        this.token = ucfLiveSyncChange.getToken();
        this.ictx = new InitializationContext(originalAttributesToReturn, originalContext);
    }

    @Override
    protected void processObjectAndDelta(OperationResult result) throws CommunicationException, ObjectNotFoundException,
            NotApplicableException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        if (isDelete()) {
            return;
        }

        AttributesToReturn actualAttributesToReturn = determineAttributesToReturn(ictx.originalContext, ictx.originalAttrsToReturn);
        if (resourceObject == null) {
            // TODO maybe we can postpone this fetch to ShadowCache.preProcessChange where it is implemented anyway
            //  But, actually, for all non-delete ConnId LS changes the object is here anyway.
            LOGGER.trace("Fetching object {} because it is not in the change", identifiers);
            fetchResourceObject(actualAttributesToReturn, result);
        } else if (ictx.originalContext.isWildcard() && !MiscUtil.equals(actualAttributesToReturn, ictx.originalAttrsToReturn)) {
            LOGGER.trace("Re-fetching object {} because mismatching attributesToReturn", identifiers);
            fetchResourceObject(actualAttributesToReturn, result);
        } else {
            localBeans.resourceObjectConverter
                    .postProcessResourceObjectRead(context, resourceObject, true, result);
        }
    }

    private void fetchResourceObject(AttributesToReturn attributesToReturn, OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, NotApplicableException {
        try {
            // todo consider whether it is always necessary to fetch the entitlements
            resourceObject = localBeans.resourceObjectConverter
                    .fetchResourceObject(context, identifiers, attributesToReturn, true, result);
        } catch (ObjectNotFoundException ex) {
            result.recordHandledError(
                    "Object detected in change log no longer exist on the resource. Skipping processing this object.", ex);
            LOGGER.warn("Object detected in change log no longer exist on the resource. Skipping processing this object "
                    + ex.getMessage());
            throw new NotApplicableException();
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
        DebugUtil.debugDumpWithLabelLn(sb, "token", token, indent + 1);
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    private static class InitializationContext {
        private final AttributesToReturn originalAttrsToReturn;
        private final ProvisioningContext originalContext;

        private InitializationContext(AttributesToReturn originalAttrsToReturn, ProvisioningContext originalContext) {
            this.originalAttrsToReturn = originalAttrsToReturn;
            this.originalContext = originalContext;
        }
    }

    @Override
    public void checkConsistence() throws SchemaException {
        super.checkConsistence();
        if (initializationState.isOk() && initializationState.isAfterInitialization()) {
            // Maybe temporary. This is a specialty of LS change.
            stateCheck(resourceObject != null || isDelete(), "No resource object for non-delete delta");
        }
    }
}
