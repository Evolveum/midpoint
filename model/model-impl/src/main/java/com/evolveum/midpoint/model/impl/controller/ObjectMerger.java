/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.util.MergeDeltas;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemMergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefMergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MergeStategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionMergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectionMergeSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Class responsible for object merging. This acts as a controller
 * for the merge operation and merge preview.
 *
 * @author semancik
 */
@Component
public class ObjectMerger {

	private static final Trace LOGGER = TraceManager.getTrace(ObjectMerger.class);

	public static final String SIDE_LEFT = "left";
	public static final String SIDE_RIGHT = "right";

	@Autowired(required = true)
	private ModelObjectResolver objectResolver;

	@Autowired(required = true)
	private SystemObjectCache systemObjectCache;

	@Autowired(required = true)
	private ExpressionFactory expressionFactory;

	@Autowired(required = true)
	PrismContext prismContext;

	// TODO: circular dependency to model controller. Not good.
	// But cannot fix it right now. TODO: later refactor.
	// MID-3459
	@Autowired(required = true)
	private ModelService modelController;

	public <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> mergeObjects(Class<O> type,
			String leftOid, String rightOid, String mergeConfigurationName, Task task, OperationResult result)
					throws ObjectNotFoundException, SchemaException, ConfigurationException,
					ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException,
					PolicyViolationException, SecurityViolationException {
		MergeDeltas<O> deltas = computeMergeDeltas(type, leftOid, rightOid, mergeConfigurationName, task, result);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Merge {} + {} = (computed deltas)\n{}", leftOid, rightOid, deltas.debugDump(1));
		}

		Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = new ArrayList<>();

		LOGGER.trace("Executing right link delta (raw): {}", deltas.getRightLinkDelta());
		executeDelta(deltas.getRightLinkDelta(), ModelExecuteOptions.createRaw(), executedDeltas, task, result);

		LOGGER.trace("Executing left link delta (raw): {}", deltas.getLeftLinkDelta());
		executeDelta(deltas.getLeftLinkDelta(), ModelExecuteOptions.createRaw(), executedDeltas, task, result);

		LOGGER.trace("Executing left object delta: {}", deltas.getLeftObjectDelta());
		executeDelta(deltas.getLeftObjectDelta(), null, executedDeltas, task, result);

		result.computeStatus();
		if (result.isSuccess()) {
			// Do not delete the other object if the execution was not success.
			// We might need to re-try the merge if it has failed and for that we need the right object.
			ObjectDelta<O> deleteDelta = ObjectDelta.createDeleteDelta(type, rightOid, prismContext);
			Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeleteDeltas = modelController.executeChanges(MiscSchemaUtil.createCollection(deleteDelta), null, task, result);
			executedDeltas.addAll(executedDeleteDeltas);
		}

		return executedDeltas;
	}

	private <O extends ObjectType> void executeDelta(ObjectDelta<O> objectDelta, ModelExecuteOptions options,
			Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas,
			Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {

		result.computeStatus();
		if (!result.isSuccess()) {
			return;
		}

		if (objectDelta != null && !objectDelta.isEmpty()) {
			Collection<ObjectDeltaOperation<? extends ObjectType>> deltaExecutedDeltas =
					modelController.executeChanges(MiscSchemaUtil.createCollection(objectDelta), options, task, result);

			executedDeltas.addAll(deltaExecutedDeltas);
		}
	}

	public <O extends ObjectType> MergeDeltas<O> computeMergeDeltas(Class<O> type, String leftOid, String rightOid,
			final String mergeConfigurationName, final Task task, final OperationResult result)
					throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {

		final PrismObject<O> objectLeft = (PrismObject<O>) objectResolver.getObjectSimple(type, leftOid, null, task, result).asPrismObject();
		final PrismObject<O> objectRight = (PrismObject<O>) objectResolver.getObjectSimple(type, rightOid, null, task, result).asPrismObject();

		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		MergeConfigurationType mergeConfiguration = selectConfiguration(systemConfiguration, mergeConfigurationName);
		if (mergeConfiguration == null) {
			throw new ConfigurationException("No merge configuration defined");
		}

		// The "left" object is always the one that will be the result. We will use its OID.
		final ObjectDelta<O> leftObjectDelta = objectLeft.createModifyDelta();
		final ObjectDelta<O> leftLinkDelta = objectLeft.createModifyDelta();
		final ObjectDelta<O> rightLinkDelta = objectRight.createModifyDelta();
		final List<ItemPath> processedPaths = new ArrayList<>();

		computeItemDeltas(leftObjectDelta, objectLeft, objectRight, processedPaths, mergeConfiguration, mergeConfigurationName, task, result);
		computeDefaultDeltas(leftObjectDelta, objectLeft, objectRight, processedPaths, mergeConfiguration, mergeConfigurationName, task, result);

		computeProjectionDeltas(leftLinkDelta, rightLinkDelta, objectLeft, objectRight, mergeConfiguration, mergeConfigurationName, task, result);

		return new MergeDeltas<>(leftObjectDelta, leftLinkDelta, rightLinkDelta);
	}

	private <O extends ObjectType> void computeItemDeltas(final ObjectDelta<O> leftObjectDelta,
			final PrismObject<O> objectLeft, final PrismObject<O> objectRight, final List<ItemPath> processedPaths,
			MergeConfigurationType mergeConfiguration, final String mergeConfigurationName, final Task task, final OperationResult result) throws SchemaException, ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {

		for (ItemRefMergeConfigurationType itemMergeConfig: mergeConfiguration.getItem()) {
			ItemPath itemPath = itemMergeConfig.getRef().getItemPath();
			processedPaths.add(itemPath);
			ItemDelta itemDelta = mergeItem(objectLeft, objectRight, mergeConfigurationName, itemMergeConfig, itemPath, task, result);
			LOGGER.trace("Item {} delta: {}", itemPath, itemDelta);
			if (itemDelta != null && !itemDelta.isEmpty()) {
				leftObjectDelta.addModification(itemDelta);
			}
		}

	}

	private <O extends ObjectType> void computeDefaultDeltas(final ObjectDelta<O> leftObjectDelta,
			final PrismObject<O> objectLeft, final PrismObject<O> objectRight, final List<ItemPath> processedPaths,
			MergeConfigurationType mergeConfiguration, final String mergeConfigurationName, final Task task, final OperationResult result) throws SchemaException, ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {

		final ItemMergeConfigurationType defaultItemMergeConfig = mergeConfiguration.getDefault();
		if (defaultItemMergeConfig != null) {
			try {

				Visitor visitor = new Visitor() {
					@Override
					public void visit(Visitable visitable) {
						if (!(visitable instanceof Item)) {
							return;
						}
						Item item = (Item)visitable;

						ItemPath itemPath = item.getPath();
						if (itemPath == null || itemPath.isEmpty()) {
							return;
						}

						if (SchemaConstants.PATH_LINK_REF.equivalent(itemPath)) {
							// Skip. There is a special processing for this.
							return;
						}

						boolean found = false;
						for (ItemPath processedPath: processedPaths) {
							// Need to check for super-paths here.
							// E.g. if we have already processed metadata, we do not want to process
							// metadata/modifyTimestamp
							CompareResult compareResult = processedPath.compareComplex(itemPath);
							if (compareResult == CompareResult.EQUIVALENT || compareResult == CompareResult.SUBPATH) {
								found = true;
								break;
							}
						}
						if (found) {
							return;
						}
						processedPaths.add(itemPath);

						if (item instanceof PrismContainer<?>) {
							if (item.getDefinition().isSingleValue()) {
								// Ignore single-valued containers such as extension or activation
								// we will handle every individual property there.
								return;
							} else {
								// TODO: we may need special handling for multi-value containers
								// such as assignment
							}
						}


						ItemDelta itemDelta;
						try {
							itemDelta = mergeItem(objectLeft, objectRight, mergeConfigurationName, defaultItemMergeConfig, itemPath,
									task, result);
						} catch (SchemaException | ConfigurationException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | SecurityViolationException e) {
							throw new TunnelException(e);
						}
						LOGGER.trace("Item {} delta (default): {}", itemPath, itemDelta);
						if (itemDelta != null && !itemDelta.isEmpty()) {
							leftObjectDelta.addModification(itemDelta);
						}
					}
				};

				objectLeft.accept(visitor);
				objectRight.accept(visitor);


			} catch (TunnelException te) {
				if (te.getCause() instanceof SchemaException) {
					throw (SchemaException)te.getCause();
				} else if (te.getCause() instanceof ConfigurationException) {
						throw (ConfigurationException)te.getCause();
				} else if (te.getCause() instanceof ExpressionEvaluationException) {
					throw (ExpressionEvaluationException)te.getCause();
				} else if (te.getCause() instanceof ObjectNotFoundException) {
					throw (ObjectNotFoundException)te.getCause();
				} else if (te.getCause() instanceof CommunicationException) {
					throw (CommunicationException)te.getCause();
				} else if (te.getCause() instanceof SecurityViolationException) {
					throw (SecurityViolationException)te.getCause();
				} else {
					throw new SystemException("Unexpected exception: "+te, te);
				}
			}
		}

	}

	private <O extends ObjectType> void computeProjectionDeltas(final ObjectDelta<O> leftLinkDelta, ObjectDelta<O> rightLinkDelta,
			final PrismObject<O> objectLeft, final PrismObject<O> objectRight,
			MergeConfigurationType mergeConfiguration, final String mergeConfigurationName, final Task task, final OperationResult result) throws SchemaException, ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {

		List<ShadowType> projectionsLeft = getProjections(objectLeft, task, result);
		List<ShadowType> projectionsRight = getProjections(objectRight, task, result);
		List<ShadowType> mergedProjections = new ArrayList<>();
		List<ShadowType> matchedProjections = new ArrayList<>();

		ProjectionMergeConfigurationType defaultProjectionMergeConfig = null;
		for (ProjectionMergeConfigurationType projectionMergeConfig: mergeConfiguration.getProjection()) {
			if (projectionMergeConfig.getProjectionDiscriminator() == null && projectionMergeConfig.getSituation() == null) {
				defaultProjectionMergeConfig = projectionMergeConfig;
			} else {
				takeProjections(projectionMergeConfig.getLeft(), mergedProjections, matchedProjections,
						projectionsLeft, projectionsLeft, projectionsRight, projectionMergeConfig);
				takeProjections(projectionMergeConfig.getRight(), mergedProjections, matchedProjections,
						projectionsRight, projectionsLeft, projectionsRight, projectionMergeConfig);
			}
		}

		LOGGER.trace("Merged projections (before default): {}", mergedProjections);
		LOGGER.trace("Matched projections (before default): {}", matchedProjections);

		if (defaultProjectionMergeConfig != null) {
			takeUnmatchedProjections(defaultProjectionMergeConfig.getLeft(), mergedProjections, matchedProjections, projectionsLeft);
			takeUnmatchedProjections(defaultProjectionMergeConfig.getRight(), mergedProjections, matchedProjections, projectionsRight);
		}

		LOGGER.trace("Merged projections: {}", mergedProjections);

		checkConflict(mergedProjections);

		for (ShadowType mergedProjection: mergedProjections) {
			PrismReferenceValue leftLinkRef = findLinkRef(objectLeft, mergedProjection);
			if (leftLinkRef == null) {
				PrismReferenceValue linkRefRight = findLinkRef(objectRight, mergedProjection);
				LOGGER.trace("Moving projection right->left: {}", mergedProjection);
				addUnlinkDelta(rightLinkDelta, linkRefRight);
				addLinkDelta(leftLinkDelta, linkRefRight);
			} else {
				LOGGER.trace("Projection already at the left: {}", mergedProjection);
			}
		}

		for (PrismReferenceValue leftLinkRef: getLinkRefs(objectLeft)) {
			if (!hasProjection(mergedProjections, leftLinkRef)) {
				LOGGER.trace("Removing left projection: {}", leftLinkRef);
				addUnlinkDelta(leftLinkDelta, leftLinkRef);
			} else {
				LOGGER.trace("Left projection stays: {}", leftLinkRef);
			}
		}

	}

	private <O extends ObjectType> void addLinkDelta(ObjectDelta<O> objectDelta, PrismReferenceValue linkRef) {
		objectDelta.addModificationAddReference(FocusType.F_LINK_REF, linkRef.clone());
	}

	private <O extends ObjectType> void addUnlinkDelta(ObjectDelta<O> objectDelta, PrismReferenceValue linkRef) {
		objectDelta.addModificationDeleteReference(FocusType.F_LINK_REF, linkRef.clone());
	}

	private <O extends ObjectType> PrismReferenceValue findLinkRef(PrismObject<O> object, ShadowType projection) {
		for (PrismReferenceValue linkRef: getLinkRefs(object)) {
			if (linkRef.getOid().equals(projection.getOid())) {
				return linkRef;
			}
		}
		return null;
	}

	private <O extends ObjectType> List<PrismReferenceValue> getLinkRefs(PrismObject<O> object) {
		PrismReference ref = object.findReference(FocusType.F_LINK_REF);
		if (ref == null) {
			return new ArrayList<>(0);
		} else {
			return ref.getValues();
		}
	}

	private boolean hasProjection(List<ShadowType> mergedProjections, PrismReferenceValue leftLinkRef) {
		for (ShadowType projection: mergedProjections) {
			if (projection.getOid().equals(leftLinkRef.getOid())) {
				return true;
			}
		}
		return false;
	}

	private boolean hasProjection(List<ShadowType> mergedProjections, ShadowType candidateProjection) {
		for (ShadowType projection: mergedProjections) {
			if (projection.getOid().equals(candidateProjection.getOid())) {
				return true;
			}
		}
		return false;
	}

	private <O extends ObjectType> List<ShadowType> getProjections(PrismObject<O> objectRight, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		if (!objectRight.canRepresent(FocusType.class)) {
			return new ArrayList<>(0);
		}
		List<ObjectReferenceType> linkRefs = ((FocusType)objectRight.asObjectable()).getLinkRef();
		List<ShadowType> projections = new ArrayList<>(linkRefs.size());
		for (ObjectReferenceType linkRef: linkRefs) {
			projections.add(getProjection(linkRef, task, result));
		}
		return projections;
	}

	private void takeProjections(MergeStategyType strategy, List<ShadowType> mergedProjections,
			List<ShadowType> matchedProjections, List<ShadowType> candidateProjections,
			List<ShadowType> projectionsLeft, List<ShadowType> projectionsRight,
			ProjectionMergeConfigurationType projectionMergeConfig) {

		if (LOGGER.isTraceEnabled()) {

			LOGGER.trace("TAKE: Evaluating situation {}, discriminator: {}",
					projectionMergeConfig.getSituation(), projectionMergeConfig.getProjectionDiscriminator());
		}

		for (ShadowType candidateProjection: candidateProjections) {

			if (projectionMatches(candidateProjection, projectionsLeft, projectionsRight, projectionMergeConfig)) {
				LOGGER.trace("Projection matches {}", candidateProjection);
				matchedProjections.add(candidateProjection);

				if (strategy == MergeStategyType.TAKE) {
					mergedProjections.add(candidateProjection);

				} else if (strategy == null || strategy == MergeStategyType.IGNORE) {
					// Nothing to do here

				} else {
					throw new UnsupportedOperationException("Merge strategy "+strategy+" is not supported");
				}

			} else {
				LOGGER.trace("Discriminator does NOT match {}", candidateProjection);
			}
		}


	}

	private boolean projectionMatches(ShadowType candidateProjection,
			List<ShadowType> projectionsLeft, List<ShadowType> projectionsRight,
			ProjectionMergeConfigurationType projectionMergeConfig) {
		ShadowDiscriminatorType discriminatorType = projectionMergeConfig.getProjectionDiscriminator();
		if (discriminatorType != null && !ShadowUtil.matchesPattern(candidateProjection, discriminatorType)) {
			return false;
		}
		ProjectionMergeSituationType situationPattern = projectionMergeConfig.getSituation();
		if (situationPattern != null) {
			ProjectionMergeSituationType projectionSituation = determineSituation(candidateProjection, projectionsLeft, projectionsRight);
			if (situationPattern != projectionSituation) {
				return false;
			}
		}
		return true;
	}

	private void takeUnmatchedProjections(MergeStategyType strategy, List<ShadowType> mergedProjections,
			List<ShadowType> matchedProjections, List<ShadowType> candidateProjections) {
		if (strategy == MergeStategyType.TAKE) {

			for (ShadowType candidateProjection: candidateProjections) {
				if (!hasProjection(matchedProjections, candidateProjection)) {
					mergedProjections.add(candidateProjection);
				}
			}

		} else if (strategy == null || strategy == MergeStategyType.IGNORE) {
			return;
		} else {
			throw new UnsupportedOperationException("Merge strategy "+strategy+" is not supported");
		}
	}

	private ProjectionMergeSituationType determineSituation(ShadowType candidateProjection, List<ShadowType> projectionsLeft,
			List<ShadowType> projectionsRight) {
		boolean matchLeft = hasMatchingProjection(candidateProjection, projectionsLeft);
		boolean matchRight = hasMatchingProjection(candidateProjection, projectionsRight);
		if (matchLeft && matchRight) {
			return ProjectionMergeSituationType.CONFLICT;
		} else if (matchLeft) {
			return ProjectionMergeSituationType.EXISTING;
		} else if (matchRight) {
			return ProjectionMergeSituationType.MERGEABLE;
		} else {
			throw new IllegalStateException("Booom! The universe has imploded.");
		}
	}

	private boolean hasMatchingProjection(ShadowType cprojection, List<ShadowType> projections) {
		for (ShadowType projection: projections) {
			if (ShadowUtil.isConflicting(projection, cprojection)) {
				return true;
			}
		}
		return false;
	}


	private void checkConflict(List<ShadowType> projections) throws SchemaException {
		for (ShadowType projection: projections) {
			for (ShadowType cprojection: projections) {
				if (cprojection == projection) {
					continue;
				}
				if (ShadowUtil.isConflicting(projection, cprojection)) {
					throw new SchemaException("Merge would result in projection conflict between "+projection+" and "+cprojection);
				}
			}
		}
	}

	private ShadowType getProjection(ObjectReferenceType linkRef, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
		return objectResolver.getObject(ShadowType.class, linkRef.getOid(), options, task, result);
	}

	private <O extends ObjectType, I extends Item> ItemDelta mergeItem(PrismObject<O> objectLeft, PrismObject<O> objectRight,
			String mergeConfigurationName, ItemMergeConfigurationType itemMergeConfig, ItemPath itemPath,
			Task task, OperationResult result) throws SchemaException, ConfigurationException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
		I itemLeft = (I) objectLeft.findItem(itemPath);
		I itemRight = (I) objectRight.findItem(itemPath);
		if (itemLeft == null && itemRight == null) {
			return null;
		}
		ItemDefinition itemDefinition = null;
		if (itemLeft != null) {
			itemDefinition = itemLeft.getDefinition();
		} else {
			itemDefinition = itemRight.getDefinition();
		}
		if (itemDefinition.isOperational()) {
			// Skip operational attributes. There are automatically computed,
			// we do not want to modify them explicitly.
			return null;
		}

		Expression<PrismValue, ItemDefinition> valueExpression = null;
		if (itemMergeConfig.getValueExpression() != null) {
			ExpressionType expressionType = itemMergeConfig.getValueExpression();
			valueExpression = expressionFactory.makeExpression(expressionType, itemDefinition,
					"value expression for item " + itemPath + " in merge configuration " + mergeConfigurationName,
					task, result);
		}

		ItemDelta itemDelta = itemDefinition.createEmptyDelta(itemPath);
		MergeStategyType leftStrategy = itemMergeConfig.getLeft();
		MergeStategyType rightStrategy = itemMergeConfig.getRight();
		if (leftStrategy == null || leftStrategy == MergeStategyType.IGNORE) {
			if (rightStrategy == null || rightStrategy == MergeStategyType.IGNORE) {
				// IGNORE both
				if (itemLeft == null) {
					return null;
				} else {
					itemDelta.setValueToReplace();
					return itemDelta;
				}
			} else {
				// IGNORE left, TAKE/EXPRESSION right
				if (itemRight == null) {
					itemDelta.setValueToReplace();
				} else {
					Collection<PrismValue> valuesToTake = getValuesToTake(objectLeft, objectRight,
							SIDE_RIGHT, itemRight, rightStrategy, valueExpression, task, result);
					itemDelta.setValuesToReplace(valuesToTake);
				}
				return itemDelta;
			}
		} else {
			if (rightStrategy == null || rightStrategy == MergeStategyType.IGNORE) {
				if (leftStrategy == MergeStategyType.TAKE) {
					// TAKE left, IGNORE right
					return null;
				} else {
					// EXPRESSION left, IGNORE right
					Collection<PrismValue> valuesToLeave = getValuesToTake(objectLeft, objectRight,
							SIDE_LEFT, itemLeft, leftStrategy, valueExpression, task, result);
					List<PrismValue> currentLeftValues = itemLeft.getValues();
					Collection<PrismValue> leftValuesToRemove = diffValues(currentLeftValues, valuesToLeave);
					if (leftValuesToRemove != null && !leftValuesToRemove.isEmpty()) {
						itemDelta.addValuesToDelete(leftValuesToRemove);
						return itemDelta;
					} else {
						return null;
					}
				}
			} else {
				// TAKE/EXPRESSION left, TAKE/EXPRESSION right
				if (itemLeft == null) {
					Collection<PrismValue> valuesToTake = getValuesToTake(objectLeft, objectRight,
							SIDE_RIGHT, itemRight, rightStrategy, valueExpression, task, result);
					itemDelta.addValuesToAdd(valuesToTake);
					return itemDelta;

				} else {
					// We want to add only those values that are not yet there.
					// E.g. adding assignments that are there can cause unnecessary churn
					Collection<PrismValue> leftValuesToLeave = getValuesToTake(objectLeft, objectRight,
							SIDE_LEFT, itemLeft, leftStrategy, valueExpression, task, result);
					Collection<PrismValue> rightValuesToTake = getValuesToTake(objectLeft, objectRight,
							SIDE_RIGHT, itemRight, rightStrategy, valueExpression, task, result);

					for (PrismValue rightValueToTake: rightValuesToTake) {
						if (!PrismValue.collectionContainsEquivalentValue(leftValuesToLeave, rightValueToTake)) {
							itemDelta.addValueToAdd(rightValueToTake);
						}
					}

					List<PrismValue> currentLeftValues = itemLeft.getValues();
					Collection<PrismValue> leftValuesToRemove = diffValues(currentLeftValues, leftValuesToLeave);

					if (leftValuesToRemove != null && !leftValuesToRemove.isEmpty()) {
						itemDelta.addValuesToDelete(leftValuesToRemove);
					}

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Merging item {} T/T case:\n  leftValuesToLeave: {}\n  rightValuesToTake: {}\n  leftValuesToRemove: {}\n itemDelta:\n{}",
								new Object[]{itemPath, leftValuesToLeave, rightValuesToTake, leftValuesToRemove, itemDelta.debugDump(2)});
					}


					return itemDelta;
				}
			}
		}
	}

	private Collection<PrismValue> diffValues(List<PrismValue> currentValues, Collection<PrismValue> valuesToLeave) {
		if (valuesToLeave == null || valuesToLeave.isEmpty()) {
			return PrismValue.cloneCollection(currentValues);
		}
		Collection<PrismValue> diff = new ArrayList<>();
		for (PrismValue currentValue: currentValues) {
			if (!PrismValue.collectionContainsEquivalentValue(valuesToLeave, currentValue)) {
				diff.add(currentValue.clone());
			}
		}
		return diff;
	}

	private <O extends ObjectType, I extends Item> Collection<PrismValue> getValuesToTake(PrismObject<O> objectLeft, PrismObject<O> objectRight,
			String side, I origItem, MergeStategyType strategy, Expression<PrismValue, ItemDefinition> valueExpression, Task task, OperationResult result)
					throws ConfigurationException, SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
		if (origItem == null) {
			return new ArrayList<>(0);
		}
		if (strategy == MergeStategyType.TAKE) {
			return cleanContainerIds(origItem.getClonedValues());
		} else if (strategy == MergeStategyType.EXPRESSION) {
			if (valueExpression == null) {
				throw new ConfigurationException("Expression strategy specified but no expression present");
			}
			List<PrismValue> origValues = origItem.getValues();
			Collection<PrismValue> valuesToTake = new ArrayList<>(origValues.size());
			for (PrismValue origValue: origValues) {
				Collection<PrismValue> expressionOutput = evaluateValueExpression(objectLeft, objectRight, side, origValue, valueExpression, task, result);
				if (expressionOutput != null) {
					valuesToTake.addAll(expressionOutput);
				}
			}
			return cleanContainerIds(valuesToTake);
		} else {
			throw new ConfigurationException("Unknown strategy "+strategy);
		}
	}


	private Collection<PrismValue> cleanContainerIds(Collection<PrismValue> pvals) {
		if (pvals == null) {
			return null;
		}
		for (PrismValue pval: pvals) {
			if (pval instanceof PrismContainerValue<?>) {
				((PrismContainerValue)pval).setId(null);
			}
		}
		return pvals;
	}


	private <O extends ObjectType> Collection<PrismValue> evaluateValueExpression(PrismObject<O> objectLeft, PrismObject<O> objectRight, String side, PrismValue origValue, Expression<PrismValue, ItemDefinition> valueExpression,
			Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_SIDE, side);
		variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT_LEFT, side);
		variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT_RIGHT, side);
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, origValue);
		variables.addVariableDefinition(ExpressionConstants.VAR_VALUE, origValue);
		ExpressionEvaluationContext exprContext = new ExpressionEvaluationContext(null, variables, "for value "+origValue, task, result);
		PrismValueDeltaSetTriple<PrismValue> triple = valueExpression.evaluate(exprContext);
		if (triple == null) {
			return null;
		}
		return triple.getNonNegativeValues();
	}

	private MergeConfigurationType selectConfiguration(
			PrismObject<SystemConfigurationType> systemConfiguration, String mergeConfigurationName) throws ConfigurationException {
		if (StringUtils.isBlank(mergeConfigurationName)) {
			throw new IllegalArgumentException("Merge configuration name not specified");
		}
		for (MergeConfigurationType mergeConfiguration: systemConfiguration.asObjectable().getMergeConfiguration()) {
			if (mergeConfigurationName.equals(mergeConfiguration.getName())) {
				return mergeConfiguration;
			}
		}
		throw new ConfigurationException("Merge configuration with name '"+mergeConfigurationName+"' was not found");
	}

}
