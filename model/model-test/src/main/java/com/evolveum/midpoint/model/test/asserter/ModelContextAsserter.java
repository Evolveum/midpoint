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

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.FocusAsserter;
import com.evolveum.midpoint.test.asserter.LinksAsserter;
import com.evolveum.midpoint.test.asserter.PrismObjectAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ModelContextAsserter<O extends ObjectType,RA> extends AbstractAsserter<RA> {
	
	private ModelContext<O> modelContext;

	public ModelContextAsserter(ModelContext<O> modelContext) {
		super();
		this.modelContext = modelContext;
	}
	
	public ModelContextAsserter(ModelContext<O> modelContext, String details) {
		super(details);
		this.modelContext = modelContext;
	}
	
	public ModelContextAsserter(ModelContext<O> modelContext, RA returnAsserter, String details) {
		super(returnAsserter, details);
		this.modelContext = modelContext;
	}
	
	public ModelContext<O> getModelContext() {
		return modelContext;
	}

	public static <O extends ObjectType> ModelContextAsserter<O,Void> forContext(ModelContext<O> ctx) {
		return new ModelContextAsserter<>(ctx);
	}
	
	public static <O extends ObjectType> ModelContextAsserter<O,Void> forContext(ModelContext<O> ctx, String details) {
		return new ModelContextAsserter<>(ctx, details);
	}
	
	public ProjectionContextsAsserter<O, ? extends ModelContextAsserter<O,RA>, RA> projectionContexts() {
		ProjectionContextsAsserter<O, ? extends ModelContextAsserter<O,RA>, RA> asserter = new ProjectionContextsAsserter<>(this, getDetails());
		copySetupTo(asserter);
		return asserter;
	}
	
	@Override
	protected String desc() {
		return "model context " + modelContext;
	}
	
	public ModelContextAsserter<O,RA> display(String message) {
		IntegrationTestTools.display(message, modelContext);
		return this;
	}

	public ModelContextAsserter<O,RA> display() {
		display(desc());
		return this;
	}
}
