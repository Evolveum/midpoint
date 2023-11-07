/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.TokenUtil;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.UcfLiveSyncChange;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
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

    AttributesToReturn determineAttributesToReturn() {
        if (effectiveCtx == originalCtx) {
            return originalAttributesToReturn;
        } else {
            return effectiveCtx.createAttributesToReturn();
        }
    }

    @Override
    boolean attributesToReturnAreDifferent(AttributesToReturn actualAttributesToReturn) {
        return !Objects.equals(actualAttributesToReturn, originalAttributesToReturn);
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
        // Currently, livesync ADD+MODIFY changes contain the whole object.
        stateCheck(completeResourceObject != null || isDelete(), "No resource object for non-delete delta");
    }
}
