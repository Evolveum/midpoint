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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.service;

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.exception.IllegalRequestException;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.aop.ResourceAccessAspect;
import com.evolveum.midpoint.provisioning.exceptions.InitialisationException;
import com.evolveum.midpoint.provisioning.exceptions.ValidationException;
import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.synchronization.ImportFromResourceTask;
import com.evolveum.midpoint.provisioning.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType.SynchronizationState;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SchemaViolationFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import org.identityconnectors.common.StringUtil;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Main entrypoint and implementation of provisioning.
 *
 * @author lazyman
 */
@Service(value = "provisioningService")
public class ProvisioningService implements ProvisioningPortType {

    /**
     * TODO, should be injected.
     */
    @Autowired(required = false)
    private ResourceObjectChangeListenerPortType objectChangeListener;
    private ResourceFactory connectorFactory = new DefaultResourceFactory();
    private static final Trace logger = TraceManager.getTrace(ProvisioningService.class);
    private ObjectValueWriter valueWriter = ObjectValueWriter.getInstance();
    @Autowired
    private RepositoryPortType repositoryService;
    private ResourceObjectShadowCache shadowCache;
    private static final String IMPORT_TASK_NAME_PREFIX = "import-from-resource-";
    /**
     * Map of running and recently finished import tasks by resource OID.
     */
    private Map<String, ImportFromResourceTask> importTasks;

    public ProvisioningService() {
        importTasks = new HashMap<String, ImportFromResourceTask>();
    }

    private RepositoryPortType getRepository() {
        return repositoryService;
    }

    // Used in test to inject dependency (test inject mock)
    public void setRepositotyService(RepositoryPortType service) {
        this.repositoryService = service;
    }

    // Used in test to inject dependency (test inject mock)
    public void setRepositoryPort(RepositoryPortType newRepository) {
        repositoryService = newRepository;
    }

    // Used in test to inject dependency (test inject mock)
    public void setConnectorFactory(ResourceFactory connectorFactory) {
        this.connectorFactory = connectorFactory;
    }

    protected synchronized ResourceObjectShadowCache getResourceObjectShadowCache() {
        if (shadowCache == null) {
            shadowCache = new ResourceObjectShadowCache();
            shadowCache.setRepositoryService(getRepository());
        }
        return shadowCache;
    }

    @Override
    public java.lang.String addObject(ObjectContainerType objectContainer, ScriptsType scripts, Holder<OperationalResultType> result) throws FaultMessage {

        logger.info("### PROVISIONING # Enter addObject({})", DebugUtil.prettyPrint(objectContainer));

        // If DEBUG level is enabled, log the attributes of the created shadow
        if (logger.isDebugEnabled()) {
            ObjectType object = objectContainer.getObject();
            if (object instanceof ResourceObjectShadowType) {
                ResourceObjectShadowType shadow = (ResourceObjectShadowType) object;
                Attributes attributes = shadow.getAttributes();
                List<Element> elements = attributes.getAny();
                for (Element el : elements) {
                    logger.debug("A: " + DebugUtil.prettyPrint(el));
                }
            }
        }

        ObjectType object = objectContainer.getObject();

        String newOid = null;

        if (object instanceof ResourceObjectShadowType) {
            try {
                //TODO this workflow is currently not atomic

                ResourceObjectShadowType shadow = (ResourceObjectShadowType) object;

                // TODO: fist add object on resource by calling identity connector

                resolveResource(shadow);

                ResourceObject updatedObject = null;

                ResourceAccessInterface rai = getResourceAccessInterface(shadow);

                ResourceSchema schema = mergeSchema(rai, shadow);


                ResourceObject resourceObject = valueWriter.buildResourceObject(shadow, schema);

                if (shadow instanceof AccountShadowType) {
                    AccountShadowType acctShadow = (AccountShadowType) shadow;
                    if (null != acctShadow.getCredentials()) {
                        processAccountCredentials(resourceObject, acctShadow);
                    }
                }
                if (valueWriter.validateBeforeCreate(resourceObject)) {

                    //TODO: The search should be executed here. The get only works if the system does not use GUID
                    ResourceObject robj = null;//rai.get(result.value, resourceObject);
                    if (robj == null) {
                        processScripts(result.value, rai, scripts, ScriptOrderType.BEFORE, OperationTypeType.ADD);
                        //This should return with the entire object, like the update
                        updatedObject = rai.add(result.value, resourceObject, shadow);
                        processScripts(result.value, rai, scripts, ScriptOrderType.AFTER, OperationTypeType.ADD);
                    } else {
                        //FIXME
                        throw new UnsupportedOperationException();
                        //updatedObject = rai.modify(result.value, resourceObject, null);
                    }
                }



                if (null == updatedObject) {
                    // TODO: Error message?
                    logger.error("### PROVISIONING # Fault addObject(..): Resource Object Creation Failed");
                    throw new FaultMessage("Resource Object Creation Failed", new IllegalArgumentFaultType());
                }

                // TODO: update the attributes and keep the ones we need to store
                valueWriter.postProcessShadow(updatedObject, shadow);

                //remove resolved resource
                unresolveResource(shadow);
                objectContainer.setObject(shadow);

                // And finally store the new object shadow in repository
                newOid = getRepository().addObject(objectContainer);

            } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
                logger.error("### PROVISIONING # Fault addObject(..): Error on add object to the repository: {}", ex.getMessage());
                logger.debug("Error on add object to the repository", ex);
                throw createFaultMessage("Repository invocation failed (addObject)", ex.getFaultInfo(), ex, result.value);
            } catch (ValidationException ex) {
                logger.error("### PROVISIONING # Fault addObject(..): Object validation error: {}", ex.getMessage());
                throw createFaultMessage("Object validation error: " + ex.getMessage(), SchemaViolationFaultType.class, false, ex, result.value);
            } catch (Exception ex) {
                logger.error("### PROVISIONING # Fault addObject(..): General error in the provisioning (addObject): {}", ex.getMessage());
                logger.debug("General error in the provisioning (addObject).", ex);
                throw createFaultMessage("General error in the provisioning (addObject): " + ex.getMessage(), SystemFaultType.class, false, ex, result.value);
            }

        } else {

            // TODO: convert the class name to proper XSD type for reporting
            // Faults should use XML terminology, not Java classes
            // TODO: check if this provides correct SOAP faults. I have some doubts.
            logger.error("### PROVISIONING # Fault addObject(..): Unsupported object type : {}", object.getClass().getName());
            throw new FaultMessage("Unsupported object type " + object.getClass().getName(), new IllegalArgumentFaultType());

        }

        logger.info("### PROVISIONING # Exit addObject(..) : ", newOid);

        return newOid;

    }

    @Override
    public ObjectContainerType getObject(java.lang.String oid, PropertyReferenceListType resolve, Holder<OperationalResultType> result) throws FaultMessage {

        logger.info("### PROVISIONING # Enter getObject({},{})", oid, DebugUtil.prettyPrint(resolve));

        testOid(oid);

        // TODO: support resolve parameter

        try {

            // Tis operation needs to support getting both ResourceObjectShadow (and subclasses)
            // and Resource. But we do not know what object type we are getting
            // unless we get that from repository first.

            ObjectType object = getRepository().getObject(oid, resolve).getObject();

            if (object instanceof ResourceType) {

                ResourceType resource = (ResourceType) object;

                completeResource(resource);

                ObjectContainerType container = new ObjectContainerType();
                container.setObject(resource);
                return container;

            } else if (object instanceof ResourceObjectShadowType) {

                ResourceObjectShadowType shadow = (ResourceObjectShadowType) object;

                completeResourceObjectShadow(shadow);

                ObjectContainerType container = new ObjectContainerType();
                container.setObject(shadow);

                logger.info("### PROVISIONING # Exit getObject(..) : ", DebugUtil.prettyPrint(container));

                return container;

            } else {

                // TODO: convert the class name to proper XSD type for reporting
                // Faults should use XML terminology, not Java classes
                // TODO: check if this provides correct SOAP faults. I have some doubts.

                logger.error("### PROVISIONING # Fault getObject(..) : Unsupported object type : {}", object.getClass().getName());
                throw createFaultMessage("Unsupported object type " + object.getClass().getName() + " (OID: " + oid + ")", IllegalArgumentFaultType.class, false, null, result.value);

            }


        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("### PROVISIONING # Fault getObject(..) : Repository error : {}", ex.getMessage());
            throw createFaultMessage("Repository invocation failed (getObject)", ex.getFaultInfo(), ex, result.value);
        } catch (RuntimeException ex) {
            logger.error("### PROVISIONING # Fault getObject(..) : Unknown error : {}", ex.getMessage());
            throw createFaultMessage("Error on getting object", SystemFaultType.class, false, ex, result.value);
        }

    }

    @Override
    public ObjectListType listObjects(java.lang.String objectType, PagingType paging, Holder<OperationalResultType> result) throws FaultMessage {

        logger.info("### PROVISIONING # Enter listObjects({})", objectType);

        if (objectType == null || objectType.isEmpty()) {
            logger.error("### PROVISIONING # Fault listObjects(..) : Object type can't be null or empty");
            throw new FaultMessage("Object type can't be null or empty.", new IllegalArgumentFaultType());
        }

        // TODO check for correct object types

        ObjectListType listType = null;
        try {

            listType = getRepository().listObjects(objectType, paging);

            // TODO: for lists of shadows
            // Iterate over the list and call completeResourceObjectShadow
            // for each element
            // TODO: for lists of resources
            // Iterate over the list and call completeResource
            // for each element

        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("### PROVISIONING # Fault listObjects(..) : Repository error : {}", ex.getMessage());
            logger.debug("Unknown repository error occured.", ex);

            throw new FaultMessage("Unknown repository error occured.", new SystemFaultType(), ex);
        }

        logger.info("### PROVISIONING # Exit listObjects(..) : ", listType);

        return listType;
    }

    @Override
    public ObjectListType searchObjects(QueryType query, PagingType paging, Holder<OperationalResultType> result) throws FaultMessage {

        logger.info("### PROVISIONING # Enter searchObjects({})", query);

        if (query == null) {
            logger.error("### PROVISIONING # Fault searchObjects(..) : Query can't be null");
            throw new FaultMessage("Query can't be null", new IllegalArgumentFaultType());
        }

        // TODO check for correct object types

        ObjectListType listType = null;
        try {

            listType = getRepository().searchObjects(query, paging);

            // TODO: for lists of shadows
            // Iterate over the list and call completeResourceObjectShadow
            // for each element
            // TODO: for lists of resources
            // Iterate over the list and call completeResource
            // for each element

        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("### PROVISIONING # Fault searchObjects(..) : Repository error : {}", ex.getMessage());
            logger.debug("Unknown error occured.", ex);

            throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);
        }

        logger.info("### PROVISIONING # Exit searchObjects(..) : ", listType);

        return listType;
    }

    @Override
    public void modifyObject(ObjectModificationType objectChange, ScriptsType scripts, Holder<OperationalResultType> result) throws FaultMessage {

        logger.info("### PROVISIONING # Enter modifyObject({})", DebugUtil.prettyPrint(objectChange));

        // Now, this will get quite tricky. There is too much to do. But we can
        // place some limitations to save some work now:
        // * We do not support renames now (change of Uid).
        // * We do not support change of resource (resourceRef) in shadow
        // * We do not support assignments of operational attributes now
        // That makes things significantly easier. In fact we just need to
        // propagate the attribute changes down to the identity connector
        // We do not need to change the object in repository

        if (objectChange == null) {
            logger.error("### PROVISIONING # Fault modifyObject(..): Object to change must not be null");
            throw new FaultMessage("Object to change must not be null", new IllegalArgumentFaultType());
        }

        String oid = objectChange.getOid();
        try {
            testOid(oid);

            ObjectContainerType oc = getRepository().getObject(oid, new PropertyReferenceListType());
            ObjectType ot = oc.getObject();

            if (ot instanceof ResourceObjectShadowType) {

                // TODO: We should modify object in the repository only if
                // explicitly asked to
                //getRepository().modifyObject(objectChange);


                ResourceObjectShadowType shadow = (ResourceObjectShadowType) ot;

                resolveResource(shadow);

                ResourceAccessInterface rai = getResourceAccessInterface(shadow);

                ResourceSchema schema = rai.getConnector().getSchema();

                ResourceObject identification = valueWriter.buildResourceObject(shadow, schema);

                ResourceObjectDefinition objectDefinition = schema.getObjectDefinition(shadow.getObjectClass());

                Set<AttributeChange> changes = processObjectChanges(schema, objectDefinition, objectChange);

                processScripts(result.value, rai, scripts, ScriptOrderType.BEFORE, OperationTypeType.MODIFY);

                getResourceAccessInterface(shadow).modify(result.value, identification, objectDefinition, changes);

                processScripts(result.value, rai, scripts, ScriptOrderType.AFTER, OperationTypeType.MODIFY);



            } else {
                logger.error("### PROVISIONING # Fault modifyObject(..): Unsupported object type {} (OID: {})", ot.getClass().getName(), oid);
                throw new FaultMessage("Unsupported object type " + ot.getClass().getName() + " (OID: " + oid + ")", new IllegalArgumentFaultType());
            }

        } catch (IllegalRequestException ex) {
            logger.error("### PROVISIONING # Fault modifyObject(..): Illegal Request : {}", ex);
            throw new FaultMessage(ex.getMessage(), new IllegalArgumentFaultType(), ex);

        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            // TODO: It really should not happen that inner code will throw WSDL fault!
            logger.error("### PROVISIONING # Fault modifyObject(..): Unknown error : {}", ex.getMessage());
            logger.debug("Unknown error occured.", ex);

            throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);
        }

        logger.info("### PROVISIONING # Exit modifyObject(..)");
    }

    @Override
    public void deleteObject(java.lang.String oid, ScriptsType scripts, Holder<OperationalResultType> result) throws FaultMessage {

        logger.info("### PROVISIONING # Enter deleteObject({})", oid);

        testOid(oid);

        try {
            ObjectContainerType oct = getRepository().getObject(oid, new PropertyReferenceListType());
            if (oct == null) {
                logger.error("### PROVISIONING # Fault deleteObject(..): No object with oid {}", oid);
                throw new FaultMessage("No object with oid " + oid, new IllegalArgumentFaultType());
            }
            ObjectType c = oct.getObject();
            if (c instanceof ResourceObjectShadowType) {
                ResourceObjectShadowType shadow = (ResourceObjectShadowType) c;
                resolveResource(shadow);
                getRepository().deleteObject(oid);

                try {
                    ResourceAccessInterface rai = getResourceAccessInterface(shadow);
                    ResourceObject resourceObject = valueWriter.buildResourceObject(shadow, rai.getConnector().getSchema());
                    processScripts(result.value, rai, scripts, ScriptOrderType.BEFORE, OperationTypeType.DELETE);
                    rai.delete(result.value, resourceObject);
                    processScripts(result.value, rai, scripts, ScriptOrderType.AFTER, OperationTypeType.DELETE);
                } catch (MidPointException ex) {
                    // TODO !!!
                    logger.error("### PROVISIONING # Fault deleteObject(..): Unknown error : {}", ex.getMessage());
                    logger.debug("Unknown error " + ex);

                    throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);
                }
            } else {
                logger.error("### PROVISIONING # Fault deleteObject(..): Unsupported object type {} (OID: {})", c.getClass().getName(), oid);
                throw new FaultMessage("Unsupported object type " + c.getClass().getName() + " (OID: " + oid + ")", new IllegalArgumentFaultType());
            }



        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("### PROVISIONING # Fault deleteObject(..): Unknown error : {}", ex.getMessage());
            logger.debug("Unknown error occured.", ex);

            throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);
        }

        logger.info("### PROVISIONING # Exit deleteObject(..)");

    }

    @Override
    public PropertyAvailableValuesListType getPropertyAvailableValues(java.lang.String oid, PropertyReferenceListType properties, Holder<OperationalResultType> result) throws FaultMessage {

        logger.info("### PROVISIONING # Enter getPropertyAvailableValues({},{})", oid, properties);

        testOid(oid);

        // This may get really tricky eventally. For now return only empty
        // list for everything. Do not even delegate the call to repository.

        PropertyAvailableValuesListType listType = null;
        try {
            listType = getRepository().getPropertyAvailableValues(oid, properties);
        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {

            logger.error("### PROVISIONING # Fault getPropertyAvailableValues(..): Unknown error : {}", ex.getMessage());
            logger.debug("Unknown error occured.", ex);

            throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);
        }

        logger.info("### PROVISIONING # Exit getPropertyAvailableValues(..) : {}", listType);

        return listType;
    }

    @Override
    public ObjectContainerType addResourceObject(String resourceOid, ObjectContainerType objectContainer, Holder<OperationalResultType> result) throws FaultMessage {
        if (objectContainer == null || objectContainer.getObject() == null) {
            throw new FaultMessage("Resource object type can't be null.", new IllegalArgumentFaultType());
        }

        //TODO implement this method
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public ObjectContainerType getResourceObject(String resourceOid, ResourceObjectIdentificationType identification, Holder<OperationalResultType> result) throws FaultMessage {

        ResourceType resource = null;
        try {
            resource = (ResourceType) getRepository().getObject(resourceOid, new PropertyReferenceListType()).getObject();
        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("Error on getting resource (oid: " + resourceOid + ") from repository");
            throw new FaultMessage("Error on getting resource (oid: " + resourceOid + ") from repository", ex.getFaultInfo(), ex);
        }
        ResourceObjectShadowType shadow = null;
//        try {
//            ResourceObject object = getResourceAccessInterface(resourceOid, resource, null).operationGET(result.value, identification, null);
//
//        } catch (MidPointException ex) {
//        }




        ObjectContainerType container = new ObjectContainerType();
        container.setObject(shadow);

        return container;
    }

    @Override
    public ObjectListType listResourceObjects(String resourceOid, String objectType, PagingType paging, Holder<OperationalResultType> result) throws FaultMessage {
        ObjectListType objectList = new ObjectListType();
        try {
            ResourceAccessInterface rai = getResourceAccessInterface(resourceOid, null);
            ResourceSchema schema = rai.getConnector().getSchema();
            ObjectReferenceType resourceRef = new ObjectReferenceType();
            resourceRef.setOid(resourceOid);
            ResourceObjectDefinition def = schema.getObjectDefinition(new QName(schema.getResourceNamespace(), objectType));
            if (null != def) {

                Collection<ResourceObject> a = rai.search(result.value, def);
                if (null != a) {
                    for (ResourceObject ro : a) {
                        ResourceObjectShadowType shadow = new ResourceObjectShadowType();
                        shadow.setResourceRef(resourceRef);
                        shadow.setObjectClass(def.getQName());
                        shadow.setName(UUID.randomUUID().toString());
                        ResourceObjectShadowType.Attributes attrs = new ResourceObjectShadowType.Attributes();
                        valueWriter.merge(ro, attrs.getAny(), false);
                        shadow.setAttributes(attrs);
                        objectList.getObject().add(shadow);
                    }
                }
            }
        } catch (MidPointException ex) {
        }
        return objectList;
    }

    @Override
    public void modifyResourceObject(String resourceOid, ObjectModificationType objectChange, Holder<OperationalResultType> result) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteResourceObject(String resourceOid, ResourceObjectIdentificationType identification, Holder<OperationalResultType> result) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Fills the required __PASSWORD__ attribute based credentials in the accountshadow
     * @param resourceObject
     * @param shadow
     * @throws DOMException
     */
    private void processAccountCredentials(ResourceObject resourceObject, AccountShadowType shadow) throws DOMException {
        ResourceObjectDefinition objectDefinition = resourceObject.getDefinition();
        //TODO: works for ICF ldap
        QName attrQName = new QName(SchemaConstants.NS_ICF_RESOURCE, "__PASSWORD__");
        ResourceAttributeDefinition attributeDefinition = objectDefinition.getAttributeDefinition(attrQName);
        ResourceAttribute resourceAttribute = new ResourceAttribute(attributeDefinition);
        Document doc = ShadowUtil.getXmlDocument();
        Element pwd = doc.createElementNS(attrQName.getNamespaceURI(), attrQName.getLocalPart());
        pwd.setTextContent(new String(Base64.decodeBase64(((Element) shadow.getCredentials().getPassword().getAny()).getTextContent())));
        resourceAttribute.addValue(pwd);
        resourceObject.addValue(resourceAttribute);
    }

    private void testOid(String oid) throws FaultMessage {
        if (StringUtil.isBlank(oid)) {
            throw new FaultMessage("Oid can't be null or empty.", new IllegalArgumentFaultType());
        }
//        else {
//            try {
//                return UUID.fromString(oid);
//            } catch (IllegalArgumentException e) {
//                throw new FaultMessage("OID is invalid UUID: " + oid, null, e);
//            }
//        }
    }

    /**
     * Return provisioning implementation.
     *
     * Later it could be use more than one provider.
     *
     * @param resource
     * @return
     */
    protected ResourceAccessInterface getResourceAccessInterface(String resourceOID, ResourceType inputResource) throws FaultMessage {
        BaseResourceIntegration connector = null;
        ResourceAccessConfigurationType resourceAccessConfiguration = null;
        try {
            ResourceType savedResource = (ResourceType) getRepository().getObject(resourceOID, new PropertyReferenceListType()).getObject();
//            resourceAccessConfiguration = getResourceAccessConfiguration(savedResource);
            //TODO: Merge the two resources instead of overwrite. This way the resource can configured from business process similar: Complete Resource
            connector = mergerResourceTypes(inputResource, savedResource);
        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("Error on resolve ObjectShadow.resourceRef from repository. resource=" + resourceOID);
            throw new FaultMessage("Error on resolve ObjectShadow.resourceRef from repository.", ex.getFaultInfo(), ex);
        }
        try {
            ResourceAccessInterface rif;
            rif = connectorFactory.checkout(ResourceAccessAspect.class, connector, resourceAccessConfiguration);
            return rif;
        } catch (InitialisationException ex) {
            logger.error("Error on ResourceAccessInterface Initialisation");
            throw new FaultMessage("Error on ResourceAccessInterface Initialisation, reason: " + ex.getMessage(), null, ex);
        }
    }

    protected ResourceAccessInterface getResourceAccessInterface(ResourceObjectShadowType shadow) throws FaultMessage {
        if (shadow.getResource() == null) {
            if (shadow.getResourceRef() == null) {
                throw new FaultMessage("Resource and resourceRef are undefined at object(oid=" + shadow.getOid() + "). One of them should be set.", new SchemaViolationFaultType());
            }
            return getResourceAccessInterface(shadow.getResourceRef().getOid(), null);
        } else {
            return getResourceAccessInterface(shadow.getResource().getOid(), shadow.getResource());
        }
    }

    private ResourceAccessConfigurationType getResourceAccessConfiguration(ResourceType resource) {
        String oid = null;
        try {
            oid = resource.getResourceAccessConfigurationRef().getOid();
            return (ResourceAccessConfigurationType) getRepository().getObject(oid, new PropertyReferenceListType()).getObject();
        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("Error on resolve ResourceType.resourceAccessConfigurationRef from repository. ResourceType=" + resource.getOid() + " , ResourceAccessConfigurationType=" + oid, ex);
        }
        return new ResourceAccessConfigurationType();
    }

    private BaseResourceIntegration mergerResourceTypes(ResourceType inputResource, ResourceType savedResource) {
        //TODO: make this dynamic type mapping;
        return new BaseResourceIntegration(savedResource);
    }

    /**
     * Complete resource with schema and possible other generated elements.
     *
     * This does nothing for now. Later it will handle resource schema.
     *
     * @param resource
     * @throws FaultMessage
     */
    private void completeResource(ResourceType resource) throws FaultMessage {
        // There is nothing we need to
        // do with this now.
        // Later we will need to add/refresh schema in the Resource
    }

    /**
     * Complete the shadow object with all the attributes.
     *
     * This method will get all the additional attributes from the resource and
     * will add it to the shadow object provided as a parameter.
     *
     * This should be independed from identity connectors concepts or any
     * other specific provisioning implementation.
     *
     * @param shadow Resource obeject shadow from the repository.
     * @throws FaultMessage
     */
    private void completeResourceObjectShadow(ResourceObjectShadowType shadow) throws FaultMessage, MidPointException, com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {

        if (shadow.getAttributes() == null) {
            // TODO
            // This is most likely an error. I cannot imagine that
            // there will be a valid shadow without any attribute
            shadow.setAttributes(new ResourceObjectShadowType.Attributes());
        }
        resolveResource(shadow);

        ResourceAccessInterface rai = getResourceAccessInterface(shadow);
        ResourceConnector connector = rai.getConnector();
        ResourceSchema schema = connector.getSchema();

        ResourceObject id = valueWriter.buildResourceObject(shadow, schema);


        OperationalResultType result = new OperationalResultType();
        ResourceObject resultObject = rai.get(result, id);

        if (resultObject == null) {
            throw new FaultMessage("The object " + shadow.getOid() + " could not be found", new SystemFaultType());
        }

        new ObjectValueWriter().merge(resultObject, shadow.getAttributes().getAny(), false);

        unresolveResource(shadow);


    }

    /**
     * Remove unresolver resource obejct and fill resource ref.
     * @param shadow
     */
    protected void unresolveResource(ResourceObjectShadowType shadow) {
        if (null != shadow.getResource()) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(shadow.getResource().getOid());
            shadow.setResourceRef(ort);
            shadow.setResource(null);
        }
    }

    protected void resolveResource(ResourceObjectShadowType shadow) throws com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
        if (shadow.getResource() == null) {
            if (shadow.getResourceRef() == null) {
                throw new com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage("Resource and resourceRef are undefined at object(oid=" + shadow.getOid() + "). One of them should be set.", new SchemaViolationFaultType());
            }

            String resourceOid = shadow.getResourceRef().getOid();

            ObjectContainerType oc = getRepository().getObject(resourceOid, new PropertyReferenceListType());
            if (oc == null) {
                throw new MidPointException("Invalid resource in object (oid=" + shadow.getOid() + ")");
            }
            ResourceType resource = (ResourceType) oc.getObject();
            shadow.setResource(resource);
        }
    }

    public Set<AttributeChange> processObjectChanges(ResourceSchema schema, ResourceObjectDefinition objectDefinition, ObjectModificationType objectChange) throws IllegalRequestException {

        Set<AttributeChange> attrChanges = new HashSet<AttributeChange>();
        //iterate over changes

        for (PropertyModificationType change : objectChange.getPropertyModification()) {

            XPathType xpath = new XPathType(change.getPath());

            List<XPathSegment> segments = xpath.toSegments();
            List<Element> any = change.getValue().getAny();

            if (any != null && !any.isEmpty()) {
                QName attrQName = null;
                Element firstElement = any.get(0);
                String localName = firstElement.getLocalName();
                String namespace = firstElement.getNamespaceURI();
                boolean credentials = false;
                if (segments.isEmpty()) {
                    if (SchemaConstants.I_CREDENTIALS.getLocalPart().equals(localName)) {
                        credentials = true;
                    }
                } else {
                    if (SchemaConstants.I_ATTRIBUTES.equals(segments.get(0).getQName())) {
                        if (segments.size() > 1) {
                            throw new IllegalRequestException("Hierarchy in " + segments.get(0).getQName() + " is not supported (XPath)");
                        }
                    } else if (SchemaConstants.I_CREDENTIALS.equals(segments.get(0).getQName())) {
                        credentials = true;
                    }
                }
                if (credentials) {
                    //TODO: works for ICF ldap
                    attrQName = new QName(SchemaConstants.NS_ICF_RESOURCE, "__PASSWORD__");
                } else {
                    attrQName = new QName(namespace, localName);
                }
                ResourceAttributeDefinition attributeDefinition = objectDefinition.getAttributeDefinition(attrQName);
                ResourceAttribute resourceAttribute = new ResourceAttribute(attributeDefinition);


                for (Element element : any) {
                    // TODO: This getFirstChild() here is a hack
                    // The RIA should take the entire element, not just
                    // the content. There may be attributes on the element
                    // or a content with several sub-nodes.
                    if (credentials) {
                        Node password = null;
                        if (segments.isEmpty()) {
                            NodeList l = element.getElementsByTagNameNS(SchemaConstants.NS_C, "password");
                            if (l.getLength() == 1) {
                                password = l.item(0).getFirstChild();
                            }
                        } else {
                            password = element.getFirstChild();
                        }
                        if (null != password) {
                        	password.setTextContent(new String(Base64.decodeBase64(password.getTextContent())));
                            resourceAttribute.addJavaValue(password.getTextContent());
                        } else {
                            resourceAttribute.addJavaValue("");
                        }
                    } else {
                        resourceAttribute.addValue(element.getFirstChild());
                    }
                }

                AttributeChange attrChange = new AttributeChange();
                attrChange.setChangeType(change.getModificationType());
                attrChange.setAttribute(resourceAttribute);

                attrChanges.add(attrChange);

            } else {
                logger.warn("modifyObject(): Received empty attribute value element for oid {}", objectChange.getOid());
            }
        }
        return attrChanges;
    }

    private QName getAttrNameFromXPath(ResourceObjectDefinition definition, XPathType xpath) throws IllegalRequestException {

        List<XPathSegment> segments = xpath.toSegments();

        if (!SchemaConstants.I_ATTRIBUTES.equals(segments.get(0).getQName())) {
            throw new IllegalRequestException("Expected XPath starting with " + SchemaConstants.I_ATTRIBUTES + ", got " + xpath);
        }

        // TODO: additional checks.

        QName attrQName = segments.get(1).getQName();

        for (ResourceAttributeDefinition rad : definition.getAttributesCopy()) {
            if (rad.getQName().equals(attrQName)) {
                return attrQName;
            }
        }
        throw new IllegalRequestException("Attribute definition not found for " + attrQName + " (xpath " + xpath + " )");
    }

    private ResourceSchema mergeSchema(ResourceAccessInterface rai, ResourceObjectShadowType shadow) {
        ResourceSchema schema = rai.getConnector().getSchema();
        //todo add activity tag to the schema
        return schema;
    }

    private FaultMessage createFaultMessage(String message, FaultType faultType, Exception ex, OperationalResultType result) {
        if (faultType.getMessage() == null || faultType.getMessage().isEmpty()) {
            faultType.setMessage(message);
        } else {
            faultType.setMessage(message + " : " + faultType.getMessage());
        }
        return new FaultMessage(message, faultType, ex);
    }

    private FaultMessage createFaultMessage(String message, Class<? extends FaultType> faultTypeClass, boolean temporary, Exception exception, OperationalResultType result) {
        FaultType fault;
        try {
            fault = faultTypeClass.newInstance();
        } catch (InstantiationException ex) {
            // This should not happen
            throw new IllegalArgumentException("Cannot instantate " + faultTypeClass.getName(), ex);
        } catch (IllegalAccessException ex) {
            // This should not happen
            throw new IllegalArgumentException("Cannot instantate " + faultTypeClass.getName(), ex);
        }
        if (exception instanceof RuntimeException) {
            fault.setMessage(message + " : " + exception.getClass().getSimpleName() + " : " + exception.getMessage());
        } else {
            fault.setMessage(message);
        }
        return new FaultMessage(message, fault, exception);
    }

    @Override
    public OperationalResultType synchronize(java.lang.String oid) throws FaultMessage {
        logger.info("### PROVISIONING # Enter synchronize({})", oid);
        OperationalResultType result = new OperationalResultType();
        testOid(oid);

        try {
            ObjectContainerType oct = getRepository().getObject(oid, new PropertyReferenceListType());
            if (oct == null) {
                logger.error("### PROVISIONING # Fault synchronize(..): No object with oid {}", oid);
                throw new FaultMessage("No object with oid " + oid, new IllegalArgumentFaultType());
            }
            ObjectType c = oct.getObject();
            if (c instanceof ResourceType) {
                ResourceType resource = (ResourceType) c;

                try {
                    ResourceAccessInterface rai = getResourceAccessInterface(resource.getOid(), resource);

                    ResourceStateType state = getResourceState(resource.getOid());

                    ResourceStateType.SynchronizationState syncToken = state == null ? null : state.getSynchronizationState();

                    ResourceSchema schema = rai.getConnector().getSchema();

                    // TODO: The hardcoded "Account" value should be removed, this must be somehow dynamic
                    ResourceObjectDefinition rad = rai.getConnector().getSchema().getObjectDefinition(new QName(schema.getResourceNamespace(), "Account"));

                    SynchronizationResult changeCollection = rai.synchronize(syncToken, result, rad);

                    if (changeCollection.getChanges().isEmpty()) {
                    	// FIXME: This should be debug, not info
                    	logger.info("Synchronization (OID:{}) detected no changes",resource.getOid());
                    }

                    for (SynchronizationResult.Change changeWrapper : changeCollection.getChanges()) {
                    	
                        ObjectChangeType change = changeWrapper.getChange();

                        // FIXME: This should be debug, not info
                    	logger.info("Synchronization (OID:{}) detected change: {}",resource.getOid(),DebugUtil.prettyPrint(change));

                        ObjectChangeAdditionType newChange = new ObjectChangeAdditionType();

                        ResourceObjectShadowChangeDescriptionType roscdt = new ResourceObjectShadowChangeDescriptionType();


                        roscdt.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));

                        roscdt.setResource(resource);

                        ObjectType originalShadow = null;
                        if (changeWrapper.getChange() instanceof ObjectChangeDeletionType) {
                            ObjectChangeDeletionType ocd = (ObjectChangeDeletionType) changeWrapper.getChange();
                            originalShadow = findOrCreateShadow(null, ocd.getOid(), resource);
                            if (originalShadow == null) {
                                // This shouldn't be an error. We are going delete something that was
                                // already deleted, so what the heck? Someone already did that work ...
                                // (this may well happen in case of sync message re-delivery).

                                // So just log a warning and pretend success.
                                logger.warn("Got delete sync message for already deleted shadow {}, resource {}. ignoring it", ocd.getOid(), resource.getName());

                                state = refreshToken(state, oid, resource.getName(), changeWrapper.getToken());
                                continue;
                            }
                            roscdt.setObjectChange(change);
                        } else {
                            originalShadow = findOrCreateShadow(changeWrapper.getIdentifier(), null, resource);
                            ((ObjectChangeModificationType) change).getObjectModification().setOid(originalShadow.getOid());
                            getRepository().modifyObject(((ObjectChangeModificationType) change).getObjectModification());
                            ObjectContainerType object = getRepository().getObject(((ObjectChangeModificationType) change).getObjectModification().getOid(), new PropertyReferenceListType());
                            ((AccountShadowType) (object.getObject())).setResource(resource);
                            newChange.setObject(object.getObject());
                            roscdt.setObjectChange(newChange);
                        }

                        if (!(originalShadow instanceof ResourceObjectShadowType)) {
                            logger.error("### PROVISIONING # Fault synchronize(..): Invalid change object. The changed object is not a resourceObject. OID={}", originalShadow.getOid());
                            // TODO: Is SystemFault appropriate here?
                            throw new FaultMessage("Invalid change object. The changed object is not a resourceObject. OID=" + originalShadow.getOid(), new SystemFaultType());
                        }
                        ((ResourceObjectShadowType) originalShadow).setResource(resource);
                        roscdt.setShadow((ResourceObjectShadowType) originalShadow);
                        objectChangeListener.notifyChange(roscdt);

                        if (changeWrapper.getChange() instanceof ObjectChangeDeletionType) {
                            getRepository().deleteObject(originalShadow.getOid());
                        }

                        state = refreshToken(state, oid, resource.getName(), changeWrapper.getToken());
                    }



                } catch (JAXBException ex) {
                    logger.error("### PROVISIONING # Fault synchronize(..): Unknown error : {}", ex.getMessage());
                    logger.debug("Unknown error " + ex);

                    throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);


                } catch (com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.FaultMessage ex) {
                    logger.error("### PROVISIONING # Fault synchronize(..): Unknown error : {}", ex.getMessage());
                    logger.debug("Unknown error " + ex);

                    throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);
                } catch (MidPointException ex) {
                    // TODO !!!
                    logger.error("### PROVISIONING # Fault synchronize(..): Unknown error : {}", ex.getMessage());
                    logger.debug("Unknown error " + ex);

                    throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);
                }
            } else {
                logger.error("### PROVISIONING # Fault syncronize(..): Unsupported object type {} (OID: {})", c.getClass().getName(), oid);
                throw new FaultMessage("Unsupported object type " + c.getClass().getName() + " (OID: " + oid + ")", new IllegalArgumentFaultType());
            }
        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("### PROVISIONING # Fault deleteObject(..): Unknown error : {}", ex.getMessage());
            logger.debug("Unknown error occured.", ex);



            throw new FaultMessage("Unknown error occured.", new SystemFaultType(), ex);


        }

        logger.info("### PROVISIONING # Exit synchronize(..): {}", result);


        return result;
    }

    @Override
    public ResourceTestResultType testResource(String resourceOid) throws FaultMessage {
        logger.info("### PROVISIONING # Enter testResource({})", resourceOid);

        // This does not really works as expected
        // It looks like that the initialization of the RAI is in fact trying
        // to reach the resource.

        ResourceAccessInterface rai = null;
        TestResultType initTestResult = new TestResultType();
        try {
            rai = getResourceAccessInterface(resourceOid, null);
            initTestResult.setSuccess(true);
        } catch (RuntimeException ex) {
            initTestResult.setSuccess(false);
            List<JAXBElement<DiagnosticsMessageType>> errorOrWarning = initTestResult.getErrorOrWarning();
            DiagnosticsMessageType message = new DiagnosticsMessageType();
            message.setMessage(ex.getClass().getName() + ": " + ex.getMessage());
            // TODO: message.setDetails();
            JAXBElement<DiagnosticsMessageType> element = new JAXBElement<DiagnosticsMessageType>(SchemaConstants.I_DIAGNOSTICS_MESSAGE_ERROR, DiagnosticsMessageType.class, message);
            errorOrWarning.add(element);
        }

        ResourceTestResultType result = rai.test();

        if (result.getConnectorInitialization() == null) {
            result.setConnectorInitialization(initTestResult);
        }

        logger.info("### PROVISIONING # Exit testResource(..): {}", result);
        return result;
    }

    @Override
    public synchronized EmptyType launchImportFromResource(String resourceOid, String objectClass) throws FaultMessage {
        logger.info("### PROVISIONING # Enter launchImportFromResource({},{})", resourceOid, objectClass);

        ResourceType resource = null;

        try {

            resource = (ResourceType) getRepository().getObject(resourceOid, new PropertyReferenceListType()).getObject();

        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("### PROVISIONING # Fault launchImportFromResource(..) : Repository error : {}", ex.getMessage());
            throw createFaultMessage("Repository invocation failed (getObject)", ex.getFaultInfo(), ex, null);
        } catch (ClassCastException ex) {
            logger.error("### PROVISIONING # Fault launchImportFromResource(..): Object with OID {} is not a resource", resourceOid);
            throw new FaultMessage("Object with OID " + resourceOid + " is not a resource", new SystemFaultType());
        }

        // Only one import task may run, so check that no other task is running
        ImportFromResourceTask task = importTasks.get(resourceOid);

        if (task != null && task.isAlive()) {
            logger.error("### PROVISIONING # Fault launchImportFromResource({},{}): An import task is already running", resourceOid, objectClass);
            throw new FaultMessage("An import task is already running", new SystemFaultType());
        }

        ResourceAccessInterface rai = getResourceAccessInterface(resourceOid, null);
        ResourceSchema schema = rai.getConnector().getSchema();
        ResourceObjectDefinition objectDefinition = null;
        try {
            // This "getter" throws an exception
            objectDefinition = schema.getObjectDefinition(new QName(schema.getResourceNamespace(), objectClass));
        } catch (MidPointException ex) {
            logger.error("### PROVISIONING # Fault launchImportFromResource(..): Definition of object class {} not found in resource schema", objectClass);
            throw new FaultMessage("Definition of object class " + objectClass + " not found in resource schema", new SystemFaultType());
        }

        // Start the import task (as a new thread)

        task = new ImportFromResourceTask(resource, rai, objectDefinition);
        task.setName(IMPORT_TASK_NAME_PREFIX + resource.getOid());
        task.setResourceObjectShadowCache(getResourceObjectShadowCache());
        task.setObjectChangeListener(getObjectChangeListener());
        logger.info("Staring import task, importing from resource {} (OID: {})", resource.getName(), resource.getOid());
        task.start();
        importTasks.put(resourceOid, task);

        logger.info("### PROVISIONING # Exit launchImportFromResource({},{})", resourceOid, objectClass);
        return new EmptyType();
    }

    @Override
    public TaskStatusType getImportStatus(String resourceOid) throws FaultMessage {
        logger.info("### PROVISIONING # Enter getImportStatus({})", resourceOid);

        ResourceType resource = null;

        try {

            resource = (ResourceType) getRepository().getObject(resourceOid, new PropertyReferenceListType()).getObject();

        } catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage ex) {
            logger.error("### PROVISIONING # Fault getImportStatus(..) : Repository error : {}", ex.getMessage());
            throw createFaultMessage("Repository invocation failed (getObject)", ex.getFaultInfo(), ex, null);
        } catch (ClassCastException ex) {
            logger.error("### PROVISIONING # Fault getImportStatus(..): Object with OID {} is not a resource, reason: {}",
                    new Object[]{resourceOid, ex.getMessage()});
            throw new FaultMessage("Object with OID " + resourceOid + " is not a resource, reason: " +
                    ex.getMessage(), new SystemFaultType());
        }

        TaskStatusType status = new TaskStatusType();
        status.setName("Import from resource " + resource.getName() + " (OID: " + resourceOid + ")");
        ImportFromResourceTask task = importTasks.get(resourceOid);
        if (task == null) {
            status.setRunning(false);
            status.setLastStatus("none");
            logger.info("### PROVISIONING # Exit getImportStatus({}): {}", resourceOid, status);
            return status;
        }

        DatatypeFactory dtf = null;
        try {
            dtf = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            // TODO
            Logger.getLogger(ProvisioningService.class.getName()).log(Level.SEVERE, null, ex);
        }

        status.setLastStatus("unknown");

        status.setRunning(task.isAlive());
        status.setProgress(task.getProgress());

        Long launchTime = task.getLaunchTime();
        if (launchTime != null) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(launchTime.longValue());
            XMLGregorianCalendar xmlLaunchTime = dtf.newXMLGregorianCalendar(gregorianCalendar);
            status.setLaunchTime(xmlLaunchTime);
        } else {
            status.setLastStatus("starting");
        }

        if (task.isAlive()) {
            status.setLastStatus("running");
        }

        if (task.getLastError() != null) {
            status.setLastStatus("error");
        }

        if (task.getLastError() != null) {
            DiagnosticsMessageType diagMsg = new DiagnosticsMessageType();
            Exception lastError = task.getLastError();
            diagMsg.setMessage(lastError.getClass().getSimpleName() + ": " + lastError.getMessage());

            long errorTime = task.getLastErrorTime();
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(errorTime);
            XMLGregorianCalendar xmlErrorTime = dtf.newXMLGregorianCalendar(gregorianCalendar);
            diagMsg.setTimestamp(xmlErrorTime);

            // TODO copy stack strace
            diagMsg.setDetails("");

            status.setLastError(diagMsg);
        }

        status.setNumberOfErrors(task.getNumberOfErrors());

        // TODO: copy other status values

        Long finishTime = task.getFinishTime();
        if (finishTime != null) {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            gregorianCalendar.setTimeInMillis(finishTime.longValue());
            XMLGregorianCalendar xmlLaunchTime = dtf.newXMLGregorianCalendar(gregorianCalendar);
            status.setFinishTime(xmlLaunchTime);
            status.setLastStatus("finished");
        }

        logger.info("### PROVISIONING # Exit getImportStatus({}): {}", resourceOid, status);
        return status;
    }

    public void setObjectChangeListener(ResourceObjectChangeListenerPortType objectChangeListener) {
        this.objectChangeListener = objectChangeListener;
    }

    public ResourceObjectChangeListenerPortType getObjectChangeListener() {
        return objectChangeListener;
    }

    public void setRepositoryService(RepositoryPortType repositoryService) {
        this.repositoryService = repositoryService;


    }

    private ResourceStateType getResourceState(String oid) throws com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {

        QueryType query = createSearchStateQuery(oid);

        PagingType paging = new PagingType();
        ObjectListType results = getRepository().searchObjects(query, paging);


        if (results.getObject().size() == 0) {
            return null;
        } else if (results.getObject().size() > 1) {
            logger.warn("Found more than one resource states for OID: {}", oid);
        }
        return (ResourceStateType) results.getObject().get(0);


    }

    // Returning newly created state object is necessary if the input state was null
    private ResourceStateType refreshToken(ResourceStateType state, String oid, String resourceName, SynchronizationState syncToken) throws com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage {
        //state.setSynchronizationState(syncToken);
        if (state == null) {
            ObjectContainerType oct = new ObjectContainerType();
            state = new ResourceStateType();
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(oid);
            state.setResourceRef(ort);
            state.setSynchronizationState(syncToken);
            state.setName(resourceName);
            oct.setObject(state);
            String newOid = getRepository().addObject(oct);
            state.setOid(newOid);
            return state;

        } else {
            ObjectModificationType obj = new ObjectModificationType();
            obj.setOid(state.getOid());

            PropertyModificationType modification = new PropertyModificationType();
            modification.setValue(new PropertyModificationType.Value());

            modification.getValue().getAny().addAll(syncToken.getAny());
            modification.setModificationType(PropertyModificationTypeType.replace);

            List<XPathSegment> segments = new ArrayList<XPathSegment>();
            segments.add(new XPathSegment(new QName(SchemaConstants.NS_C, "synchronizationState")));
            XPathType xpath = new XPathType(segments);
            modification.setPath(xpath.toElement(SchemaConstants.NS_C, "path"));

            obj.getPropertyModification().add(modification);
            getRepository().modifyObject(obj);
            return state;
        }


    }

    private ObjectType findOrCreateShadow(ResourceObject identifier, String acctOid, ResourceType resource) throws JAXBException, com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage, FaultMessage {

        PagingType paging = new PagingType();

        QueryType query = createSearchShadowQuery(identifier, acctOid);
        ObjectListType results = getRepository().searchObjects(query, paging);


        if (results == null || results.getObject().size() != 1) {
            //Delete case but no shadow object found
            if (identifier == null) {
                // This may be an error or may not. The caller should decide.
                // This code really should not log an error. Threfore changing this
                // to warning for now, but even that is not good.
                logger.warn("Object with OID " + acctOid + " can not be found");
                return null;
            }
            //create
            Holder holder = new Holder(new OperationalResultType());
            ObjectContainerType oct = new ObjectContainerType();

            AccountShadowType shadow = new AccountShadowType();

            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(resource.getOid());
            shadow.setResourceRef(ort);

            shadow.setAttributes(new ResourceObjectShadowType.Attributes());

            new ObjectValueWriter().merge(identifier, shadow.getAttributes().getAny(), false);

            // TODO: remove hardcoded string literal
            shadow.setObjectClass(new QName(resource.getNamespace(), "Account"));

            oct.setObject(shadow);

            ResourceAccessInterface rai = getResourceAccessInterface(shadow);
            ResourceSchema schema = rai.getConnector().getSchema();
            ResourceObjectDefinition objectDefinition = schema.getObjectDefinition(shadow.getObjectClass());

            ResourceAttributeDefinition attributeDefinition = objectDefinition.getPrimaryIdentifier();
            shadow.setName((String) identifier.getValue(attributeDefinition.getQName()).getSingleJavaValue());

            String oid = getRepository().addObject(oct);
            return getObject(oid, new PropertyReferenceListType(), holder).getObject();

        } else {
            return results.getObject().get(0);
        }

    }

    /**
     *
     * @param oid
     * @return
     * @throws JAXBException
     * @todo use a query builder instead.s
     */
    protected QueryType createSearchStateQuery(String oid) {

        Document doc = ShadowUtil.getXmlDocument();


        Element resourceRef = doc.createElementNS(SchemaConstants.I_RESOURCE_REF.getNamespaceURI(), SchemaConstants.I_RESOURCE_REF.getLocalPart());
        resourceRef.setAttribute("oid", oid);

        Element filter =
                QueryUtil.createAndFilter(doc,
                QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_RESOURCE_STATE_TYPE)),
                QueryUtil.createEqualFilter(doc, null, resourceRef));

        QueryType query = new QueryType();
        query.setFilter(filter);

        return query;
    }

    /**
     *
     * @param oid
     * @return
     * @throws JAXBException
     * @todo use a query builder instead.s
     */
    protected QueryType createSearchShadowQuery(ResourceObject identifier, String oid) {

        XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
        List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
        xpathSegments.add(xpathSegment);
        XPathType xpath = new XPathType(xpathSegments);

        List<Element> values = new ArrayList<Element>();

        Document doc = ShadowUtil.getXmlDocument();
        Element uid = null;
        if (oid != null) {
            uid = doc.createElementNS(SchemaConstants.NS_ICF_RESOURCE, "__UID__");
            uid.setTextContent(oid);
        } else {
            valueWriter.merge(identifier, values, false);
            for (Element element : values) {
                if ("__UID__".equals(element.getLocalName())) {
                    uid = element;
                }
            }
        }


        Element filter =
                QueryUtil.createAndFilter(doc,
                QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_TYPE)),
                QueryUtil.createEqualFilter(doc, xpath, uid));

        QueryType query = new QueryType();
        query.setFilter(filter);

        return query;
    }
    // TODO: following XMLs are no longer used. They should be removed later.
    // But leaving them here now for reference, as the search methods above are
    // not finished yet.
    public static final String SEARCH_SHADOW_BY_UUID = "<c:query  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + "    xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'" + "    xmlns:dj=\"http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ\"" + "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" + "    xsi:schemaLocation='http://www.w3.org/2001/XMLSchema ../standard/XMLSchema.xsd" + "    http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd ../../../main/resources/META-INF/wsdl/xml/ns/public/common/common-1.xsd'" + "     xmlns:foo=\"http://foo.com/\">" + "     <c:and>" + "         <c:type uri=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd#AccountShadowType\"/>" + "          <c:equal>" + "             <c:path>c:attributes</c:path>" + "             <c:value>" + "                 <dj:__UID__>cn=foobar,uo=people,dc=nlight,dc=eu</dj:__UID__>" + "             </c:value>" + "          </c:equal>" + "     </c:and>" + " </c:query>";
    public static final String SEARCH_BY_UUID = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "          <c:query  xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'" + "   xmlns:c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'" + "   xmlns:i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd'" + "   xmlns:dj=\"http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ\"" + "   xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"" + "   xsi:schemaLocation='http://www.w3.org/2001/XMLSchema ../standard/XMLSchema.xsd" + "   http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd ../../../main/resources/META-INF/wsdl/xml/ns/public/common/common-1.xsd'" + "   xmlns:foo=\"http://foo.com/\">" + "    <c:and>" + "        <c:type uri=\"http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd#ResourceStateType\"/>" + "        <c:equal>" + "            <c:value>" + "                <i:resourceRef oid=\"1234\"/>" + "            </c:value>" + "        </c:equal>" + "    </c:and>" + " </c:query>";

    private void processScripts(OperationalResultType result, ResourceAccessInterface rai, ScriptsType scripts, ScriptOrderType order, OperationTypeType type) {
        logger.trace("Enter processScripts({})");

        if (scripts != null) {
            for (ScriptType script : scripts.getScript()) {
                if (order.equals(script.getOrder()) && script.getOperation().contains(type)) {
                    logger.debug("Executing script: " + script.getCode());
                    try {
                        rai.executeScript(result, script);
                    } catch (MidPointException ex) {
                        // Just log it, but otherwise ignore the error
                        // TODO: provide more context in the message
                        logger.error("Script execution failed: {}", ex.getMessage(), ex);

                        // Ignore the error for now. We don't consider script
                        // errors to be critical. If we do, the account may be
                        // created but it will not be linked as the model will
                        // detect error and assume that the creation failed
                        //
                        // We need to fix that later, at least by starting to
                        // use a composite result and provide the error in
                        // that. Now the user will not see any error.
                    }
                }
            }
        }
    }
}
