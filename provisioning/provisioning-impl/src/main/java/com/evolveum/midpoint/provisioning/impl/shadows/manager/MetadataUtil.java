/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

class MetadataUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    /**
     * Just minimal metadata for now, maybe we need to expand that later
     * those are needed to properly manage dead shadows
     */
    static void addCreationMetadata(@NotNull ShadowType repoShadow) {
        ValueMetadataTypeUtil.addCreationMetadata(repoShadow, Clock.get().currentTimeXMLGregorianCalendar());
    }

    /**
     * Just minimal metadata for now, maybe we need to expand that later
     * those are needed to properly manage dead shadows
     */
    static void addModificationMetadataDeltas(RepoShadowModifications modifications, RepoShadow repoShadow)
            throws SchemaException {
        if (ValueMetadataTypeUtil.hasModifyTimestampDelta(modifications.getItemDeltas())) {
            return;
        }
        LOGGER.debug("'modify timestamp' delta not found, adding it");
        modifications.add(
                ValueMetadataTypeUtil.createModifyTimestampDelta(
                        repoShadow.getBean(),
                        Clock.get().currentTimeXMLGregorianCalendar()));
    }
}
