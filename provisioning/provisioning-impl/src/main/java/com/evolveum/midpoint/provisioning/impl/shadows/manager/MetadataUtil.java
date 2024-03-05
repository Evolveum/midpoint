/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.datatype.XMLGregorianCalendar;

class MetadataUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    /**
     * Just minimal metadata for now, maybe we need to expand that later
     * those are needed to properly manage dead shadows
     */
    static void addCreationMetadata(ShadowType repoShadow) {
        MetadataType metadata = repoShadow.getMetadata();
        if (metadata != null) {
            return;
        }
        metadata = new MetadataType();
        repoShadow.setMetadata(metadata);
        metadata.setCreateTimestamp(Clock.get().currentTimeXMLGregorianCalendar());
    }

    /**
     * Just minimal metadata for now, maybe we need to expand that later
     * those are needed to properly manage dead shadows
     */
    static void addModificationMetadataDeltas(RepoShadowModifications modifications, RepoShadow repoShadow) {
        if (modifications.hasItemDelta(SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP)) {
            return;
        }
        LOGGER.debug("Metadata not found, adding minimal metadata. Modifications:\n{}",
                DebugUtil.debugDumpLazily(modifications, 1));
        PrismPropertyDefinition<XMLGregorianCalendar> def =
                repoShadow.getPrismDefinition().findPropertyDefinition(SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
        PropertyDelta<XMLGregorianCalendar> modifyTimestampDelta = def.createEmptyDelta(SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
        modifyTimestampDelta.setRealValuesToReplace(Clock.get().currentTimeXMLGregorianCalendar());
        modifications.add(modifyTimestampDelta);
    }
}
