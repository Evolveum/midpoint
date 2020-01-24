package com.evolveum.midpoint.rest.impl;

import com.evolveum.midpoint.model.impl.ModelRestService;
import com.evolveum.midpoint.model.impl.util.RestServiceUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest2")
public class ModelRestController {

    private static final Trace LOGGER = TraceManager.getTrace(ModelRestService.class);

    @Autowired private TaskManager taskManager;

    @GetMapping(
            value = "/self",
            produces = {
                    MediaType.APPLICATION_XML_VALUE,
                    MediaType.APPLICATION_JSON_UTF8_VALUE,
                    RestServiceUtil.APPLICATION_YAML
            })
    public String getSelf() { //@Context MessageContext mc){
        LOGGER.debug("model rest service for get operation start");
//        Task task = initRequest(mc);
//        OperationResult parentResult = task.getResult().createSubresult(OPERATION_SELF);
//        Response response;

        try {
            UserType loggedInUser = SecurityUtil.getPrincipal().getUser();
            System.out.println("loggedInUser = " + loggedInUser);
//            PrismObject<UserType> user = model.getObject(UserType.class, loggedInUser.getOid(), null, task, parentResult);
//            response = RestServiceUtil.createResponse(Response.Status.OK, user, parentResult, true);
//            parentResult.recordSuccessIfUnknown();
        } catch (SecurityViolationException e) {
//            response = RestServiceUtil.handleException(parentResult, e);
            e.printStackTrace();
            return e.getMessage();
        }

        //        finishRequest(task, mc.getHttpServletRequest());
//        return response;
        return "yes";
    }

}
