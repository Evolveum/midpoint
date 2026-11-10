/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

import javax.xml.namespace.QName;

public class PolicyRuleIndexVisitor  implements PolicyRuleTypeUtil.ConstraintVisitor {
    @Override
    public boolean visit(QName name, AbstractPolicyConstraintType constraint) {
        return false;
    }
}
