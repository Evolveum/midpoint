/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Collection;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class EvaluatedPolicyRulesAsserter<RA> extends AbstractAsserter<RA> {

    private final Collection<EvaluatedPolicyRule> evaluatedPolicyRules;

    public EvaluatedPolicyRulesAsserter(Collection<EvaluatedPolicyRule> evaluatedPolicyRules, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.evaluatedPolicyRules = evaluatedPolicyRules;
    }

    EvaluatedPolicyRuleAsserter<EvaluatedPolicyRulesAsserter<RA>> forEvaluatedPolicyRule(EvaluatedPolicyRule rule) {
        EvaluatedPolicyRuleAsserter<EvaluatedPolicyRulesAsserter<RA>> asserter = new EvaluatedPolicyRuleAsserter<>(rule, this, "evaluated policy rule in "+desc());
        copySetupTo(asserter);
        return asserter;
    }

//    public  EvaluatedPolicyRuleFinder<RA> by() {
//        return new  EvaluatedPolicyRuleFinder<>(this);
//    }

    public Collection<EvaluatedPolicyRule> getEvaluatedPolicyRules() {
        return evaluatedPolicyRules;
    }

    public EvaluatedPolicyRulesAsserter<RA> assertItems(int expected) {
        assertEquals("Wrong number of evaluated policy rules in "+desc(), expected, evaluatedPolicyRules.size());
        return this;
    }

    public EvaluatedPolicyRuleAsserter<EvaluatedPolicyRulesAsserter<RA>> single() {
        assertItems(1);
        return forEvaluatedPolicyRule(evaluatedPolicyRules.iterator().next());
    }

    public EvaluatedPolicyRulesAsserter<RA> display() {
        display(desc());
        return this;
    }

    public EvaluatedPolicyRulesAsserter<RA> display(String message) {
        IntegrationTestTools.display(message, evaluatedPolicyRules);
        return this;
    }

    @Override
    protected String desc() {
        return "evaluated policy rules of " + getDetails();
    }


}
