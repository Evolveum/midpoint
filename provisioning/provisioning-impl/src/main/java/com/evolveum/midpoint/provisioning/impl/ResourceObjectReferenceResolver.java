/**
 * Copyright (c) 2015-2016 Evolveum
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

import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author semancik
 *
 */
@Component
public class ResourceObjectReferenceResolver {
	
	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
	@Qualifier("shadowCacheProvisioner")
	private ShadowCache shadowCache;
	
	PrismObject<ShadowType> resolve(ProvisioningContext ctx, ResourceObjectReferenceType resourceObjectReference, 
			QName objectClass, final String desc, OperationResult result) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, 
					SecurityViolationException {
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
	
	public PrismObject<ShadowType> fetchResourceObject(ProvisioningContext ctx,
			Collection<? extends ResourceAttribute<?>> identifiers, 
			AttributesToReturn attributesToReturn,
			OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException, SecurityViolationException, ConfigurationException {
		ResourceType resource = ctx.getResource();
		ConnectorInstance connector = ctx.getConnector(parentResult);
		RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
		
		try {
		
			if (!ResourceTypeUtil.isReadCapabilityEnabled(resource)){
				throw new UnsupportedOperationException("Resource does not support 'read' operation");
			}
			
			ResourceObjectIdentification identification = new ResourceObjectIdentification(objectClassDefinition, identifiers, null);
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
		} catch (ConfigurationException e) {
			parentResult.recordFatalError(e);
			throw e;
		}

	}

}
