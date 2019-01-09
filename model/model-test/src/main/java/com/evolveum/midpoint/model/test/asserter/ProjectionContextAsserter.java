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
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ObjectDeltaAsserter;
import com.evolveum.midpoint.test.asserter.PrismObjectAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_4.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.TriggerType;
import com.evolveum.prism.xml.ns._public.types_4.ObjectDeltaType;

/**
 * @author semancik
 *
 */
public class ProjectionContextAsserter<RA> extends AbstractAsserter<RA> {
	
	final private ModelProjectionContext projectionContext;

	public ProjectionContextAsserter(ModelProjectionContext projectionContext) {
		super();
		this.projectionContext = projectionContext;
	}
	
	public ProjectionContextAsserter(ModelProjectionContext projectionContext, String detail) {
		super(detail);
		this.projectionContext = projectionContext;
	}
	
	public ProjectionContextAsserter(ModelProjectionContext projectionContext, RA returnAsserter, String detail) {
		super(returnAsserter, detail);
		this.projectionContext = projectionContext;
	}
	
	public ModelProjectionContext getProjectionContext() {
		return projectionContext;
	}

	public ShadowAsserter<ProjectionContextAsserter<RA>> objectOld() {
		ShadowAsserter<ProjectionContextAsserter<RA>> shadowAsserter = new ShadowAsserter<ProjectionContextAsserter<RA>>(
				projectionContext.getObjectOld(), this, "object old in "+desc());
		copySetupTo(shadowAsserter);
		return shadowAsserter;
	}
	
	public ShadowAsserter<ProjectionContextAsserter<RA>> objectCurrent() {
		ShadowAsserter<ProjectionContextAsserter<RA>> shadowAsserter = new ShadowAsserter<ProjectionContextAsserter<RA>>(
				projectionContext.getObjectCurrent(), this, "object current in "+desc());
		copySetupTo(shadowAsserter);
		return shadowAsserter;
	}
	
	public ShadowAsserter<ProjectionContextAsserter<RA>> objectNew() {
		ShadowAsserter<ProjectionContextAsserter<RA>> shadowAsserter = new ShadowAsserter<ProjectionContextAsserter<RA>>(
				projectionContext.getObjectNew(), this, "object new in "+desc());
		copySetupTo(shadowAsserter);
		return shadowAsserter;
	}
	
	public ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>> primaryDelta() {
		ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>> deltaAsserter = new ObjectDeltaAsserter<>(
				projectionContext.getPrimaryDelta(), this, "primary delta in "+desc());
		copySetupTo(deltaAsserter);
		return deltaAsserter;
	}
	
	public ProjectionContextAsserter<RA> assertNoPrimaryDelta() {
		assertNull("Unexpected primary delta in "+desc(), projectionContext.getPrimaryDelta());
		return this;
	}
	
	public ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>> secondaryDelta() {
		ObjectDeltaAsserter<ShadowType, ProjectionContextAsserter<RA>> deltaAsserter = new ObjectDeltaAsserter<>(
				projectionContext.getSecondaryDelta(), this, "secondary delta in "+desc());
		copySetupTo(deltaAsserter);
		return deltaAsserter;
	}

	public ProjectionContextAsserter<RA> assertNoSecondaryDelta() {
		assertNull("Unexpected secondary delta in "+desc(), projectionContext.getSecondaryDelta());
		return this;
	}

	protected String desc() {
		// TODO: better desc
		return descWithDetails(projectionContext);
	}
	
	public ProjectionContextAsserter<RA> display() {
		display(desc());
		return this;
	}
	
	public ProjectionContextAsserter<RA> display(String message) {
		IntegrationTestTools.display(message, projectionContext);
		return this;
	}	
}
