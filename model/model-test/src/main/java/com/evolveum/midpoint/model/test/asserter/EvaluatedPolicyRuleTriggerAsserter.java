/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
