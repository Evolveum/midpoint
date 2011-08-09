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
import java.util.List;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationConstants;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.validator.ObjectHandler;
import com.evolveum.midpoint.validator.ValidationMessage;
import com.evolveum.midpoint.validator.ValidationMessage.Type;
import com.evolveum.midpoint.validator.Validator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

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
	
				@Override
				public void handleObject(ObjectType object, List<ValidationMessage> objectErrors) {
	
					OperationResult result = parentResult.createSubresult(OperationConstants.IMPORT_OBJECT);
					// TODO: params, context
					
					boolean shouldContinue = applyValidationMessages(objectErrors, object,result);
					
					if (shouldContinue) {
						
						shouldContinue = resolveReferences(object,repository,result);
					
						if (shouldContinue) {
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
		
		List<ValidationMessage> messages = validator.validate(input);
	}

	protected static boolean resolveReferences(ObjectType object, RepositoryService repository, OperationResult result) {
		// TODO Auto-generated method stub
		return true;
	}

	protected static boolean applyValidationMessages(List<ValidationMessage> objectErrors, ObjectType object, OperationResult result) {
		boolean shouldContinue = true;
		for (ValidationMessage message: objectErrors) {
			if (message.getType() == Type.ERROR) {
				result.recordFatalError(message.toString());
				result.appendDetail(message.toString());
				shouldContinue = false;
			} else {
				result.recordPartialError(message.toString());
				result.appendDetail(message.toString());
			}
		}
		return shouldContinue;
	}
}
