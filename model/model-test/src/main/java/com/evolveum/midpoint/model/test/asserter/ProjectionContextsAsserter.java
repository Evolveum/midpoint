/**
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
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
public class ProjectionContextsAsserter<F extends ObjectType, MA extends ModelContextAsserter<F, RA>,RA> extends AbstractAsserter<MA> {
	
	private MA modelContextAsserter;

	public ProjectionContextsAsserter(MA focusAsserter) {
		super();
		this.modelContextAsserter = focusAsserter;
	}
	
	public ProjectionContextsAsserter(MA focusAsserter, String details) {
		super(details);
		this.modelContextAsserter = focusAsserter;
	}
		
	Collection<? extends ModelProjectionContext> getProjectionContexts() {
		return modelContextAsserter.getModelContext().getProjectionContexts();
	}
	
	public ProjectionContextsAsserter<F, MA, RA> assertSize(int expected) {
		assertEquals("Wrong number of projections contexts in " + desc(), expected, getProjectionContexts().size());
		return this;
	}
	
	public ProjectionContextsAsserter<F, MA, RA> assertNone() {
		assertSize(0);
		return this;
	}
	
	ProjectionContextAsserter<ProjectionContextsAsserter<F, MA, RA>> forProjectionContext(ModelProjectionContext projectionContext) {
		ProjectionContextAsserter<ProjectionContextsAsserter<F, MA, RA>> asserter = new ProjectionContextAsserter<>(projectionContext, this, "projection context of "+desc());
		copySetupTo(asserter);
		return asserter;
	}

	public ProjectionContextAsserter<ProjectionContextsAsserter<F, MA, RA>> single() {
		assertSize(1);
		return forProjectionContext(getProjectionContexts().iterator().next());
	}
		
	@Override
	public MA end() {
		return modelContextAsserter;
	}

	@Override
	protected String desc() {
		return descWithDetails("projection contexts of "+modelContextAsserter.getModelContext());
	}
	
	public ProjectionContextFinder<F,MA,RA> by() {
		return new ProjectionContextFinder<>(this);
	}
	
}
