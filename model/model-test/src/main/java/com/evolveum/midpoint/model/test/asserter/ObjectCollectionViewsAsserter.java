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

import java.util.List;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;

/**
 * @author semancik
 *
 */
public class ObjectCollectionViewsAsserter<RA> extends AbstractAsserter<RA> {
	
	private final List<CompiledObjectCollectionView> objectCollectionViews;
	
	public ObjectCollectionViewsAsserter(List<CompiledObjectCollectionView> objectCollectionViews, RA returnAsserter, String desc) {
		super(returnAsserter, desc);
		this.objectCollectionViews = objectCollectionViews;
	}

	ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> forView(CompiledObjectCollectionView view) {
		ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> asserter = new ObjectCollectionViewAsserter<>(view, this, "view in "+desc());
		copySetupTo(asserter);
		return asserter;
	}
	
	public ObjectCollectionViewFinder<RA> by() {
		return new ObjectCollectionViewFinder<>(this);
	}

	public List<CompiledObjectCollectionView> getViews() {
		return objectCollectionViews;
	}
	
	public ObjectCollectionViewsAsserter<RA> assertViews(int expected) {
		assertEquals("Wrong number of views in "+desc(), expected, getViews().size());
		return this;
	}
	
	public ObjectCollectionViewAsserter<ObjectCollectionViewsAsserter<RA>> single() {
		assertViews(1);
		return forView(getViews().get(0));
	}
	
	@Override
	protected String desc() {
		return "object collection views of " + getDetails();
	}

}
