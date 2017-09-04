/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ConstraintViolationConfirmer;
import com.evolveum.midpoint.provisioning.api.ConstraintsCheckingResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class ShadowConstraintsChecker<F extends FocusType> {

	private static final Trace LOGGER = TraceManager.getTrace(ShadowConstraintsChecker.class);

	private LensProjectionContext projectionContext;
	private LensContext<F> context;
	private PrismContext prismContext;
	private ProvisioningService provisioningService;
	private boolean satisfiesConstraints;
	private ConstraintsCheckingResult constraintsCheckingResult;

	public ShadowConstraintsChecker(LensProjectionContext accountContext) {
		this.projectionContext = accountContext;
	}

	public LensProjectionContext getAccountContext() {
		return projectionContext;
	}

	public void setAccountContext(LensProjectionContext accountContext) {
		this.projectionContext = accountContext;
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public ProvisioningService getProvisioningService() {
		return provisioningService;
	}

	public void setProvisioningService(ProvisioningService provisioningService) {
		this.provisioningService = provisioningService;
	}

	public LensContext<F> getContext() {
		return context;
	}

	public void setContext(LensContext<F> context) {
		this.context = context;
	}

	public boolean isSatisfiesConstraints() {
		return satisfiesConstraints;
	}

	public String getMessages() {
		return constraintsCheckingResult.getMessages();
	}

	public PrismObject getConflictingShadow() {
		return constraintsCheckingResult.getConflictingShadow();
	}

	public void check(Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		RefinedObjectClassDefinition projOcDef = projectionContext.getCompositeObjectClassDefinition();
		PrismObject<ShadowType> projectionNew = projectionContext.getObjectNew();
		if (projectionNew == null) {
			// This must be delete
			LOGGER.trace("No new object in projection context. Current shadow satisfy constraints");
			satisfiesConstraints = true;
			return;
		}

		PrismContainer<?> attributesContainer = projectionNew.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			// No attributes no constraint violations
			LOGGER.trace("Current shadow does not contain attributes, skipping checking uniqueness.");
			satisfiesConstraints = true;
			return;
		}

		ConstraintViolationConfirmer confirmer = (conflictingShadowCandidate) -> {
				boolean violation = true;
				LensProjectionContext foundContext = context.findProjectionContextByOid(conflictingShadowCandidate.getOid());
				if (foundContext != null) {
					if (foundContext.getResourceShadowDiscriminator() != null) {
						if (foundContext.getResourceShadowDiscriminator().isThombstone()) {
							violation = false;
						}
						LOGGER.trace("Comparing with account in other context resulted to violation confirmation of {}", violation);
					}
				}
				return violation;
			};

		constraintsCheckingResult = provisioningService.checkConstraints(projOcDef, projectionNew,
				projectionContext.getResource(), projectionContext.getOid(), projectionContext.getResourceShadowDiscriminator(),
				confirmer, task, result);

		if (constraintsCheckingResult.isSatisfiesConstraints()) {
			satisfiesConstraints = true;
			return;
		}
		for (QName checkedAttributeName: constraintsCheckingResult.getCheckedAttributes()) {
			if (constraintsCheckingResult.getConflictingAttributes().contains(checkedAttributeName)) {
				if (isInDelta(checkedAttributeName, projectionContext.getPrimaryDelta())) {
					throw new ObjectAlreadyExistsException("Attribute "+checkedAttributeName+" conflicts with existing object (and it is present in primary "+
							"account delta therefore no iteration is performed)");
				}
			}
		}
		if (projectionContext.getResourceShadowDiscriminator() != null && projectionContext.getResourceShadowDiscriminator().isThombstone()) {
			satisfiesConstraints = true;
		} else {
			satisfiesConstraints = false;
		}
	}


	private boolean isInDelta(QName attrName, ObjectDelta<ShadowType> delta) {
		if (delta == null) {
			return false;
		}
		return delta.hasItemDelta(new ItemPath(ShadowType.F_ATTRIBUTES, attrName));
	}

}
