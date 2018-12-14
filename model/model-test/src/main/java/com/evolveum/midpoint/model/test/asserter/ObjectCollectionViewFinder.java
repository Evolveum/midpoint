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

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class ObjectCollectionViewFinder<RA> {
	
	private final ObjectCollectionViewsAsserter<RA> viewsAsserter;
	private String name;
	
	public ObjectCollectionViewFinder(ObjectCollectionViewsAsserter<RA> viewsAsserter) {
		this.viewsAsserter = viewsAsserter;
	}
	
	public ObjectCollectionViewFinder<RA> name(String name) {
		this.name = name;
		return this;
	}
	
	public ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> find() throws ObjectNotFoundException, SchemaException {
		CompiledObjectCollectionView found = null;
		for (CompiledObjectCollectionView view: viewsAsserter.getViews()) {
			if (matches(view)) {
				if (found == null) {
					found = view;
				} else {
					fail("Found more than one link that matches search criteria");
				}
			}
		}
		if (found == null) {
			fail("Found no link that matches search criteria");
		}
		return viewsAsserter.forView(found);
	}
	
	public ObjectCollectionViewsAsserter<RA> assertCount(int expectedCount) throws ObjectNotFoundException, SchemaException {
		int foundCount = 0;
		for (CompiledObjectCollectionView view: viewsAsserter.getViews()) {
			if (matches(view)) {
				foundCount++;
			}
		}
		assertEquals("Wrong number of links for specified criteria in "+viewsAsserter.desc(), expectedCount, foundCount);
		return viewsAsserter;
	}
	
	private boolean matches(CompiledObjectCollectionView view) throws ObjectNotFoundException, SchemaException {
		
		if (name != null) {
			if (!name.equals(view.getViewName())) {
				return false;
			}
		}
		
		// TODO: more criteria
		return true;
	}
	
	protected void fail(String message) {
		AssertJUnit.fail(message);
	}

}
