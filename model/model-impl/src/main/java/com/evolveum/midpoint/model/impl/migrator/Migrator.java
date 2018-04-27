/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.migrator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author semancik
 *
 */
@Component
public class Migrator {

	private static final Trace LOGGER = TraceManager.getTrace(Migrator.class);

	@Autowired private PrismContext prismContext;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService repositoryService;

	public <I extends ObjectType, O extends ObjectType> PrismObject<O> migrate(PrismObject<I> original) {
		Class<I> origType = original.getCompileTimeClass();
		if (ObjectTemplateType.class.isAssignableFrom(origType)) {
			PrismObject<ObjectTemplateType> out = migrateObjectTemplate((PrismObject<ObjectTemplateType>) original);
			return (PrismObject<O>) out;
		}
		if (ResourceType.class.isAssignableFrom(origType)) {
			PrismObject<ResourceType> out = migrateResource((PrismObject<ResourceType>) original);
			return (PrismObject<O>) out;
		}
		if (FocusType.class.isAssignableFrom(origType)) {
			PrismObject<FocusType> out = migrateFocus((PrismObject<FocusType>) original);
			original = (PrismObject<I>) out;
		}
		if (UserType.class.isAssignableFrom(origType)) {
			PrismObject<UserType> out = migrateUser((PrismObject<UserType>) original);
			return (PrismObject<O>) out;
		}
		return (PrismObject<O>) original;
	}

	private PrismObject<ObjectTemplateType> migrateObjectTemplate(PrismObject<ObjectTemplateType> orig) {
		QName elementName = orig.getElementName();
		if (elementName.equals(SchemaConstants.C_OBJECT_TEMPLATE)) {
			return orig;
		}
		PrismObject<ObjectTemplateType> migrated = orig.clone();
		migrated.setElementName(SchemaConstants.C_OBJECT_TEMPLATE);
		return migrated;
	}

	private PrismObject<ResourceType> migrateResource(PrismObject<ResourceType> orig) {
		return orig;
	}

	private void migrateObjectSynchronization(ObjectSynchronizationType sync) {
		if (sync == null || sync.getReaction() == null){
			return;
		}

		List<SynchronizationReactionType> migratedReactions = new ArrayList<>();
		for (SynchronizationReactionType reaction : sync.getReaction()){
			if (reaction.getAction() == null){
				continue;
			}
			List<SynchronizationActionType> migratedAction = new ArrayList<>();
			for (SynchronizationActionType action : reaction.getAction()){
				migratedAction.add(migrateAction(action));
			}
			SynchronizationReactionType migratedReaction = reaction.clone();
			migratedReaction.getAction().clear();
			migratedReaction.getAction().addAll(migratedAction);
			migratedReactions.add(migratedReaction);
		}

		sync.getReaction().clear();
		sync.getReaction().addAll(migratedReactions);
	}

	private SynchronizationActionType migrateAction(SynchronizationActionType action){
		if (action.getUserTemplateRef() == null){
			return action;
		}

		action.setObjectTemplateRef(action.getUserTemplateRef());

		return action;
	}

	private PrismObject<FocusType> migrateFocus(PrismObject<FocusType> orig) {
		return orig;
	}

	private PrismObject<UserType> migrateUser(PrismObject<UserType> orig) {
		return orig;
	}

	public <F extends ObjectType> void executeAfterOperationMigration(LensContext<F> context, OperationResult result) {
		removeEvaluatedTriggers(context, result);           // from 3.6 to 3.7
	}

	private <F extends ObjectType> void removeEvaluatedTriggers(LensContext<F> context, OperationResult result) {
		LensFocusContext<F> focusContext = context.getFocusContext();
		if (focusContext == null || focusContext.getObjectNew() == null) {
			return;
		}
		F objectNew = focusContext.getObjectNew().asObjectable();
		if (!(objectNew instanceof FocusType) || objectNew.getOid() == null) {
			return;
		}
		try {
			List<ItemDelta<?, ?>> deltas = new ArrayList<>();
			for (AssignmentType assignment : ((FocusType) objectNew).getAssignment()) {
				if (!assignment.getTrigger().isEmpty()) {
					Long id = assignment.getId();
					if (id == null) {
						LOGGER.warn("Couldn't remove evaluated triggers from an assignment because the assignment has no ID. "
								+ "Object: {}, assignment: {}", objectNew, assignment);
					} else {
						deltas.add(
								DeltaBuilder.deltaFor(FocusType.class, prismContext)
										.item(FocusType.F_ASSIGNMENT, id, AssignmentType.F_TRIGGER)
										.replace()
										.asItemDelta());
					}
				}
			}
			if (!deltas.isEmpty()) {
				LOGGER.info("Removing old-style evaluated triggers from {}", objectNew);
				repositoryService.modifyObject(objectNew.getClass(), objectNew.getOid(), deltas, result);
			}
		} catch (ObjectNotFoundException|SchemaException|ObjectAlreadyExistsException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove old-style evaluated triggers from object {}", e, objectNew);
		}
	}
}
