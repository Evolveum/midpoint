/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;

public class MPolicyRuleReference extends MReference {

    public Long policyRuleCid;
    public MPolicyReferenceType policyReferenceType;

    @Override
    public String toString() {
        return "MPolicyRuleReference{" +
                "policiRuleCid=" + policyRuleCid +
                ", policyReferenceType=" + policyReferenceType +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", relationId=" + relationId +
                '}';
    }
}
