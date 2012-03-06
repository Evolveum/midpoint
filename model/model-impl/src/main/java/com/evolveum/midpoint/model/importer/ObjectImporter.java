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
 */
package com.evolveum.midpoint.model.importer;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Extension of validator used to import objects to the repository.
 * <p/>
 * In addition to validating the objects the importer also tries to resolve the
 * references and may also do other repository-related stuff.
 *
 * @author Radovan Semancik
 */
@Component
public class ObjectImporter {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectImporter.class);
    private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName()
            + ".resolveReference";

    @Autowired(required = true)
    private Protector protector;
    @Autowired(required = true)
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired(required = true)
    private PrismContext prismContext;

    public void importObjects(InputStream input, final ImportOptionsType options, final Task task, final OperationResult parentResult,
                              final RepositoryService repository) {

        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <T extends ObjectType> EventResult postMarshall(PrismObject<T> object, Element objectElement, OperationResult objectResult) {
                LOGGER.debug("Importing object {}", object);
                T objectType = object.asObjectable();
                
                if (objectResult.isAcceptable()) {
                    resolveReferences(objectType, repository, 
                    		options.isReferentialIntegrity() == null ? false : options.isReferentialIntegrity(), objectResult);
                }
                
                if (objectResult.isAcceptable()) {
                    generateIdentifiers(objectType, repository,  objectResult);
                }

                PrismContainer dynamicPart = null;
                if (BooleanUtils.isTrue(options.isValidateDynamicSchema()) && objectResult.isAcceptable()) {
                    dynamicPart = validateWithDynamicSchemas(objectType, objectElement, repository, objectResult);
                }

                if (BooleanUtils.isTrue(options.isEncryptProtectedValues()) && objectResult.isAcceptable()) {
                    encryptValuesInStaticPart(objectType, objectResult);
                    if (dynamicPart != null) {
                        encryptValuesInDynamicPart(dynamicPart, objectType, objectResult);
                    }
                }

                if (objectResult.isAcceptable()) {
                    try {

                        importObjectToRepository(objectType, options, repository, objectResult);

                        LOGGER.info("Imported object {}", object);

                    } catch (SchemaException e) {
                        objectResult.recordFatalError("Schema violation", e);
                        LOGGER.error("Import of object {} failed: Schema violation: {}",
                                new Object[]{object, e.getMessage(), e});
                    } catch (RuntimeException e) {
                        objectResult.recordFatalError("Unexpected problem", e);
                        LOGGER.error("Import of object {} failed: Unexpected problem: {}",
                                new Object[]{object, e.getMessage(), e});
                    } catch (ObjectAlreadyExistsException e) {
                        LOGGER.error("Import of object {} failed: Object already exists: {}",
                                new Object[]{object, e.getMessage(), e});
                        LOGGER.error("Object already exists", e);
                    }

                }

                if (objectResult.isAcceptable()) {
                    // Continue import
                    return EventResult.cont();
                } else {
                    // Continue import, but skip the rest of the processing of this object
                    return EventResult.skipObject();
                }
            }

			@Override
            public void handleGlobalError(OperationResult currentResult) {
                // No reaction
            }

        };

        Validator validator = new Validator(prismContext, handler);
        validator.setVerbose(true);
        validator.setValidateSchema(BooleanUtils.isTrue(options.isValidateStaticSchema()));
        if (options.getStopAfterErrors() != null) {
            validator.setStopAfterErrors(options.getStopAfterErrors().longValue());
        }

        validator.validate(input, parentResult, OperationConstants.IMPORT_OBJECT);

    }

    private void importObjectToRepository(ObjectType object, ImportOptionsType options, RepositoryService repository,
                                          OperationResult objectResult) throws SchemaException, ObjectAlreadyExistsException {

        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".importObjectToRepository");

        try {

            repository.addObject(object.asPrismObject(), result);
            result.recordSuccess();

        } catch (ObjectAlreadyExistsException e) {
            if (BooleanUtils.isTrue(options.isOverwrite())) {
                // Try to delete conflicting object
                String deletedOid = deleteObject(object, repository, result);
                if (deletedOid != null) {
                    if (BooleanUtils.isTrue(options.isKeepOid())) {
                        object.setOid(deletedOid);
                    }
                    repository.addObject(object.asPrismObject(), result);
                    result.recordSuccess();
                } else {
                    // cannot delete, throw original exception
                    result.recordFatalError("Object already exists, cannot overwrite", e);
                    throw e;
                }
            } else {
                result.recordFatalError("Object already exists", e);
                throw e;
            }
        }
    }


    /**
     * @return OID of the deleted object or null (if nothing was deleted)
     */
    private String deleteObject(ObjectType object, RepositoryService repository, OperationResult objectResult) throws SchemaException {
        if (object.getOid() != null) {
            // The conflict is either UID or we should not proceed as we could delete wrong object
            try {
                repository.deleteObject(object.getClass(), object.getOid(), objectResult);
            } catch (ObjectNotFoundException e) {
                // Cannot delete. The conflicting thing was obviously not OID. Just throw the original exception
                return null;
            }
            // deleted
            return object.getOid();
        } else {
            // The conflict was obviously name. As we have no explicit OID in the object to import
            // it is pretty safe to try to delete the conflicting object
            // look for an object by name and type and delete it
            QueryType query = QueryUtil.createNameQuery(object);
            List<PrismObject<? extends ObjectType>> objects = (List) repository.searchObjects(object.getClass(), query, null, objectResult);
            if (objects.size() != 1) {
                // too few or too much results, not safe to delete
                return null;
            }
            String oidToDelete = objects.get(0).getOid();
            try {
                repository.deleteObject(object.getClass(), oidToDelete, objectResult);
            } catch (ObjectNotFoundException e) {
                // Cannot delete. Some strange conflict ...
                return null;
            }
            return oidToDelete;
        }
    }

    protected PrismContainer validateWithDynamicSchemas(ObjectType object, Element objectElement,
                                                           RepositoryService repository, OperationResult objectResult) {

//        if (object instanceof ExtensibleObjectType) {
            // TODO: check extension schema (later)
            //objectResult.computeStatus("Extension schema error");
//        }

        if (object instanceof ConnectorType) {
            ConnectorType connector = (ConnectorType) object;
            checkSchema(connector.getSchema(), "connector", objectResult);
            objectResult.computeStatus("Connector schema error");

        } else if (object instanceof ResourceType) {


            // Only two object types have XML snippets that conform to the dynamic schema


            ResourceType resource = (ResourceType) object;
            List<Object> configurationElements = resource.getConfiguration().getAny();
            if (configurationElements.isEmpty()) {
                // Nothing to check
                objectResult.recordWarning("The resource has no configuration");
                return null;
            }

            // Check the resource configuration. The schema is in connector, so fetch the connector first
            String connectorOid = resource.getConnectorRef().getOid();
            if (connectorOid == null) {
                objectResult.recordFatalError("The connector reference (connectorRef) is null");
                return null;
            }

            ConnectorType connector = null;
            try {
                connector = repository.getObject(ConnectorType.class, connectorOid, null, objectResult).asObjectable();
            } catch (ObjectNotFoundException e) {
                // No connector, no fun. We can't check the schema. But this is referential integrity problem.
                // Mark the error ... there is nothing more to do
                objectResult.recordFatalError("Connector (OID:" + connectorOid + ") referenced from the resource is not in the repository", e);
                return null;
            } catch (SchemaException e) {
                // Probably a malformed connector. To be kind of robust, lets allow the import.
                // Mark the error ... there is nothing more to do
                objectResult.recordPartialError("Connector (OID:" + connectorOid + ") referenced from the resource has schema problems: " + e.getMessage(), e);
                LOGGER.error("Connector (OID:{}) referenced from the imported resource \"{}\" has schema problems: {}", new Object[]{connectorOid, resource.getName(), e.getMessage(), e});
                return null;
            }
            QName configurationElementRef = new QName(connector.getNamespace(), SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_ELEMENT_LOCAL_NAME);
            PrismContainer propertyContainer = validateDynamicSchema(configurationElements, configurationElementRef, connector.getSchema(), "resourceConfiguration", objectResult);

            // Also check integrity of the resource schema
            checkSchema(resource.getSchema(), "resource", objectResult);

            objectResult.computeStatus("Dynamic schema error");
            return propertyContainer;

        } else if (object instanceof ResourceObjectShadowType) {
            // TODO

            //objectResult.computeStatus("Dynamic schema error");
        }

        return null;
    }

    /**
     * Try to parse the schema using schema processor. Report errors.
     *
     * @param dynamicSchema
     * @param schemaName
     * @param objectResult
     */
    private void checkSchema(XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".check" + StringUtils.capitalize(schemaName) + "Schema");

        Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);

        if (dynamicSchema == null || xsdElement == null) {
            result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Missing dynamic " + schemaName + " schema");
            return;
        }

        try {
            com.evolveum.midpoint.prism.schema.PrismSchema.parse(xsdElement, prismContext);
        } catch (SchemaException e) {
            result.recordFatalError("Error during " + schemaName + " schema integrity check: " + e.getMessage(), e);
            return;
        }
        result.recordSuccess();
    }

    /**
     * Validate the provided XML snippet with schema definition fetched in runtime.
     *
     * @param contentElements DOM tree to validate
     * @param elementRef      the "correct" name of the root element
     * @param dynamicSchema   dynamic schema
     * @param schemaName
     * @param objectResult
     */
    private PrismContainer validateDynamicSchema(List<Object> contentElements, QName elementRef,
                                                    XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".validate" + StringUtils.capitalize(schemaName) + "Schema");

        Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);
        if (xsdElement == null) {
        	result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "No "+schemaName+" schema present");
        	return null;
        }

        com.evolveum.midpoint.prism.schema.PrismSchema schema = null;
        try {
            schema = com.evolveum.midpoint.prism.schema.PrismSchema.parse(xsdElement, prismContext);
        } catch (SchemaException e) {
            result.recordFatalError("Error during " + schemaName + " schema parsing: " + e.getMessage(), e);
            LOGGER.trace("Validation error: {}" + e.getMessage());
            return null;
        }

        PrismContainerDefinition containerDefinition = schema.findItemDefinition(elementRef, PrismContainerDefinition.class);

        PrismContainer propertyContainer = null;
//        try {
//            propertyContainer = containerDefinition.parseAsContent(elementRef, contentElements, null);
//        } catch (SchemaException e) {
//            result.recordFatalError("Error during " + schemaName + " schema validation: " + e.getMessage(), e);
//            LOGGER.trace("Validation error: {}" + e.getMessage());
//            return null;
//        }

        result.recordSuccess();
        return propertyContainer;

//		// Shallow clone the tree under a correct element name 
//		Document doc = element.getOwnerDocument();
//		Element clonedElement = doc.createElementNS(elementRef.getNamespaceURI(), elementRef.getLocalPart());
//		NamedNodeMap attributes = element.getAttributes();
//		for (int i = 0; i < attributes.getLength(); i++) {
//			clonedElement.setAttributeNodeNS((Attr) ((Attr) attributes.item(i)).cloneNode(true));
//		}
//		NodeList childNodes = element.getChildNodes();
//		for (int i = 0; i < childNodes.getLength(); i++) {
//			clonedElement.appendChild(childNodes.item(i).cloneNode(true));
//		}

//		try {
//			SchemaRegistry reg = new SchemaRegistry();
//			reg.registerSchema(xsdElement);
//			reg.initialize();
//			Schema midPointSchema = reg.getMidPointSchema();		
//			javax.xml.validation.Validator xsdValidator = midPointSchema.newValidator();
//			if (logger.isTraceEnabled()) {
//				logger.trace("Validating following content with dynamic {} schema:\n{}",schemaName,DOMUtil.serializeDOMToString(clonedElement));
//			}
//			xsdValidator.validate(new DOMSource(clonedElement));
//		} catch (SAXException e) {
//			result.recordFatalError("Error during " + schemaName + " schema validation: " + e.getMessage(), e);
//			logger.trace("Validation error: {}"+e.getMessage());
//			return;
//		} catch (IOException e) {
//			result.recordFatalError("OI error during " + schemaName + " schema validation: " + e.getMessage(), e);
//			logger.error("IO error during {} schema validation: {}",schemaName,e.getMessage());
//			return;
//		}
//		result.recordSuccess();
    }

    protected void resolveReferences(ObjectType object, RepositoryService repository,
    		boolean enforceReferentialIntegrity, OperationResult result) {
        // We need to look up all object references. Probably the only efficient
        // way to do it is to use reflection.
        Class<?> type = object.getClass();
        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Class<?> returnType = method.getReturnType();
            if (ObjectReferenceType.class.isAssignableFrom(returnType)) {
                // we have a method that returns ObjectReferenceType, try to
                // resolve it.
                String propName = parseGetterPropName(method);
                if (propName != null) {
                    LOGGER.trace("Found reference property {}, method {}", propName, method.getName());
                    try {
                        Object returnVal = method.invoke(object);
                        ObjectReferenceType ref = (ObjectReferenceType) returnVal;
                        resolveRef(ref, propName, repository, enforceReferentialIntegrity, result);
                        if (!result.isAcceptable()) {
                            LOGGER.error("Error resolving reference {}: {}", propName, result.getMessage());
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        // Should not happen, getters have no arguments
                        result.recordFatalError("Cannot invoke getter " + method.getName()
                                + " due to IllegalArgumentException", e);
                        return;
                    } catch (IllegalAccessException e) {
                        // Should not happen, getters have no arguments
                        result.recordFatalError("Cannot invoke getter " + method.getName()
                                + " due to IllegalAccessException", e);
                        return;
                    } catch (InvocationTargetException e) {
                        // Should not happen, getters have no arguments
                        result.recordFatalError("Cannot invoke getter " + method.getName()
                                + " due to InvocationTargetException", e);
                        return;
                    }
                    LOGGER.trace("Reference {} processed", propName);
                }
            }
        }
    }

    private void resolveRef(ObjectReferenceType ref, String propName, RepositoryService repository, 
    				boolean enforceReferentialIntegrity, OperationResult parentResult) {
        if (ref == null) {
            // Nothing to do
            return;
        }

        OperationResult result = parentResult.createSubresult(OPERATION_RESOLVE_REFERENCE);
        result.addContext(OperationResult.CONTEXT_PROPERTY, propName);

        Element filter = ref.getFilter();
        if (ref.getOid() != null && !ref.getOid().isEmpty()) {
            // We have OID
            if (filter != null) {
                // We have both filter and OID. We will choose OID, but let's at
                // least log a warning
                result.appendDetail("Both OID and filter for property " + propName);
                result.recordPartialError("Both OID and filter for property " + propName);
                ref.setFilter(null);
            }
            // Nothing to resolve, but let's check if the OID exists
            Class<? extends ObjectType> type = ObjectType.class;
            if (ref.getType() != null) {
                ObjectTypes refType = ObjectTypes.getObjectTypeFromTypeQName(ref.getType());
                if (refType == null) {
                    result.recordWarning("Unknown type specified in reference " + propName + ": "
                            + ref.getType());
                } else {
                    type = refType.getClassDefinition();
                }
            }
            ObjectType object = null;
            try {
                object = repository.getObject(type, ref.getOid(), null, result).asObjectable();
            } catch (ObjectNotFoundException e) {
            	String message = "Reference " + propName + " refers to a non-existing object " + ref.getOid();
            	if (enforceReferentialIntegrity) {
            		LOGGER.error(message);
            		result.recordFatalError(message);
            	} else {
            		LOGGER.warn(message);
            		result.recordWarning(message);
            	}
            } catch (SchemaException e) {
            	
                result.recordPartialError("Schema error while trying to retrieve object " + ref.getOid()
                        + " : " + e.getMessage(), e);
                LOGGER.error(
                        "Schema error while trying to retrieve object " + ref.getOid() + " : "
                                + e.getMessage(), e);
                // But continue otherwise
            }
            if (object != null && ref.getType() != null) {
                // Check if declared and actual type matches
                if (!object.getClass().equals(type)) {
                    result.recordWarning("Type mismatch on property " + propName + ": declared:"
                            + ref.getType() + ", actual: " + object.getClass());
                }
            }
            result.recordSuccessIfUnknown();
            parentResult.computeStatus();
            return;
        }
        if (filter == null) {
            // No OID and no filter. We are lost.
            result.recordFatalError("Neither OID nor filter for property " + propName
                    + ": cannot resolve reference");
            return;
        }
        // No OID and we have filter. Let's check the filter a bit
        LOGGER.trace("Resolving using filter {}", DOMUtil.serializeDOMToString(filter));
        NodeList childNodes = filter.getChildNodes();
        if (childNodes.getLength() == 0) {
            result.recordFatalError("OID not specified and filter is empty for property " + propName);
            return;
        }
        // Let's do resolving
        QueryType query = new QueryType();
        query.setFilter(filter);
        List<PrismObject<? extends ObjectType>> objects = null;
        QName objectType = ref.getType();
        if (objectType == null) {
            result.recordFatalError("Missing definition of type of reference " + propName);
            return;
        }
        try {

            objects = (List)repository.searchObjects(ObjectTypes.getObjectTypeFromTypeQName(objectType)
                    .getClassDefinition(), query, null, result);

        } catch (SchemaException e) {
            // This is unexpected, but may happen. Record fatal error
            result.recordFatalError("Repository schema error during resolution of reference " + propName, e);
            return;
        } catch (SystemException e) {
            // We don't want this to tear down entire import.
            result.recordFatalError("Repository system error during resolution of reference " + propName, e);
            return;
        }
        if (objects.isEmpty()) {
            result.recordFatalError("Repository reference " + propName
                    + " cannot be resolved: filter matches no object");
            return;
        }
        if (objects.size() > 1) {
            result.recordFatalError("Repository reference " + propName
                    + " cannot be resolved: filter matches " + objects.size() + " objects");
            return;
        }
        // Bingo. We have exactly one object.
        String oid = objects.get(0).getOid();
        ref.setOid(oid);
        result.recordSuccessIfUnknown();
    }

    private void generateIdentifiers(ObjectType object, RepositoryService repository,
			OperationResult objectResult) {
		if (object instanceof TaskType) {
			TaskType task = (TaskType)object;
			if (task.getTaskIdentifier() == null || task.getTaskIdentifier().isEmpty()) {
				task.setTaskIdentifier(lightweightIdentifierGenerator.generate().toString());
			}
		}
	}
    
    private void encryptValuesInStaticPart(ObjectType object, OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".encryptValues");
        encryptValuesInStaticPartRecursive(object, object, result);
        result.recordSuccessIfUnknown();
    }

    private void encryptValuesInStaticPartRecursive(Object object, ObjectType objectType, OperationResult result) {
        // We need to look up all object references. Probably the only efficient
        // way to do it is to use reflection.
        Class<?> type = object.getClass();
        Method[] methods = type.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String propName = parseGetterPropName(method);
//            if (propName != null) {
//                // It has to return a value in JAXB package or List to be interesting
//                Class<?> returnType = method.getReturnType();
//                if (JAXBUtil.isJaxbClass(returnType) || returnType.equals(List.class)) {
//                    Object fieldValue;
//                    try {
//                        fieldValue = method.invoke(object);
//                    } catch (IllegalArgumentException e) {
//                        result.recordFatalError("Access to field " + propName + " failed (illegal argument): " + e.getMessage(), e);
//                        return;
//                    } catch (IllegalAccessException e) {
//                        result.recordFatalError("Access to field " + propName + " failed (illegal access): " + e.getMessage(), e);
//                        return;
//                    } catch (InvocationTargetException e) {
//                        result.recordFatalError("Access to field " + propName + " failed (invocation target): " + e.getMessage(), e);
//                        return;
//                    }
//                    if (fieldValue == null) {
//                        continue;
//                    }
//                    if (returnType.equals(List.class)) {
//                        List<?> valueList = (List<?>) fieldValue;
//                        for (Object value : valueList) {
//                            if (value != null && JAXBUtil.isJaxbClass(value.getClass())) {
//                                encryptValueInStaticPartAndRecurse(value, propName, objectType, result);
//                            }
//                        }
//                    } else {
//                        encryptValueInStaticPartAndRecurse(fieldValue, propName, objectType, result);
//                    }
//                }
//            }
        }
    }

    private void encryptValueInStaticPartAndRecurse(Object value, String propName, ObjectType objectType, OperationResult result) {
        if (value instanceof ProtectedStringType) {
            ProtectedStringType ps = (ProtectedStringType) value;
            if (ps.getClearValue() != null) {
                try {
                    LOGGER.info("Encrypting cleartext value for field " + propName + " while importing " + ObjectTypeUtil.toShortString(objectType));
                    protector.encrypt(ps);
                } catch (EncryptionException e) {
                    LOGGER.info("Faild to encrypt cleartext value for field " + propName + " while importing " + ObjectTypeUtil.toShortString(objectType));
                    result.recordFatalError("Faild to encrypt value for field " + propName + ": " + e.getMessage(), e);
                    return;
                }
            }
        }
        encryptValuesInStaticPartRecursive(value, objectType, result);
    }

    private String parseGetterPropName(Method method) {
        if (method.getParameterTypes().length != 0) {
            // Has parameters -> not getter
            return null;
        }
        String methodName = method.getName();
        if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
            String suffix = methodName.substring(3);
            String propName = suffix.substring(0, 1).toLowerCase() + suffix.substring(1);
            return propName;
        }
        return null;
    }


    private void encryptValuesInDynamicPart(PrismContainer dynamicPart, ObjectType objectType,
                                            OperationResult objectResult) {
        OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName() + ".encryptValues");
        encryptValuesInDynamicPartRecursive(dynamicPart, objectType, result);
        result.recordSuccessIfUnknown();
    }

    private void encryptValuesInDynamicPartRecursive(PrismContainer dynamicPart, ObjectType objectType,
                                                     OperationResult result) {
//        for (Item i : dynamicPart.getItems()) {
//            if (i instanceof PrismProperty) {
//                encryptProperty((PrismProperty) i, objectType, result);
//            } else if (i instanceof PrismContainer) {
//                encryptValuesInDynamicPartRecursive((PrismContainer) i, objectType, result);
//            } else {
//                throw new IllegalArgumentException("Unexpected item in property container: " + i);
//            }
//        }
    }

    private void encryptProperty(PrismProperty property, ObjectType objectType, OperationResult result) {
        if (!property.getDefinition().getTypeName().equals(SchemaConstants.R_PROTECTED_STRING_TYPE)) {
            return;
        }
        LOGGER.debug("Encrypting property {}", property.getName());
        ProtectedStringType ps = (ProtectedStringType) property.getValue().getValue();
        try {
            protector.encrypt(ps);
        } catch (EncryptionException e) {
            LOGGER.info("Faild to encrypt cleartext value for field " + property.getName() + " while importing " + ObjectTypeUtil.toShortString(objectType));
            result.recordFatalError("Faild to encrypt value for field " + property.getName() + ": " + e.getMessage(), e);
            return;
        }
//        try {
//            property.applyValueToElement();
//        } catch (SchemaException e) {
//            LOGGER.info("Faild to apply encrypted value for field " + property.getName() + " while importing " + ObjectTypeUtil.toShortString(objectType));
//            result.recordFatalError("Faild to apply encrypted value for field " + property.getName() + ": " + e.getMessage(), e);
//            return;
//        }
    }
}
 