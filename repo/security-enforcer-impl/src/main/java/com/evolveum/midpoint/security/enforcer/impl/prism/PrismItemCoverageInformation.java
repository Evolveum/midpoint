/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import static com.evolveum.midpoint.security.enforcer.impl.prism.PrismEntityCoverage.PARTIAL;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Informs whether given {@link Item} and its values are covered in the specified context.
 *
 * (The context can be e.g. items/values that are allowed or denied by a particular operation, e.g. `#get`)
 *
 * @see PrismValueCoverageInformation
 */
class PrismItemCoverageInformation implements PrismEntityCoverageInformation {

    /** Coverage information for specified values of the current item. */
    @NotNull private final Map<PrismValue, PrismValueCoverageInformation> valueMap = new HashMap<>();

    /** Coverage information for all other values, not mentioned in {@link #valueMap}. */
    @NotNull private final PrismValueCoverageInformation otherValues;

    private PrismItemCoverageInformation(@NotNull PrismValueCoverageInformation otherValues) {
        this.otherValues = otherValues;
    }

    static PrismItemCoverageInformation single(@NotNull PrismValueCoverageInformation defaultCoverageInformation) {
        return new PrismItemCoverageInformation(defaultCoverageInformation);
    }

    static PrismItemCoverageInformation fullCoverage() {
        return single(
                PrismValueCoverageInformation.fullCoverage());
    }

    static PrismItemCoverageInformation noCoverage() {
        return single(
                PrismValueCoverageInformation.noCoverage());
    }

    public @NotNull PrismEntityCoverage getCoverage() {
        if (!valueMap.isEmpty()) {
            return PARTIAL;
        } else {
            return otherValues.getCoverage();
        }
    }

    @NotNull PrismValueCoverageInformation getValueCoverageInformation(@NotNull PrismValue value) {
        return Objects.requireNonNullElse(
                valueMap.get(value),
                otherValues);
    }

    @NotNull PrismValueCoverageInformation getOtherValueCoverageInformation() {
        return otherValues;
    }

    void merge(@NotNull PrismItemCoverageInformation increment) {
        // 1. Coverages for specific values from "this" are updated with corresponding values from "increment".
        for (var tEntry : valueMap.entrySet()) {
            PrismValue tValue = tEntry.getKey();
            var tCoverage = tEntry.getValue();
            // This coverage info may come from the value or from "increment" - does not matter here.
            var iCoverage = increment.getValueCoverageInformation(tValue);
            tCoverage.merge(iCoverage);
        }
        // 2. Coverages for not yet matched values from "increment" are updated with "otherValues" from "this".
        for (var iEntry : increment.valueMap.entrySet()) {
            PrismValue iValue = iEntry.getKey();
            if (valueMap.containsKey(iValue)) {
                // This value from increment was already processed
                continue;
            }
            var iCoverage = iEntry.getValue();
            iCoverage.merge(otherValues);
            valueMap.put(iValue, iCoverage);
        }
        // Finally, "other values" in this are merged with "other values" in the increment.
        otherValues.merge(increment.otherValues);
    }

    void addForValue(@NotNull PrismValue value, @NotNull PrismValueCoverageInformation coverageInformation) {
        var previous = valueMap.put(value, coverageInformation);
        stateCheck(previous == null, "overwriting?! %s", previous); // TODO reconsider
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(
                String.format("Item values coverage information [%s]\n", getClass().getSimpleName()),
                indent);
        if (!valueMap.isEmpty()) {
            DebugUtil.debugDumpWithLabelLn(sb, "Specific values", valueMap, indent + 1);
        }
        String label = valueMap.isEmpty() ? "All values" : "Other values";
        DebugUtil.debugDumpWithLabel(sb, label, otherValues, indent + 1);
        return sb.toString();
    }
}
