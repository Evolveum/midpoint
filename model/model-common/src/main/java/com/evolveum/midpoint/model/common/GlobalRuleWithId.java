/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.config.GlobalPolicyRuleConfigItem;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;

/** TEMPORARY */
public record GlobalRuleWithId(
        @NotNull GlobalPolicyRuleConfigItem ruleCI,
        @NotNull String ruleId) implements Serializable {

    // Very temporary/fragile constructor
    public static GlobalRuleWithId of(
            @NotNull GlobalPolicyRuleConfigItem ruleCI,
            @NotNull String originatingObjectOid) {
        var ruleBean = ruleCI.value();
        var ruleId = PolicyRuleTypeUtil.createId(
                originatingObjectOid,
                MiscUtil.stateNonNull(ruleBean.getId(), () -> "Policy rule ID is null: " + ruleBean));
        return new GlobalRuleWithId(ruleCI, ruleId);
    }

    @Override
    public String toString() {
        return ruleId + ":" + ruleCI;
    }
}
