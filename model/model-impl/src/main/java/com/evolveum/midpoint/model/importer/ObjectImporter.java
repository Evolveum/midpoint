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

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.result.OperationConstants;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.validator.ObjectHandler;
import com.evolveum.midpoint.validator.ValidationMessage;
import com.evolveum.midpoint.validator.ValidationMessage.Type;
import com.evolveum.midpoint.validator.Validator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;

/**
 * Extension of validator used to import objects to the repository.
 * 
 * In addition to validating the objects the importer also tries to resolve the
 * references and may also do other repository-related stuff.
 * 
 * @author Radovan Semancik
 * 
 */
public class ObjectImporter {
	
	private static final Trace logger = TraceManager.getTrace(ObjectImporter.class);

	public static void importObjects(InputStream input, final Task task, final OperationResult parentResult, final RepositoryService repository) {
		
		Validator validator = new Validator(

			new ObjectHandler() {
				
				long progress = 0;
	
				@Override
				public void handleObject(ObjectType object, List<ValidationMessage> objectErrors) {
	
					progress++;
					
					logger.debug("Starting import of object {}",ObjectTypeUtil.toShortString(object));
					
					OperationResult result = parentResult.createSubresult(OperationConstants.IMPORT_OBJECT);
					result.addParam(OperationResult.PARAM_OBJECT, object);
					result.addContext(OperationResult.CONTEXT_PROGRESS, progress);
					// TODO: params, context
					
					applyValidationMessages(objectErrors, object,result);
					
					if (result.isAcceptable()) {
						
						resolveReferences(object,repository,result);
					
						if (result.isAcceptable()) {
							try {
					
								repository.addObject(object, result);
								result.recordSuccess();
								
							} catch (ObjectAlreadyExistsException e) {
								result.recordFatalError("Object already exists",e);
								logger.error("Object already exists",e);
							} catch (SchemaException e) {
								result.recordFatalError("Schema violation",e);
								logger.error("Schema violation",e);
							} catch (RuntimeException e) {
								result.recordFatalError("Unexpected problem", e);
								logger.error("Unexpected problem", e);
							}
							
						}
					}
					
					// TODO check if there are too many errors
	
				}
				
			});
		
		validator.setVerbose(true);
		
		List<ValidationMessage> messages = validator.validate(input);
		
		for (ValidationMessage message: messages) {
			if (message.getType() == Type.ERROR) {
				logger.error("Import: {}",message);
			} else {
				logger.warn("Import: {}",message);
			}
		}
		
		applyValidationMessages(messages, null, parentResult);
	}

	protected static void resolveReferences(ObjectType object, RepositoryService repository, OperationResult result) {
		// We need to look up all object references. Probably the only efficient way to do it is to use reflection.
		Class type = object.getClass();
		Method[] methods = type.getMethods();
		for(int i=0;i<methods.length;i++) {
			Method method = methods[i];
			Class returnType = method.getReturnType();
			if (ObjectReferenceType.class.isAssignableFrom(returnType)) {
				// we have a method that returns ObjectReferenceType, try to resolve it.
				if (method.getName().startsWith("get")) {
					String suffix = method.getName().substring(3);
					String propName = suffix.substring(0, 1).toLowerCase()+suffix.substring(1);
					logger.debug("Found reference property {}, method {}",propName,method.getName());
					try {
						Object returnVal = method.invoke(object);
						ObjectReferenceType ref = (ObjectReferenceType) returnVal;
						resolveRef(ref, propName, repository, result);
						if (!result.isAcceptable()) {
							logger.error("Error resolving reference {}: {}",propName,result.getMessage());
							return;
						}
					} catch (IllegalArgumentException e) {
						// Should not happen, getters have no arguments
						result.recordFatalError("Cannot invoke getter "+method.getName()+" due to IllegalArgumentException",e);
						return;
					} catch (IllegalAccessException e) {
						// Should not happen, getters have no arguments
						result.recordFatalError("Cannot invoke getter "+method.getName()+" due to IllegalAccessException",e);
						return;
					} catch (InvocationTargetException e) {
						// Should not happen, getters have no arguments
						result.recordFatalError("Cannot invoke getter "+method.getName()+" due to InvocationTargetException",e);
						return;
					}
					logger.debug("Reference {} processed",propName);
				}
			}
		}
	}

	private static void resolveRef(ObjectReferenceType ref, String propName, RepositoryService repository, OperationResult result) {
		if (ref==null) {
			// Nothing to do
			return;
		}
		Element filter = ref.getFilter();
		if (ref.getOid()!=null && !ref.getOid().isEmpty()) {
			// We have OID
			if (filter!=null) {
				// We have both filter and OID. We will choose OID, but let's at least log a warning
				result.appendDetail("Both OID and filter for property "+propName);
				result.recordPartialError("Both OID and filter for property "+propName);
				ref.setFilter(null);
			}
			// Nothing to resolve
			return;
		}
		if (filter==null) {
			// No OID and no filter. We are lost.
			result.recordFatalError("Neither OID nor filter for property "+propName+": cannot resolve reference");
			return;
		}
		// No OID and we have filter. Let's check the filter a bit
		logger.debug("Resolving using filter {}",DOMUtil.serializeDOMToString(filter));
		NodeList childNodes = filter.getChildNodes();
		if(childNodes.getLength()==0) {
			result.recordFatalError("OID not specified and filter is empty for property "+propName);
			return;
		}
		// Let's do resolving
		QueryType query = new QueryType();
		query.setFilter(filter);
		List<? extends ObjectType> objects = null;
		try {
		
			objects = repository.searchObjects(ObjectTypes.getObjectTypeFromTypeQName(ref.getType())
					.getClassDefinition(),query, null, result);
		
		} catch (SchemaException e) {
			// This is unexpected, but may happen. Record fatal error
			result.recordFatalError("Repository schema error during resolution of reference "+propName,e);
			return;
		} catch (SystemException e) {
			// We don't want this to tear down entire import.
			result.recordFatalError("Repository system error during resolution of reference "+propName,e);
			return;
		}
		if (objects.isEmpty()) {
			result.recordFatalError("Repository reference "+propName+" cannot be resolved: filter matches no object");
			return;
		}
		if (objects.size()>1) {
			result.recordFatalError("Repository reference "+propName+" cannot be resolved: filter matches "+objects.size()+" objects");
			return;
		}
		// Bingo. We have exactly one object.
		String oid = objects.get(0).getOid();
		ref.setOid(oid);
	}

	protected static void applyValidationMessages(List<ValidationMessage> objectErrors, ObjectType object, OperationResult result) {
		for (ValidationMessage message: objectErrors) {
			if (message.getType() == Type.ERROR) {
				result.recordFatalError(message.toString());
				result.appendDetail(message.toString());
			} else {
				result.recordPartialError(message.toString());
				result.appendDetail(message.toString());
			}
		}
	}
}
