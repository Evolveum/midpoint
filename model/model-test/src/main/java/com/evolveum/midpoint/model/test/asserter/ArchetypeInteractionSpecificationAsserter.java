/**
 * Copyright (c) 2018 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.ArchetypeInteractionSpecification;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ArchetypePolicyAsserter;
import com.evolveum.midpoint.test.asserter.DisplayTypeAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;

/**
 * @author semancik
 *
 */
public class ArchetypeInteractionSpecificationAsserter<RA> extends AbstractAsserter<RA> {
	
	private final ArchetypeInteractionSpecification archetypeSpec;

	public ArchetypeInteractionSpecificationAsserter(ArchetypeInteractionSpecification archetypeSpec, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.archetypeSpec = archetypeSpec;
	}
	
	ArchetypeInteractionSpecification getArchetypeInteractionSpecification() {
		assertNotNull("Null " + desc(), archetypeSpec);
		return archetypeSpec;
	}
	
	public ArchetypeInteractionSpecificationAsserter<RA> assertNull() {
		AssertJUnit.assertNull("Unexpected " + desc(), archetypeSpec);
		return this;
	}
	
	public ArchetypePolicyAsserter<ArchetypeInteractionSpecificationAsserter<RA>> archetypePolicy() {
		ArchetypePolicyAsserter<ArchetypeInteractionSpecificationAsserter<RA>> displayAsserter = new ArchetypePolicyAsserter<>(getArchetypeInteractionSpecification().getArchetypePolicy(), this, "in " + desc());
		copySetupTo(displayAsserter);
		return displayAsserter;
	}
	
	public ArchetypeInteractionSpecificationAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public ArchetypeInteractionSpecificationAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, archetypeSpec);
		return this;
	}
	
	@Override
	protected String desc() {
		return descWithDetails("archetype interaction specification");
	}
	
}
