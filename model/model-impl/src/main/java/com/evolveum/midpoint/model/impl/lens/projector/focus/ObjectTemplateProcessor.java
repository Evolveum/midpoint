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
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.ItemValueWithOrigin;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.MappingEvaluator;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocalAutoassignSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleManagementConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Processor to handle object template.
 *
 * @author Radovan Semancik
 *
 */
@Component
public class ObjectTemplateProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

	@Autowired
	private MappingFactory mappingFactory;

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private ModelObjectResolver modelObjectResolver;

	@Autowired
	@Qualifier("cacheRepositoryService")
	private transient RepositoryService cacheRepositoryService;

	@Autowired
    private MappingEvaluator mappingEvaluator;

	@Autowired
	private MatchingRuleRegistry matchingRuleRegistry;

	/**
	 * Process focus template: application of object template where focus is both source and target.
	 */
	public <F extends FocusType> void processTemplate(LensContext<F> context, ObjectTemplateMappingEvaluationPhaseType phase,
			XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, ObjectAlreadyExistsException, SecurityViolationException, ConfigurationException, CommunicationException {
		LensFocusContext<F> focusContext = context.getFocusContext();
    	if (focusContext.isDelete()) {
    		LOGGER.trace("Skipping processing of object template: focus delete");
    		return;
    	}

    	ObjectTemplateType objectTemplate = context.getFocusTemplate();
    	String objectTemplateDesc = "(no template)";
		if (objectTemplate != null) {
			objectTemplateDesc = objectTemplate.toString();
		}

		int iteration = focusContext.getIteration();
		String iterationToken = focusContext.getIterationToken();
		ObjectDeltaObject<F> focusOdo = focusContext.getObjectDeltaObject();
		PrismObjectDefinition<F> focusDefinition = getObjectDefinition(focusContext.getObjectTypeClass());

		LOGGER.trace("Applying object template {} to {}, iteration {} ({}), phase {}",
				objectTemplate, focusContext.getObjectNew(), iteration, iterationToken, phase);

		Map<ItemPath,ObjectTemplateItemDefinitionType> itemDefinitionsMap = collectItemDefinitionsFromTemplate(objectTemplate, objectTemplateDesc, task, result);

		List<FocalMappingSpec> mappings = new ArrayList<>();
		collectMappingsFromTemplate(context, mappings, objectTemplate, objectTemplateDesc, task, result);
		collectAutoassignMappings(context, mappings, task, result);

		Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap = new HashMap<>();
		XMLGregorianCalendar nextRecomputeTime = collectTripleFromMappings(context, mappings, phase, focusOdo, focusOdo.getNewObject(), 
				outputTripleMap, iteration, iterationToken, now, task, result);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("outputTripleMap before item delta computation:\n{}", DebugUtil.debugDumpMapMultiLine(outputTripleMap));
		}

		String contextDesc = "object template "+objectTemplateDesc+ " for focus "+focusOdo.getAnyObject();
		Collection<ItemDelta<?,?>> itemDeltas = computeItemDeltas(outputTripleMap, itemDefinitionsMap, focusOdo.getObjectDelta(), focusOdo.getNewObject(),
				focusDefinition, contextDesc);

		focusContext.applyProjectionWaveSecondaryDeltas(itemDeltas);

		if (nextRecomputeTime != null) {

			boolean alreadyHasTrigger = false;
			PrismObject<F> objectCurrent = focusContext.getObjectCurrent();
			if (objectCurrent != null) {
				for (TriggerType trigger: objectCurrent.asObjectable().getTrigger()) {
					if (RecomputeTriggerHandler.HANDLER_URI.equals(trigger.getHandlerUri()) &&
							nextRecomputeTime.equals(trigger.getTimestamp())) {
								alreadyHasTrigger = true;
								break;
					}
				}
			}

			if (!alreadyHasTrigger) {
				PrismObjectDefinition<F> objectDefinition = focusContext.getObjectDefinition();
				PrismContainerDefinition<TriggerType> triggerContDef = objectDefinition.findContainerDefinition(ObjectType.F_TRIGGER);
				ContainerDelta<TriggerType> triggerDelta = triggerContDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
				PrismContainerValue<TriggerType> triggerCVal = triggerContDef.createValue();
				triggerDelta.addValueToAdd(triggerCVal);
				TriggerType triggerType = triggerCVal.asContainerable();
				triggerType.setTimestamp(nextRecomputeTime);
				triggerType.setHandlerUri(RecomputeTriggerHandler.HANDLER_URI);

				focusContext.swallowToProjectionWaveSecondaryDelta(triggerDelta);
			}
		}

	}

	/**
	 * Processing object mapping: application of object template where focus is the source and another object is the target.
	 * Used to map focus to personas.
	 */
	public <F extends FocusType, T extends FocusType> Collection<ItemDelta<?,?>> processObjectMapping(LensContext<F> context,
			ObjectTemplateType objectMappingType, ObjectDeltaObject<F> focusOdo, PrismObject<T> target, ObjectDelta<T> targetAPrioriDelta,
			String contextDesc, XMLGregorianCalendar now, Task task, OperationResult result)
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException, ObjectAlreadyExistsException, SecurityViolationException, ConfigurationException, CommunicationException {
		LensFocusContext<F> focusContext = context.getFocusContext();

		int iteration = 0;
		String iterationToken = null;
		PrismObjectDefinition<F> focusDefinition = getObjectDefinition(focusContext.getObjectTypeClass());

		LOGGER.trace("Applying object mapping {} from {} to {}, iteration {} ({})",
				objectMappingType, focusContext.getObjectNew(), target, iteration, iterationToken);

		Map<ItemPath,ObjectTemplateItemDefinitionType> itemDefinitionsMap = collectItemDefinitionsFromTemplate(objectMappingType,
				objectMappingType.toString(), task, result);
		
		List<FocalMappingSpec> mappings = new ArrayList<>();
		collectMappingsFromTemplate(context, mappings, objectMappingType, objectMappingType.toString(), task, result);
		collectAutoassignMappings(context, mappings, task, result);

		Map<ItemPath,DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap = new HashMap<>();
		XMLGregorianCalendar nextRecomputeTime = collectTripleFromMappings(context, mappings, ObjectTemplateMappingEvaluationPhaseType.BEFORE_ASSIGNMENTS, 
				focusOdo, target, outputTripleMap, iteration, iterationToken, now, task, result);
		

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("outputTripleMap before item delta computation:\n{}", DebugUtil.debugDumpMapMultiLine(outputTripleMap));
		}

		Collection<ItemDelta<?,?>> itemDeltas = computeItemDeltas(outputTripleMap, itemDefinitionsMap, targetAPrioriDelta, target, focusDefinition, contextDesc);

		return itemDeltas;
	}

	private Map<ItemPath, ObjectTemplateItemDefinitionType> collectItemDefinitionsFromTemplate(ObjectTemplateType objectTemplateType, String contextDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Map<ItemPath, ObjectTemplateItemDefinitionType> definitions = new HashMap<>();
		if (objectTemplateType == null) {
			return definitions;
		}
		// Process includes (TODO refactor as a generic method)
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> includeObject = includeRef.asReferenceValue().getObject();
			if (includeObject == null) {
				ObjectTemplateType includeObjectType = modelObjectResolver.resolve(includeRef, ObjectTemplateType.class,
						null, "include reference in "+objectTemplateType + " in " + contextDesc, task, result);
				includeObject = includeObjectType.asPrismObject();
				// Store resolved object for future use (e.g. next waves).
				includeRef.asReferenceValue().setObject(includeObject);
			}
			LOGGER.trace("Including template {}", includeObject);
			ObjectTemplateType includeObjectType = includeObject.asObjectable();
			Map<ItemPath, ObjectTemplateItemDefinitionType> includedDefinitions = collectItemDefinitionsFromTemplate(includeObjectType, "include "+includeObject+" in "+objectTemplateType + " in " + contextDesc, task, result);
			if (includedDefinitions != null) {
				ItemPathUtil.putAllToMap(definitions, includedDefinitions);
			}
		}

		// Process own definitions
		for (ObjectTemplateItemDefinitionType def : objectTemplateType.getItem()) {
			if (def.getRef() == null) {
				throw new IllegalStateException("Item definition with null ref in " + contextDesc);
			}
			ItemPathUtil.putToMap(definitions, def.getRef().getItemPath(), def);	// TODO check for incompatible overrides
		}
		return definitions;
	}
	
	<F extends FocusType, T extends FocusType> Collection<ItemDelta<?,?>> computeItemDeltas(Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap,
			@Nullable Map<ItemPath,ObjectTemplateItemDefinitionType> itemDefinitionsMap,
			ObjectDelta<T> targetObjectAPrioriDelta, PrismObject<T> targetObject, PrismObjectDefinition<F> focusDefinition, String contextDesc) throws ExpressionEvaluationException, PolicyViolationException, SchemaException {

		Collection<ItemDelta<?,?>> itemDeltas = new ArrayList<>();

		LOGGER.trace("Computing deltas in {}, focusDelta:\n{}", contextDesc, targetObjectAPrioriDelta);

		boolean addUnchangedValues = false;
		if (targetObjectAPrioriDelta != null && targetObjectAPrioriDelta.isAdd()) {
			addUnchangedValues = true;
		}

		for (Entry<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> entry: outputTripleMap.entrySet()) {
			ItemPath itemPath = entry.getKey();
			DeltaSetTriple<? extends ItemValueWithOrigin<?,?>> outputTriple = entry.getValue();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Computed triple for {}:\n{}", itemPath, outputTriple.debugDump());
			}
			final ObjectTemplateItemDefinitionType templateItemDefinition;
			if (itemDefinitionsMap != null) {
				templateItemDefinition = ItemPathUtil.getFromMap(itemDefinitionsMap, itemPath);
			} else {
				templateItemDefinition = null;
			}
			boolean isNonTolerant = templateItemDefinition != null && Boolean.FALSE.equals(templateItemDefinition.isTolerant());

			ItemDelta aprioriItemDelta = getAprioriItemDelta(targetObjectAPrioriDelta, itemPath);
			boolean filterExistingValues = !isNonTolerant;	// if non-tolerant, we want to gather ZERO & PLUS sets
			ItemDefinition itemDefinition = focusDefinition.findItemDefinition(itemPath);
			ItemDelta itemDelta = LensUtil.consolidateTripleToDelta(itemPath, (DeltaSetTriple)outputTriple,
					itemDefinition, aprioriItemDelta, targetObject, null, null,
					addUnchangedValues, filterExistingValues, false, contextDesc, true);


			// Do a quick version of reconciliation. There is not much to reconcile as both the source and the target
			// is focus. But there are few cases to handle, such as strong mappings, and sourceless normal mappings.
			Collection<? extends ItemValueWithOrigin<?,?>> zeroSet = outputTriple.getZeroSet();
			Item<PrismValue, ItemDefinition> itemNew = null;
			if (targetObject != null) {
				itemNew = targetObject.findItem(itemPath);
			}
			for (ItemValueWithOrigin<?,?> zeroSetIvwo: zeroSet) {

				PrismValueDeltaSetTripleProducer<?, ?> mapping = zeroSetIvwo.getMapping();
				if ((mapping.getStrength() == null || mapping.getStrength() == MappingStrengthType.NORMAL)) {
					if (aprioriItemDelta != null && !aprioriItemDelta.isEmpty()) {
						continue;
					}
					if (!mapping.isSourceless()) {
						continue;
					}
					LOGGER.trace("Adding zero values from normal mapping {}, a-priori delta: {}, isSourceless: {}",
							mapping, aprioriItemDelta, mapping.isSourceless());
				} else if (mapping.getStrength() == MappingStrengthType.WEAK) {
					if ((itemNew != null && !itemNew.isEmpty()) || (itemDelta != null && itemDelta.addsAnyValue())) {
						continue;
					}
					LOGGER.trace("Adding zero values from weak mapping {}, itemNew: {}, itemDelta: {}",
							mapping, itemNew, itemDelta);
				} else {
					LOGGER.trace("Adding zero values from strong mapping {}", mapping);
				}

				PrismValue valueFromZeroSet = zeroSetIvwo.getItemValue();
				if (itemNew == null || !itemNew.containsRealValue(valueFromZeroSet)) {
					LOGGER.trace("Reconciliation will add value {} for item {}. Existing item: {}", valueFromZeroSet, itemPath, itemNew);
					itemDelta.addValuesToAdd(valueFromZeroSet.clone());
				}
			}


			if (isNonTolerant) {
				if (itemDelta.isDelete()) {
					LOGGER.trace("Non-tolerant item with values to DELETE => removing them");
					itemDelta.resetValuesToDelete();        // these are useless now - we move everything to REPLACE
				}
				if (itemDelta.isReplace()) {
					LOGGER.trace("Non-tolerant item with resulting REPLACE delta => doing nothing");
				} else {
					for (ItemValueWithOrigin<?,?> zeroSetIvwo: zeroSet) {
						itemDelta.addValuesToAdd(zeroSetIvwo.getItemValue().clone());
					}
					itemDelta.addToReplaceDelta();
					LOGGER.trace("Non-tolerant item with resulting ADD delta => converted ADD to REPLACE values: {}", itemDelta.getValuesToReplace());
				}

				// To avoid phantom changes, compare with existing values (MID-2499).
				// TODO this should be maybe moved into LensUtil.consolidateTripleToDelta (along with the above code), e.g.
				// under a special option "createReplaceDelta", but for the time being, let's keep it here
				if (itemDelta instanceof PropertyDelta) {
					PropertyDelta propertyDelta = ((PropertyDelta) itemDelta);
					QName matchingRuleName = templateItemDefinition != null ? templateItemDefinition.getMatchingRule() : null;
					MatchingRule matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleName, null);
					if (propertyDelta.isRedundant(targetObject, matchingRule)) {
						LOGGER.trace("Computed property delta is redundant => skipping it. Delta = \n{}", propertyDelta.debugDump());
						continue;
					}
				} else {
					if (itemDelta.isRedundant(targetObject)) {
						LOGGER.trace("Computed item delta is redundant => skipping it. Delta = \n{}", itemDelta.debugDump());
						continue;
					}
				}
				PrismUtil.setDeltaOldValue(targetObject, itemDelta);
			}

			itemDelta.simplify();
			itemDelta.validate(contextDesc);
			itemDeltas.add(itemDelta);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Computed delta:\n{}", itemDelta.debugDump());
			}
		}
		return itemDeltas;
	}

	private <F extends FocusType> ItemDelta getAprioriItemDelta(ObjectDelta<F> focusDelta, ItemPath itemPath) {
		return focusDelta != null ? focusDelta.findItemDelta(itemPath) : null;
	}

	
	private <F extends FocusType, T extends FocusType> void collectMappingsFromTemplate(LensContext<F> context,
			List<FocalMappingSpec> mappings, ObjectTemplateType objectTemplateType, String contextDesc, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {
		if (objectTemplateType == null) {
			return;
		}
		LOGGER.trace("Collecting mappings from {}", objectTemplateType);

		// Process includes
		for (ObjectReferenceType includeRef: objectTemplateType.getIncludeRef()) {
			PrismObject<ObjectTemplateType> includeObject = includeRef.asReferenceValue().getObject();
			if (includeObject == null) {
				ObjectTemplateType includeObjectType = modelObjectResolver.resolve(includeRef, ObjectTemplateType.class,
						null, "include reference in "+objectTemplateType + " in " + contextDesc, task, result);
				includeObject = includeObjectType.asPrismObject();
				// Store resolved object for future use (e.g. next waves).
				includeRef.asReferenceValue().setObject(includeObject);
			}
			LOGGER.trace("Including template {}", includeObject);
			ObjectTemplateType includeObjectType = includeObject.asObjectable();
			collectMappingsFromTemplate(context, mappings, includeObjectType, "include "+includeObject+" in "+objectTemplateType + " in " + contextDesc, task, result);
		}

		// Process own mappings
		collectMappings(mappings, objectTemplateType);
	}

	private void collectMappings(List<FocalMappingSpec> mappings, ObjectTemplateType objectTemplateType) {
		for (ObjectTemplateMappingType mapping: objectTemplateType.getMapping()) {
			mappings.add(new FocalMappingSpec(mapping, objectTemplateType));
		}
		for (ObjectTemplateItemDefinitionType templateItemDefType: objectTemplateType.getItem()) {
			for (ObjectTemplateMappingType mapping: templateItemDefType.getMapping()) {
				setMappingTarget(mapping, templateItemDefType.getRef());
				mappings.add(new FocalMappingSpec(mapping, objectTemplateType));
			}
		}
	}
	
	private <F extends FocusType, T extends FocusType> void collectAutoassignMappings(LensContext<F> context,
			List<FocalMappingSpec> mappings, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

		if (!autoassignEnabled(context.getSystemConfiguration())) {
			return;
		}
		
		// TODO TODO TODO
		// WARNING DANGER MONSTERS LIONS DRAGONS
		// TODO: put the right query here. When the property is indexed. Otherwise performance will suck 
		
//		ObjectQuery query = QueryBuilder
//				.queryFor(AbstractRoleType.class, prismContext)
//				.item(SchemaConstants.PATH_AUTOASSIGN_ENABLED)
//				.eq(true)
//				.build();
		
		ObjectQuery query = null;
		
		ResultHandler<AbstractRoleType> handler = (role, objectResult) -> {
			AutoassignSpecificationType autoassign = role.asObjectable().getAutoassign();
			if (autoassign == null) {
				return true;
			}
			if (!BooleanUtils.isTrue(autoassign.isEnabled())) {
				return true;
			}
			FocalAutoassignSpecificationType focalAutoassignSpec = autoassign.getFocus();
			if (focalAutoassignSpec == null) {
				return true;
			}
			for (AutoassignMappingType autoMapping: focalAutoassignSpec.getMapping()) {
				AutoassignMappingType mapping = autoMapping.clone();
				setMappingTarget(mapping, SchemaConstants.PATH_ASSIGNMENT.asItemPathType());
				mappings.add(new FocalMappingSpec(mapping, role.asObjectable()));
				LOGGER.trace("Collected autoassign mapping {} from {}", mapping.getName(), role);
			}
			return true;
		};
		cacheRepositoryService.searchObjectsIterative(AbstractRoleType.class, query, handler, GetOperationOptions.createReadOnlyCollection(), false, result);		
	}

	private void setMappingTarget(MappingType mapping, ItemPathType path) {
		VariableBindingDefinitionType target = mapping.getTarget();
		if (target == null) {
			target = new VariableBindingDefinitionType();
			target.setPath(path);
			mapping.setTarget(target);
		} else if (target.getPath() == null) {
			target = target.clone();
			target.setPath(path);
			mapping.setTarget(target);
		}
	}

	private boolean autoassignEnabled(PrismObject<SystemConfigurationType> systemConfiguration) {
		if (systemConfiguration == null) {
			return false;
		}
		RoleManagementConfigurationType roleManagement = systemConfiguration.asObjectable().getRoleManagement();
		if (roleManagement == null) {
			return false;
		}
		return BooleanUtils.isTrue(roleManagement.isAutoassignEnabled());
	}

	
	/**
	 * If M2 has a source of X, and M1 has a target of X, then M1 must be placed before M2; we want also to detect cycles.
	 *
	 * So let's stratify mappings according to their dependencies.
 	 */
	private List<FocalMappingSpec> sortMappingsByDependencies(List<FocalMappingSpec> mappings) {
		// map.get(X) = { Y1 ... Yn } means that mapping X depends on output of mappings Y1 ... Yn
		// using indices instead of actual mappings because of equality issues
		Map<Integer, Set<Integer>> dependencyMap = createDependencyMap(mappings);
		LOGGER.trace("sortMappingsByDependencies: dependencyMap: {}", dependencyMap);

		List<Integer> processed = new ArrayList<>();
		List<Integer> toProcess = Stream.iterate(0, t -> t+1).limit(mappings.size()).collect(Collectors.toList());		// not a set: to preserve original order
		while (!toProcess.isEmpty()) {
			LOGGER.trace("sortMappingsByDependencies: toProcess: {}, processed: {}", toProcess, processed);
			Integer available = toProcess.stream()
					.filter(i -> CollectionUtils.isSubCollection(dependencyMap.get(i), processed))	// cannot depend on yet-unprocessed mappings
					.findFirst().orElse(null);
			if (available == null) {
				LOGGER.warn("Cannot sort mappings according to dependencies, there is a cycle. Processing in the original order: {}", mappings);
				return mappings;
			}
			processed.add(available);
			toProcess.remove(available);
		}
		LOGGER.trace("sortMappingsByDependencies: final ordering: {}", processed);
		return processed.stream().map(i -> mappings.get(i)).collect(Collectors.toList());
	}

	private Map<Integer, Set<Integer>> createDependencyMap(List<FocalMappingSpec> mappings) {
		Map<Integer, Set<Integer>> map = new HashMap<>();
		for (int i = 0; i < mappings.size(); i++) {
			Set<Integer> dependsOn = new HashSet<>();
			for (int j = 0; j < mappings.size(); j++) {
				if (i == j) {
					continue;
				}
				if (dependsOn(mappings.get(i), mappings.get(j))) {
					dependsOn.add(j);
				}
			}
			map.put(i, dependsOn);
		}
		return map;
	}

	// true if any source of mapping1 is equivalent to the target of mapping2
	private boolean dependsOn(FocalMappingSpec mappingSpec1, FocalMappingSpec mappingSpec2) {
		MappingType mapping1 = mappingSpec1.getMappingType();
		MappingType mapping2 = mappingSpec2.getMappingType();
		if (mapping2.getTarget() == null || mapping2.getTarget().getPath() == null) {
			return false;
		}
		ItemPath targetPath = mapping2.getTarget().getPath().getItemPath().stripVariableSegment();

		for (VariableBindingDefinitionType source : mapping1.getSource()) {
			ItemPath sourcePath = source.getPath() != null ? source.getPath().getItemPath() : null;
			if (sourcePath != null && stripFocusVariableSegment(sourcePath).equivalent(targetPath)) {
				return true;
			}
		}
		return false;
	}

	private <T extends Objectable> ItemPath stripFocusVariableSegment(ItemPath sourcePath) {
		if (sourcePath.startsWithVariable()
			&& QNameUtil.matchAny(ItemPath.getFirstName(sourcePath), MappingEvaluator.FOCUS_VARIABLE_NAMES)) {
			return sourcePath.stripVariableSegment();
		} else {
			return sourcePath;
		}
	}

	private <V extends PrismValue, D extends ItemDefinition, F extends FocusType, T extends FocusType> XMLGregorianCalendar collectTripleFromMappings(
			LensContext<F> context, List<FocalMappingSpec> mappings, ObjectTemplateMappingEvaluationPhaseType phase,
			ObjectDeltaObject<F> focusOdo, PrismObject<T> target,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?,?>>> outputTripleMap,
			int iteration, String iterationToken,
			XMLGregorianCalendar now, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, PolicyViolationException, SecurityViolationException, ConfigurationException, CommunicationException {

		List<FocalMappingSpec> sortedMappings = sortMappingsByDependencies(mappings);
		
		XMLGregorianCalendar nextRecomputeTime = null;

		for (FocalMappingSpec mappingSpec : sortedMappings) {
			ObjectTemplateMappingEvaluationPhaseType mappingPhase = mappingSpec.getEvaluationPhase();
			if (phase != null && mappingPhase != phase) {
				continue;
			}
			String mappingDesc = mappingSpec.shortDump();
			LOGGER.trace("Starting evaluation of {}", mappingDesc);
			ObjectDeltaObject<F> updatedFocusOdo = getUpdatedFocusOdo(context, focusOdo, outputTripleMap, mappingSpec, mappingDesc);		// for mapping chaining

			Mapping<V,D> mapping = mappingEvaluator.createFocusMapping(mappingFactory, context, mappingSpec.getMappingType(), 
					mappingSpec.getOriginObject(), updatedFocusOdo, mappingSpec.getDefaultSource(focusOdo), target,
					null, iteration, iterationToken, context.getSystemConfiguration(), now, mappingDesc, task, result);
			if (mapping == null) {
				continue;
			}

			Boolean timeConstraintValid = mapping.evaluateTimeConstraintValid(task, result);

			if (timeConstraintValid != null && !timeConstraintValid) {
				// Delayed mapping. Just schedule recompute time
				XMLGregorianCalendar mappingNextRecomputeTime = mapping.getNextRecomputeTime();
				LOGGER.trace("Evaluation of mapping {} delayed to {}", mapping, mappingNextRecomputeTime);
				if (mappingNextRecomputeTime != null) {
					if (nextRecomputeTime == null || nextRecomputeTime.compare(mappingNextRecomputeTime) == DatatypeConstants.GREATER) {
						nextRecomputeTime = mappingNextRecomputeTime;
					}
				}
				continue;
			}

			mappingEvaluator.evaluateMapping(mapping, context, task, result);

			ItemPath itemPath = mapping.getOutputPath();
            if (itemPath == null) {
                continue;
            }
			DeltaSetTriple<ItemValueWithOrigin<V,D>> outputTriple = ItemValueWithOrigin.createOutputTriple(mapping);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Output triple for {}:\n{}", mapping, DebugUtil.debugDump(outputTriple));
			}
			if (outputTriple == null) {
				continue;
			}
			DeltaSetTriple<ItemValueWithOrigin<V,D>> mapTriple = (DeltaSetTriple<ItemValueWithOrigin<V,D>>) outputTripleMap.get(itemPath);
			if (mapTriple == null) {
				outputTripleMap.put(itemPath, outputTriple);
			} else {
				mapTriple.merge(outputTriple);
			}
		}

		return nextRecomputeTime;
	}

	private <F extends FocusType> ObjectDeltaObject<F> getUpdatedFocusOdo(LensContext<F> context, ObjectDeltaObject<F> focusOdo,
			Map<ItemPath, DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>>> outputTripleMap,
			FocalMappingSpec mappingSpec, String contextDesc) throws ExpressionEvaluationException,
			PolicyViolationException, SchemaException {
		ObjectDeltaObject<F> focusOdoCloned = null;
		for (VariableBindingDefinitionType source : mappingSpec.getMappingType().getSource()) {
			if (source.getPath() == null) {
				continue;
			}
			ItemPath path = stripFocusVariableSegment(source.getPath().getItemPath());
			if (path.startsWithVariable()) {
				continue;
			}
			DeltaSetTriple<? extends ItemValueWithOrigin<?, ?>> triple = DeltaSetTriple.find(outputTripleMap, path);
			if (triple == null) {
				continue;
			}
			if (focusOdoCloned == null) {
				LOGGER.trace("Cloning and updating focusOdo because of chained mappings; chained source path: {}", path);
				focusOdoCloned = focusOdo.clone();
			} else {
				LOGGER.trace("Updating focusOdo because of chained mappings; chained source path: {}", path);
			}
			Class<F> focusClass = context.getFocusContext().getObjectTypeClass();
			ItemDefinition<?> itemDefinition = getObjectDefinition(focusClass).findItemDefinition(path);
			// TODO not much sure about the parameters
			ItemDelta itemDelta = LensUtil
					.consolidateTripleToDelta(path, (DeltaSetTriple) triple, itemDefinition,
							getAprioriItemDelta(focusOdo.getObjectDelta(), path), focusOdo.getNewObject(), null, null,
							true, true, false, " updating chained source (" + path + ") in " + contextDesc, true);
			LOGGER.trace("Updating focus ODO with delta:\n{}", itemDelta.debugDumpLazily());
			focusOdoCloned.update(itemDelta);
		}
		return focusOdoCloned != null ? focusOdoCloned : focusOdo;
	}

	private <F extends ObjectType> PrismObjectDefinition<F> getObjectDefinition(Class<F> focusClass) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(focusClass);
	}

}
