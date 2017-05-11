/**
 * Copyright (c) 2015-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * @author semancik
 *
 */
@Component
public class ResourceObjectReferenceResolver {
	
	private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectReferenceResolver.class);
	
	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
	@Qualifier("shadowCacheProvisioner")
	private ShadowCache shadowCache;
	
	@Autowired(required = true)
	private ShadowManager shadowManager;
	
	PrismObject<ShadowType> resolve(ProvisioningContext ctx, ResourceObjectReferenceType resourceObjectReference, 
			QName objectClass, final String desc, OperationResult result) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, 
					SecurityViolationException, ExpressionEvaluationException {
		if (resourceObjectReference == null) {
			return null;
		}
		ObjectReferenceType shadowRef = resourceObjectReference.getShadowRef();
		if (shadowRef != null && shadowRef.getOid() != null) {
			if (resourceObjectReference.getResolutionFrequency() == null 
					|| resourceObjectReference.getResolutionFrequency() == ResourceObjectReferenceResolutionFrequencyType.ONCE) {
				PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, shadowRef.getOid(), null, result);
				return shadow;
			}
		} else if (resourceObjectReference.getResolutionFrequency() == ResourceObjectReferenceResolutionFrequencyType.NEVER) {
			throw new ObjectNotFoundException("No shadowRef OID in "+desc+" and resolution frequency set to NEVER");
		}
		if (resourceObjectReference.getObjectClass() != null) {
			objectClass = resourceObjectReference.getObjectClass();
			if (objectClass.getNamespaceURI() == null) {
				objectClass = new QName(ResourceTypeUtil.getResourceNamespace(ctx.getResource()), objectClass.getLocalPart());
			}
		}
		ProvisioningContext subctx = ctx.spawn(objectClass);
		// Use "raw" definitions from the original schema to avoid endless loops
		subctx.setUseRefinedDefinition(false);
		subctx.assertDefinition();
		
		ObjectQuery refQuery = QueryJaxbConvertor.createObjectQuery(ShadowType.class, resourceObjectReference.getFilter(), prismContext);
		ObjectFilter baseFilter = ObjectQueryUtil.createResourceAndObjectClassFilter(ctx.getResource().getOid(), objectClass, prismContext);
		ObjectFilter filter = AndFilter.createAnd(baseFilter, refQuery.getFilter());
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		
		// TODO: implement "repo" search strategies
		
		Collection<SelectorOptions<GetOperationOptions>> options = null;
		
		final Holder<ShadowType> shadowHolder = new Holder<>();
		ShadowHandler<ShadowType> handler = new ShadowHandler<ShadowType>() {
			@Override
			public boolean handle(ShadowType shadow) {
				if (shadowHolder.getValue() != null) {
					throw new IllegalStateException("More than one search results for " + desc);
				}
				shadowHolder.setValue(shadow);
				return true;
			}
		};
		
		shadowCache.searchObjectsIterative(subctx, query, options, handler, true, result);
		
		// TODO: implement storage of OID (ONCE search frequency)
		
		ShadowType shadowType = shadowHolder.getValue();
		return shadowType==null?null:shadowType.asPrismObject();
	}
	
	/**
	 * Resolve primary identifier from a collection of identifiers that may contain only secondary identifiers. 
	 */
	Collection<? extends ResourceAttribute<?>> resolvePrimaryIdentifier(ProvisioningContext ctx,
			Collection<? extends ResourceAttribute<?>> identifiers, final String desc, OperationResult result) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, 
					SecurityViolationException, ExpressionEvaluationException {
		if (identifiers == null) {
			return null;
		}
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(identifiers, ctx.getObjectClassDefinition());
		PrismObject<ShadowType> repoShadow = shadowManager.lookupShadowBySecondaryIdentifiers(ctx, secondaryIdentifiers, result);
		if (repoShadow == null) {
			return null;
		}
		PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			return null;
		}
		RefinedObjectClassDefinition ocDef = ctx.getObjectClassDefinition();
		Collection primaryIdentifiers = new ArrayList<>();
		for (PrismProperty<?> property: attributesContainer.getValue().getProperties()) {
			if (ocDef.isPrimaryIdentifier(property.getElementName())) {
				RefinedAttributeDefinition<?> attrDef = ocDef.findAttributeDefinition(property.getElementName());
				ResourceAttribute<?> primaryIdentifier = new ResourceAttribute<>(property.getElementName(), 
						attrDef, prismContext);
				primaryIdentifier.setRealValue(property.getRealValue());
				primaryIdentifiers.add(primaryIdentifier);
			}
		}
		LOGGER.trace("Resolved identifiers {} to primary identifiers {} (object class {})", identifiers, primaryIdentifiers, ocDef);
		return primaryIdentifiers;
	}
	
	/**
	 * Resolve primary identifier from a collection of identifiers that may contain only secondary identifiers. 
	 */
	private ResourceObjectIdentification resolvePrimaryIdentifiers(ProvisioningContext ctx,
			ResourceObjectIdentification identification, OperationResult result) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, 
					SecurityViolationException, ExpressionEvaluationException {
		if (identification == null) {
			return identification;
		}
		if (identification.hasPrimaryIdentifiers()) {
			return identification;
		}
		Collection<ResourceAttribute<?>> secondaryIdentifiers = (Collection<ResourceAttribute<?>>) identification.getSecondaryIdentifiers();
		PrismObject<ShadowType> repoShadow = shadowManager.lookupShadowBySecondaryIdentifiers(ctx, secondaryIdentifiers, result);
		if (repoShadow == null) {
			// TODO: we should attempt resource search here
			throw new ObjectNotFoundException("No repository shadow for "+secondaryIdentifiers+", cannot resolve identifiers");
		}
		PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null) {
			throw new SchemaException("No attributes in "+repoShadow+", cannot resolve identifiers "+secondaryIdentifiers);
		}
		RefinedObjectClassDefinition ocDef = ctx.getObjectClassDefinition();
		Collection primaryIdentifiers = new ArrayList<>();
		for (PrismProperty<?> property: attributesContainer.getValue().getProperties()) {
			if (ocDef.isPrimaryIdentifier(property.getElementName())) {
				RefinedAttributeDefinition<?> attrDef = ocDef.findAttributeDefinition(property.getElementName());
				ResourceAttribute<?> primaryIdentifier = new ResourceAttribute<>(property.getElementName(), 
						attrDef, prismContext);
				primaryIdentifier.setRealValue(property.getRealValue());
				primaryIdentifiers.add(primaryIdentifier);
			}
		}
		LOGGER.trace("Resolved {} to primary identifiers {} (object class {})", identification, primaryIdentifiers, ocDef);
		return new ResourceObjectIdentification(identification.getObjectClassDefinition(), primaryIdentifiers, 
				identification.getSecondaryIdentifiers());
	}
	
	
	public PrismObject<ShadowType> fetchResourceObject(ProvisioningContext ctx,
			Collection<? extends ResourceAttribute<?>> identifiers, 
			AttributesToReturn attributesToReturn,
			OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
		ResourceType resource = ctx.getResource();
		ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, parentResult);
		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		
		try {
		
			if (!ResourceTypeUtil.isReadCapabilityEnabled(resource)){
				throw new UnsupportedOperationException("Resource does not support 'read' operation");
			}
			
			ResourceObjectIdentification identification = ResourceObjectIdentification.create(objectClassDefinition, identifiers);
			identification = resolvePrimaryIdentifiers(ctx, identification, parentResult);
			identification.validatePrimaryIdenfiers();
			return connector.fetchObject(ShadowType.class, identification, attributesToReturn, ctx,
					parentResult);
		} catch (ObjectNotFoundException e) {
			parentResult.recordFatalError(
					"Object not found. Identifiers: " + identifiers + ". Reason: " + e.getMessage(), e);
			throw new ObjectNotFoundException("Object not found. identifiers=" + identifiers + ", objectclass="+
						PrettyPrinter.prettyPrint(objectClassDefinition.getTypeName())+": "
					+ e.getMessage(), e);
		} catch (CommunicationException e) {
			parentResult.recordFatalError("Error communication with the connector " + connector
					+ ": " + e.getMessage(), e);
			throw e;
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError(
					"Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
			throw new GenericConnectorException("Generic error in the connector " + connector + ". Reason: "
					+ e.getMessage(), e);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't get resource object, schema error: " + ex.getMessage(), ex);
			throw ex;
		} catch (ExpressionEvaluationException ex) {
			parentResult.recordFatalError("Can't get resource object, expression error: " + ex.getMessage(), ex);
			throw ex;
		} catch (ConfigurationException e) {
			parentResult.recordFatalError(e);
			throw e;
		}

	}

}
