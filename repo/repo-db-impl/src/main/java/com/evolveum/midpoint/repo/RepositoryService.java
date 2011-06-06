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

package com.evolveum.midpoint.repo;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.patch.PatchingListener;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.Account;
import com.evolveum.midpoint.model.GenericEntityBase;
import com.evolveum.midpoint.model.User;
import com.evolveum.midpoint.model.Property;
import com.evolveum.midpoint.model.Resource;
import com.evolveum.midpoint.model.ResourceAccessConfiguration;
import com.evolveum.midpoint.model.ResourceObjectShadow;
import com.evolveum.midpoint.model.ResourceState;
import com.evolveum.midpoint.model.SimpleDomainObject;
import com.evolveum.midpoint.model.StringProperty;
import com.evolveum.midpoint.model.UserTemplate;
import com.evolveum.midpoint.repo.spring.GenericDao;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.constants.MidPointConstants;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectAlreadyExistsFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ReferentialIntegrityFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.UnsupportedObjectTypeFaultType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import javax.xml.bind.JAXBElement;
import org.apache.commons.lang.StringUtils;
import javax.xml.namespace.QName;
import org.springframework.stereotype.Service;
import org.w3c.dom.NodeList;

/**
 * TODO
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
@Service
@Transactional
public class RepositoryService implements RepositoryPortType {

    Trace logger = TraceManager.getTrace(RepositoryService.class);
    @Autowired(required = true)
    private GenericDao genericDao;
    // Ugly, bad ... but this is thwor-away code anyway ...
    ObjectFactory objectFactory = new ObjectFactory();

    private void addExtensionProperty(ExtensibleObjectType objectType, Property property) throws DOMException {
        Extension extension = objectType.getExtension();
        if (null == extension) {
            extension = new Extension();
            objectType.setExtension(extension);
        }

//        String propertyName = property.getPropertyName();
//        Element element;
//        if (propertyName.contains(":")) {
//            String namespace = propertyName.substring(0, propertyName.lastIndexOf(":"));
//            String localName = propertyName.substring(propertyName.lastIndexOf(":") + 1);
//            element = DOMUtil.getDocument().createElementNS(namespace, localName);
//        } else {
//            element = DOMUtil.getDocument().createElement(propertyName);
//        }
        Document doc = DOMUtil.parseDocument((String) property.getPropertyValue());
        extension.getAny().add((Element) doc.getFirstChild());
        //extension.getAny().add(element);
    }

    private void storeOrImport(SimpleDomainObject entity) throws FaultMessage {
        //OPENIDM-137 - import has to preserve oid and fail if importing object is already stored in db
        if (null != entity.getOid()) {
            if (null == genericDao.findById(entity.getOid())) {
                genericDao.persist(entity);
            } else {
                throw new FaultMessage("Could not import, because object with oid '" + entity.getOid() + "' already exists in store", new ObjectAlreadyExistsFaultType());
            }
        } else {
            genericDao.persist(entity);
        }
    }

    //copy method are preparation for refactoring for deep copy general method.
    private void copyExtensionToProperties(List<Element> extension, List<Property> properties) {
        for (Element element : extension) {
            Property property = new StringProperty();
            property.setPropertyName(element.getNamespaceURI() + ":" + element.getLocalName());
            property.setPropertyValue(DOMUtil.serializeDOMToString(element));
            addOrReplaceProperty(properties, property);
        }
    }

    private void copyGenericEntityToGenericOjectType(GenericEntityBase genericEntity, GenericObjectType genericObjectType) {
        Utils.copyPropertiesSilent(genericObjectType, genericEntity);

        for (Property property : genericEntity.getProperties()) {
            //properties are only from extension
            addExtensionProperty(genericObjectType, property);
        }

    }

    private void copyGenericObjectTypeToGenericEntity(GenericObjectType genericObjectType, GenericEntityBase genericEntity) {
        String oid = genericObjectType.getOid();
        genericObjectType.setOid(null);
        Utils.copyPropertiesSilent(genericEntity, genericObjectType);
        if (null != oid) {
            genericEntity.setOid(UUID.fromString(oid));
        }
        genericObjectType.setOid(oid);

        List<Property> properties;
        if (null == genericEntity.getProperties()) {
            properties = new LinkedList<Property>();
            genericEntity.setProperties(properties);
        } else {
            properties = genericEntity.getProperties();
        }

        //Note: extension is not processed as one property, but it is decompossed into many properties
        if (null != genericObjectType.getExtension()) {
            List<Element> extension = genericObjectType.getExtension().getAny();
            //TODO: possible problem
            copyExtensionToProperties(extension, properties);
        }

    }

    private void copyUserToUserType(User user, UserType userType) throws FaultMessage {
        //1. preparing objects, that cannot be copied by shallow copy method

        //2. setting the properties to null (not to copy them by shallow method)

        //3. making shallow copy
        Utils.copyPropertiesSilent(userType, user);

        //TODO: review this
//        if (user.getEmail() != null) {
//            userType.getEMailAddress().add(user.getEmail());
//        }

        //6.a copy account's references
        Set<Account> accounts = user.getAccounts();
        List<AccountShadowType> accountShadowTypeList = new ArrayList<AccountShadowType>();
        List<ObjectReferenceType> accountsRef = new ArrayList<ObjectReferenceType>();
        for (Account account : accounts) {
            AccountShadowType accountType = new AccountShadowType();
            copyAccountToAccountShadowType(account, accountType);
            accountShadowTypeList.add(accountType);
            ObjectReferenceType objectReferenceType = new ObjectReferenceType();
            objectReferenceType.setOid(String.valueOf(account.getOid()));
            objectReferenceType.setType(SchemaConstants.I_ACCOUNT_REF);
            accountsRef.add(objectReferenceType);

        }
        userType.getAccountRef().addAll(accountsRef);

        for (Property property : user.getProperties()) {

            //6.b copy credentials
            if ("credentials".equals(property.getPropertyName())) {
                CredentialsType credentials;
                try {
                    credentials = ((JAXBElement<CredentialsType>) JAXBUtil.unmarshal((String) property.getPropertyValue())).getValue();
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }

                userType.setCredentials(credentials);
            } else {
                //otherwise copy the remaining properties to extension
                addExtensionProperty(userType, property);
            }
        }

    }

    private void copyUserTypeToUser(UserType userType, User user) throws FaultMessage {
        String oid = userType.getOid();
        userType.setOid(null);
        Utils.copyPropertiesSilent(user, userType);
        if (null != oid) {
            user.setOid(UUID.fromString(oid));
        }
        userType.setOid(oid);

        List<Property> properties;
        if (null == user.getProperties()) {
            properties = new LinkedList<Property>();
            user.setProperties(properties);
        } else {
            properties = user.getProperties();
        }

        //Note: extension is not processed as one property, but it is decompossed into many properties
        if (null != userType.getExtension()) {
            List<Element> extension = userType.getExtension().getAny();
            //TODO: possible problem
            copyExtensionToProperties(extension, properties);
        }

        //credentials begin
        try {
            CredentialsType credentials = userType.getCredentials();
            if (null != credentials) {
                Property credentialsProperty = new StringProperty();
                credentialsProperty.setPropertyName("credentials");
                credentialsProperty.setPropertyValue(JAXBUtil.marshalWrap(credentials, SchemaConstants.I_CREDENTIALS));
                addOrReplaceProperty(properties, credentialsProperty);
            }
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //credentials end

        //TODO: review this
//        List<String> emails = userType.getEMailAddress();
//        if (!emails.isEmpty()) {
//            user.setEmail(emails.get(0));
//        }
    }

    private void copyAccountToAccountShadowType(Account account, AccountShadowType accountShadowType) throws FaultMessage {
        //1. preparing object, that cannot be copied by shallow copy method
        Resource resource = account.getResource();
        Map<String, String> attributes = account.getAttributes();
        String objectClass = account.getObjectClass();

        //2. setting the properties to null (not to copy them by shallow method)
        account.setResource(null);
        account.setAttributes(null);
        account.setObjectClass(null);

        //3. making shallow copy
        Utils.copyPropertiesSilent(accountShadowType, account);

        //4. setting back 
        account.setResource(resource);
        account.setAttributes(attributes);
        account.setObjectClass(objectClass);

        //5. making copy of the remaining objects, that were not copied by shallow copy
        //5.a - resource copy - only resource ref will be created
        //ResourceType resourceType = (new ObjectFactory()).createResourceType();
        //copyResourceToResourceType(resource, resourceType);
        //accountShadowType.setResource(resourceType);
        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setOid(String.valueOf(resource.getOid()));
        accountShadowType.setResourceRef(objectReferenceType);

        //5.b - attributes copy
        List<Element> attributesType = new ArrayList();
        for (String attributeName : attributes.keySet()) {
            String attributeValue = attributes.get(attributeName);
            String attributeNamespace = attributeName.substring(0, attributeName.lastIndexOf(":"));
            attributeName = attributeName.substring(attributeName.lastIndexOf(":") + 1);
            Document doc = DOMUtil.getDocument();
            doc.setDocumentURI(attributeNamespace);
            //TODO: generalize
            Node node = doc.createElementNS(attributeNamespace, "ns66:" + attributeName);
            node.setTextContent(attributeValue);
            doc.appendChild(node);
            //ElementImpl element = new ElementImpl(doc, attributeName);
            //element.setTextContent(attributeValue);
            attributesType.add(doc.getDocumentElement());
        }
        ResourceObjectShadowType.Attributes rost = new ResourceObjectShadowType.Attributes();
        rost.getAny().addAll(attributesType);
        accountShadowType.setAttributes(rost);

        //5.c - handling object class
        objectClass = account.getObjectClass();
        String objectClassNamespace = StringUtils.substring(objectClass, 0, StringUtils.lastIndexOf(objectClass, ":"));
        String objectClassName = StringUtils.substring(objectClass, StringUtils.lastIndexOf(objectClass, ":") + 1);
        accountShadowType.setObjectClass(new QName(objectClassNamespace, objectClassName));

        for (Property property : account.getProperties()) {
            if ("credentials".equals(property.getPropertyName())) {
                CredentialsType credentials;
                try {
                    credentials = ((JAXBElement<CredentialsType>) JAXBUtil.unmarshal((String) property.getPropertyValue())).getValue();
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }

                accountShadowType.setCredentials(credentials);
            }
        }

    }

    private void copyResourceObjectShadowToResourceObjectShadowType(ResourceObjectShadow account, ResourceObjectShadowType accountShadowType) throws FaultMessage {
        //1. preparing object, that cannot be copied by shallow copy method
        Resource resource = account.getResource();
        Map<String, String> attributes = account.getAttributes();
        String objectClass = account.getObjectClass();

        //2. setting the properties to null (not to copy them by shallow method)
        account.setResource(null);
        account.setAttributes(null);
        account.setObjectClass(null);

        //3. making shallow copy
        Utils.copyPropertiesSilent(accountShadowType, account);

        //4. setting back
        account.setResource(resource);
        account.setAttributes(attributes);
        account.setObjectClass(objectClass);

        //5. making copy of the remaining objects, that were not copied by shallow copy
        //5.a - resource copy - only resource ref will be created
        //ResourceType resourceType = (new ObjectFactory()).createResourceType();
        //copyResourceToResourceType(resource, resourceType);
        //accountShadowType.setResource(resourceType);
        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setOid(String.valueOf(resource.getOid()));
        accountShadowType.setResourceRef(objectReferenceType);

        //5.b - attributes copy
        List<Element> attributesType = new ArrayList();
        for (String attributeName : attributes.keySet()) {
            String attributeValue = attributes.get(attributeName);
            String attributeNamespace = attributeName.substring(0, attributeName.lastIndexOf(":"));
            attributeName = attributeName.substring(attributeName.lastIndexOf(":") + 1);
            Document doc = DOMUtil.getDocument();
            doc.setDocumentURI(attributeNamespace);
            //TODO: generalize
            Node node = doc.createElementNS(attributeNamespace, "ns66:" + attributeName);
            node.setTextContent(attributeValue);
            doc.appendChild(node);
            //ElementImpl element = new ElementImpl(doc, attributeName);
            //element.setTextContent(attributeValue);
            attributesType.add(doc.getDocumentElement());
        }
        ResourceObjectShadowType.Attributes rost = new ResourceObjectShadowType.Attributes();
        rost.getAny().addAll(attributesType);
        accountShadowType.setAttributes(rost);

        //5.c - handling object class
        objectClass = account.getObjectClass();
        String objectClassNamespace = StringUtils.substring(objectClass, 0, StringUtils.lastIndexOf(objectClass, ":"));
        String objectClassName = StringUtils.substring(objectClass, StringUtils.lastIndexOf(objectClass, ":") + 1);
        accountShadowType.setObjectClass(new QName(objectClassNamespace, objectClassName));

    }

    private void copyAccountShadowTypeToAccount(AccountShadowType accountShadowType, Account account) throws FaultMessage {
        ResourceObjectShadowType.Attributes attributesType = accountShadowType.getAttributes();
        accountShadowType.setAttributes(null);
        String oid = accountShadowType.getOid();
        accountShadowType.setOid(null);
        QName objectClass = accountShadowType.getObjectClass();

        Utils.copyPropertiesSilent(account, accountShadowType);
        if (null != oid) {
            account.setOid(UUID.fromString(oid));
        }
        accountShadowType.setAttributes(attributesType);
        accountShadowType.setOid(oid);
        accountShadowType.setObjectClass(objectClass);

        if (null != accountShadowType.getExtension()) {
            List<Element> extension = accountShadowType.getExtension().getAny();
            List<Property> properties = new LinkedList<Property>();
            copyExtensionToProperties(extension, properties);
            account.setProperties(properties);
        }

        Map<String, String> attributes = new HashMap<String, String>(0);
        if (null != attributesType) {

            for (Object object : attributesType.getAny()) {

                Element element = (Element) object;

                //ElementImpl element = (ElementImpl) object;
                if (null != element.getNamespaceURI()) {
                    attributes.put(element.getNamespaceURI() + ":" + element.getLocalName(), element.getTextContent());
                } else {
                    attributes.put(element.getLocalName(), element.getTextContent());
                }
            }
        }
        account.setAttributes(attributes);

        //account's resource
        ObjectReferenceType resourceRef = accountShadowType.getResourceRef();
        SimpleDomainObject resource = genericDao.findById(UUID.fromString(resourceRef.getOid()));
        account.setResource((Resource) resource);

        //account's object class
        String objectClassString = objectClass.getNamespaceURI() + ":" + objectClass.getLocalPart();
        account.setObjectClass(objectClassString);

        List<Property> properties;
        if (null == account.getProperties()) {
            properties = new LinkedList<Property>();
            account.setProperties(properties);
        } else {
            properties = account.getProperties();
        }

        //credentials begin
        try {
            CredentialsType credentials = accountShadowType.getCredentials();
            if (null != credentials) {
                Property credentialsProperty = new StringProperty();
                credentialsProperty.setPropertyName("credentials");
                credentialsProperty.setPropertyValue(JAXBUtil.marshalWrap(credentials, SchemaConstants.I_CREDENTIALS));
                addOrReplaceProperty(properties, credentialsProperty);
            }
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //credentials end

    }

    private void copyResourceObjectShadowTypeToResourceObjectShadow(ResourceObjectShadowType accountShadowType, ResourceObjectShadow account) {
        ResourceObjectShadowType.Attributes attributesType = accountShadowType.getAttributes();
        accountShadowType.setAttributes(null);
        String oid = accountShadowType.getOid();
        if (null != oid) {
            account.setOid(UUID.fromString(oid));
        }
        accountShadowType.setOid(null);
        QName objectClass = accountShadowType.getObjectClass();

        Utils.copyPropertiesSilent(account, accountShadowType);
        accountShadowType.setAttributes(attributesType);
        accountShadowType.setOid(oid);
        accountShadowType.setObjectClass(objectClass);

        if (null != accountShadowType.getExtension()) {
            List<Element> extension = accountShadowType.getExtension().getAny();
            List<Property> properties = new LinkedList<Property>();
            copyExtensionToProperties(extension, properties);
            account.setProperties(properties);
        }

        Map<String, String> attributes = new HashMap<String, String>(0);
        if (null != attributesType) {

            for (Object object : attributesType.getAny()) {

                Element element = (Element) object;

                //ElementImpl element = (ElementImpl) object;
                if (null != element.getNamespaceURI()) {
                    attributes.put(element.getNamespaceURI() + ":" + element.getLocalName(), element.getTextContent());
                } else {
                    attributes.put(element.getLocalName(), element.getTextContent());
                }
            }
        }
        account.setAttributes(attributes);

        //account's resource
        ObjectReferenceType resourceRef = accountShadowType.getResourceRef();
        SimpleDomainObject resource = genericDao.findById(UUID.fromString(resourceRef.getOid()));
        account.setResource((Resource) resource);

        //account's object class
        String objectClassString = objectClass.getNamespaceURI() + ":" + objectClass.getLocalPart();
        account.setObjectClass(objectClassString);
    }

    private void copyResourceToResourceType(Resource resource, ResourceType resourceType) throws FaultMessage {
        Utils.copyPropertiesSilent(resourceType, resource);
        for (Property property : resource.getProperties()) {
            logger.trace("(String) property.getPropertyValue() = {}", (String) property.getPropertyValue());
            if ("schema".equals(property.getPropertyName())) {
                if (StringUtils.isNotEmpty((String) property.getPropertyValue())) {
                    Document doc = DOMUtil.parseDocument((String) property.getPropertyValue());
                    XmlSchemaType schema = new XmlSchemaType();
                    List<Element> any = new ArrayList();
                    any.add(doc.getDocumentElement());
                    schema.getAny().addAll(any);
                    resourceType.setSchema(schema);
                }
            } else if ("schemaHandling".equals(property.getPropertyName())) {
                try {
                    JAXBElement<SchemaHandlingType> rsht = (JAXBElement<SchemaHandlingType>) JAXBUtil.unmarshal((String) property.getPropertyValue());
                    resourceType.setSchemaHandling(rsht.getValue());
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }
            } else if ("configuration".equals(property.getPropertyName())) {
                try {
                    Configuration configuration = (Configuration) JAXBUtil.unmarshal((String) property.getPropertyValue());
                    resourceType.setConfiguration(configuration);
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }
            } else if ("synchronization".equals(property.getPropertyName())) {
                try {
                    JAXBElement<SynchronizationType> jaxbSynchronization = (JAXBElement<SynchronizationType>) JAXBUtil.unmarshal((String) property.getPropertyValue());
                    if (null != jaxbSynchronization) {
                        resourceType.setSynchronization(jaxbSynchronization.getValue());
                    } else {
                        resourceType.setSynchronization(null);
                    }
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }
            } else if ("scripts".equals(property.getPropertyName())) {
                try {
                    JAXBElement<ScriptsType> jaxbScripts = (JAXBElement<ScriptsType>) JAXBUtil.unmarshal((String) property.getPropertyValue());
                    if (null != jaxbScripts) {
                        resourceType.setScripts(jaxbScripts.getValue());
                    } else {
                        resourceType.setScripts(null);
                    }
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }
            } else if ("resourceAccessConfigurationRef".equals(property.getPropertyName())) {
                if (true) {
                    continue;
                }
                try {
                    JAXBElement<ResourceAccessConfigurationReferenceType> racReference = (JAXBElement<ResourceAccessConfigurationReferenceType>) JAXBUtil.unmarshal((String) property.getPropertyValue());
                    Object obj = racReference.getValue();
                    if (obj instanceof ResourceAccessConfigurationReferenceType) {
                        resourceType.setResourceAccessConfigurationRef((ResourceAccessConfigurationReferenceType) racReference.getValue());
                    }
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }
            } else {
                //otherwise copy the remaining properties to extension
                addExtensionProperty(resourceType, property);
            }

        }
    }

    private void copyResourceAccessConfigurationToResourceAccessConfiguration(ResourceAccessConfiguration resource, ResourceAccessConfigurationType resourceType) throws FaultMessage {
        Utils.copyPropertiesSilent(resourceType, resource);
        for (Property property : resource.getProperties()) {
            logger.trace("(String) property.getPropertyValue() = {}", (String) property.getPropertyValue());
            if ("schema".equals(property.getPropertyName())) {
                Document doc = DOMUtil.parseDocument((String) property.getPropertyValue());
                ResourceAccessConfigurationType.Schema schema = new ResourceAccessConfigurationType.Schema();
                List<Element> any = new ArrayList();
                any.add(doc.getDocumentElement());
                schema.getAny().addAll(any);
                resourceType.setSchema(schema);
            } else if ("schemaHandling".equals(property.getPropertyName())) {
                try {
                    SchemaHandlingType rsht = null;
                    Object o = JAXBUtil.unmarshal((String) property.getPropertyValue());
                    if (o instanceof SchemaHandlingType) {
                        rsht = (SchemaHandlingType) o;
                    } else if (o instanceof JAXBElement) {
                        rsht = (SchemaHandlingType) ((JAXBElement) o).getValue();
                    } else {
                        throw new AssertionError("Unexpected type " + o.getClass());
                    }

                    resourceType.setSchemaHandling(rsht);
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }
            } else if ("configuration".equals(property.getPropertyName())) {
                try {
                    Configuration configuration = (Configuration) JAXBUtil.unmarshal((String) property.getPropertyValue());
                    resourceType.setConfiguration(configuration);
                } catch (JAXBException ex) {
                    logger.error("Failed to unmarshal", ex);
                    throw new FaultMessage("Failed to unmarshal", new IllegalArgumentFaultType(), ex);
                }
            } else if ("namespace".equals(property.getPropertyName())) {
                resourceType.setNamespace((String) property.getPropertyValue());
            }
        }
    }

    private void copyResourceAccessConfigurationTypeToResourceAccessConfiguration(ResourceAccessConfigurationType resourceType, ResourceAccessConfiguration resource) throws FaultMessage {
        String oid = resourceType.getOid();
        resourceType.setOid(null);
        Utils.copyPropertiesSilent(resource, resourceType);
        if (null != oid) {
            resource.setOid(UUID.fromString(oid));
        }
        resourceType.setOid(oid);
        List<Property> properties = new LinkedList<Property>();
        resource.setProperties(properties);

        //namespace property
        Property namespaceProperty = new StringProperty();
        namespaceProperty.setPropertyName("namespace");
        namespaceProperty.setPropertyValue(resourceType.getNamespace());
        //properties.add(namespaceProperty);
        addOrReplaceProperty(properties, namespaceProperty);

        //extension processing begin
        if (null != resourceType.getExtension()) {
            List<Element> extension = resourceType.getExtension().getAny();
            copyExtensionToProperties(extension, properties);
        }
        //extension processing end

        //schema processing begin
        ResourceAccessConfigurationType.Schema schema = resourceType.getSchema();

        // Schema is optional. It is almost always there, but in rare cases
        // it may not be there.
        if (schema != null && !schema.getAny().isEmpty()) {

            Document doc = DOMUtil.getDocument();
            for (Object object : schema.getAny()) {
                Element element = (Element) object;
                Node node = doc.importNode(element, true);
                doc.appendChild(node);
            }

            //xml document serialization end
            Property schemaProperty = new StringProperty();
            schemaProperty.setPropertyName("schema");
            schemaProperty.setPropertyValue(DOMUtil.serializeDOMToString(doc));
            //properties.add(schemaProperty);
            addOrReplaceProperty(properties, schemaProperty);
        }
        //schema processing end

        //schema handling begin
        try {
            SchemaHandlingType rsht = resourceType.getSchemaHandling();
            Property schemaHandlingProperty = new StringProperty();
            schemaHandlingProperty.setPropertyName("schemaHandling");
            schemaHandlingProperty.setPropertyValue(JAXBUtil.marshal(rsht));
            //properties.add(schemaHandlingProperty);
            addOrReplaceProperty(properties, schemaHandlingProperty);
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //schema handling end

        //configuration begin
        try {
            Configuration configuration = resourceType.getConfiguration();
            Property configurationProperty = new StringProperty();
            configurationProperty.setPropertyName("configuration");
            configurationProperty.setPropertyValue(JAXBUtil.marshal(configuration));
            //properties.add(scriptsProperty);
            addOrReplaceProperty(properties, configurationProperty);
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //configuration end

        logger.trace("properties.size() = " + properties.size());
        for (Property property : properties) {
            logger.trace("Property name {}, value {}", property.getPropertyName(), property.getPropertyValue());
        }

    }

    private void copyResourceTypeToResource(ResourceType resourceType, Resource resource) throws FaultMessage {
        String oid = resourceType.getOid();
        resourceType.setOid(null);
        Utils.copyPropertiesSilent(resource, resourceType);
        if (null != oid) {
            resource.setOid(UUID.fromString(oid));
        }
        resourceType.setOid(oid);
        List<Property> properties;
        if (null == resource.getProperties()) {
            properties = new LinkedList<Property>();
            resource.setProperties(properties);
        } else {
            properties = resource.getProperties();
        }

        //extension processing begin
        if (null != resourceType.getExtension()) {
            List<Element> extension = resourceType.getExtension().getAny();
            copyExtensionToProperties(extension, properties);
        }
        //extension processing end

        //resourceAccessConfigurationRef begin
        if (false && (null != resourceType.getResourceAccessConfigurationRef())) {
            try {
                JAXBElement<ResourceAccessConfigurationReferenceType> racReference = new JAXBElement(SchemaConstants.C_RAC_REF, ResourceAccessConfigurationReferenceType.class, null, resourceType.getResourceAccessConfigurationRef());
                Property resourceAccessConfigurationRef = new StringProperty();
                resourceAccessConfigurationRef.setPropertyName("resourceAccessConfigurationRef");
                resourceAccessConfigurationRef.setPropertyValue(JAXBUtil.marshal(racReference));
                //properties.add(resourceAccessConfigurationRef);
                addOrReplaceProperty(properties, resourceAccessConfigurationRef);
            } catch (JAXBException ex) {
                logger.error("Failed to marshal", ex);
                throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
            }
        } else {
            //TODO: Invalid resource, can not be saved
        }
        //resourceAccessConfigurationRef end


        //schema processing begin
        XmlSchemaType schema = resourceType.getSchema();

        // Schema is optional. It is almost always there, but in rare cases
        // it may not be there.
        if (schema != null && !schema.getAny().isEmpty()) {

            Document doc = DOMUtil.getDocument();
            for (Object object : schema.getAny()) {
                Element element = (Element) object;
                Node node = doc.importNode(element, true);
                doc.appendChild(node);
            }

            //xml document serialization end
            Property schemaProperty = new StringProperty();
            schemaProperty.setPropertyName("schema");
            schemaProperty.setPropertyValue(DOMUtil.serializeDOMToString(doc));
            addOrReplaceProperty(properties, schemaProperty);
            //properties.add(schemaProperty);
        } else {
            Property schemaProperty = new StringProperty();
            schemaProperty.setPropertyName("schema");
            schemaProperty.setPropertyValue(null);
            addOrReplaceProperty(properties, schemaProperty);
        }
        //schema processing end

        //schema handling begin
        try {
            SchemaHandlingType rsht = resourceType.getSchemaHandling();
            Property schemaHandlingProperty = new StringProperty();
            schemaHandlingProperty.setPropertyName("schemaHandling");
            schemaHandlingProperty.setPropertyValue(JAXBUtil.marshal(objectFactory.createSchemaHandling(rsht)));
            addOrReplaceProperty(properties, schemaHandlingProperty);
            //properties.add(schemaHandlingProperty);
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //schema handling end

        //configuration begin
        try {
            Configuration configuration = resourceType.getConfiguration();
            Property configurationProperty = new StringProperty();
            configurationProperty.setPropertyName("configuration");
            configurationProperty.setPropertyValue(JAXBUtil.marshal(configuration));
            //properties.add(scriptsProperty);
            addOrReplaceProperty(properties, configurationProperty);
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //configuration end

        //synchronization begin
        try {
            SynchronizationType synchronization = resourceType.getSynchronization();
            if (null != synchronization) {
                Property synchronizationProperty = new StringProperty();
                synchronizationProperty.setPropertyName("synchronization");
                synchronizationProperty.setPropertyValue(JAXBUtil.marshalWrap(synchronization, SchemaConstants.I_SYNCHRONIZATION));
                addOrReplaceProperty(properties, synchronizationProperty);
            } else {
                Property synchronizationProperty = new StringProperty();
                synchronizationProperty.setPropertyName("synchronization");
                synchronizationProperty.setPropertyValue(null);
                addOrReplaceProperty(properties, synchronizationProperty);
            }
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //synchronization end

        //scripts begin
        try {
            ScriptsType scripts = resourceType.getScripts();
            if (null != scripts) {
                Property scriptsProperty = new StringProperty();
                scriptsProperty.setPropertyName("scripts");
                scriptsProperty.setPropertyValue(JAXBUtil.marshalWrap(scripts, SchemaConstants.I_SCRIPTS));
                addOrReplaceProperty(properties, scriptsProperty);
            } else {
                Property scriptsProperty = new StringProperty();
                scriptsProperty.setPropertyName("scripts");
                scriptsProperty.setPropertyValue(null);
                addOrReplaceProperty(properties, scriptsProperty);
            }
        } catch (JAXBException ex) {
            logger.error("Failed to marshal", ex);
            throw new FaultMessage("Failed to marshal", new IllegalArgumentFaultType(), ex);
        }
        //scripts end

        logger.trace("properties.size() = " + properties.size());
        for (Property property : properties) {
            logger.trace("Property name {}, value {}", property.getPropertyName(), property.getPropertyValue());
        }

    }

    private void copyResourceStateTypeToResourceState(ResourceStateType resourceStateType, ResourceState resourceState) {
        ResourceStateType.SynchronizationState state = resourceStateType.getSynchronizationState();
        resourceStateType.setSynchronizationState(null);
        String oid = resourceStateType.getOid();
        resourceStateType.setOid(null);
        ObjectReferenceType resourceRef = resourceStateType.getResourceRef();

        Validate.notNull(resourceRef, "ResourceStateType object with oid = " + oid + " does not contain resource reference");

        Utils.copyPropertiesSilent(resourceState, resourceStateType);
        if (null != oid) {
            resourceState.setOid(UUID.fromString(oid));
        }

        resourceStateType.setSynchronizationState(state);
        resourceStateType.setOid(oid);
        resourceStateType.setResourceRef(resourceRef);

        //Prerequisite: list contains only one element - an xml fragment
        String serializedState = DOMUtil.serializeDOMToString((Node) state.getAny().get(0));

        resourceState.setState(serializedState);

        Resource resource = (Resource) genericDao.findById(UUID.fromString(resourceRef.getOid()));

        Validate.notNull(resource, "ResourceStateType object instance with oid = " + oid + " references not existing resource wit oid = " + resourceRef.getOid());

        resourceState.setResource(resource);

    }

    private void copyResourceStateToResourceStateType(ResourceState resourceState, ResourceStateType resourceStateType) {
        //1. preparing object, that cannot be copied by shallow copy method
        Resource resource = resourceState.getResource();
        String state = resourceState.getState();

        //2. setting the properties to null (not to copy them by shallow method)
        resourceState.setResource(null);
        resourceState.setState(null);

        //3. making shallow copy
        Utils.copyPropertiesSilent(resourceStateType, resourceState);

        //4. setting back
        resourceState.setResource(resource);
        resourceState.setState(state);

        //5. making copy of the remaining objects, that were not copied by shallow copy
        //5.a - resource copy - only resource ref will be created
        ObjectReferenceType objectReferenceType = new ObjectReferenceType();
        objectReferenceType.setOid(String.valueOf(resource.getOid()));
        resourceStateType.setResourceRef(objectReferenceType);

        //5.b - state copy
        ResourceStateType.SynchronizationState deserializedState = new ResourceStateType.SynchronizationState();
        deserializedState.getAny().add(DOMUtil.parseDocument(state).getDocumentElement());

        resourceStateType.setSynchronizationState(deserializedState);

    }

    private void copyUserTemplateTypeToUserTemplate(UserTemplateType userTemplateType, UserTemplate userTemplate) {
        userTemplate.setOid(UUID.fromString(userTemplateType.getOid()));
        userTemplate.setName(userTemplateType.getName());

        String serializedUserTemplateType = JAXBUtil.silentMarshalWrap(userTemplateType, SchemaConstants.I_USER_TEMPLATE);
        
        userTemplate.setTemplate(serializedUserTemplateType);
    }

    private void copyUserTemplateToUserTemplateType(UserTemplate userTemplate, UserTemplateType userTemplateType) {
        try {
            UserTemplateType localUserTemplateType = ((JAXBElement<UserTemplateType>) JAXBUtil.unmarshal(userTemplate.getTemplate())).getValue();
            //Note: copy from deserialized object to method parameter object is required
            userTemplateType.setName(localUserTemplateType.getName());
            userTemplateType.setExtension(localUserTemplateType.getExtension());
            userTemplateType.setOid(localUserTemplateType.getOid());
            userTemplateType.setVersion(localUserTemplateType.getVersion());
            userTemplateType.getAccountConstruction().addAll(localUserTemplateType.getAccountConstruction());
            userTemplateType.getPropertyConstruction().addAll(localUserTemplateType.getPropertyConstruction());
        } catch (JAXBException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void addOrReplaceProperty(List<Property> properties, Property newProperty) {
        for (Property existingProperty : properties) {
            if (StringUtils.equals(existingProperty.getPropertyName(), newProperty.getPropertyName())) {
                existingProperty.setPropertyValue(newProperty.getPropertyValue());
                return;
            }
        }

        //property does not exists
        properties.add(newProperty);

    }

    public GenericDao getGenericDao() {
        return genericDao;
    }

    public void setGenericDao(GenericDao genericDao) {
        this.genericDao = genericDao;
    }

    private class RepositoryPatchingListener implements PatchingListener {

        private User user;

        public RepositoryPatchingListener(User user) {
            this.user = user;
        }

        @Override
        public boolean isApplicable(PropertyModificationType change) {
            //Model sends add account as add of accountRef
            Node node = change.getValue().getAny().get(0);
            String newValue = DOMUtil.serializeDOMToString(node);
            if (StringUtils.contains(newValue, "accountRef")) {
                if (PropertyModificationTypeType.add.equals(change.getModificationType())) {
                    Node oidNode = node.getAttributes().getNamedItem(MidPointConstants.ATTR_OID_NAME);
                    //TODO: check if oid is really there, from GUI there could be also accountsRef without oid
                    if (null == oidNode) {
                        return false;
                    }
                    String oid = oidNode.getNodeValue();
                    Account account = (Account) genericDao.findById(UUID.fromString(oid));
                    if (account == null) {
                        // Attepmt for a quick fix during a stressful period
                        // TODO: review
                        return false;
                    }
                    account.setUser(user);
                    //merge is not required
                    //genericDao.merge(account);

                    //account ref won't be added to patched xml
                    return false;
                }
                if (PropertyModificationTypeType.delete.equals(change.getModificationType())) {
                    Node oidNode = node.getAttributes().getNamedItem(MidPointConstants.ATTR_OID_NAME);
                    //TODO: check if oid is really there, from GUI there could be also accountsRef without oid
                    if (null == oidNode) {
                        return false;
                    }
                    String oid = oidNode.getNodeValue();
                    Account account = (Account) genericDao.findById(UUID.fromString(oid));
                    if (account == null) {
                        //account ref will be removed from xml, even when account does not exist
                        return true;
                    }
                    account.setUser(null);
                    //merge is not required
                    //genericDao.merge(account);
                    

                    //account ref will be removed from xml
                    return true;
                }
            }

            return true;
        }

        @Override
        public void applied(PropertyModificationType change) {
            //no action required
        }
    }

    private void modifyUser(ObjectModificationType objectChange) throws FaultMessage {
        try {
            logger.trace("[Repository] modifyUser begin");
            Validate.notNull(objectChange);
            SimpleDomainObject user = genericDao.findById(UUID.fromString(objectChange.getOid()));
            UserType userType = new UserType();
            copyUserToUserType((User) user, userType);
            PatchXml xmlPatchTool = new PatchXml();
            PatchingListener listener = new RepositoryPatchingListener((User) user);
            xmlPatchTool.setPatchingListener(listener);
            String xmlObject = xmlPatchTool.applyDifferences(objectChange, userType);
            JAXBElement<UserType> jaxb = (JAXBElement<UserType>) JAXBUtil.unmarshal(xmlObject);
            copyUserTypeToUser(jaxb.getValue(), (User) user);
            //not needed
            //genericDao.merge(user);
            logger.trace("[Repository] modifyUser end");
        } catch (PatchException ex) {
            logger.error("Failed to patch user", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        } catch (JAXBException ex) {
            logger.error("Failed to marshall user", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        }
    }

    private void modifyAccount(ObjectModificationType objectChange) throws FaultMessage {
        try {
            logger.trace("[Repository] modifyAccount begin");
            Validate.notNull(objectChange);
            SimpleDomainObject account = genericDao.findById(UUID.fromString(objectChange.getOid()));
            AccountShadowType accountShadowType = new AccountShadowType();
            copyAccountToAccountShadowType((Account) account, accountShadowType);
            PatchXml xmlPatchTool = new PatchXml();
            String xmlObject = xmlPatchTool.applyDifferences(objectChange, accountShadowType);
            JAXBElement<AccountShadowType> jaxb = (JAXBElement<AccountShadowType>) JAXBUtil.unmarshal(xmlObject);
            copyAccountShadowTypeToAccount(jaxb.getValue(), (Account) account);
            //not needed
            //genericDao.merge(account);
            logger.trace("[Repository] modifyAccount end");
        } catch (PatchException ex) {
            logger.error("Failed to patch account", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        } catch (JAXBException ex) {
            logger.error("Failed to marshall account", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        }

    }

    private void modifyResource(ObjectModificationType objectChange) throws FaultMessage {
        try {
            logger.trace("[Repository] modifyResource begin");
            Validate.notNull(objectChange);
            SimpleDomainObject resource = genericDao.findById(UUID.fromString(objectChange.getOid()));
            ResourceType resourceType = new ResourceType();
            copyResourceToResourceType((Resource) resource, resourceType);
            PatchXml xmlPatchTool = new PatchXml();
            String xmlObject = xmlPatchTool.applyDifferences(objectChange, resourceType);
            JAXBElement<ResourceType> jaxb = (JAXBElement<ResourceType>) JAXBUtil.unmarshal(xmlObject);
            copyResourceTypeToResource(jaxb.getValue(), (Resource) resource);
            //not needed
            //genericDao.merge(resource);
            logger.trace("[Repository] modifyResource end");
        } catch (PatchException ex) {
            logger.error("Failed to patch resource", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        } catch (JAXBException ex) {
            logger.error("Failed to marshall resource", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        }

    }

    private void modifyResourceState(ObjectModificationType objectChange) throws FaultMessage {
        try {
            logger.trace("[Repository] modifyResourceState begin");
            Validate.notNull(objectChange);
            SimpleDomainObject resourceState = genericDao.findById(UUID.fromString(objectChange.getOid()));
            ResourceStateType resourceStateType = new ResourceStateType();
            copyResourceStateToResourceStateType((ResourceState) resourceState, resourceStateType);
            PatchXml xmlPatchTool = new PatchXml();
            String xmlObject = xmlPatchTool.applyDifferences(objectChange, resourceStateType);
            JAXBElement<ResourceStateType> jaxb = (JAXBElement<ResourceStateType>) JAXBUtil.unmarshal(xmlObject);
            copyResourceStateTypeToResourceState(jaxb.getValue(), (ResourceState) resourceState);
            //not needed
            //genericDao.merge(resourceState);
            logger.trace("[Repository] modifyResourceState end");
        } catch (PatchException ex) {
            logger.error("Failed to patch resourceState", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        } catch (JAXBException ex) {
            logger.error("Failed to marshall resourceState", ex);
            throw new FaultMessage(ex.getMessage(), new ReferentialIntegrityFaultType());
        }
    }

    @Override
    public String addObject(ObjectContainerType objectContainer) throws FaultMessage {

        logger.info("### REPOSITORY # Enter addObject({})", DebugUtil.prettyPrint(objectContainer));
        Validate.notNull(objectContainer);

        if (objectContainer.getObject().getName() == null || objectContainer.getObject().getName().isEmpty()) {
            logger.error("### REPOSITORY # Fault addObject(..): Attempt to add object without name (OID:{})", objectContainer.getObject().getOid());
            IllegalArgumentFaultType ft = new IllegalArgumentFaultType();
            ft.setMessage("Attempt to add object without name");
            throw new FaultMessage("Attempt to add object without name", ft);
        }

        if (objectContainer.getObject() instanceof UserType) {
            logger.trace("[Repository] addUser begin");
            UserType userType = (UserType) objectContainer.getObject();
            User user = new User();
            copyUserTypeToUser(userType, user);
            storeOrImport(user);
            for (ObjectReferenceType ort : userType.getAccountRef()) {
                Account acc = (Account) genericDao.findById(UUID.fromString(ort.getOid()));
                if (null == acc) {
                    throw new FaultMessage("Account with oid = " + ort.getOid() + " referenced in accountRef does not exist.", new ObjectNotFoundFaultType());
                }
                acc.setUser(user);
                //merge is not required
                //genericDao.merge(acc);
            }
            userType.setOid(user.getOid().toString());
            logger.trace("[Repository] addUser end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", user.getOid().toString());

            return user.getOid().toString();

        }

        if (objectContainer.getObject() instanceof AccountShadowType) {
            logger.trace("[Repository] addAccount begin");
            AccountShadowType accountShadowType = (AccountShadowType) objectContainer.getObject();
            Account account = new Account();
            copyAccountShadowTypeToAccount(accountShadowType, account);
            storeOrImport(account);
            accountShadowType.setOid(account.getOid().toString());
            logger.trace("[Repository] addAccount end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", account.getOid().toString());

            return account.getOid().toString();

        }

        if (objectContainer.getObject() instanceof ResourceObjectShadowType) {
            logger.trace("[Repository] addResourceObjectShadow begin");
            ResourceObjectShadowType accountShadowType = (ResourceObjectShadowType) objectContainer.getObject();
            ResourceObjectShadow account = new ResourceObjectShadow();
            copyResourceObjectShadowTypeToResourceObjectShadow(accountShadowType, account);
            storeOrImport(account);
            accountShadowType.setOid(account.getOid().toString());
            logger.trace("[Repository] addResourceObjectShadow end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", account.getOid().toString());

            return account.getOid().toString();

        }

        if (objectContainer.getObject() instanceof ResourceType) {
            logger.trace("[Repository] addResource begin");
            ResourceType resourceType = (ResourceType) objectContainer.getObject();
            Resource resource = new Resource();
            copyResourceTypeToResource(resourceType, resource);
//            logger.trace("properties.size() = " + resource.getProperties().size());
//            for (Property property : resource.getProperties()) {
//                logger.trace("Property name {}, value {}", property.getPropertyName(), property.getPropertyValue());
//            }
//
            storeOrImport(resource);
            resourceType.setOid(resource.getOid().toString());
            logger.trace("[Repository] addResource end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", resource.getOid().toString());

            return resource.getOid().toString();
        }
        if (objectContainer.getObject() instanceof ResourceAccessConfigurationType) {
            logger.trace("[Repository] addResourceAccessConfiguration begin");
            ResourceAccessConfigurationType resourceAccessConfigurationType = (ResourceAccessConfigurationType) objectContainer.getObject();
            ResourceAccessConfiguration resource = new ResourceAccessConfiguration();
            copyResourceAccessConfigurationTypeToResourceAccessConfiguration(resourceAccessConfigurationType, resource);
//            logger.trace("properties.size() = " + resource.getProperties().size());
//            for (Property property : resource.getProperties()) {
//                logger.trace("Property name {}, value {}", property.getPropertyName(), property.getPropertyValue());
//            }
//
            storeOrImport(resource);
            resourceAccessConfigurationType.setOid(resource.getOid().toString());
            logger.trace("[Repository] addResourceAccessConfiguration end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", resource.getOid().toString());

            return resource.getOid().toString();
        }

        if (objectContainer.getObject() instanceof ResourceStateType) {
            logger.trace("[Repository] addResourceState begin");
            ResourceStateType resourceStateType = (ResourceStateType) objectContainer.getObject();
            ResourceState resourceState = new ResourceState();
            copyResourceStateTypeToResourceState(resourceStateType, resourceState);
            storeOrImport(resourceState);
            resourceStateType.setOid(resourceState.getOid().toString());
            logger.trace("[Repository] addResourceState end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", resourceState.getOid().toString());

            return resourceState.getOid().toString();

        }

        if (objectContainer.getObject() instanceof UserTemplateType) {
            logger.trace("[Repository] addUserTemplate begin");
            UserTemplateType userTemplateType = (UserTemplateType) objectContainer.getObject();
            UserTemplate userTemplate = new UserTemplate();
            copyUserTemplateTypeToUserTemplate(userTemplateType, userTemplate);
            storeOrImport(userTemplate);
            userTemplateType.setOid(userTemplate.getOid().toString());
            logger.trace("[Repository] addUserTemplate end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", userTemplate.getOid().toString());

            return userTemplate.getOid().toString();

        }

        if (objectContainer.getObject() instanceof GenericObjectType) {
            logger.trace("[Repository] addGenericObject begin");
            GenericObjectType genericObjectType = (GenericObjectType) objectContainer.getObject();
            GenericEntityBase genericEntity = new GenericEntityBase();
            copyGenericObjectTypeToGenericEntity(genericObjectType, genericEntity);
            storeOrImport(genericEntity);
            genericObjectType.setOid(genericEntity.getOid().toString());
            logger.trace("[Repository] addGenericObject end");

            logger.info("### REPOSITORY # Exit addObject(..) : {}", genericEntity.getOid().toString());

            return genericEntity.getOid().toString();

        }

        throw new FaultMessage("Object of type " + objectContainer.getObject() + " is not supported", new UnsupportedObjectTypeFaultType());

    }

    @Override
    public ObjectContainerType getObject(String oid, PropertyReferenceListType resolve) throws FaultMessage {

        logger.info("### REPOSITORY # Enter getObject({})", oid);

        logger.trace("[Repository] getObject begin");

        Validate.notNull(oid);

        ObjectContainerType objectContainerType = new ObjectContainerType();
        SimpleDomainObject object = genericDao.findById(UUID.fromString(oid));
        if (null == object) {
            throw new FaultMessage("Object with oid = " + oid + " not found", new ObjectNotFoundFaultType());
        }
        if (object instanceof User) {
            User user = (User) object;
            UserType userType = new UserType();
            copyUserToUserType(user, userType);
            objectContainerType.setObject(userType);
            logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            return objectContainerType;
        }

        if (object instanceof Account) {
            Account account = (Account) object;
            AccountShadowType accountShadowType = new AccountShadowType();
            copyAccountToAccountShadowType(account, accountShadowType);
            objectContainerType.setObject(accountShadowType);
            logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            return objectContainerType;
        }

        if (object instanceof ResourceObjectShadow) {
            ResourceObjectShadow account = (ResourceObjectShadow) object;
            ResourceObjectShadowType accountShadowType = new ResourceObjectShadowType();
            copyResourceObjectShadowToResourceObjectShadowType(account, accountShadowType);
            objectContainerType.setObject(accountShadowType);
            if (logger.isInfoEnabled()) {
                logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            }
            return objectContainerType;
        }

        if (object instanceof Resource) {
            Resource resource = (Resource) object;
            ResourceType resourceType = new ResourceType();
            copyResourceToResourceType(resource, resourceType);
            objectContainerType.setObject(resourceType);
            logger.trace("Returning object: {}", JAXBUtil.silentMarshalWrap(resourceType, SchemaConstants.I_RESOURCE_TYPE));
            logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            return objectContainerType;
        }

        if (object instanceof ResourceAccessConfiguration) {
            ResourceAccessConfiguration resource = (ResourceAccessConfiguration) object;
            ResourceAccessConfigurationType resourceType = new ResourceAccessConfigurationType();
            copyResourceAccessConfigurationToResourceAccessConfiguration(resource, resourceType);
            objectContainerType.setObject(resourceType);
            logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            return objectContainerType;
        }

        if (object instanceof ResourceState) {
            ResourceState resourceState = (ResourceState) object;
            ResourceStateType resourceStateType = new ResourceStateType();
            copyResourceStateToResourceStateType(resourceState, resourceStateType);
            objectContainerType.setObject(resourceStateType);
            logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            return objectContainerType;

        }

        if (object instanceof UserTemplate) {
            UserTemplate userTemplate = (UserTemplate) object;
            UserTemplateType userTemplateType = new UserTemplateType();
            copyUserTemplateToUserTemplateType(userTemplate, userTemplateType);
            objectContainerType.setObject(userTemplateType);
            logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            return objectContainerType;
        }

        if (object instanceof GenericEntityBase) {
            GenericEntityBase genericEntity = (GenericEntityBase) object;
            GenericObjectType genericObjectType = new GenericObjectType();
            copyGenericEntityToGenericOjectType(genericEntity, genericObjectType);
            objectContainerType.setObject(genericObjectType);
            logger.info("### REPOSITORY # Exit getObject(..): {}", DebugUtil.prettyPrint(objectContainerType));
            return objectContainerType;
        }

        logger.error("### REPOSITORY # Fault getObject(..): Object with oid {} is not supported", oid);
        throw new FaultMessage("Object with oid " + oid + " is not supported", new UnsupportedObjectTypeFaultType());
    }

    private <T, U extends ObjectType> ObjectListType convertListOfObjects(List<T> objects, Class clazz) throws FaultMessage {
        logger.trace("[Repository] convertListOfObjects begin");
        ObjectListType olt = new ObjectListType();

        for (T object : objects) {
            ObjectType objectType = null;
            try {
                objectType = (ObjectType) clazz.newInstance();
            } catch (InstantiationException ex) {
                logger.warn("Failed to copy properties for instances {}, {}. Error message was {}", new Object[]{object, objectType, ex.getMessage()});

            } catch (IllegalAccessException ex) {
                logger.warn("Failed to copy properties for instances {}, {}. Error message was {}", new Object[]{object, objectType, ex.getMessage()});
            }

            if (clazz.getSimpleName().equals("UserType")) {
                copyUserToUserType((User) object, (UserType) objectType);
            }
            if (clazz.getSimpleName().equals("AccountShadowType")) {
                copyAccountToAccountShadowType((Account) object, (AccountShadowType) objectType);
            }
            if (clazz.getSimpleName().equals("ResourceType")) {
                copyResourceToResourceType((Resource) object, (ResourceType) objectType);
            }
            if (clazz.getSimpleName().equals("ResourceStateType")) {
                copyResourceStateToResourceStateType((ResourceState) object, (ResourceStateType) objectType);
            }
            if (clazz.getSimpleName().equals("UserTemplateType")) {
                copyUserTemplateToUserTemplateType((UserTemplate) object, (UserTemplateType) objectType);
            }
            if (clazz.getSimpleName().equals("GenericObjectType")) {
                copyGenericEntityToGenericOjectType((GenericEntityBase) object, (GenericObjectType) objectType);
            }

            olt.getObject().add(objectType);
        }
        logger.trace("[Repository] convertListOfObjects end");
        return olt;

    }

    @Override
    public ObjectListType listObjects(String objectType, PagingType paging) throws FaultMessage {

        logger.info("### REPOSITORY # Enter listObjects({})", objectType);
        try {
            if (Utils.getObjectType("UserType").equals(objectType)) {
//                List<SimpleDomainObject> users = genericDao.findAllOfType("User");
                PagingRepositoryType pagingType = new PagingRepositoryType();
                pagingType.fillPagingProperties(paging);
                List<SimpleDomainObject> users = genericDao.findRangeUsers("User", pagingType);
                ObjectListType listOfObjects = convertListOfObjects(users, UserType.class);
                for (ObjectType objType : listOfObjects.getObject()) {
                    logger.debug(objType.getName());
                }
                logger.info("### REPOSITORY # Exit listObjects(..) : {}", DebugUtil.prettyPrint(listOfObjects));
                return listOfObjects;
            }

            if (Utils.getObjectType("AccountType").equals(objectType)) {
                List<SimpleDomainObject> accounts = genericDao.findAllOfType("Account");
                ObjectListType listOfObjects = convertListOfObjects(accounts, AccountShadowType.class);
                logger.info("### REPOSITORY # Exit listObjects(..) : {}", DebugUtil.prettyPrint(listOfObjects));
                return listOfObjects;
            }

            if (Utils.getObjectType("ResourceType").equals(objectType)) {
                List<SimpleDomainObject> resources = genericDao.findAllOfType("Resource");
                ObjectListType listOfObjects = convertListOfObjects(resources, ResourceType.class);
                logger.info("### REPOSITORY # Exit listObjects(..) : {}", DebugUtil.prettyPrint(listOfObjects));
                return listOfObjects;
            }

            if (Utils.getObjectType("ResourceStateType").equals(objectType)) {
                List<SimpleDomainObject> resourceStates = genericDao.findAllOfType("ResourceState");
                ObjectListType listOfObjects = convertListOfObjects(resourceStates, ResourceStateType.class);
                logger.info("### REPOSITORY # Exit listObjects(..) : {}", DebugUtil.prettyPrint(listOfObjects));
                return listOfObjects;
            }

            if (Utils.getObjectType("UserTemplateType").equals(objectType)) {
                List<SimpleDomainObject> userTemplates = genericDao.findAllOfType("UserTemplate");
                ObjectListType listOfObjects = convertListOfObjects(userTemplates, UserTemplateType.class);
                logger.info("### REPOSITORY # Exit listObjects(..) : {}", DebugUtil.prettyPrint(listOfObjects));
                return listOfObjects;
            }
            if (Utils.getObjectType("GenericObjectType").equals(objectType)) {
                List<SimpleDomainObject> genericEntities = genericDao.findAllOfType("GenericEntityBase");
                ObjectListType listOfObjects = convertListOfObjects(genericEntities, GenericObjectType.class);
                logger.info("### REPOSITORY # Exit listObjects(..) : {}", DebugUtil.prettyPrint(listOfObjects));
                return listOfObjects;
            }

        } catch (IllegalArgumentException ex) {
            throw new FaultMessage("Unsupported object type", new IllegalArgumentFaultType(), ex);
        }
        logger.error("### REPOSITORY # Fault getObject(..): Unsupported object type");
        throw new FaultMessage("Unsupported object type", new IllegalArgumentFaultType());
    }

    /**
     * Convert filter type to SQL query.
     *
     * Note I made it public because I would like to use it in other tests. (elek 101222)
     *
     * @param filter
     * @return
     */
    public static String convertFilterToQuery(Element filter) {

        //TODO: following functionality supports only minimal set of required features
        //TODO: elements namespaces are not checked
        NodeList children = filter.getChildNodes();
        String path = null;
        String criteriaName = null;
        String criteriaValue = null;
//        List<String> criteriaNames = new ArrayList<String>();
//        List<String> criteriaValues = new ArrayList<String>();
        String objectType = null;
        String resourceRef = null;
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                // Skipping all non-element nodes
                continue;
            }
            String nodeLocalName = child.getLocalName();
            if ("type".equals(nodeLocalName)) {
                objectType = child.getAttributes().getNamedItem("uri").getTextContent();
            } else if ("equal".equals(nodeLocalName)) {
                if ((StringUtils.isNotEmpty(criteriaName) && StringUtils.isNotEmpty(resourceRef) && QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE).equals(objectType)) ||
                        ((StringUtils.isNotEmpty(criteriaName) && ((QNameUtil.qNameToUri(SchemaConstants.I_RESOURCE_STATE_TYPE).equals(objectType)) || (QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE).equals(objectType))))) ||
                        (!QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE).equals(objectType) && QNameUtil.qNameToUri(SchemaConstants.I_RESOURCE_STATE_TYPE).equals(objectType) && QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE).equals(objectType))) {
                    throw new IllegalArgumentException("Provided unsupported filter in search query: " + DOMUtil.serializeDOMToString(filter));
                }
                Node criteria = DOMUtil.getFirstChildElement(child);
                if ("path".equals(criteria.getLocalName())) {
                    //TODO: use XpathType here
                    if (StringUtils.contains(criteria.getTextContent(), ":")) {
                        path = StringUtils.substringAfter(criteria.getTextContent(), ":");
                    } else {
                        path = criteria.getTextContent();
                    }
                    criteria = DOMUtil.getNextSiblingElement(criteria);
                    if (null == criteria) {
                        throw new IllegalArgumentException("Query filter does not contain any values to search by");
                    }
                    Node firstChild = DOMUtil.getFirstChildElement(criteria);
                    if (null == firstChild) {
                        throw new IllegalArgumentException("Query filter contains empty list of values to search by");
                    }
                    criteriaName = firstChild.getNamespaceURI() + ":" + firstChild.getLocalName();
                    criteriaValue = criteria.getTextContent();
                } else {
                    criteria = DOMUtil.getFirstChildElement(DOMUtil.getFirstChildElement(child));
                    if ("resourceRef".equals(criteria.getLocalName())) {
                        //criteriaName = "resource_uuid";
                        resourceRef = criteria.getAttributes().getNamedItem("oid").getNodeValue();
                    } else {
                        criteriaName = criteria.getLocalName();
                        criteriaValue = criteria.getTextContent();
                    }
                }
            } else {
                throw new IllegalArgumentException("Unsupported element " + nodeLocalName + " in filter");
            }
        }

        //check extractions
        if (StringUtils.isEmpty(objectType)) {
            throw new IllegalArgumentException("Element type is required in filter");
        }

        if (criteriaValue != null) {
            criteriaValue = criteriaValue.trim();
        }

        String dbObject = StringUtils.substringBefore(StringUtils.substringAfter(objectType, "#"), "Type");
        String query;
        if (StringUtils.isEmpty(path)) {
            if (StringUtils.isNotEmpty(resourceRef)) {
                query = "from " + dbObject + " where resource_uuid = '" + resourceRef + "'";
            } else {
                query = "from " + dbObject + " where " + criteriaName + " = '" + criteriaValue + "'";
            }
        } else {
            if ("User".equals(dbObject)) {
                query = "from " + dbObject + " where " + path + " = '" + criteriaValue + "' ";
            } else {
                dbObject = "Account";
                query = "from " + dbObject + " where " + path + "['" + criteriaName + "'] = '" + criteriaValue + "' ";
                if (StringUtils.isNotEmpty(resourceRef)) {
                    query = query + "and resource_uuid = '" + resourceRef + "'";
                }
            }
        }

        return query;

    }

    @Override
    public ObjectListType searchObjects(QueryType queryType, PagingType paging) throws FaultMessage {
        logger.info("### REPOSITORY # Enter searchObjects({})", DebugUtil.prettyPrint(queryType));
        String query = convertFilterToQuery(queryType.getFilter());
        ObjectListType result = new ObjectListType();

        List<SimpleDomainObject> returnedObjects = (List<SimpleDomainObject>) genericDao.execute(query);
        for (SimpleDomainObject dbObject : returnedObjects) {
            if (dbObject instanceof User) {
                UserType user = new UserType();
                copyUserToUserType((User) dbObject, user);
                result.getObject().add(user);
            } else if (dbObject instanceof Account) {
                AccountShadowType accountShadow = new AccountShadowType();
                copyAccountToAccountShadowType((Account) dbObject, accountShadow);
                result.getObject().add(accountShadow);
            } else if (dbObject instanceof Resource) {
                ResourceType resource = new ResourceType();
                copyResourceToResourceType((Resource) dbObject, resource);
                result.getObject().add(resource);
            } else if (dbObject instanceof ResourceState) {
                ResourceStateType resourceState = new ResourceStateType();
                copyResourceStateToResourceStateType((ResourceState) dbObject, resourceState);
                result.getObject().add(resourceState);
            } else {
                throw new FaultMessage("Object of type " + dbObject.getClass() + " is not supported", new UnsupportedObjectTypeFaultType());
            }
        }
        logger.info("### REPOSITORY # Exit searchObjects({})", DebugUtil.prettyPrint(result));
        return result;
    }

    @Override
    public void modifyObject(ObjectModificationType objectChange) throws FaultMessage {

        logger.info("### REPOSITORY # Enter modifyObject({})", DebugUtil.prettyPrint(objectChange));

        if (StringUtils.isEmpty(objectChange.getOid()) && objectChange.getPropertyModification().isEmpty()) {
            logger.warn("### REPOSITORY # Method modifyObject({}) was called with empty changes, Note: XMLUtil returns immediately if there are no results", DebugUtil.prettyPrint(objectChange));
            return;
        }

        SimpleDomainObject object;
        String oid = objectChange.getOid();

        if ((object = genericDao.findById(UUID.fromString(oid))) != null) {
            if (object instanceof User) {
                modifyUser(objectChange);
            } else if (object instanceof Account) {
                modifyAccount(objectChange);
            } else if (object instanceof Resource) {
                modifyResource(objectChange);
            } else if (object instanceof ResourceState) {
                modifyResourceState(objectChange);
            } else {
                throw new UnsupportedOperationException("Operation modifyObject() is unsupported (or not implemented yet) for object of class" + object.getClass());
            }
            logger.info("### REPOSITORY # Exit modifyObject(..)");
            return;
        }
        logger.error("### REPOSITORY # Fault modifyObject(..) : Object not modified");
        throw new FaultMessage("Object not modified", new ObjectNotFoundFaultType());
    }

    @Override
    public void deleteObject(String oid) throws FaultMessage {

        logger.info("### REPOSITORY # Enter deleteObject({})", oid);

        logger.trace("[Repository] deleteObject begin");
        Validate.notNull(oid);

        SimpleDomainObject object;
        if ((object = genericDao.findById(UUID.fromString(oid))) != null) {
            genericDao.remove(object);
            logger.trace("[Repository] deleteObject end");
            logger.info("### REPOSITORY # Exit deleteObject(..)");
            return;
        }

        logger.error("### REPOSITORY # Fault deleteObject(..) : Oid not found : {}", oid);
        throw new FaultMessage("Oid '" + oid + "' not found", new ObjectNotFoundFaultType());

    }

    @Override
    public PropertyAvailableValuesListType getPropertyAvailableValues(String oid, PropertyReferenceListType properties) throws FaultMessage {
        //TODO implement this method
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public UserContainerType listAccountShadowOwner(String accountOid) throws FaultMessage {
        logger.info("### REPOSITORY # Enter listAccountShadowOwner({})", accountOid);
        Validate.notNull(accountOid, "provided accountShadow oid was null");

        UserContainerType userContainerType = new UserContainerType();

        String query = "from User as user inner join user.accounts as account where account.oid = '" + accountOid + "' ";
        List returnedObjects = (List) genericDao.execute(query);

        if (null == returnedObjects || returnedObjects.isEmpty()) {
            logger.warn("### REPOSITORY # Fault listAccountShadowOwner(..) : Owner of account with oid {} does not exist", accountOid);
            return userContainerType;
        }

        if (returnedObjects.size() > 1) {
            logger.error("### REPOSITORY # Fault listAccountShadowOwner(..) : Integrity problem: more than one owner found for account with oid {}", accountOid);
            throw new FaultMessage("Integrity problem: more than one owner found for account with oid " + accountOid, new ReferentialIntegrityFaultType());
        }

        UserType userType = new UserType();
        //returned are User and Account
        User user = (User) (((Object[]) (returnedObjects.get(0)))[0]);
        copyUserToUserType(user, userType);
        userContainerType.setUser(userType);

        //TODO: reimplement - retrieve owner from DB, not by iteration over all users in Java
//        if (genericDao.findById(UUID.fromString(accountOid)) != null) {
//
//            List<SimpleDomainObject> users = genericDao.findAllOfType(User.class.getSimpleName());
//
//            for (SimpleDomainObject user : users) {
//                UserType userType = new UserType();
//                Utils.copyPropertiesSilent(userType, user);
//                Set<Account> accounts = (Set<Account>) ((User) user).getAccounts();
//                for (Account account : accounts) {
//                    if (account.getOid().equals(UUID.fromString(accountOid))) {
//                        userContainerType.setUser(userType);
//                    }
//                }
//            }
//        } else {
//            logger.error("### REPOSITORY # Fault listAccountShadowOwner(..) : Account with oid {} does not exist", accountOid);
//            throw new FaultMessage("Account with oid " + accountOid + " does not exist", new ObjectNotFoundFaultType());
//        }

        logger.info("### REPOSITORY # Exit listAccountShadowOwner(..): {}", DebugUtil.prettyPrint(userContainerType));
        return userContainerType;
    }

    //TODO: roles etc
    @Override
    public ResourceObjectShadowListType listResourceObjectShadows(String resourceOid, String resourceObjectShadowType) throws FaultMessage {
        logger.trace("[Repository] listResourceObjectShadows begin");
        Validate.notNull(resourceOid);
        Validate.notNull(resourceObjectShadowType);

        ResourceObjectShadowListType result = new ResourceObjectShadowListType();

        if (genericDao.findById(UUID.fromString(resourceOid)) != null) {

            //TODO: reimplement - retrieve all accounts on the resource by proper query on DB, not filter them in applications
            if (Utils.getObjectType("AccountType").equals(resourceObjectShadowType)) {

                List<SimpleDomainObject> accounts = genericDao.findAllOfType(Account.class.getSimpleName());
                List<AccountShadowType> accountTypeList = new ArrayList<AccountShadowType>();
                for (SimpleDomainObject account : accounts) {
                    if (((Account) account).getResource().getOid().equals(UUID.fromString(resourceOid))) {
                        AccountShadowType accountShadowType = new AccountShadowType();
                        copyAccountToAccountShadowType((Account) account, accountShadowType);
                        accountTypeList.add(accountShadowType);
                    }
                }
                result.getObject().addAll(accountTypeList);
            }
        } else {
            throw new FaultMessage("Resource with oid " + resourceOid + " not found", new ObjectNotFoundFaultType());
        }
        return result;
    }
}
