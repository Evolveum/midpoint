/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.model.api.*;
import com.evolveum.midpoint.model.common.util.AbstractModelWebService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.*;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsResponseType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ExecuteScriptsType;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineDataType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineItemType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionEvaluationOptionsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * @author lazyman
 * 
 */
@Service
public class ModelWebService extends AbstractModelWebService implements ModelPortType, ModelPort {

	private static final Trace LOGGER = TraceManager.getTrace(ModelWebService.class);
	
	@Autowired(required = true)
	private ModelCrudService model;
	
    @Autowired
    private ScriptingService scriptingService;

	@Override
	public void getObject(QName objectType, String oid, SelectorQualifiedGetOptionsType optionsType,
			Holder<ObjectType> objectHolder, Holder<OperationResultType> resultHolder) throws FaultMessage {
        notNullArgument(objectType, "Object type must not be null.");
		notEmptyArgument(oid, "Oid must not be null or empty.");

		Task task = createTaskInstance(GET_OBJECT);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
            Class objectClass = ObjectTypes.getObjectTypeFromTypeQName(objectType).getClassDefinition();
            Collection<SelectorOptions<GetOperationOptions>> options = MiscSchemaUtil.optionsTypeToOptions(optionsType);
            PrismObject<? extends ObjectType> object = model.getObject(objectClass, oid, options, task, operationResult);
			handleOperationResult(operationResult, resultHolder);
			objectHolder.value = object.asObjectable();
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL getObject() failed", ex);
			throwFault(ex, operationResult);
		} finally {
			auditLogout(task);
		}
	}

	@Override
	public void searchObjects(QName objectType, QueryType query, SelectorQualifiedGetOptionsType optionsType,
                  Holder<ObjectListType> objectListHolder, Holder<OperationResultType> result) throws FaultMessage {
        notNullArgument(objectType, "Object type must not be null.");

		Task task = createTaskInstance(SEARCH_OBJECTS);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
            Class objectClass = ObjectTypes.getObjectTypeFromTypeQName(objectType).getClassDefinition();
            Collection<SelectorOptions<GetOperationOptions>> options = MiscSchemaUtil.optionsTypeToOptions(optionsType);
			ObjectQuery q = QueryJaxbConvertor.createObjectQuery(objectClass, query, prismContext);
			List<PrismObject<? extends ObjectType>> list = (List)model.searchObjects(objectClass, q, options, task, operationResult);
			handleOperationResult(operationResult, result);
			ObjectListType listType = new ObjectListType();
			for (PrismObject<? extends ObjectType> o : list) {
				listType.getObject().add(o.asObjectable());
			}
			objectListHolder.value = listType;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL searchObjects() failed", ex);
			throwFault(ex, operationResult);
		} finally {
			auditLogout(task);
		}
	}

    @Override
    public ObjectDeltaOperationListType executeChanges(ObjectDeltaListType deltaList, ModelExecuteOptionsType optionsType) throws FaultMessage {
		notNullArgument(deltaList, "Object delta list must not be null.");

		Task task = createTaskInstance(EXECUTE_CHANGES);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			Collection<ObjectDelta> deltas = DeltaConvertor.createObjectDeltas(deltaList, prismContext);
            for (ObjectDelta delta : deltas) {
                prismContext.adopt(delta);
            }
            ModelExecuteOptions options = ModelExecuteOptions.fromModelExecutionOptionsType(optionsType);
            Collection<ObjectDeltaOperation<? extends ObjectType>> objectDeltaOperations = modelService.executeChanges((Collection) deltas, options, task, operationResult);        // brutally eliminating type-safety compiler barking
			ObjectDeltaOperationListType retval = new ObjectDeltaOperationListType();
            for (ObjectDeltaOperation objectDeltaOperation : objectDeltaOperations) {
                ObjectDeltaOperationType objectDeltaOperationType = DeltaConvertor.toObjectDeltaOperationType(objectDeltaOperation, null);
                retval.getDeltaOperation().add(objectDeltaOperationType);
            }
            return retval;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL executeChanges() failed", ex);
			throwFault(ex, operationResult);
			// notreached
			return null;
		} finally {
			auditLogout(task);
		}
	}

	@Override
	public void findShadowOwner(String accountOid, Holder<UserType> userHolder, Holder<OperationResultType> result)
			throws FaultMessage {
		notEmptyArgument(accountOid, "Account oid must not be null or empty.");

		Task task = createTaskInstance(LIST_ACCOUNT_SHADOW_OWNER);
		auditLogin(task);
		OperationResult operationResult = task.getResult();
		try {
			PrismObject<UserType> user = model.findShadowOwner(accountOid, task, operationResult);
			handleOperationResult(operationResult, result);
			if (user != null) {
				userHolder.value = user.asObjectable();
			}
			return;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL findShadowOwner() failed", ex);
			throwFault(ex, operationResult);
		} finally {
			auditLogout(task);
		}
	}

	@Override
	public OperationResultType testResource(String resourceOid) throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");

		Task task = createTaskInstance(TEST_RESOURCE);
		auditLogin(task);
		try {
			OperationResult testResult = model.testResource(resourceOid, task);
			return handleOperationResult(testResult);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL testResource() failed", ex);
			OperationResult faultResult = new OperationResult(TEST_RESOURCE);
			faultResult.recordFatalError(ex);
			throwFault(ex, faultResult);
			// notreached
			return null;
		} finally {
			auditLogout(task);
		}
	}

    @Override
    public ExecuteScriptsResponseType executeScripts(ExecuteScriptsType parameters) throws FaultMessage {
        Task task = createTaskInstance(EXECUTE_SCRIPTS);
        auditLogin(task);
        OperationResult result = task.getResult();
        try {
            List<JAXBElement<?>> scriptsToExecute = parseScripts(parameters);
            return doExecuteScripts(scriptsToExecute, parameters.getOptions(), task, result);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "# MODEL executeScripts() failed", ex);
            throwFault(ex, null);
            // notreached
         	return null;
		} finally {
			auditLogout(task);
        }
    }

    private List<JAXBElement<?>> parseScripts(ExecuteScriptsType parameters) throws JAXBException, SchemaException {
        List<JAXBElement<?>> scriptsToExecute = new ArrayList<>();
        if (parameters.getXmlScripts() != null) {
            for (Object scriptAsObject : parameters.getXmlScripts().getAny()) {
                if (scriptAsObject instanceof JAXBElement) {
                    scriptsToExecute.add((JAXBElement) scriptAsObject);
                } else {
                    throw new IllegalArgumentException("Invalid script type: " + scriptAsObject.getClass());
                }
            }
        } else {
            // here comes MSL script decoding (however with a quick hack to allow passing XML as text here)
            String scriptsAsString = parameters.getMslScripts();
            if (scriptsAsString.startsWith("<?xml")) {
                PrismProperty expressionType = (PrismProperty) prismContext.parserFor(scriptsAsString).xml().parseItem();
                if (expressionType.size() != 1) {
                    throw new IllegalArgumentException("Unexpected number of scripting expressions at input: " + expressionType.size() + " (expected 1)");
                }
                scriptsToExecute.add(expressionType.getAnyValue().toJaxbElement());
            }
        }
        return scriptsToExecute;
    }

    private ExecuteScriptsResponseType doExecuteScripts(List<JAXBElement<?>> scriptsToExecute, ExecuteScriptsOptionsType options, Task task, OperationResult result) {
        ExecuteScriptsResponseType response = new ExecuteScriptsResponseType();
        ScriptOutputsType outputs = new ScriptOutputsType();
        response.setOutputs(outputs);

        try {
            for (JAXBElement<?> script : scriptsToExecute) {
            	
            	Object scriptValue = script.getValue();
            	if (!(scriptValue instanceof ScriptingExpressionType)) {
            		throw new SchemaException("Expected that scripts will be of type ScriptingExpressionType, but it was "+scriptValue.getClass().getName());
            	}

                ScriptExecutionResult executionResult = scriptingService.evaluateExpression((ScriptingExpressionType) script.getValue(), task, result);

                SingleScriptOutputType output = new SingleScriptOutputType();
                outputs.getOutput().add(output);

                output.setTextOutput(executionResult.getConsoleOutput());
                if (options == null || options.getOutputFormat() == null || options.getOutputFormat() == OutputFormatType.XML) {
                    output.setDataOutput(prepareXmlData(executionResult.getDataOutput(), null));
                } else {
                    // temporarily we send serialized XML in the case of MSL output
                    PipelineDataType jaxbOutput = prepareXmlData(executionResult.getDataOutput(), null);
                    output.setMslData(prismContext.xmlSerializer().serializeAnyData(jaxbOutput, SchemaConstants.C_VALUE));
                }
            }
            result.computeStatusIfUnknown();
        } catch (ScriptExecutionException|JAXBException|SchemaException|RuntimeException|SecurityViolationException e) {
            result.recordFatalError(e.getMessage(), e);
            LoggingUtils.logException(LOGGER, "Exception while executing script", e);
        }
        result.summarize();
        response.setResult(result.createOperationResultType());
        return response;
    }

    public static PipelineDataType prepareXmlData(List<PipelineItem> output,
			ScriptingExpressionEvaluationOptionsType options) throws JAXBException, SchemaException {
		boolean hideResults = options != null && Boolean.TRUE.equals(options.isHideOperationResults());
        PipelineDataType rv = new PipelineDataType();
        if (output != null) {
            for (PipelineItem item: output) {
				PipelineItemType itemType = new PipelineItemType();
				PrismValue value = item.getValue();
				if (value instanceof PrismReferenceValue) {
					// This is a bit of hack: value.getRealValue() would return unserializable object (PRV$1 - does not have type QName)
					ObjectReferenceType ort = new ObjectReferenceType();
					ort.setupReferenceValue((PrismReferenceValue) value);
					itemType.setValue(ort);
				} else {
					itemType.setValue(value.getRealValue());                        // TODO - ok?
				}
				if (!hideResults) {
					itemType.setResult(item.getResult().createOperationResultType());
				}
				rv.getItem().add(itemType);
            }
        }
        return rv;
    }

	private void handleOperationResult(OperationResult result, Holder<OperationResultType> holder) {
		result.recordSuccess();
		OperationResultType resultType = result.createOperationResultType();
		if (holder.value == null) {
			holder.value = resultType;
		} else {
			holder.value.getPartialResults().add(resultType);
		}
	}

	private OperationResultType handleOperationResult(OperationResult result) {
		result.recordSuccess();
		return result.createOperationResultType();
	}
	
	private void notNullResultHolder(Holder<OperationResultType> holder) throws FaultMessage {
		notNullArgument(holder, "Holder must not be null.");
		notNullArgument(holder.value, "Result type must not be null.");
	}

	private <T> void notNullHolder(Holder<T> holder) throws FaultMessage {
		notNullArgument(holder, "Holder must not be null.");
		notNullArgument(holder.value, holder.getClass().getSimpleName() + " must not be null (in Holder).");
	}

	private void notEmptyArgument(String object, String message) throws FaultMessage {
		if (StringUtils.isEmpty(object)) {
			throw createIllegalArgumentFault(message);
		}
	}

	private void notNullArgument(Object object, String message) throws FaultMessage {
		if (object == null) {
			throw createIllegalArgumentFault(message);
		}
	}

	public FaultMessage createIllegalArgumentFault(String message) {
		FaultType faultType = new IllegalArgumentFaultType();
		return new FaultMessage(message, faultType);
	}

    public void throwFault(Throwable ex, OperationResult result) throws FaultMessage {
		if (result != null) {
			result.recordFatalError(ex.getMessage(), ex);
		}

		FaultType faultType;
		if (ex instanceof ObjectNotFoundException) {
			faultType = new ObjectNotFoundFaultType();
		} else if (ex instanceof IllegalArgumentException) {
			faultType = new IllegalArgumentFaultType();
		} else if (ex instanceof ObjectAlreadyExistsException) {
			faultType = new ObjectAlreadyExistsFaultType();
		} else if (ex instanceof CommunicationException) {
			faultType = new CommunicationFaultType();
		} else if (ex instanceof ConfigurationException) {
			faultType = new ConfigurationFaultType();
		} else if (ex instanceof ExpressionEvaluationException) {
			faultType = new SystemFaultType();
		} else if (ex instanceof SchemaException) {
			faultType = new SchemaViolationFaultType();
		} else if (ex instanceof PolicyViolationException) {
			faultType = new PolicyViolationFaultType();
		} else if (ex instanceof AuthorizationException) {
			throw new Fault(new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION),
					WSSecurityException.ErrorCode.FAILED_AUTHENTICATION.getQName());
		} else if (ex instanceof SecurityViolationException) {
			throw new Fault(new WSSecurityException(WSSecurityException.ErrorCode.FAILURE),
					WSSecurityException.ErrorCode.FAILURE.getQName());
		} else{
			faultType = new SystemFaultType();
		}
		faultType.setMessage(ex.getMessage());
		if (result != null) {
			faultType.setOperationResult(result.createOperationResultType());
		}

		FaultMessage fault = new FaultMessage(ex.getMessage(), faultType, ex);
		LOGGER.trace("Throwing fault message type: {}", faultType.getClass(), fault);
		throw fault;
	}

	@Override
	public TaskType importFromResource(String resourceOid, QName objectClass)
			throws FaultMessage {
		notEmptyArgument(resourceOid, "Resource oid must not be null or empty.");
		notNullArgument(objectClass, "Object class must not be null.");

		Task task = createTaskInstance(IMPORT_FROM_RESOURCE);
		auditLogin(task);
		OperationResult operationResult = task.getResult();

		try {
			model.importFromResource(resourceOid, objectClass, task, operationResult);
			operationResult.computeStatus();
			return handleTaskResult(task);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "# MODEL importFromResource() failed", ex);
			auditLogout(task);
			throwFault(ex, operationResult);
			// notreached
			return null;
		}
	}
	
	@Override
	public TaskType notifyChange(ResourceObjectShadowChangeDescriptionType changeDescription)
			throws FaultMessage {
		// TODO Auto-generated method stub
		notNullArgument(changeDescription, "Change description must not be null");
		LOGGER.trace("notify change started");
		
		Task task = createTaskInstance(NOTIFY_CHANGE);
		OperationResult parentResult = task.getResult();
		
		try {
			model.notifyChange(changeDescription, parentResult, task);
			} catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ObjectAlreadyExistsException | ExpressionEvaluationException | RuntimeException | Error ex) {
				LoggingUtils.logException(LOGGER, "# MODEL notifyChange() failed", ex);
				auditLogout(task);
				throwFault(ex, parentResult);
			}
		
		
		LOGGER.info("notify change ended.");
		LOGGER.info("result of notify change: {}", parentResult.debugDump());
		return handleTaskResult(task);
	}
	
	/**
	 * return appropriate form of taskType (and result) to
	 * return back to a web service caller.
	 * 
	 * @param task
	 */
	private TaskType handleTaskResult(Task task) {
		return task.getTaskPrismObject().asObjectable();
	}
		
}
