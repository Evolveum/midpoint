/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common;

import java.io.Serializable;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

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
                MiscUtil.stateNonNull(ruleBean.getId(), "Policy rule ID is null: %s", ruleBean));
        return new GlobalRuleWithId(ruleCI, ruleId);
    }

    public @NotNull ConfigurationItemOrigin ruleOrigin() {
        return ruleCI.origin();
    }

    @Override
    public String toString() {
        return ruleId + ":" + ruleCI;
    }
}
