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
package com.evolveum.midpoint.test.asserter;

import static org.testng.AssertJUnit.assertNotNull;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ArchetypePolicyType;

/**
 * @author semancik
 *
 */
public class ArchetypePolicyAsserter<RA> extends AbstractAsserter<RA> {
	
	private final ArchetypePolicyType archetypePolicy;

	public ArchetypePolicyAsserter(ArchetypePolicyType archetypePolicy, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.archetypePolicy = archetypePolicy;
	}
	
	ArchetypePolicyType getArchetypePolicy() {
		assertNotNull("Null " + desc(), archetypePolicy);
		return archetypePolicy;
	}
	
	public ArchetypePolicyAsserter<RA> assertNull() {
		AssertJUnit.assertNull("Unexpected " + desc(), archetypePolicy);
		return this;
	}
	
	public DisplayTypeAsserter<ArchetypePolicyAsserter<RA>> displayType() {
		DisplayTypeAsserter<ArchetypePolicyAsserter<RA>> displayAsserter = new DisplayTypeAsserter<>(getArchetypePolicy().getDisplay(), this, "in " + desc());
		copySetupTo(displayAsserter);
		return displayAsserter;
	}
	
	public ArchetypePolicyAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public ArchetypePolicyAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, archetypePolicy);
		return this;
	}
	
	@Override
	protected String desc() {
		return descWithDetails("archetype policy");
	}
	
}
