/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.GlobalPolicyRuleConfigItem;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/** TEMPORARY */
public record GlobalRuleWithId(
        @NotNull GlobalPolicyRuleConfigItem ruleCI,
        @NotNull String ruleId) implements Serializable {

    public static GlobalRuleWithId of(@NotNull GlobalPolicyRuleType embeddedRuleBean) {
        GlobalPolicyRuleConfigItem item = GlobalPolicyRuleConfigItem.embedded(embeddedRuleBean);
        var ruleId = PolicyRuleTypeUtil.createId(
                ((ConfigurationItemOrigin.InObject) item.origin()).getOriginatingObjectOid(),
                MiscUtil.stateNonNull(embeddedRuleBean.getId(), () -> "Policy rule ID is null: " + embeddedRuleBean));
        return new GlobalRuleWithId(item, ruleId);
    }

    @Override
    public String toString() {
        return ruleId + ":" + ruleCI;
    }
}
