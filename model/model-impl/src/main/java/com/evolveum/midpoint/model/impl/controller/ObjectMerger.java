/**
 * Copyright (c) 2016 Evolveum
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemMergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefMergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MergeConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MergeStategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
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
	
	@Autowired(required = true)
	private ModelObjectResolver objectResolver;
	
	@Autowired(required = true)
	private SystemObjectCache systemObjectCache;
	
	@Autowired(required = true)
	PrismContext prismContext;
	
	// TODO: circular dependency to model controller. Not good.
	// But cannot fix it right now. TODO: later refactor.
	// MID-3459
	@Autowired(required = true)
	private ModelController modelController;
	
	public <O extends ObjectType> Collection<ObjectDeltaOperation<? extends ObjectType>> mergeObjects(Class<O> type, 
			String leftOid, String rightOid, Task task, OperationResult result) 
					throws ObjectNotFoundException, SchemaException, ConfigurationException, 
					ObjectAlreadyExistsException, ExpressionEvaluationException, CommunicationException, 
					PolicyViolationException, SecurityViolationException {
		ObjectDelta<O> objectDelta = computeMergeDelta(type, leftOid, rightOid, task, result);
		
		if (objectDelta != null && !objectDelta.isEmpty()) {
			Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas = 
					modelController.executeChanges(MiscSchemaUtil.createCollection(objectDelta), null, task, result);
			
			result.computeStatus();
			if (result.isSuccess()) {
				// Do not delete the other object if the execution was not success. 
				// We might need to re-try the merge if it has failed and for that we need the right object.		
				ObjectDelta<O> deleteDelta = ObjectDelta.createDeleteDelta(type, rightOid, prismContext);
				Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeleteDeltas = modelController.executeChanges(MiscSchemaUtil.createCollection(deleteDelta), null, task, result);
				executedDeltas.addAll(executedDeleteDeltas);
			}
			
			return executedDeltas;
			
		} else {
			return null;
		}
	}
	
	public <O extends ObjectType> ObjectDelta<O> computeMergeDelta(Class<O> type, String leftOid, String rightOid, 
			Task task, OperationResult result) 
					throws ObjectNotFoundException, SchemaException, ConfigurationException {
		
		final PrismObject<O> objectLeft = objectResolver.getObjectSimple(type, leftOid, null, task, result).asPrismObject();
		final PrismObject<O> objectRight = objectResolver.getObjectSimple(type, rightOid, null, task, result).asPrismObject();
		
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		MergeConfigurationType mergeConfiguration = systemConfiguration.asObjectable().getMergeConfiguration();
		if (mergeConfiguration == null) {
			throw new ConfigurationException("No merge configuration defined");
		}
		
		// The "left" object is always the one that will be the result. We will use its OID.
		final ObjectDelta<O> leftObjectDelta = objectLeft.createModifyDelta();
		
		final List<ItemPath> processedPaths = new ArrayList<>();
		for (ItemRefMergeConfigurationType itemMergeConfig: mergeConfiguration.getItem()) {
			ItemPath itemPath = itemMergeConfig.getRef().getItemPath();
			processedPaths.add(itemPath);
			ItemDelta itemDelta = mergeItem(objectLeft, objectRight, itemMergeConfig, itemPath);
			LOGGER.trace("Item {} delta: {}", itemPath, itemDelta);
			if (itemDelta != null && !itemDelta.isEmpty()) {
				leftObjectDelta.addModification(itemDelta);
			}
		}
		
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
							itemDelta = mergeItem(objectLeft, objectRight, defaultItemMergeConfig, itemPath);
						} catch (SchemaException e) {
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
				} else {
					throw new SystemException("Unexpected exception: "+te, te);
				}
			}
		}
		
		return leftObjectDelta;
		
	}
	
	private <O extends ObjectType, I extends Item> ItemDelta mergeItem(PrismObject<O> objectLeft, PrismObject<O> objectRight,
			ItemMergeConfigurationType itemMergeConfig, ItemPath itemPath) throws SchemaException {
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
				// IGNORE left, TAKE right
				if (itemRight == null) {
					itemDelta.setValueToReplace();
				} else {
					itemDelta.setValuesToReplace(itemRight.getClonedValues());
				}
				return itemDelta;
			}
		} else {
			if (rightStrategy == null || rightStrategy == MergeStategyType.IGNORE) {
				// TAKE left, IGNORE right
				return null;
			} else {
				// TAKE left, TAKE right
				if (itemDefinition.isSingleValue()) {					
					if (itemLeft == null) {
						itemDelta.setValuesToReplace(itemRight.getClonedValues());
						return itemDelta;
					} else if (itemRight != null) {
						throw new SchemaException("Attempt to put multiple values in a single-valued item "+itemPath);
					} else {
						return null;
					}
				} else {
					if (itemLeft == null) {
						itemDelta.addValuesToAdd(itemRight.getClonedValues());
						return itemDelta;
					} else {
						// We want to add only those values that are not yet there.
						// E.g. adding assignments that are there can cause unneccesary churn
						List<PrismValue> valuesRight = itemRight.getValues();
						for (PrismValue valueRight: valuesRight) {
							if (!itemLeft.containsEquivalentValue(valueRight)) {
								PrismValue clonedValue = valueRight.clone();
								if (clonedValue instanceof PrismContainerValue<?>) {
									((PrismContainerValue)clonedValue).setId(null);
								}
								itemDelta.addValueToAdd(clonedValue);
							}
						}
						return itemDelta;
					}
				}
			}
		}
	}

}
