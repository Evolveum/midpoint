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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ArchetypePolicyAsserter;
import com.evolveum.midpoint.test.asserter.DisplayTypeAsserter;

/**
 * @author semancik
 *
 */
public class ObjectCollectionViewAsserter<RA> extends AbstractAsserter<RA> {
	
	private final CompiledObjectCollectionView view;
	
	public ObjectCollectionViewAsserter(CompiledObjectCollectionView view, RA returnAsserter, String desc) {
		super(returnAsserter, desc);
		this.view = view;
	}

	public ObjectCollectionViewAsserter<RA> assertName(String expected) {
		assertEquals("Wrong view name in "+desc(), expected, view.getViewName());
		return this;
	}
	
	public ObjectCollectionViewAsserter<RA> assertFilter() {
		assertNotNull("Null filter in "+desc(), view.getFilter());
		return this;
	}
	
	public DisplayTypeAsserter<ObjectCollectionViewAsserter<RA>> displayType() {
		DisplayTypeAsserter<ObjectCollectionViewAsserter<RA>> displayAsserter = new DisplayTypeAsserter<>(view.getDisplay(), this, "in " + desc());
		copySetupTo(displayAsserter);
		return displayAsserter;
	}
	
	// TODO
	
	public ObjectFilter getFilter() {
		return view.getFilter();
	}

	@Override
	protected String desc() {
		return descWithDetails(view);
	}

}
