package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.springframework.expression.spel.ast.Assign;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Honchar.
 */
public class RequestAssignmentTabPanel<F extends FocusType> extends AbstractObjectTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = RequestAssignmentTabPanel.class.getName();
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final Trace LOGGER = TraceManager.getTrace(RequestAssignmentTabPanel.class);
    private static final String ID_MAIN_PANEL = "mainPanel";

    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
    private PrismObject<UserType> user;

    public RequestAssignmentTabPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusWrapperModel,
                                    LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase page) {
        super(id, mainForm, focusWrapperModel, page);
        this.assignmentsModel = assignmentsModel;
//        loadUser();
        initLayout(focusWrapperModel);
    }

    private void initLayout(LoadableModel<ObjectWrapper<F>> focusWrapperModel) {
    	Class targetFocusClass = getObjectWrapper().getObject().getCompileTimeClass();
        MultipleAssignmentSelectorPanel<F, UserType, RoleType> panel = new MultipleAssignmentSelectorPanel<F, UserType, RoleType>(ID_MAIN_PANEL,
                assignmentsModel, focusWrapperModel.getObject().getObject(), targetFocusClass, RoleType.class);
        add(panel);
    }

    private void loadUser() {
        LOGGER.debug("Loading user and accounts.");
        MyPasswordsDto dto = new MyPasswordsDto();
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        try {
            String userOid = SecurityUtils.getPrincipalUser().getOid();
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_USER);
            user = getPageBase().getModelService().getObject(UserType.class, userOid, null, task, result);
            result.recordSuccessIfUnknown();

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
    }

}
