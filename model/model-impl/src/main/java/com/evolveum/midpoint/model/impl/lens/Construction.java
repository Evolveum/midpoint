/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.PrismValueDeltaSetTripleProducer;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Live class that contains "construction" - a definition how to construct a resource object. It in fact reflects
 * the definition of ConstructionType but it also contains "live" objects and can evaluate the construction. It also
 * contains intermediary and side results of the evaluation.
 * 
 * @author Radovan Semancik
 *
 * This class is Serializable but it is not in fact serializable. It implements Serializable interface only
 * to be storable in the PrismPropertyValue.
 */
public class Construction<F extends FocusType> implements DebugDumpable, Serializable {

	private AssignmentPath assignmentPath;
	private ConstructionType constructionType;
	private ObjectType source;
	private ObjectType orderOneObject;
	private OriginType originType;
	private String channel;
	private LensContext<F> lensContext;
	private ObjectDeltaObject<F> focusOdo;
	private ResourceType resource;
	private ObjectResolver objectResolver;
	private MappingFactory mappingFactory;
	private Collection<Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>>> attributeMappings;
	private Collection<Mapping<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>> associationMappings;
	private RefinedObjectClassDefinition refinedObjectClassDefinition;
	private List<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions;
	private AssignmentPathVariables assignmentPathVariables = null;
	private PrismContext prismContext;
	private PrismContainerDefinition<ShadowAssociationType> associationContainerDefinition;
	private PrismObject<SystemConfigurationType> systemConfiguration;		// only to provide $configuration variable (MID-2372)
	private boolean isValid = true;
	
	private static final Trace LOGGER = TraceManager.getTrace(Construction.class);
	
	public Construction(ConstructionType constructionType, ObjectType source) {
		this.constructionType = constructionType;
		this.source = source;
		this.assignmentPath = null;
		this.attributeMappings = null;
	}

	public void setSource(ObjectType source) {
		this.source = source;
	}
		
	public ObjectType getSource() {
		return source;
	}

	public OriginType getOriginType() {
		return originType;
	}

	public void setOriginType(OriginType originType) {
		this.originType = originType;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}
	
	public LensContext<F> getLensContext() {
		return lensContext;
	}

	public void setLensContext(LensContext<F> lensContext) {
		this.lensContext = lensContext;
	}

	public void setFocusOdo(ObjectDeltaObject<F> focusOdo) {
		this.focusOdo = focusOdo;
	}

	public ObjectResolver getObjectResolver() {
		return objectResolver;
	}

	public void setObjectResolver(ObjectResolver objectResolver) {
		this.objectResolver = objectResolver;
	}

	public ObjectType getOrderOneObject() {
		return orderOneObject;
	}

	public void setOrderOneObject(ObjectType orderOneObject) {
		this.orderOneObject = orderOneObject;
	}

	PrismContext getPrismContext() {
		return prismContext;
	}

	void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	public MappingFactory getMappingFactory() {
		return mappingFactory;
	}

	public void setMappingFactory(MappingFactory mappingFactory) {
		this.mappingFactory = mappingFactory;
	}

	public PrismObject<SystemConfigurationType> getSystemConfiguration() {
		return systemConfiguration;
	}

	public void setSystemConfiguration(PrismObject<SystemConfigurationType> systemConfiguration) {
		this.systemConfiguration = systemConfiguration;
	}

	public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
		return refinedObjectClassDefinition;
	}
	
	public void setRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
		this.refinedObjectClassDefinition = refinedObjectClassDefinition;
	}

	public List<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions() {
		return auxiliaryObjectClassDefinitions;
	}
	
	public void addAuxiliaryObjectClassDefinition(RefinedObjectClassDefinition auxiliaryObjectClassDefinition) {
		if (auxiliaryObjectClassDefinitions == null) {
			auxiliaryObjectClassDefinitions = new ArrayList<>();
		}
		auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDefinition);
	}


	public ShadowKindType getKind() {
		if (refinedObjectClassDefinition == null) {
			throw new IllegalStateException("Kind can only be fetched from evaluated Construction");
		}
		return refinedObjectClassDefinition.getKind();
	}

	public String getIntent() {
		if (refinedObjectClassDefinition == null) {
			throw new IllegalStateException("Intent can only be fetched from evaluated Construction");
		}
		return refinedObjectClassDefinition.getIntent();
	}

	public Object getDescription() {
		return constructionType.getDescription();
	}
	
	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public Collection<Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>>> getAttributeMappings() {
		if (attributeMappings == null) {
			attributeMappings = new ArrayList<>();
		}
		return attributeMappings;
	}
	
	public Mapping<? extends PrismPropertyValue<?>, ? extends PrismPropertyDefinition<?>> getAttributeMapping(QName attrName) {
		for (Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>> myVc : getAttributeMappings()) {
			if (myVc.getItemName().equals(attrName)) {
				return myVc;
			}
		}
		return null;
	}
	
	public void addAttributeMapping(Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>> mapping) {
		getAttributeMappings().add(mapping);
	}

	public boolean containsAttributeMapping(QName attributeName) {
		for (Mapping<?,?> mapping: getAttributeMappings()) {
			if (attributeName.equals(mapping.getItemName())) {
				return true;
			}
		}
		return false;
	}
		
	public Collection<Mapping<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>> getAssociationMappings() {
		if (associationMappings == null) {
			associationMappings = new ArrayList<Mapping<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>>>();
		}
		return associationMappings;
	}
	
	public void addAssociationMapping(Mapping<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> mapping) {
		getAssociationMappings().add(mapping);
	}

	public boolean containsAssociationMapping(QName assocName) {
		for (Mapping<?,?> mapping: getAssociationMappings()) {
			if (assocName.equals(mapping.getItemName())) {
				return true;
			}
		}
		return false;
	}

	public AssignmentPath getAssignmentPath() {
		return assignmentPath;
	}

	public void setAssignmentPath(AssignmentPath assignmentPath) {
		this.assignmentPath = assignmentPath;
	}

	public ResourceType getResource(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (resource == null) {
			if (constructionType.getResource() != null) {
				resource = constructionType.getResource();
			} else if (constructionType.getResourceRef() != null) {
				try {
					resource = LensUtil.getResource(lensContext, constructionType.getResourceRef().getOid(), objectResolver, task, result);
				} catch (ObjectNotFoundException e) {
					throw new ObjectNotFoundException("Resource reference seems to be invalid in account construction in " + source + ": "+e.getMessage(), e);
				} catch (SecurityViolationException|CommunicationException|ConfigurationException e) {
					throw new SystemException("Couldn't fetch the resource in account construction in " + source + ": " + e.getMessage(), e);
				}
			}
			if (resource == null) {
				throw new SchemaException("No resource set in account construction in " + source);
			}
		}
		return resource;
	}
	
	public void evaluate(Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		evaluateKindIntentObjectClass(task, result);
		assignmentPathVariables = LensUtil.computeAssignmentPathVariables(assignmentPath);
		evaluateAttributes(task, result);
		evaluateAssociations(task, result);
	}
	
	private void evaluateKindIntentObjectClass(Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		String resourceOid = null;
		if (constructionType.getResourceRef() != null) {
			resourceOid = constructionType.getResourceRef().getOid();
		}
		if (constructionType.getResource() != null) {
			resourceOid = constructionType.getResource().getOid();
		}
		ResourceType resource = getResource(task, result);
		if (!resource.getOid().equals(resourceOid)) {
			throw new IllegalStateException("The specified resource and the resource in construction does not match");
		}
		
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource, LayerType.MODEL, prismContext);
		if (refinedSchema == null) {
			// Refined schema may be null in some error-related border cases
			throw new SchemaException("No (refined) schema for "+resource);
		}
		
		ShadowKindType kind = constructionType.getKind();
		if (kind == null) {
			kind = ShadowKindType.ACCOUNT;
		}
		refinedObjectClassDefinition = refinedSchema.getRefinedDefinition(kind, constructionType.getIntent());
		
		if (refinedObjectClassDefinition == null) {
			if (constructionType.getIntent() != null) {
				throw new SchemaException("No "+kind+" type '"+constructionType.getIntent()+"' found in "+getResource(task, result)+" as specified in construction in "+source);
			} else {
				throw new SchemaException("No default "+kind+" type found in " + resource + " as specified in construction in "+source);
			}
		}
		
		auxiliaryObjectClassDefinitions = new ArrayList<>(constructionType.getAuxiliaryObjectClass().size());
		for (QName auxiliaryObjectClassName: constructionType.getAuxiliaryObjectClass()) {
			RefinedObjectClassDefinition auxOcDef = refinedSchema.getRefinedDefinition(auxiliaryObjectClassName);
			if (auxOcDef == null) {
				throw new SchemaException("No auxiliary object class "+auxiliaryObjectClassName+" found in "+getResource(task, result)+" as specified in construction in "+source);
			}
			auxiliaryObjectClassDefinitions.add(auxOcDef);
		}
	}

	private void evaluateAttributes(Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		attributeMappings = new ArrayList<>();
//		LOGGER.trace("Assignments used for account construction for {} ({}): {}", new Object[]{this.resource,
//				assignments.size(), assignments});
		for (ResourceAttributeDefinitionType attribudeDefinitionType : constructionType.getAttribute()) {
			QName attrName = ItemPathUtil.getOnlySegmentQName(attribudeDefinitionType.getRef());
			if (attrName == null) {
				throw new SchemaException("No attribute name (ref) in attribute definition in account construction in "+source);
			}
			if (!attribudeDefinitionType.getInbound().isEmpty()) {
				throw new SchemaException("Cannot process inbound section in definition of attribute "+attrName+" in account construction in "+source);
			}
			MappingType outboundMappingType = attribudeDefinitionType.getOutbound();
			if (outboundMappingType == null) {
				throw new SchemaException("No outbound section in definition of attribute "+attrName+" in account construction in "+source);
			}
			Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>> attributeMapping = evaluateAttribute(attribudeDefinitionType, task, result);
			if (attributeMapping != null) {
				attributeMappings.add(attributeMapping);
			}
		}
	}

	private <T> Mapping<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> evaluateAttribute(ResourceAttributeDefinitionType attribudeDefinitionType,
			Task task, OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		QName attrName = ItemPathUtil.getOnlySegmentQName(attribudeDefinitionType.getRef());
		if (attrName == null) {
			throw new SchemaException("Missing 'ref' in attribute construction in account construction in "+ObjectTypeUtil.toShortString(source));
		}
		if (!attribudeDefinitionType.getInbound().isEmpty()) {
			throw new SchemaException("Cannot process inbound section in definition of attribute "+attrName+" in account construction in "+source);
		}
		MappingType outboundMappingType = attribudeDefinitionType.getOutbound();
		if (outboundMappingType == null) {
			throw new SchemaException("No outbound section in definition of attribute "+attrName+" in account construction in "+source);
		}
		ResourceAttributeDefinition<T> outputDefinition = findAttributeDefinition(attrName);
		if (outputDefinition == null) {
			throw new SchemaException("Attribute "+attrName+" not found in schema for account type "+getIntent()+", "+ObjectTypeUtil.toShortString(getResource(task, result))+" as definied in "+ObjectTypeUtil.toShortString(source), attrName);
		}
		Mapping<PrismPropertyValue<T>,ResourceAttributeDefinition<T>> mapping = mappingFactory.createMapping(outboundMappingType,
				"for attribute " + PrettyPrinter.prettyPrint(attrName)  + " in "+source);
		
		Mapping<PrismPropertyValue<T>, ResourceAttributeDefinition<T>> evaluatedMapping = evaluateMapping(mapping, attrName, outputDefinition, null, task, result);		
		
		LOGGER.trace("Evaluated mapping for attribute "+attrName+": "+evaluatedMapping);
		return evaluatedMapping;
	}

	public <T> RefinedAttributeDefinition<T> findAttributeDefinition(QName attributeName) {
		if (refinedObjectClassDefinition == null) {
			throw new IllegalStateException("Construction "+this+" was not evaluated:\n"+this.debugDump());
		}
		RefinedAttributeDefinition<T> attrDef = refinedObjectClassDefinition.findAttributeDefinition(attributeName);
		if (attrDef != null) {
			return attrDef;
		}
		for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
			attrDef = auxiliaryObjectClassDefinition.findAttributeDefinition(attributeName);
			if (attrDef != null) {
				return attrDef;
			}
		}
		return null;
	}
	
	public boolean hasValueForAttribute(QName attributeName) {
		for (Mapping<? extends PrismPropertyValue<?>,? extends PrismPropertyDefinition<?>> attributeConstruction: attributeMappings) {
			if (attributeName.equals(attributeConstruction.getItemName())) {
				PrismValueDeltaSetTriple<? extends PrismPropertyValue<?>> outputTriple = attributeConstruction.getOutputTriple();
				if (outputTriple != null && !outputTriple.isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void evaluateAssociations(Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		associationMappings = new ArrayList<>();
		for (ResourceObjectAssociationType associationDefinitionType : constructionType.getAssociation()) {
			QName assocName = ItemPathUtil.getOnlySegmentQName(associationDefinitionType.getRef());
			if (assocName == null) {
				throw new SchemaException("No association name (ref) in association definition in construction in "+source);
			}
			MappingType outboundMappingType = associationDefinitionType.getOutbound();
			if (outboundMappingType == null) {
				throw new SchemaException("No outbound section in definition of association "+assocName+" in construction in "+source);
			}
			Mapping<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> assocMapping = evaluateAssociation(associationDefinitionType, task, result);
			if (assocMapping != null) {
				associationMappings.add(assocMapping);
			}
		}
	}

	private Mapping<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> evaluateAssociation(ResourceObjectAssociationType associationDefinitionType,
			Task task, OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		QName assocName = ItemPathUtil.getOnlySegmentQName(associationDefinitionType.getRef());
		if (assocName == null) {
			throw new SchemaException("Missing 'ref' in association in construction in "+source);
		}
		MappingType outboundMappingType = associationDefinitionType.getOutbound();
		if (outboundMappingType == null) {
			throw new SchemaException("No outbound section in definition of association "+assocName+" in construction in "+source);
		}
		PrismContainerDefinition<ShadowAssociationType> outputDefinition = getAssociationContainerDefinition();
		Mapping<PrismContainerValue<ShadowAssociationType>,PrismContainerDefinition<ShadowAssociationType>> mapping = mappingFactory.createMapping(outboundMappingType,
				"for association " + PrettyPrinter.prettyPrint(assocName)  + " in " + source);
		mapping.setOriginType(OriginType.ASSIGNMENTS);
		mapping.setOriginObject(source);
		
		RefinedAssociationDefinition rAssocDef = refinedObjectClassDefinition.findAssociation(assocName);
		if (rAssocDef == null) {
			throw new SchemaException("No association "+assocName+" in object class "+refinedObjectClassDefinition.getHumanReadableName()
					+" in construction in "+source);
		}
		
		Mapping<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> evaluatedMapping = evaluateMapping(mapping, assocName, outputDefinition, 
				rAssocDef.getAssociationTarget(), task, result);
		
		LOGGER.trace("Evaluated mapping for association "+assocName+": "+evaluatedMapping);
		return evaluatedMapping;
	}
	
	private <V extends PrismValue,D extends ItemDefinition> Mapping<V, D> evaluateMapping(Mapping<V,D> mapping, QName mappingQName, D outputDefinition,
			RefinedObjectClassDefinition assocTargetObjectClassDefinition, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		if (!mapping.isApplicableToChannel(channel)) {
			return null;
		}
		
		mapping.addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo);
		mapping.addVariableDefinition(ExpressionConstants.VAR_SOURCE, source);
		mapping.setMappingQName(mappingQName);
		mapping.setSourceContext(focusOdo);
		mapping.setRootNode(focusOdo);
		mapping.setDefaultTargetDefinition(outputDefinition);
		mapping.setOriginType(originType);
		mapping.setOriginObject(source);
		mapping.setRefinedObjectClassDefinition(refinedObjectClassDefinition);
		
		mapping.addVariableDefinition(ExpressionConstants.VAR_CONTAINING_OBJECT, source);
		mapping.addVariableDefinition(ExpressionConstants.VAR_ORDER_ONE_OBJECT, orderOneObject);
		if (assocTargetObjectClassDefinition != null) {
			mapping.addVariableDefinition(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION, assocTargetObjectClassDefinition);
		}
		LensUtil.addAssignmentPathVariables(mapping, assignmentPathVariables);
		if (getSystemConfiguration() != null) {
			mapping.addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION, getSystemConfiguration());
		}
		// TODO: other variables ?
		
		// Set condition masks. There are used as a brakes to avoid evaluating to nonsense values in case user is not present
		// (e.g. in old values in ADD situations and new values in DELETE situations).
		if (focusOdo.getOldObject() == null) {
			mapping.setConditionMaskOld(false);
		}
		if (focusOdo.getNewObject() == null) {
			mapping.setConditionMaskNew(false);
		}

		LensUtil.evaluateMapping(mapping, lensContext, task, result);

		return mapping;
	}
	
	private PrismContainerDefinition<ShadowAssociationType> getAssociationContainerDefinition() {
		if (associationContainerDefinition == null) {
			PrismObjectDefinition<ShadowType> shadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
			associationContainerDefinition = shadowDefinition.findContainerDefinition(ShadowType.F_ASSOCIATION);
		}
		return associationContainerDefinition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((assignmentPath == null) ? 0 : assignmentPath.hashCode());
		result = prime * result + ((attributeMappings == null) ? 0 : attributeMappings.hashCode());
		result = prime * result + ((associationMappings == null) ? 0 : associationMappings.hashCode());
		result = prime * result
				+ ((refinedObjectClassDefinition == null) ? 0 : refinedObjectClassDefinition.hashCode());
		result = prime * result + ((resource == null) ? 0 : resource.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		Construction other = (Construction) obj;
		if (assignmentPath == null) {
			if (other.assignmentPath != null)
				return false;
		} else if (!assignmentPath.equals(other.assignmentPath))
			return false;
		if (attributeMappings == null) {
			if (other.attributeMappings != null)
				return false;
		} else if (!attributeMappings.equals(other.attributeMappings))
			return false;
		if (associationMappings == null) {
			if (other.associationMappings != null)
				return false;
		} else if (!associationMappings.equals(other.associationMappings))
			return false;
		if (refinedObjectClassDefinition == null) {
			if (other.refinedObjectClassDefinition != null)
				return false;
		} else if (!refinedObjectClassDefinition.equals(other.refinedObjectClassDefinition))
			return false;
		if (resource == null) {
			if (other.resource != null)
				return false;
		} else if (!resource.equals(other.resource))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "Construction", indent);
		if (refinedObjectClassDefinition == null) {
			sb.append(" (no object class definition)");
			if (constructionType != null && constructionType.getResourceRef() != null) {		// should be always the case
				sb.append("\n");
				DebugUtil.debugDumpLabel(sb, "resourceRef / kind / intent", indent + 1);
				sb.append(" ");
				sb.append(ObjectTypeUtil.toShortString(constructionType.getResourceRef()));
				sb.append(" / ");
				sb.append(constructionType.getKind());
				sb.append(" / ");
				sb.append(constructionType.getIntent());
			}
		} else {
			sb.append(refinedObjectClassDefinition.getShadowDiscriminator());
		}
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "isValid", isValid, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpLabel(sb, "auxiliary object classes", indent + 1);
		if (auxiliaryObjectClassDefinitions == null) {
			sb.append(" (null)");
		} else if (auxiliaryObjectClassDefinitions.isEmpty()) {
			sb.append(" (empty)");
		} else {
			for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(auxiliaryObjectClassDefinition.getTypeName());
			}
		}
		if (constructionType != null && constructionType.getDescription() != null) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "description", indent+1);
			sb.append(" ").append(constructionType.getDescription());
		}
		if (attributeMappings != null && !attributeMappings.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "attribute mappings", indent+1);
			for (Mapping mapping: attributeMappings) {
				sb.append("\n");
				sb.append(mapping.debugDump(indent+2));
			}
		}
		if (associationMappings != null && !associationMappings.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "association mappings", indent+1);
			for (Mapping mapping: associationMappings) {
				sb.append("\n");
				sb.append(mapping.debugDump(indent+2));
			}
		}
		if (assignmentPath != null) {
			sb.append("\n");
			sb.append(assignmentPath.debugDump(indent+1));
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Construction(" + (refinedObjectClassDefinition == null ? constructionType : refinedObjectClassDefinition.getShadowDiscriminator()) + ")";
	}
	
}
