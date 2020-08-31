/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.mapping.metadata;

import java.util.*;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import static java.util.Collections.emptySet;

/**
 * Convenient data structure to provide input for metadata computation for custom transformational
 * metadata mappings.
 *
 * A sketch only. Probably will be changed in the future.
 */
@Experimental
public class MetadataComputationInput implements DebugDumpable {

    /**
     * Input data values that take part in the derivation of the current output value.
     * For example, "Jack" (for given name) and "Sparrow" (for family name) in the computation
     * of full name of "Jack Sparrow".
     */
    private final List<InputDataValue> inputDataValues = new ArrayList<>();

    public static class InputDataValue implements DebugDumpable {

        private final PrismValue prismValue;

        private final Map<String, Collection<?>> metadataSourceMap;

        // A reference to the mapping source (at least item name) would be helpful

        public InputDataValue(PrismValue value, Map<String, Collection<?>> metadataSourceMap) {
            this.prismValue = value;
            this.metadataSourceMap = metadataSourceMap;
        }

        /**
         * Data value, e.g. "Jack" (prism value with metadata).
         */
        public PrismValue getPrismValue() {
            return prismValue;
        }

        /**
         * Real data value, e.g. "Jack" (String or PolyString with no metadata).
         */
        public Object getRealValue() {
            return prismValue != null ? prismValue.getRealValue() : null;
        }

        /**
         * Real values of metadata sources for this mapping.
         * E.g. for LoA mapping (loa -> loa) here is only single entry:
         *  - key = "loa"
         *  - values = list of all LoA values for individual yields of prismValue metadata.
         */
        public Map<String, Collection<?>> getMetadataSourceMap() {
            return metadataSourceMap;
        }

        /**
         * Values of metadata source, assuming that there is a single metadata source.
         * A convenience method.
         */
        public Collection<?> getMetadataValues() {
            if (metadataSourceMap.size() > 1) {
                throw new IllegalStateException("More than one source: " + metadataSourceMap.keySet());
            } else if (metadataSourceMap.size() == 1) {
                return metadataSourceMap.values().iterator().next();
            } else {
                return emptySet();
            }
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = new StringBuilder();
            DebugUtil.debugDumpWithLabelLn(sb, "data value", prismValue, indent);
            DebugUtil.debugDumpWithLabel(sb, "metadata sources", metadataSourceMap, indent);
            return sb.toString();
        }
    }

    public void add(PrismValue inputValue, Map<String, Collection<?>> metadataSourceMap) {
        inputDataValues.add(new InputDataValue(inputValue, metadataSourceMap));
    }

    public List<InputDataValue> getInputDataValues() {
        return inputDataValues;
    }

    @Override
    public String debugDump(int indent) {
        return DebugUtil.debugDump(inputDataValues, indent);
    }
}
