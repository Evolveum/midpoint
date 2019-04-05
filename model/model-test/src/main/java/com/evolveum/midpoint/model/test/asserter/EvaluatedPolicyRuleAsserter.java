/**
 * Copyright (c) 2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

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
