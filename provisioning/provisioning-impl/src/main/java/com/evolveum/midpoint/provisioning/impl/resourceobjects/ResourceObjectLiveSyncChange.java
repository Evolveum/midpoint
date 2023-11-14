/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;

import com.evolveum.midpoint.provisioning.impl.TokenUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.UcfLiveSyncChange;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.Objects;

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
    @NotNull private final LiveSyncToken token;

    /** The value provided by original caller of the `synchronize` method. */
    private final AttributesToReturn originalAttributesToReturn;

    /**
     * @param originalContext Provisioning context determined from the parameters of the synchronize method. It can be wildcard.
     * @param originalAttributesToReturn Attributes to return determined from the parameters of the synchronize method. It can be null.
     */
    ResourceObjectLiveSyncChange(
            UcfLiveSyncChange ucfLiveSyncChange,
            ProvisioningContext originalContext,
            AttributesToReturn originalAttributesToReturn) {
        super(ucfLiveSyncChange, originalContext);
        this.token = TokenUtil.fromUcf(ucfLiveSyncChange.getToken());
        this.originalAttributesToReturn = originalAttributesToReturn;
    }

    @Override
    protected void processObjectAndDelta(OperationResult result) throws CommunicationException, ObjectNotFoundException,
            NotApplicableException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        if (isDelete()) {
            return;
        }

        AttributesToReturn actualAttributesToReturn = determineAttributesToReturn();
        if (resourceObject == null) {
            // TODO maybe we can postpone this fetch to ShadowCache.preProcessChange where it is implemented anyway
            //  But, actually, for all non-delete ConnId LS changes the object is here anyway.
            LOGGER.trace("Fetching object {} because it is not in the change", identifiers);
            fetchResourceObject(actualAttributesToReturn, result);
        } else if (originalCtx.isWildcard() && !Objects.equals(actualAttributesToReturn, originalAttributesToReturn)) {
            LOGGER.trace("Re-fetching object {} because mismatching attributesToReturn", identifiers);
            fetchResourceObject(actualAttributesToReturn, result);
        } else {
            b.resourceObjectConverter.postProcessResourceObjectRead(effectiveCtx, resourceObject, true, result);
        }
    }

    private void fetchResourceObject(AttributesToReturn attributesToReturn, OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, NotApplicableException {
        try {
            // todo consider whether it is always necessary to fetch the entitlements
            resourceObject = b.resourceObjectConverter
                    .fetchResourceObject(effectiveCtx, identifiers, attributesToReturn, null, true, result);
        } catch (ObjectNotFoundException ex) {
            result.recordHandledError(
                    "Object detected in change log no longer exist on the resource. Skipping processing this object.", ex);
            LOGGER.warn("Object detected in change log no longer exist on the resource. Skipping processing this object "
                    + ex.getMessage());
            throw new NotApplicableException();
        }
    }

    private AttributesToReturn determineAttributesToReturn() {
        if (effectiveCtx == originalCtx) {
            return originalAttributesToReturn;
        } else {
            return effectiveCtx.createAttributesToReturn();
        }
    }

    public @NotNull LiveSyncToken getToken() {
        return token;
    }

    @Override
    protected String toStringExtra() {
        return ", token=" + token;
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "token", String.valueOf(token), indent + 1);
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public void checkConsistence() throws SchemaException {
        super.checkConsistence();
        if (isOk() && isInitialized()) {
            // Maybe temporary. This is a specialty of LS change.
            stateCheck(resourceObject != null || isDelete(), "No resource object for non-delete delta");
        }
    }
}
