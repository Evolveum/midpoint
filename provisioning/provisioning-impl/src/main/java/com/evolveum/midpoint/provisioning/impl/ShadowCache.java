/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.object.ResourceObjectShadowUtil;
import com.evolveum.midpoint.common.object.ResourceTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfException;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.sun.org.apache.xerces.internal.impl.xs.SchemaGrammar.Schema4Annotations;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This class manages the "cache" of ResourceObjectShadows in the repository.
 * 
 * In short, this class takes care of aligning the shadow objects in repository
 * with the real state of the resource.
 * 
 * The repository is considered a cache when it comes to Shadow objects. That's
 * why they are called shadows after all. When a new state (values) of the
 * resource object is detected, the shadow in the repository should be updated.
 * No matter if that was detected by synchronization, reconciliation or an
 * ordinary get from resource. This class is supposed to do that.
 *
 * Now it assumes we are only storing primary identifier in the repository.
 * That should be made configurable later. It also only support Account objects
 * now.
 * 
 * This is work in progress ...
 *
 * @author Radovan Semancik
 */
public class ShadowCache {
	
	private RepositoryWrapper repositoryService;
	private ConnectorManager connectorManager;

    public ShadowCache() {
        repositoryService = null;
    }

    /**
     * Get the value of repositoryService.
     *
     * @return the value of repositoryService
     */
    public RepositoryWrapper getRepositoryService() {
        return repositoryService;
    }

    /**
     * Set the value of repositoryService
     *
     * Expected to be injected.
     * 
     * @param repositoryService new value of repositoryService
     */
    public void setRepositoryService(RepositoryWrapper repositoryService) {
        this.repositoryService = repositoryService;
    }
	
	public ConnectorManager getConnectorManager() {
		return connectorManager;
	}
	
	/**
	 * Set the value of connector manager.
	 * 
	 * Expected to be injected.
	 * 
	 * @param connectorManager
	 */
	public void setConnectorManager(ConnectorManager connectorManager) {
		this.connectorManager = connectorManager;
	}

	/**
	 * Gets the object with specified OID
	 * 
	 * The shadow will be read from the repository and missing information
	 * will be fetched from the resource.
	 * 
	 * If no resource is specified, appropriate resource definition will
	 * be fetched from the repository. Specifying resource is just an
	 * optimization.
	 * 
	 * This method is using identification by OID. This is intended for normal
	 * usage. Method that uses native identification will be provided later.
	 * 
	 * @param oid OID of shadow to get.
	 * @param resource ResourceType where to get the object from (optional)
	 * @return retrieved shadow (merged attributes from repository and resource)
	 * @throws ObjectNotFoundException shadow was not found or object was not found
	 * 				on the resource
	 * @throws CommunicationException problem communicating with the resource 
	 * @throws SchemaException problem processing schema or schema violation
	 */
	public ResourceObjectShadowType getObject(String oid, ResourceType resource, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException {

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.
				
		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.
		// Later, the repository object may have a fully cached object from.
		ResourceObjectShadowType repositoryShadow = (ResourceObjectShadowType) getRepositoryService().getObject(oid, null, parentResult);
		
		if (resource==null) {
			resource = getResource(ResourceObjectShadowUtil.getResourceOid(repositoryShadow), parentResult);
		}
		
		// Get the fresh object from UCF
		ConnectorInstance connector = getConnectorInstance(resource);
		Schema schema = null;
		schema = getResourceSchema(resource,connector,parentResult);
		
		QName objectClass = repositoryShadow.getObjectClass();
		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema.findContainerDefinitionByType(objectClass);
		
		if (rod==null) {
			// Unknown objectclass
			SchemaException ex = new SchemaException("Object class "+objectClass+" defined in the repository shadow is not known in schema of resource "+ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError("Object class "+objectClass+" defined in the repository shadow is not known in resource schema", ex);
			throw ex;
		}
		
		// Let's get all the identifiers from the Shadow <attributes> part
		Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(repositoryShadow.getAttributes().getAny());
		
		if (identifiers==null || identifiers.isEmpty()) {
			// No identifiers found
			SchemaException ex = new SchemaException("No identifiers found in the respository shadow "+ObjectTypeUtil.toShortString(repositoryShadow)+" with respect to resource "+ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError("No identifiers found in the respository shadow "+ObjectTypeUtil.toShortString(repositoryShadow), ex);
			throw ex;			
		}
		
		ResourceObject ro = null;

		try {
			// Passing ResourceObjectDefinition instead object class. The returned
			// ResourceObject will have a proper links to the schema.
			ro = connector.fetchObject(rod, identifiers, parentResult);

		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
			// TODO: Discovery
			throw new ObjectNotFoundException("Object "+identifiers+" not found on the Resource "+ObjectTypeUtil.toShortString(resource),ex);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			throw new CommunicationException("Error communicating with the connector",ex);
		} catch (GenericFrameworkException ex) {
			throw new GenericConnectorException("Generic error in connector "+connector+": "+ex.getMessage(),ex);
		}
		
		// Let's replace the attribute values fetched from repository with the
		// ResourceObject content fetched from resource. The resource is more
		// fresh and the attributes more complete.
		// TODO: Discovery
		Element firstElement = repositoryShadow.getAttributes().getAny().get(0);
		Document doc = firstElement.getOwnerDocument();
		// TODO: Optimize the use of XML namespaces
		List<Element> xmlAttributes;
		try {
			xmlAttributes = ro.serializePropertiesToDom(doc);
		} catch (SchemaProcessorException ex) {
			throw new SchemaException("Schema error: "+ex.getMessage(),ex);
		}
		repositoryShadow.getAttributes().getAny().clear();
		repositoryShadow.getAttributes().getAny().addAll(xmlAttributes);
		
        return repositoryShadow;
	}

	// TODO: native identification - special cases

	
	// OLD METHODS
	// TODO: refactor to current needs
	
    /**
     * Locates the appropriate Shadow in repository, updates it as necessary and
     * returns updated shadow.
     *
     * The returned shadow object has all the values from the provided resource
     * object (the implementation is merging values).
     *
     * This does not do any modifications. We are only storing identifiers in
     * the shadows and we do not support renames yet. So we don't need to modify
     * anything. The implementation only creates the shadow object if it does
     * not exist yet.
     *
     * @param resourceObject new state of the resource object
     * @return updated shadow object
     */
//    public ResourceObjectShadowType update(ResourceObject resourceObject, ResourceType resource) throws FaultMessage {
//
//        ResourceObjectShadowType shadow = getCurrentShadow(resourceObject, resource);
//
//        ObjectValueWriter objectValueWriter = new ObjectValueWriter();
//
//        if (shadow==null) {
//            // We have to create new object
//            //Holder holder = new Holder(new OperationalResultType());
//            ObjectContainerType oct = new ObjectContainerType();
//            // Account-only for now, more generic later
//            shadow = new AccountShadowType();
//            ObjectReferenceType resourceRef = new ObjectReferenceType();
//            resourceRef.setOid(resource.getOid());
//            shadow.setResourceRef(resourceRef);
//            shadow.setAttributes(new ResourceObjectShadowType.Attributes());
//
//            // Not sure about this ....
//            objectValueWriter.merge(resourceObject.getIdentifier(), shadow.getAttributes().getAny(), false);
//
//            shadow.setObjectClass(resourceObject.getDefinition().getQName());
//
//            // TODO: This has to be smarter, later
//            shadow.setName((String)resourceObject.getIdentifier().getSingleJavaValue());
//
//            oct.setObject(shadow);
//
//            String oid = getRepositoryService().addObject(oct);
//
//            // Repository will assign OID, so put it back to the shadow
//            shadow.setOid(oid);
//        }
//
//        // Merge all the values from resourceObject to shadow now
//
//        objectValueWriter.merge(resourceObject,shadow.getAttributes().getAny(), false);
//
//        // For shadow that we are returning we want to set resource instead of
//        // resourceRef. Resource is easiet to use and as we have it here anyway
//        // it is not harn setting it.
//        shadow.setResource(resource);
//        shadow.setResourceRef(null);
//        
//        return shadow;
//    }

    /**
     * Locates the appropriate Shadow in repository that corresponds to the
     * provided resource object.
     *
     * No update is done, just the current repository state is returned. This
     * operation is NOT supposed for generic usage. It is expected to be used
     * in the rare cases where old repository state is required
     * (e.g. synchronization)
     *
     * TODO: Fix error handling (both runtime exceptions and Fault)
     * 
     * @param resourceObject any state of resource objects, it just to contain
     *        valid identifiers
     * @return current unchanged shadow object that corresponds to provided
     *         resource object or null if the object does not exist
     */
//    public ResourceObjectShadowType getCurrentShadowFromRepository(String oid, ResourceType resource) {

//        QueryType query = createSearchShadowQuery(resourceObject);
//        PagingType paging = new PagingType();
//        
//        // TODO: check for errors
//        ObjectListType results;
//
//        results = getRepositoryService().searchObjects(query, paging);
//
//        if (results.getObject().size()==0) {
//            return null;
//        }
//        if (results.getObject().size()>1) {
//            // TODO: Better error handling later
//            throw new IllegalStateException("More than one shadows found for "+resourceObject);
//        }
//
//        return (ResourceObjectShadowType) results.getObject().get(0);
 //   }


//    protected QueryType createSearchShadowQuery(ResourceObject resourceObject) {
//
//        // We are going to query for attributes, so setup appropriate
//        // XPath for the filter
//        XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
//        List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
//        xpathSegments.add(xpathSegment);
//        XPathType xpath = new XPathType(xpathSegments);
//
//        // Now we need to determine what is the identifer and set corrent
//        // value for it in the filter
//        ResourceAttribute identifier = resourceObject.getIdentifier();
//
//        List<Node> idNodes = identifier.getValues();
//        // Only one value is supported for an identifier
//        if (idNodes.size()>1) {
//            // TODO: This should probably be switched to checked exception later
//            throw new IllegalArgumentException("More than one identifier value is not supported");
//        }
//        if (idNodes.size()<1) {
//            // TODO: This should probably be switched to checked exception later
//            throw new IllegalArgumentException("The identifier has no value");
//        }
//        Element idElement = (Element) idNodes.get(0);
//
//        // We have all the data, we can construct the filter now
//        Document doc = ShadowUtil.getXmlDocument();
//        Element filter =
//                QueryUtil.createAndFilter(doc,
//                // TODO: The account type is hardcoded now, it should determined
//                // from the shcema later, or maybe we can make it entirelly
//                // generic (use ResourceObjectShadowType instead).
//                QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_TYPE)),
//                QueryUtil.createEqualFilter(doc, xpath, idElement));
//
//        QueryType query = new QueryType();
//        query.setFilter(filter);
//
//        return query;
//    }
	
	
	// UTILITY METHODS
	
	private ConnectorInstance getConnectorInstance(ResourceType resource) {
		// TODO: Add caching later
		return getConnectorManager().createConnectorInstance(resource);
	}
	
	private Schema getResourceSchema(ResourceType resource, ConnectorInstance connector, OperationResult parentResult) throws CommunicationException {

		// TEMPORARY HACK: Fetch schema from connector
		
		try {
			return connector.fetchResourceSchema(parentResult);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			throw new CommunicationException("Error communicating with the connector "+connector,ex);
		} catch (GenericFrameworkException ex) {
			throw new GenericConnectorException("Generic error in connector "+connector+": "+ex.getMessage(),ex);
		}
		
		// Need to add some form of caching here.
		// For now just parse it from the resource definition.
		
//		Element schemaElement = ResourceTypeUtil.getResourceXsdSchema(resource);
//		if (schemaElement==null) {
//			throw new SchemaException("No schema found in definition of resource "+ObjectTypeUtil.toShortString(resource));
//		}
//		return Schema.parse(schemaElement);
	}
	
	private ResourceType getResource(String oid ,OperationResult parentResult) throws ObjectNotFoundException {
		// TODO: add some caching
		return (ResourceType) getRepositoryService().getObject(oid,null,parentResult);
	}
		
}
