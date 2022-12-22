/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common;

import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;

import org.jetbrains.annotations.NotNull;

/** TEMPORARY */
public class GlobalRuleWithId {
    @NotNull final GlobalPolicyRuleType ruleBean;
    @NotNull final String ruleId;

    private GlobalRuleWithId(@NotNull GlobalPolicyRuleType ruleBean, @NotNull String containingObjectOid) {
        this.ruleBean = ruleBean;
        this.ruleId = PolicyRuleTypeUtil.createId(
                containingObjectOid,
                MiscUtil.stateNonNull(ruleBean.getId(), () -> "Policy rule ID null: " + ruleBean));
    }

    public static GlobalRuleWithId of(@NotNull GlobalPolicyRuleType ruleBean, @NotNull String containingObjectOid) {
        return new GlobalRuleWithId(ruleBean, containingObjectOid);
    }

    public @NotNull GlobalPolicyRuleType getRuleBean() {
        return ruleBean;
    }

    public @NotNull String getRuleId() {
        return ruleId;
    }

    @Override
    public String toString() {
        return ruleId + ":" + ruleBean;
    }
}
