/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.model.impl.lens.projector.ComplexConstructionConsumer;
import com.evolveum.midpoint.model.impl.lens.projector.ConstructionProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.ObjectTemplateProcessor;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaMapTriple;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.HumanReadableDescribable;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

/**
 * Runs persona-related changes after the primary operation is all done. Executed directly from clockwork.
 * 
 * Not entirely clean solution. Ideally, this should be somehow integrated in the clockwork process (nested contexts).
 * But should be good enough for now.
 * 
 * @author semancik
 */
@Component
public class PersonaProcessor {
	
	private static final Trace LOGGER = TraceManager.getTrace(PersonaProcessor.class);
	
	@Autowired(required=true)
    private ConstructionProcessor constructionProcessor;
	
	@Autowired(required=true)
	private ObjectTemplateProcessor objectTemplateProcessor;
	
	@Autowired(required=true)
    private ObjectResolver objectResolver;
	
	@Autowired(required=true)
	private ModelService modelService;
	
	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService repositoryService;
	
	@Autowired(required=true)
	private Clock clock;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public <O extends ObjectType> HookOperationMode processPersonaChanges(LensContext<O> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
		
		LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return HookOperationMode.FOREGROUND;
    	}
    	if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for FocusType.
    		return HookOperationMode.FOREGROUND;
    	}
    	
    	if (focusContext.isDelete()) {
    		// Special case. Simply delete all the existing personas.
    		// TODO: maybe we need to do this before actual focus delete?
    		LOGGER.trace("Focus delete -> delete all personas");
    		// TODO
    		return HookOperationMode.FOREGROUND;
    	}
		
		return processPersonaChangesFocus((LensContext) context, task, result);
	}
	
	public <F extends FocusType> HookOperationMode processPersonaChangesFocus(LensContext<F> context, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException {
		DeltaSetTriple<EvaluatedAssignmentImpl<F>> evaluatedAssignmentTriple = (DeltaSetTriple)context.getEvaluatedAssignmentTriple();
		if (evaluatedAssignmentTriple == null || evaluatedAssignmentTriple.isEmpty()) {
			return HookOperationMode.FOREGROUND;
		}
		
		DeltaSetTriple<PersonaKey> activePersonaKeyTriple = new DeltaSetTriple<>();
		
		ComplexConstructionConsumer<PersonaKey,PersonaConstruction<F>> consumer = new ComplexConstructionConsumer<PersonaKey,PersonaConstruction<F>>() {

			@Override
			public boolean before(PersonaKey key) {
				return true;
			}

			@Override
			public void onAssigned(PersonaKey key, String desc) {
				activePersonaKeyTriple.addToPlusSet(key);
			}

			@Override
			public void onUnchangedValid(PersonaKey key, String desc) {
				activePersonaKeyTriple.addToZeroSet(key);
			}

			@Override
			public void onUnchangedInvalid(PersonaKey key, String desc) {
				
			}

			@Override
			public void onUnassigned(PersonaKey key, String desc) {
				activePersonaKeyTriple.addToMinusSet(key);
			}

			@Override
			public void after(PersonaKey key, String desc,
					DeltaMapTriple<PersonaKey, ConstructionPack<PersonaConstruction<F>>> constructionMapTriple) {
			}

			
		};
		
		DeltaMapTriple<PersonaKey, ConstructionPack<PersonaConstruction<F>>> constructionMapTriple = 
			constructionProcessor.processConstructions(context, evaluatedAssignmentTriple, 
				evaluatedAssignment -> evaluatedAssignment.getPersonaConstructionTriple(),
        		construction -> new PersonaKey(construction.getConstructionType()),
        		consumer,
        		task, result);
		
		LOGGER.trace("activePersonaKeyTriple:\n{}", activePersonaKeyTriple.debugDumpLazily());
		
		List<FocusType> existingPersonas = readExistingPersonas(context, task, result);
		LOGGER.trace("existingPersonas:\n{}", existingPersonas);
		
		for (PersonaKey key: activePersonaKeyTriple.getNonNegativeValues()) {
			FocusType existingPersona = findPersona(existingPersonas, key);
			LOGGER.trace("existingPersona: {}", existingPersona);
			// TODO: add ability to merge several constructions
			ConstructionPack<PersonaConstruction<F>> pack = constructionMapTriple.getPlusMap().get(key);
			if (pack == null) {
				pack = constructionMapTriple.getZeroMap().get(key);
			}
			Collection<PrismPropertyValue<PersonaConstruction<F>>> constructions = pack.getConstructions();
			if (constructions.isEmpty()) {
				continue;
			}
			if (constructions.size() > 1) {
				throw new UnsupportedOperationException("Merging of multiple persona constructions is not supported yet");
			}
			PersonaConstruction<F> construction = constructions.iterator().next().getValue();
			LOGGER.trace("construction:\n{}", construction.debugDumpLazily());
			if (existingPersona == null) {
				personaAdd(context, key, construction, task, result);
			} else {
				personaModify(context, key, construction, existingPersona.asPrismObject(), task, result);
			}
		}
		
		for (PersonaKey key: activePersonaKeyTriple.getMinusSet()) {
			FocusType existingPersona = findPersona(existingPersonas, key);
			if (existingPersona != null) {
				personaDelete(context, key, existingPersona, task, result);
			}
		}
		
		return HookOperationMode.FOREGROUND;
	}
	
	
	public <F extends FocusType> List<FocusType> readExistingPersonas(LensContext<F> context, Task task, OperationResult result) {
		LensFocusContext<F> focusContext = context.getFocusContext();
		PrismObject<F> focus = focusContext.getObjectNew();
		
		List<FocusType> personas = new ArrayList<>();
		String desc = "personaRef in "+focus;
		for (ObjectReferenceType personaRef: focus.asObjectable().getPersonaRef()) {
			try {
				FocusType persona = objectResolver.resolve(personaRef, FocusType.class, null, desc, task, result);
				personas.add(persona);
			} catch (ObjectNotFoundException | SchemaException e) {
				LOGGER.warn("Cannot find persona {} referenced from {}", personaRef.getOid(), focus);
				// But go on...
			}
		}
		
		return personas;
	}

	private FocusType findPersona(List<FocusType> personas, PersonaKey key) {
		for (FocusType persona: personas) {
			if (personaMatches(persona, key)) {
				return persona;
			}
		}
		return null;
	}

	private boolean personaMatches(FocusType persona, PersonaKey key) {
		PrismObject<? extends FocusType> personaObj = persona.asPrismObject();
		QName personaType = personaObj.getDefinition().getTypeName();
		if (!QNameUtil.match(personaType, key.getType())) {
			return false;
		}
		List<String> objectSubtypes = FocusTypeUtil.determineSubTypes(personaObj);
		for (String keySubtype: key.getSubtypes()) {
			if (!objectSubtypes.contains(keySubtype)) {
				return false;
			}
		}
		return true;
	}
	
	public <F extends FocusType, T extends FocusType> void personaAdd(LensContext<F> context, PersonaKey key, PersonaConstruction<F> construction, 
			Task task, OperationResult result) 
					throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<F> focus = context.getFocusContext().getObjectNew();
		LOGGER.debug("Adding persona {} for {} using construction {}", key, focus, construction);
		PersonaConstructionType constructionType = construction.getConstructionType();
		ObjectReferenceType objectMappingRef = constructionType.getObjectMappingRef();
		ObjectTemplateType objectMappingType = objectResolver.resolve(objectMappingRef, ObjectTemplateType.class, null, "object mapping in persona construction in "+focus, task, result);
		
		QName targetType = constructionType.getTargetType();
		PrismObjectDefinition<T> objectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(targetType);
		PrismObject<T> target = objectDef.instantiate();
		
		FocusTypeUtil.setSubtype(target, constructionType.getTargetSubtype());
		
		// pretend ADD focusOdo. We need to push all the items through the object template
		ObjectDeltaObject<F> focusOdo = new ObjectDeltaObject<>(null, focus.createAddDelta(), focus);
		ObjectDelta<T> targetDelta = target.createAddDelta();
		
		String contextDesc = "object mapping "+objectMappingType+ " for persona construction for "+focus;
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		
		Collection<ItemDelta<?, ?>> itemDeltas = objectTemplateProcessor.processObjectMapping(context, objectMappingType, 
				focusOdo, target, targetDelta, contextDesc, now, task, result);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("itemDeltas:\n{}", DebugUtil.debugDump(itemDeltas));
		}
		
		for (ItemDelta itemDelta: itemDeltas) {
			itemDelta.applyTo(target);
		}
		
		LOGGER.trace("Creating persona:\n{}", target.debugDumpLazily());
		
		modelService.executeChanges(MiscSchemaUtil.createCollection(targetDelta), null, task, result);
		
		link(context, target.asObjectable(), result);
	}
	
	public <F extends FocusType, T extends FocusType> void personaModify(LensContext<F> context, PersonaKey key, PersonaConstruction<F> construction, 
			PrismObject<T> existingPersona, Task task, OperationResult result) 
					throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<F> focus = context.getFocusContext().getObjectNew();
		LOGGER.debug("Modifying persona {} for {} using construction {}", key, focus, construction);
		PersonaConstructionType constructionType = construction.getConstructionType();
		ObjectReferenceType objectMappingRef = constructionType.getObjectMappingRef();
		ObjectTemplateType objectMappingType = objectResolver.resolve(objectMappingRef, ObjectTemplateType.class, null, "object mapping in persona construction in "+focus, task, result);
		
		ObjectDeltaObject<F> focusOdo = context.getFocusContext().getObjectDeltaObject();
		String contextDesc = "object mapping "+objectMappingType+ " for persona construction for "+focus;
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		
		Collection<ItemDelta<?, ?>> itemDeltas = objectTemplateProcessor.processObjectMapping(context, objectMappingType, 
				focusOdo, existingPersona, null, contextDesc, now, task, result);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("itemDeltas:\n{}", DebugUtil.debugDump(itemDeltas));
		}
		
		ObjectDelta<T> targetDelta = existingPersona.createModifyDelta();
		for (ItemDelta itemDelta: itemDeltas) {
			targetDelta.addModification(itemDelta);
		}
		
		modelService.executeChanges(MiscSchemaUtil.createCollection(targetDelta), null, task, result);
	}
	
	public <F extends FocusType> void personaDelete(LensContext<F> context, PersonaKey key, FocusType existingPersona, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {		
		PrismObject<F> focus = context.getFocusContext().getObjectOld();
		LOGGER.debug("Deleting persona {} for {}: ", key, focus, existingPersona);
		ObjectDelta<? extends FocusType> delta = existingPersona.asPrismObject().createDeleteDelta();		
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		unlink(context, existingPersona, result);
	}

	private <F extends FocusType>  void link(LensContext<F> context, FocusType persona, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		ObjectDelta<F> delta = context.getFocusContext().getObjectNew().createModifyDelta();
		PrismReferenceValue refValue = new PrismReferenceValue();
		refValue.setOid(persona.getOid());
		refValue.setTargetType(persona.asPrismObject().getDefinition().getTypeName());
		delta.addModificationAddReference(FocusType.F_PERSONA_REF, refValue);
		
		repositoryService.modifyObject(delta.getObjectTypeClass(), delta.getOid(), delta.getModifications(), result);
	}
	
	private <F extends FocusType>  void unlink(LensContext<F> context, FocusType persona, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		ObjectDelta<F> delta = context.getFocusContext().getObjectNew().createModifyDelta();
		PrismReferenceValue refValue = new PrismReferenceValue();
		refValue.setOid(persona.getOid());
		refValue.setTargetType(persona.asPrismObject().getDefinition().getTypeName());
		delta.addModificationDeleteReference(FocusType.F_PERSONA_REF, refValue);
		
		repositoryService.modifyObject(delta.getObjectTypeClass(), delta.getOid(), delta.getModifications(), result);
	}

	class PersonaKey implements HumanReadableDescribable {
		
		private QName type;
		private List<String> subtypes;

		public PersonaKey(PersonaConstructionType constructionType) {
			super();
			this.type = constructionType.getTargetType();
			this.subtypes = constructionType.getTargetSubtype();
		}
		
		public QName getType() {
			return type;
		}

		public List<String> getSubtypes() {
			return subtypes;
		}

		@Override
		public String toHumanReadableDescription() {
			return "persona "+type.getLocalPart()+"/"+subtypes+"'";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((subtypes == null) ? 0 : subtypes.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PersonaKey other = (PersonaKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (subtypes == null) {
				if (other.subtypes != null)
					return false;
			} else if (!subtypes.equals(other.subtypes))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "PersonaKey(" + type + "/" + subtypes + ")";
		}

		private PersonaProcessor getOuterType() {
			return PersonaProcessor.this;
		}
		
	}

}
