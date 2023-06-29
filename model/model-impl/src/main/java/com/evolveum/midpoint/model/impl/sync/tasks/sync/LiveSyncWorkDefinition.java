/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.sync;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.util.task.work.LegacyWorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LiveSyncWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

public class LiveSyncWorkDefinition extends AbstractWorkDefinition implements ResourceObjectSetSpecificationProvider {

    /** Mutable, disconnected from the source. */
    @NotNull private final ResourceObjectSetType resourceObjects;
    private final Integer batchSize;
    private final boolean updateLiveSyncTokenInDryRun;
    private final boolean updateLiveSyncTokenInPreviewMode;

    LiveSyncWorkDefinition(WorkDefinitionSource source) {
        Boolean updateLiveSyncTokenInDryRunRaw;
        if (source instanceof LegacyWorkDefinitionSource legacy) {
            resourceObjects = ResourceObjectSetUtil.fromLegacySource(legacy);
            batchSize = legacy.getExtensionItemRealValue(SchemaConstants.MODEL_EXTENSION_LIVE_SYNC_BATCH_SIZE, Integer.class);
            updateLiveSyncTokenInDryRunRaw =
                    legacy.getExtensionItemRealValue(SchemaConstants.MODEL_EXTENSION_UPDATE_LIVE_SYNC_TOKEN_IN_DRY_RUN,
                            Boolean.class);
            updateLiveSyncTokenInPreviewMode = false; // not supported here
        } else {
            LiveSyncWorkDefinitionType typedDefinition = (LiveSyncWorkDefinitionType)
                    ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
            resourceObjects = ResourceObjectSetUtil.fromConfiguration(typedDefinition.getResourceObjects());
            batchSize = typedDefinition.getBatchSize();
            updateLiveSyncTokenInDryRunRaw = typedDefinition.isUpdateLiveSyncTokenInDryRun();
            updateLiveSyncTokenInPreviewMode = Boolean.TRUE.equals(typedDefinition.isUpdateLiveSyncTokenInPreviewMode());
        }
        ResourceObjectSetUtil.removeQuery(resourceObjects);
        updateLiveSyncTokenInDryRun = Boolean.TRUE.equals(updateLiveSyncTokenInDryRunRaw);
    }

    @Override
    public @NotNull ResourceObjectSetType getResourceObjectSetSpecification() {
        return resourceObjects;
    }

    Integer getBatchSize() {
        return batchSize;
    }

    boolean isUpdateLiveSyncTokenInDryRun() {
        return updateLiveSyncTokenInDryRun;
    }

    boolean isUpdateLiveSyncTokenInPreviewMode() {
        return updateLiveSyncTokenInPreviewMode;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObjects", resourceObjects, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "batchSize", batchSize, indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "updateLiveSyncTokenInDryRun", updateLiveSyncTokenInDryRun, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "updateLiveSyncTokenInPreviewMode", updateLiveSyncTokenInPreviewMode, indent+1);
    }
}
