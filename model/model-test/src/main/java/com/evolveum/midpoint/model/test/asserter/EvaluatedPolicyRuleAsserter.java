/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRuleAsserter<RA> extends AbstractAsserter<RA> {

    private final EvaluatedPolicyRule evaluatedPolicyRule;

    public EvaluatedPolicyRuleAsserter(EvaluatedPolicyRule evaluatedPolicyRule, RA returnAsserter, String desc) {
        super(returnAsserter, desc);
        this.evaluatedPolicyRule = evaluatedPolicyRule;
    }

    public EvaluatedPolicyRuleAsserter<RA> assertName(String expected) {
        assertEquals("Wrong name in "+desc(), expected, evaluatedPolicyRule.getName());
        return this;
    }

    public EvaluatedPolicyRuleAsserter<RA> assertPolicySituation(String expected) {
        assertEquals("Wrong policySituation in "+desc(), expected, evaluatedPolicyRule.getPolicySituation());
        return this;
    }

    public EvaluatedPolicyRuleAsserter<RA> assertNotTriggered() {
        assertTrue("Evaluated policy rule "+evaluatedPolicyRule.getName()+" was triggerd, while it was not expected; in "+desc(),
                evaluatedPolicyRule.getAllTriggers().isEmpty());
        return this;
    }

    public EvaluatedPolicyRuleAsserter<RA> assertTriggers(int expected) {
        assertEquals("Unexpected number of triggers in evaluated policy rule "+evaluatedPolicyRule.getName()+" in "+desc(),
                expected, evaluatedPolicyRule.getAllTriggers().size());
        return this;
    }

    public <CT extends AbstractPolicyConstraintType> EvaluatedPolicyRuleTriggerAsserter<CT,EvaluatedPolicyRuleAsserter<RA>> singleTrigger() {
        assertTriggers(1);
        EvaluatedPolicyRuleTriggerAsserter<CT,EvaluatedPolicyRuleAsserter<RA>> asserter =
                new EvaluatedPolicyRuleTriggerAsserter<>((EvaluatedPolicyRuleTrigger<CT>)evaluatedPolicyRule.getAllTriggers().iterator().next(), this,
                        "trigger in policy rule "+evaluatedPolicyRule.getName()+" in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

    // TODO

    @Override
    protected String desc() {
        return descWithDetails(evaluatedPolicyRule);
    }

}
