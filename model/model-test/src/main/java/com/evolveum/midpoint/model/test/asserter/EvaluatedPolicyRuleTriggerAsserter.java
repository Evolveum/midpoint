/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRuleTriggerAsserter<CT extends AbstractPolicyConstraintType,RA> extends AbstractAsserter<RA> {

    private final EvaluatedPolicyRuleTrigger<CT> evaluatedPolicyRuleTrigger;

    public EvaluatedPolicyRuleTriggerAsserter(EvaluatedPolicyRuleTrigger<CT> evaluatedPolicyRuleTrigger, RA returnAsserter, String desc) {
        super(returnAsserter, desc);
        this.evaluatedPolicyRuleTrigger = evaluatedPolicyRuleTrigger;
    }

    public EvaluatedPolicyRuleTriggerAsserter<CT,RA> assertConstraintKind(PolicyConstraintKindType expected) {
        assertEquals("Wrong constraint kind in "+desc(), expected, evaluatedPolicyRuleTrigger.getConstraintKind());
        return this;
    }

    // TODO

    @Override
    protected String desc() {
        return descWithDetails(evaluatedPolicyRuleTrigger);
    }

}
