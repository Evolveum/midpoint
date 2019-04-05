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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ArchetypePolicyAsserter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

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
		
//	public  EvaluatedPolicyRuleFinder<RA> by() {
//		return new  EvaluatedPolicyRuleFinder<>(this);
//	}

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
