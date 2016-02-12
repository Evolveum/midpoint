package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.certification.PageAdminCertification;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by Honchar on 13.01.2016.
 */
@PageDescriptor(url = "/admin/users/request",
        action = {
                @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,         //TODO add new auth path
                        label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                        description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION) })

public class PageAssignmentRequest<F extends FocusType, O extends ObjectType> extends PageAdmin {
    private static final String ID_MULTIPLE_CHOICE_PANEL = "panel";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentRequest.class);

    private static final String DOT_CLASS = PageAssignmentRequest.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE_TYPES = DOT_CLASS + "loadRoleTypes";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_USERS = DOT_CLASS + "loadUsers";
    private LoadableModel<ObjectWrapper<O>> objectModel;


    public PageAssignmentRequest() {
        initLayout();
    }

    private void initLayout() {
    }


    private List<String> createAvailableRoleTypesList() {
        List<String> roleTypes = new ArrayList<>();
        List<? extends DisplayableValue<String>> displayableValues = getLookupDisplayableList();

        if (displayableValues != null) {
            for (DisplayableValue<String> displayable : displayableValues) {
                roleTypes.add(displayable.getLabel());
            }
        }

        return roleTypes;
    }

    private List<? extends DisplayableValue<String>> getLookupDisplayableList() {
        List<DisplayableValue<String>> list = new ArrayList<>();
        ModelInteractionService interactionService = WebComponentUtil.getPageBase(this).getModelInteractionService();
        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE_TYPES);

        try {
            RoleSelectionSpecification roleSpecification = interactionService.getAssignableRoleSpecification(getUserDefinition(), result);
            return roleSpecification.getRoleTypes();

        } catch (SchemaException | ConfigurationException | ObjectNotFoundException e) {
            LOGGER.error("Could not retrieve available role types for search purposes.", e);
            result.recordFatalError("Could not retrieve available role types for search purposes.", e);
        }

        return list;
    }

    protected PrismObject<UserType> getUserDefinition() {
        try {
            return getSecurityEnforcer().getPrincipal().getUser().asPrismObject();
        } catch (SecurityViolationException e) {
            LOGGER.error("Could not retrieve logged user for security evaluation.", e);
        }
        return null;
    }


    private List<AssignmentEditorDto> loadAssignments() {
        List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

//        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);
//
//        ObjectWrapper focusWrapper = getObjectModel().getObject();
//        PrismObject<F> focus = focusWrapper.getObject();
//        List<AssignmentType> assignments = focus.asObjectable().getAssignment();
//        for (AssignmentType assignment : assignments) {
//
//            list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
//        }
//
//        Collections.sort(list);

        return list;


    }

    private List<PrismObject<UserType>> loadUsers() {
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        try {
            List<PrismObject<UserType>> users = WebModelServiceUtils.searchObjects(UserType.class, new ObjectQuery(),
                    result, (PageBase)getPage());

            return users;
        } catch (Exception e) {
            error(getString("pageUsers.message.queryError") + " " + e.getMessage());
            return null;
        }
    }

    protected void initializeModel(final PrismObject<O> objectToEdit) {
        objectModel = new LoadableModel<ObjectWrapper<O>>(false) {

            @Override
            protected ObjectWrapper<O> load() {
//                return loadObjectWrapper(objectToEdit);
                return null;
            }
        };

    }

    private PrismObject<UserType> loadCurrentUser() {
        LOGGER.debug("Loading user");
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        PrismObject<UserType> user = null;
        try {
            String userOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = createSimpleTask(OPERATION_LOAD_USER);
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_USER);
            user = getModelService().getObject(UserType.class, userOid, null, task, subResult);
            subResult.recordSuccessIfUnknown();


            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load accounts", ex);
            result.recordFatalError("Couldn't load accounts", ex);
        } finally {
            result.recomputeStatus();
        }

        if (!result.isSuccess() && !result.isHandledError()) {
            showResult(result);
        }

        return user;
    }

}