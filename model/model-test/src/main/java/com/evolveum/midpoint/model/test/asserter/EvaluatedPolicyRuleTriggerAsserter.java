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
