/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.model.importer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.result.OperationConstants;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

/**
 * Extension of validator used to import objects to the repository.
 * 
 * In addition to validating the objects the importer also tries to resolve the
 * references and may also do other repository-related stuff.
 * 
 * @author Radovan Semancik
 * 
 */
@Component
public class ObjectImporter {

	private static final Trace logger = TraceManager.getTrace(ObjectImporter.class);
	private static final String OPERATION_RESOLVE_REFERENCE = ObjectImporter.class.getName()
			+ ".resolveReference";
	
	@Autowired(required=true)
	private Protector protector;

	public void importObjects(InputStream input, final Task task, final OperationResult parentResult,
			final RepositoryService repository) {

		EventHandler handler = new EventHandler() {

			long progress = 0;

			@Override
			public boolean preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
				
				return true;
			}
			
			@Override
			public void postMarshall(ObjectType object, Element objectElement, OperationResult objectResult) {

				progress++;

				logger.debug("Starting import of object {}", ObjectTypeUtil.toShortString(object));

				objectResult.addContext(OperationResult.CONTEXT_PROGRESS, progress);
				// TODO: params, context

				if (objectResult.isAcceptable()) {					
					resolveReferences(object, repository, objectResult);
				}
				
				if (objectResult.isAcceptable()) {
					validateWithDynamicSchemas(object, objectElement, repository, objectResult);
				}
				
				if (objectResult.isAcceptable()) {
					encryptValues(object, objectResult);
				}
				
				if (objectResult.isAcceptable()) {
					try {

						repository.addObject(object, objectResult);
						objectResult.recordSuccess();

					} catch (ObjectAlreadyExistsException e) {
						objectResult.recordFatalError("Object already exists", e);
						logger.error("Object already exists", e);
					} catch (SchemaException e) {
						objectResult.recordFatalError("Schema violation", e);
						logger.error("Schema violation", e);
					} catch (RuntimeException e) {
						objectResult.recordFatalError("Unexpected problem", e);
						logger.error("Unexpected problem", e);
					}

				}

				// TODO check if there are too many errors

			}

			@Override
			public void handleGlobalError(OperationResult currentResult) {
				// No reaction
			}

		};

		Validator validator = new Validator(handler);
		validator.setVerbose(true);

		validator.validate(input, parentResult, OperationConstants.IMPORT_OBJECT);

	}

	protected void validateWithDynamicSchemas(ObjectType object, Element objectElement,
			RepositoryService repository, OperationResult objectResult) {
		
		if (object instanceof ExtensibleObjectType) {
			// TODO: check extension schema (later)
			//objectResult.computeStatus("Extension schema error");
		}
		
		if (object instanceof ConnectorType) {
			ConnectorType connector = (ConnectorType)object;
			checkSchema(connector.getSchema(), "connector", objectResult);
			objectResult.computeStatus("Connector schema error");
			
		} else if (object instanceof ResourceType) {
			
			
			// Only two object types have XML snippets that conform to the dynamic schema
			
			
			ResourceType resource = (ResourceType)object;
			// Use DOM representation of the object, so we get the "real" namespace prefixes from the import file
			// This will provide better information to system admin attempting import
			NodeList configurationElements = objectElement.getElementsByTagNameNS(SchemaConstants.C_RESOURCE_CONFIGURATION.getNamespaceURI(), SchemaConstants.C_RESOURCE_CONFIGURATION.getLocalPart());
			if (configurationElements.getLength()==0) {
				// Nothing to check
				objectResult.recordWarning("The resource has no configuration");
				return;
			}
			if (configurationElements.getLength()>1) {
				// Nothing to check
				objectResult.recordFatalError("The resource has multiple configuration elements");
				return;
			}
			Element configurationElement = (Element) configurationElements.item(0);
			
			// Check the resource configuration. The schema is in connector, so fetch the connector first
			String connectorOid = resource.getConnectorRef().getOid();
			if (connectorOid==null) {
				objectResult.recordFatalError("The connector reference (connectorRef) is null");
				return;
			}
			
			ConnectorType connector = null;
			try {
				connector = repository.getObject(ConnectorType.class, connectorOid, null, objectResult);
			} catch (ObjectNotFoundException e) {
				// No connector, no fun. We can't check the schema. But this is referential integrity problem.
				// Mark the error ... there is nothing more to do
				objectResult.recordFatalError("Connector (OID:"+connectorOid+") referenced from the resource is not in the repository", e);
				return;
			} catch (SchemaException e) {
				// Probably a malformed connector. To be kind of robust, lets allow the import. 
				// Mark the error ... there is nothing more to do
				objectResult.recordPartialError("Connector (OID:"+connectorOid+") referenced from the resource has schema problems: "+e.getMessage(), e);
				logger.error("Connector (OID:{}) referenced from the imported resource \"{}\" has schema problems: {}",new Object[]{connectorOid,resource.getName(),e.getMessage(), e});
				return;
			}
			QName configurationElementRef = new QName(connector.getNamespace(),SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_ELEMENT_LOCAL_NAME);
			validateDynamicSchema(configurationElement, configurationElementRef, connector.getSchema(), "resourceConfiguration", objectResult);
			
			// Also check integrity of the resource schema
			checkSchema(resource.getSchema(), "resource", objectResult);
			
			objectResult.computeStatus("Dynamic schema error");
			
		} else if (object instanceof ResourceObjectShadowType) {
			// TODO
			
			//objectResult.computeStatus("Dynamic schema error");
		}
	}

	/**
	 * Try to parse the schema using schema processor. Report errors.
	 * 
	 * @param schema
	 * @param objectResult
	 */
	private void checkSchema(XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {
		// TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO 
		// Disabling this now a while ... until the BaseX namespace problem is resolved
		// TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO 
		
//		OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName()+".check"+StringUtils.capitalize(schemaName)+"Schema");
//		
//		Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);
//		
//		try {
//			com.evolveum.midpoint.schema.processor.Schema.parse(xsdElement);
//		} catch (SchemaProcessorException e) {
//			result.recordFatalError("Error during " + schemaName + " schema integrity check: " + e.getMessage(), e);
//			return;
//		}
//		result.recordSuccess();
	}

	/**
	 * Validate the provided XML snippet with schema definition fetched in runtime.
	 * 
	 * @param element DOM tree to validate
	 * @param elementRef the "correct" name of the root element
	 * @param dynamicSchema dynamic schema
	 * @param objectResult
	 */
	private void validateDynamicSchema(Element element, QName elementRef,
			XmlSchemaType dynamicSchema, String schemaName, OperationResult objectResult) {

		// TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO 
		// Disabling this now a while ... until the BaseX namespace problem is resolved
		// TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO 

//		OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName()+".validate"+StringUtils.capitalize(schemaName)+"Schema");
//	
//		// Shallow clone the tree under a correct element name 
//		Document doc = element.getOwnerDocument();
//		Element clonedElement = doc.createElementNS(elementRef.getNamespaceURI(), elementRef.getLocalPart());
//		NodeList childNodes = element.getChildNodes();
//		for (int i = 0; i < childNodes.getLength(); i++) {
//			clonedElement.appendChild(childNodes.item(i));
//		}
//		
//		Element xsdElement = ObjectTypeUtil.findXsdElement(dynamicSchema);
//		
//		try {
//			SchemaRegistry reg = new SchemaRegistry();
//			reg.addExtraSchema(xsdElement);
//			reg.initialize();
//			Schema midPointSchema = reg.getMidPointSchema();		
//			javax.xml.validation.Validator xsdValidator = midPointSchema.newValidator();		
//			xsdValidator.validate(new DOMSource(clonedElement));
//		} catch (SAXException e) {
//			result.recordFatalError("Error during " + schemaName + " schema validation: " + e.getMessage(), e);
//			return;
//		} catch (IOException e) {
//			result.recordFatalError("OI error during " + schemaName + " schema validation: " + e.getMessage(), e);
//			return;
//		}
//		result.recordSuccess();
	}

	protected void resolveReferences(ObjectType object, RepositoryService repository,
			OperationResult result) {
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
				if (propName!=null) {
					logger.debug("Found reference property {}, method {}", propName, method.getName());
					try {
						Object returnVal = method.invoke(object);
						ObjectReferenceType ref = (ObjectReferenceType) returnVal;
						resolveRef(ref, propName, repository, result);
						if (!result.isAcceptable()) {
							logger.error("Error resolving reference {}: {}", propName, result.getMessage());
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
					logger.debug("Reference {} processed", propName);
				}
			}
		}
	}

	private void resolveRef(ObjectReferenceType ref, String propName, RepositoryService repository,
			OperationResult parentResult) {
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
				object = repository.getObject(type, ref.getOid(), null, result);
			} catch (ObjectNotFoundException e) {
				result.recordWarning("Reference " + propName + " refers to a non-existing object "
						+ ref.getOid());
			} catch (SchemaException e) {
				result.recordPartialError("Schema error while trying to retrieve object " + ref.getOid()
						+ " : " + e.getMessage(), e);
				logger.error(
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
			return;
		}
		if (filter == null) {
			// No OID and no filter. We are lost.
			result.recordFatalError("Neither OID nor filter for property " + propName
					+ ": cannot resolve reference");
			return;
		}
		// No OID and we have filter. Let's check the filter a bit
		logger.debug("Resolving using filter {}", DOMUtil.serializeDOMToString(filter));
		NodeList childNodes = filter.getChildNodes();
		if (childNodes.getLength() == 0) {
			result.recordFatalError("OID not specified and filter is empty for property " + propName);
			return;
		}
		// Let's do resolving
		QueryType query = new QueryType();
		query.setFilter(filter);
		List<? extends ObjectType> objects = null;
		QName objectType = ref.getType();
		if (objectType==null) {
			result.recordFatalError("Missing definition of type of reference " + propName);
			return;
		}
		try {

			objects = repository.searchObjects(ObjectTypes.getObjectTypeFromTypeQName(objectType)
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

	private void encryptValues(ObjectType object, OperationResult objectResult) {
		OperationResult result = objectResult.createSubresult(ObjectImporter.class.getName()+".encryptValues");
		encryptValuesRecursive(object,object,result);
		result.recordSuccessIfUnknown();
	}
		
	private void encryptValuesRecursive(Object object, ObjectType objectType, OperationResult result) {
		// We need to look up all object references. Probably the only efficient
		// way to do it is to use reflection.
		Class<?> type = object.getClass();
		Method[] methods = type.getMethods();
		for (int i = 0; i < methods.length; i++) {
			Method method= methods[i];
			String propName = parseGetterPropName(method);
			if (propName !=null) {
				// It has to return a value in JAXB package or List to be interesting
				Class<?> returnType = method.getReturnType();
				if (JAXBUtil.isJaxbClass(returnType) || returnType.equals(List.class)) {
					Object fieldValue;
					try {
						fieldValue = method.invoke(object);
					} catch (IllegalArgumentException e) {
						result.recordFatalError("Access to field "+propName+" failed (illegal argument): "+e.getMessage(),e);
						return;
					} catch (IllegalAccessException e) {
						result.recordFatalError("Access to field "+propName+" failed (illegal access): "+e.getMessage(),e);
						return;
					} catch (InvocationTargetException e) {
						result.recordFatalError("Access to field "+propName+" failed (invocation target): "+e.getMessage(),e);
						return;
					}
					if (fieldValue==null) {
						continue;
					}
					if (returnType.equals(List.class)) {
						List valueList = (List) fieldValue;
						for (Object value : valueList) {
							if (value != null && JAXBUtil.isJaxbClass(value.getClass())) {
								encryptValueAndRecurse(value,propName,objectType,result);
							}
						}
					} else {
						encryptValueAndRecurse(fieldValue,propName,objectType,result);
					}
				}
			}
		}
	}

	private void encryptValueAndRecurse(Object value, String propName, ObjectType objectType, OperationResult result) {
		if (value instanceof ProtectedStringType) {
			ProtectedStringType ps = (ProtectedStringType) value;
			if (ps.getClearValue()!=null) {
				try {
					logger.info("Encrypting cleartext value for field "+propName+" while importing "+ObjectTypeUtil.toShortString(objectType));
					protector.encrypt(ps);
				} catch (EncryptionException e) {
					logger.info("Faild to encrypt cleartext value for field "+propName+" while importing "+ObjectTypeUtil.toShortString(objectType));
					result.recordFatalError("Faild to encrypt value for field "+propName+": "+e.getMessage(), e);
					return;
				}
			}
		}
		encryptValuesRecursive(value,objectType,result);
	}
	
	private String parseGetterPropName(Method method) {
		if (method.getParameterTypes().length!=0) {
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
	
}
