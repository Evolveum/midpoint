/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ProcessingUtil {

    /**
     * Extracts processing information from specified item definition.
     */
    static ItemProcessingType getProcessingOfItem(MetadataItemDefinitionType item) throws SchemaException {
        Set<ItemProcessingType> processing = new HashSet<>();
        for (PropertyLimitationsType limitation : item.getLimitations()) {
            if (limitation.getLayer().isEmpty() || limitation.getLayer().contains(LayerType.MODEL)) {
                if (limitation.getProcessing() != null) {
                    processing.add(limitation.getProcessing());
                }
            }
        }
        return MiscUtil.extractSingleton(processing,
                () -> new SchemaException("Contradicting 'processing' values for " + item + ": " + processing));
    }

    /**
     * Checks applicability w.r.t. given data item path.
     */
    static boolean doesApplicabilityMatch(MetadataProcessingApplicabilitySpecificationType applicability, ItemPath dataItemPath)
            throws SchemaException {
        if (applicability == null) {
            return true;
        } else {
            List<ItemPath> includes = getPathsFromSpecs(applicability.getInclude());
            List<ItemPath> excludes = getPathsFromSpecs(applicability.getExclude());
            while (dataItemPath != null && !dataItemPath.isEmpty()) {
                boolean yes = ItemPathCollectionsUtil.containsEquivalent(includes, dataItemPath);
                boolean no = ItemPathCollectionsUtil.containsEquivalent(excludes, dataItemPath);
                if (yes && no) {
                    throw new SchemaException("Item path " + dataItemPath + " is both included and excluded from applicability in metadata processing");
                } else if (yes) {
                    return true;
                } else if (no) {
                    return false;
                }
                dataItemPath = dataItemPath.allExceptLast();
            }
            // Default value is true ... but only if there are no explicit includes.
            return includes.isEmpty();
        }
    }

    private static List<ItemPath> getPathsFromSpecs(List<MetadataProcessingItemApplicabilitySpecificationType> specs) throws SchemaException {
        List<ItemPath> paths = new ArrayList<>();
        for (MetadataProcessingItemApplicabilitySpecificationType spec : specs) {
            if (spec.getPath() == null) {
                throw new SchemaException("No path in applicability specification: " + spec);
            } else {
                paths.add(spec.getPath().getItemPath());
            }
        }
        return paths;
    }
}
