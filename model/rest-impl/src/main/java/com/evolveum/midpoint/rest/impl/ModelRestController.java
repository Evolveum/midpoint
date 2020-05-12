package com.evolveum.midpoint.rest.impl;

import static com.evolveum.midpoint.model.impl.ModelRestService.OPERATION_SELF;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.ModelRestService;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@RestController
@RequestMapping(value = "/rest2") //, produces = "*/*", consumes = "*/*")
public class ModelRestController {

    private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);

    @Autowired private TaskManager taskManager;
    @Autowired private ModelCrudService model;
    @Autowired private SecurityHelper securityHelper;

    @GetMapping("/self")
    public ResponseEntity<?> getSelf(HttpServletRequest request) {
        //@Context MessageContext mc){ TODO: do we need it in init request in new era?
        LOGGER.debug("model rest service for get operation start");
        // uses experimental version, does not require CXF/JAX-RS
        Task task = RestServiceUtil.initRequest(taskManager);
        OperationResult parentResult = task.getResult().createSubresult(OPERATION_SELF);
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
            response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

        finishRequest(task, request);
        return response;
    }

    public static <T> ResponseEntity<?> createResponse(
            HttpStatus statusCode, T body, OperationResult result, boolean sendOriginObjectIfNotSuccess) {
        result.computeStatusIfUnknown();

//        if (result.isPartialError()) {
//            return createBody(Response.status(250), sendOriginObjectIfNotSuccess, body, result);
//        } else if (result.isHandledError()) {
//            return createBody(Response.status(240), sendOriginObjectIfNotSuccess, body, result);
//        }

        return ResponseEntity.status(statusCode).body(body);
    }

//    private static <T> ResponseEntity<?> createBody(
//            Response.ResponseBuilder builder, boolean sendOriginObjectIfNotSuccess, T body, OperationResult result) {
//        if (sendOriginObjectIfNotSuccess) {
//            return builder.entity(body);
//        }
//        return builder.entity(result);
//    }

    private void finishRequest(Task task, HttpServletRequest request) {
//        if (isExperimentalEnabled()) {
//            auditEvent(request);
//            SecurityContextHolder.getContext().setAuthentication(null);
//        } else {
        RestServiceUtil.finishRequest(task, securityHelper);
//        }
    }
}
