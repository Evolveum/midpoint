/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.model.lens.projector;

import java.util.List;

import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class FocusConstraintsChecker<F extends FocusType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(FocusConstraintsChecker.class);
	
	private LensContext<F> context;
	private PrismContext prismContext;
	private RepositoryService repositoryService;
	private boolean satisfiesConstraints;
	private StringBuilder messageBuilder = new StringBuilder();
	private PrismObject<F> conflictingObject;

	public FocusConstraintsChecker() {
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public LensContext<F> getContext() {
		return context;
	}
	
	public void setContext(LensContext<F> context) {
		this.context = context;
	}
	
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public boolean isSatisfiesConstraints() {
		return satisfiesConstraints;
	}
	
	public String getMessages() {
		return messageBuilder.toString();
	}

	public PrismObject<F> getConflictingObject() {
		return conflictingObject;
	}
	public void check(PrismObject<F> objectNew, OperationResult result) throws SchemaException {
		
		if (objectNew == null) {
			// This must be delete
			LOGGER.trace("No new object. Therefore it satisfies constraints");
			satisfiesConstraints = true;
			return;
		}
		
		// Hardcode to name ... for now		
		satisfiesConstraints = checkPropertyUniqueness(objectNew, new ItemPath(ObjectType.F_NAME), context, result);
	}
	
	private <T> boolean checkPropertyUniqueness(PrismObject<F> objectNew, ItemPath propPath, LensContext<F> context, OperationResult result) throws SchemaException {
		
		PrismProperty<T> property = objectNew.findProperty(propPath);
		if (property == null || property.isEmpty()) {
			throw new SchemaException("No property "+propPath+" in new object "+objectNew+", cannot check uniqueness");
		}
		String oid = objectNew.getOid();
		
		ObjectQuery query = ObjectQuery.createObjectQuery(
				EqualsFilter.createEqual(propPath, property.clone()));
		
		List<PrismObject<F>> foundObjects = repositoryService.searchObjects(objectNew.getCompileTimeClass(), query, null, result);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Uniqueness check of {}, property {} resulted in {} results, using query:\n{}",
				new Object[]{objectNew, propPath, foundObjects.size(), query.debugDump()});
		}
		if (foundObjects.isEmpty()) {
			return true;
		}
		if (foundObjects.size() > 1) {
			LOGGER.trace("Found more than one object with property "+propPath+" = " + property);
			message("Found more than one object with property "+propPath+" = " + property);
			return false;
		} 
		
		LOGGER.trace("Comparing {} and {}", foundObjects.get(0).getOid(), oid);
		boolean match = foundObjects.get(0).getOid().equals(oid);
		if (!match) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Found conflicting existing object with property "+propPath+" = " + property + ":\n"
						+ foundObjects.get(0).debugDump());
			}
			message("Found conflicting existing object with property "+propPath+" = " + property + ": "
					+ foundObjects.get(0));

			conflictingObject = foundObjects.get(0);
		}
		
		return match;
	}
	
	private void message(String message) {
		if (messageBuilder.length() != 0) {
			messageBuilder.append(", ");
		}
		messageBuilder.append(message);
	}

}
