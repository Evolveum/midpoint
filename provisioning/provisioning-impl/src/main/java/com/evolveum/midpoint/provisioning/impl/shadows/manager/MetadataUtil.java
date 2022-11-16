/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;

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
    static void addModificationMetadataDeltas(Collection<ItemDelta<?, ?>> changes, ShadowType repoShadow) {
        PropertyDelta<XMLGregorianCalendar> modifyTimestampDelta =
                ItemDeltaCollectionsUtil.findPropertyDelta(changes, SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
        if (modifyTimestampDelta != null) {
            return;
        }
        LOGGER.debug("Metadata not found, adding minimal metadata. Modifications:\n{}",
                DebugUtil.debugDumpLazily(changes, 1));
        PrismPropertyDefinition<XMLGregorianCalendar> def =
                repoShadow.asPrismObject().getDefinition()
                        .findPropertyDefinition(SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
        modifyTimestampDelta = def.createEmptyDelta(SchemaConstants.PATH_METADATA_MODIFY_TIMESTAMP);
        modifyTimestampDelta.setRealValuesToReplace(Clock.get().currentTimeXMLGregorianCalendar());
        changes.add(modifyTimestampDelta);
    }
}
