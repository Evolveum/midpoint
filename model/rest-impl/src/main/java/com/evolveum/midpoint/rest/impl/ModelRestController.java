package com.evolveum.midpoint.rest.impl;

import static org.springframework.http.ResponseEntity.status;

import static com.evolveum.midpoint.model.impl.util.RestServiceUtil.initRequest;

import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.ModelRestService;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.DefinitionProcessingOption;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@RestController
@RequestMapping(value = "/rest2")
public class ModelRestController {

    private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);

    private static final String CURRENT = "current";
    private static final String VALIDATE = "validate";

    @Autowired private TaskManager taskManager;
    @Autowired private ModelCrudService model;
    @Autowired private ModelInteractionService modelInteraction;
    @Autowired private PrismContext prismContext;
    @Autowired private SecurityHelper securityHelper;

    @PostMapping("/{type}/{oid}/generate")
    public ResponseEntity<?> generateValue(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest(taskManager);
        OperationResult parentResult = createSubresult(task, "generateValue");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);

        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, parentResult);
            response = generateValue(object, policyItemsDefinition, task, parentResult);
        } catch (Exception ex) {
            parentResult.computeStatus();
            response = handleException(parentResult, ex);
        }

        finishRequest(task);
        return response;
    }

    @PostMapping("/rpc/generate")
    public ResponseEntity<?> generateValueRpc(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {
        Task task = initRequest(taskManager);
        OperationResult parentResult = task.getResult().createSubresult("generateValueRpc");

        ResponseEntity<?> response = generateValue(null, policyItemsDefinition, task, parentResult);
        finishRequest(task);

        return response;
    }

    private <O extends ObjectType> ResponseEntity<?> generateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
            Task task, OperationResult parentResult) {

        ResponseEntity<?> response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", parentResult);
        } else {
            try {
                modelInteraction.generateValue(object, policyItemsDefinition, task, parentResult);
                parentResult.computeStatusIfUnknown();
                if (parentResult.isSuccess()) {
                    response = createResponse(HttpStatus.OK, policyItemsDefinition, parentResult, true);
                } else {
                    response = createResponse(HttpStatus.BAD_REQUEST, parentResult, parentResult);
                }

            } catch (Exception ex) {
                parentResult.recordFatalError("Failed to generate value, " + ex.getMessage(), ex);
                response = handleException(parentResult, ex);
            }
        }
        return response;
    }

    @PostMapping("/{type}/{oid}/validate")
    public ResponseEntity<?> validateValue(
            @PathVariable("type") String type,
            @PathVariable("oid") String oid,
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {

        Task task = initRequest(taskManager);
        OperationResult parentResult = task.getResult().createSubresult("validateValue");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object = model.getObject(clazz, oid, null, task, parentResult);
            response = validateValue(object, policyItemsDefinition, task, parentResult);
        } catch (Exception ex) {
            parentResult.computeStatus();
            response = handleException(parentResult, ex);
        }

        finishRequest(task);
        return response;
    }

    @PostMapping("/rpc/validate")
    public ResponseEntity<?> validateValue(
            @RequestBody PolicyItemsDefinitionType policyItemsDefinition) {
        Task task = initRequest(taskManager);
        OperationResult parentResult = task.getResult().createSubresult("validateValue");

        ResponseEntity<?> response = validateValue(null, policyItemsDefinition, task, parentResult);
        finishRequest(task);
        return response;
    }

    private <O extends ObjectType> ResponseEntity<?> validateValue(
            PrismObject<O> object, PolicyItemsDefinitionType policyItemsDefinition,
            Task task, OperationResult parentResult) {
        ResponseEntity<?> response;
        if (policyItemsDefinition == null) {
            response = createBadPolicyItemsDefinitionResponse("Policy items definition must not be null", parentResult);
            finishRequest(task);
            return response;
        }

        if (CollectionUtils.isEmpty(policyItemsDefinition.getPolicyItemDefinition())) {
            response = createBadPolicyItemsDefinitionResponse("No definitions for items", parentResult);
            finishRequest(task);
            return response;
        }

        try {
            modelInteraction.validateValue(object, policyItemsDefinition, task, parentResult);

            parentResult.computeStatusIfUnknown();
            if (parentResult.isAcceptable()) {
                response = createResponse(HttpStatus.OK, policyItemsDefinition, parentResult, true);
            } else {
                response = ResponseEntity.status(HttpStatus.CONFLICT).body(parentResult);
            }

        } catch (Exception ex) {
            parentResult.computeStatus();
            response = handleException(parentResult, ex);
        }

        return response;
    }

    private ResponseEntity<?> createBadPolicyItemsDefinitionResponse(
            String message, OperationResult parentResult) {
        LOGGER.error(message);
        parentResult.recordFatalError(message);
        return status(HttpStatus.BAD_REQUEST).body(parentResult);
    }

    @GetMapping("/users/{id}/policy")
    public ResponseEntity<?> getValuePolicyForUser(
            @PathVariable("id") String oid) {
        LOGGER.debug("getValuePolicyForUser start");

        Task task = initRequest(taskManager);
        OperationResult parentResult = task.getResult().createSubresult("getValuePolicyForUser");

        ResponseEntity<?> response;
        try {
            Collection<SelectorOptions<GetOperationOptions>> options =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());
            PrismObject<UserType> user = model.getObject(UserType.class, oid, options, task, parentResult);

            CredentialsPolicyType policy = modelInteraction.getCredentialsPolicy(user, task, parentResult);

            response = createResponse(HttpStatus.OK, policy, parentResult);
        } catch (Exception ex) {
            response = handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task);

        LOGGER.debug("getValuePolicyForUser finish");
        return response;
    }

    @GetMapping("/{type}/{id}")
    public ResponseEntity<?> getObject(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames,
            HttpServletRequest request) {
        LOGGER.debug("model rest service for get operation start");

        Task task = initRequest(taskManager);
        OperationResult parentResult = createSubresult(task, "getObject");

        Class<? extends ObjectType> clazz = ObjectTypes.getClassFromRestType(type);
        Collection<SelectorOptions<GetOperationOptions>> getOptions =
                GetOperationOptions.fromRestOptions(options, include, exclude,
                        resolveNames, DefinitionProcessingOption.ONLY_IF_EXISTS, prismContext);

        ResponseEntity<?> response;
        try {
            PrismObject<? extends ObjectType> object;
            if (NodeType.class.equals(clazz) && CURRENT.equals(id)) {
                String nodeId = taskManager.getNodeId();
                ObjectQuery query = prismContext.queryFor(NodeType.class)
                        .item(NodeType.F_NODE_IDENTIFIER).eq(nodeId)
                        .build();
                List<PrismObject<NodeType>> objects = model.searchObjects(NodeType.class, query, getOptions, task, parentResult);
                if (objects.isEmpty()) {
                    throw new ObjectNotFoundException("Current node (id " + nodeId + ") couldn't be found.");
                } else if (objects.size() > 1) {
                    throw new IllegalStateException("More than one 'current' node (id " + nodeId + ") found.");
                } else {
                    object = objects.get(0);
                }
            } else {
                object = model.getObject(clazz, id, getOptions, task, parentResult);
            }
            removeExcludes(object, exclude);        // temporary measure until fixed in repo

            response = createResponse(HttpStatus.OK, object, parentResult);
        } catch (Exception ex) {
            response = handleException(parentResult, ex);
        }

        parentResult.computeStatus();
        finishRequest(task);
        return response;
    }

    private void removeExcludes(PrismObject<? extends ObjectType> object, List<String> exclude)
            throws SchemaException {
        object.getValue().removePaths(ItemPathCollectionsUtil.pathListFromStrings(exclude, prismContext));
    }

    @GetMapping("/self")
    public ResponseEntity<?> getSelf(HttpServletRequest request) {
        //@Context MessageContext mc){ TODO: do we need it in init request in new era?
        LOGGER.debug("model rest service for get operation start");
        // uses experimental version, does not require CXF/JAX-RS
        Task task = initRequest(taskManager);
        OperationResult parentResult = createSubresult(task, "self");
        ResponseEntity<?> response;

        try {
            FocusType loggedInUser = SecurityUtil.getPrincipal().getFocus();
            System.out.println("loggedInUser = " + loggedInUser);
            PrismObject<UserType> user = model.getObject(UserType.class, loggedInUser.getOid(), null, task, parentResult);
            response = createResponse(HttpStatus.OK, user, parentResult, true);
            parentResult.recordSuccessIfUnknown();
        } catch (SecurityViolationException | ObjectNotFoundException | SchemaException |
                CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            e.printStackTrace();
            response = status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        finishRequest(task);
        return response;
    }

    private static final String OP_NAME_PREFIX = ModelRestController.class.getName() + ".";

    private OperationResult createSubresult(Task task, String operation) {
        return task.getResult().createSubresult(OP_NAME_PREFIX + operation);
    }

    public <T> ResponseEntity<?> createResponse(
            HttpStatus httpStatus, T body, OperationResult result) {
        return createResponse(httpStatus, body, result, false);
    }

    public <T> ResponseEntity<?> createResponse(HttpStatus httpStatus,
            T body, OperationResult result, boolean sendOriginObjectIfNotSuccess) {
        result.computeStatusIfUnknown();

        if (result.isPartialError()) {
            return createBody(status(250), sendOriginObjectIfNotSuccess, body, result);
        } else if (result.isHandledError()) {
            return createBody(status(240), sendOriginObjectIfNotSuccess, body, result);
        }

        return status(httpStatus).body(body);
    }

    private <T> ResponseEntity<?> createBody(ResponseEntity.BodyBuilder builder,
            boolean sendOriginObjectIfNotSuccess, T body, OperationResult result) {
        if (sendOriginObjectIfNotSuccess) {
            return builder.body(body);
        }
        return builder.body(result);
    }

    public ResponseEntity<?> handleException(OperationResult result, Throwable t) {
        LoggingUtils.logUnexpectedException(LOGGER, "Got exception while servicing REST request: {}", t,
                result != null ? result.getOperation() : "(null)");
        return handleExceptionNoLog(result, t);
    }

    public ResponseEntity<?> handleExceptionNoLog(OperationResult result, Throwable t) {
        return createErrorResponseBuilder(result, t);
    }

    public static ResponseEntity<?> createErrorResponseBuilder(OperationResult result, Throwable t) {
        if (t instanceof ObjectNotFoundException) {
            return createErrorResponseBuilder(HttpStatus.NOT_FOUND, result);
        }

        if (t instanceof CommunicationException || t instanceof TunnelException) {
            return createErrorResponseBuilder(HttpStatus.GATEWAY_TIMEOUT, result);
        }

        if (t instanceof SecurityViolationException) {
            return createErrorResponseBuilder(HttpStatus.FORBIDDEN, result);
        }

        if (t instanceof ConfigurationException) {
            return createErrorResponseBuilder(HttpStatus.BAD_GATEWAY, result);
        }

        if (t instanceof SchemaException || t instanceof ExpressionEvaluationException) {
            return createErrorResponseBuilder(HttpStatus.BAD_REQUEST, result);
        }

        if (t instanceof PolicyViolationException
                || t instanceof ObjectAlreadyExistsException
                || t instanceof ConcurrencyException) {
            return createErrorResponseBuilder(HttpStatus.CONFLICT, result);
        }

        return createErrorResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR, result);
    }

    public static ResponseEntity<?> createErrorResponseBuilder(
            HttpStatus status, OperationResult result) {
        OperationResultType resultBean;
        if (result != null) {
            result.computeStatusIfUnknown();
            resultBean = result.createOperationResultType();
        } else {
            resultBean = null;
        }
        return status(status).body(resultBean);
    }

    private void finishRequest(Task task) {
        // TODO what level of auditing do we want anyway?
//        if (isExperimentalEnabled()) {
//            auditEvent(request);
//            SecurityContextHolder.getContext().setAuthentication(null);
//        } else {
        RestServiceUtil.finishRequest(task, securityHelper);
//        }
    }

    // TODO just for debug until exception handling is not up'n'running
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> defaultHandler(
            Exception ex, HttpServletRequest request, HttpServletResponse response) {
        LOGGER.warn("Unexpected exception causing HTTP 500", ex);
        return null;
    }
}
