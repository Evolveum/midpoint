/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeTupleStatisticsType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * The persisted statistics keep fully-qualified item paths (e.g. `c:attributes/ri:emptype`).
 * For the service request we strip the common ("c:") namespace from path segments, so that the
 * service sees and echoes back simpler paths like `attributes/ri:emptype`. Other namespaces
 * (`ri:`, `icfs:`, extension ones) are kept intact.
 *
 * The original statistics object is not modified.
 */
class ServiceStatisticsSanitizer {

    private ServiceStatisticsSanitizer() {
    }

    static ObjectSetStatisticsType sanitize(@NotNull ObjectSetStatisticsType statistics) {
        var copy = new ObjectSetStatisticsType()
                .size(statistics.getSize())
                .coverage(statistics.getCoverage())
                .timestamp(statistics.getTimestamp());
        for (ShadowAttributeStatisticsType attribute : statistics.getAttribute()) {
            copy.getAttribute().add(sanitize(attribute));
        }
        for (ShadowAttributeTupleStatisticsType tuple : statistics.getAttributeTuple()) {
            copy.getAttributeTuple().add(sanitize(tuple));
        }
        return copy;
    }

    private static ShadowAttributeStatisticsType sanitize(@NotNull ShadowAttributeStatisticsType attribute) {
        var copy = new ShadowAttributeStatisticsType()
                .ref(sanitize(attribute.getRef()))
                .uniqueValueCount(attribute.getUniqueValueCount())
                .missingValueCount(attribute.getMissingValueCount());
        copy.getValueCount().addAll(attribute.getValueCount());
        copy.getValuePatternCount().addAll(attribute.getValuePatternCount());
        return copy;
    }

    private static ShadowAttributeTupleStatisticsType sanitize(@NotNull ShadowAttributeTupleStatisticsType tuple) {
        var copy = new ShadowAttributeTupleStatisticsType();
        for (ItemPathType ref : tuple.getRef()) {
            copy.getRef().add(sanitize(ref));
        }
        copy.getTupleCount().addAll(tuple.getTupleCount());
        return copy;
    }

    private static ItemPathType sanitize(ItemPathType ref) {
        if (ref == null) {
            return null;
        }
        return new ItemPathType(stripCommonNamespace(ref.getItemPath()));
    }

    /** Replaces all segments in the common ("c:") namespace with unqualified names. */
    private static ItemPath stripCommonNamespace(ItemPath path) {
        List<Object> segments = new ArrayList<>();
        for (Object segment : path.getSegments()) {
            if (ItemPath.isName(segment)) {
                QName name = ItemPath.toName(segment);
                if (SchemaConstantsGenerated.NS_COMMON.equals(name.getNamespaceURI())) {
                    segments.add(new QName(name.getLocalPart()));
                } else {
                    segments.add(name);
                }
            } else {
                segments.add(segment);
            }
        }
        return ItemPath.create(segments);
    }
}
