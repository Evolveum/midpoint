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

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.login.LoginPanel;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenu;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.message.MainFeedback;
import com.evolveum.midpoint.web.component.message.OpResult;
import com.evolveum.midpoint.web.component.message.TempFeedback;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.wf.WfDataAccessor;
import com.evolveum.midpoint.wf.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.bootstrap.Bootstrap;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.resource.CoreLibrariesContributor;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageBase extends WebPage {

    private static final Trace LOGGER = TraceManager.getTrace(PageBase.class);

    private static final String ID_TITLE = "title";
    private static final String ID_DEBUG_PANEL = "debugPanel";
    private static final String ID_TOP_MENU = "topMenu";
    private static final String ID_LOGIN_PANEL = "loginPanel";
    private static final String ID_PAGE_TITLE = "pageTitle";
    private static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_TEMP_FEEDBACK = "tempFeedback";

    @SpringBean(name = "modelController")
    private ModelService modelService;
    @SpringBean(name = "modelController")
    private ModelInteractionService modelInteractionService;
    @SpringBean(name = "modelDiagController")
    private ModelDiagnosticService modelDiagnosticService;
    @SpringBean(name = "taskManager")
    private TaskManager taskManager;
    @SpringBean(name = "workflowManager")
    private WorkflowManager workflowManager;

    public PageBase() {
        Injector.get().inject(this);
        validateInjection(modelService, "Model service was not injected.");
        validateInjection(taskManager, "Task manager was not injected.");
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        //this attaches jquery.js as first header item, which is used in our scripts.
        CoreLibrariesContributor.contribute(getApplication(), response);
//        Bootstrap.renderHeadPlain(response);
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();

        //we try to remove messages (and operation results) that were stored in session, but only
        //if all session messages were already rendered.
        boolean allRendered = true;
        FeedbackMessages messages = getSession().getFeedbackMessages();
        Iterator<FeedbackMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            FeedbackMessage message = iterator.next();
            if (!message.isRendered()) {
                allRendered = false;
                break;
            }
        }

        if (allRendered) {
            getSession().getFeedbackMessages().clear();
        }
    }

    private void initLayout() {
        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);

        DebugBar debugPanel = new DebugBar(ID_DEBUG_PANEL);
        add(debugPanel);

        List<TopMenuItem> topMenuItems = getTopMenuItems();
        List<BottomMenuItem> bottomMenuItems = getBottomMenuItems();
        add(new TopMenu(ID_TOP_MENU, topMenuItems, bottomMenuItems));

        LoginPanel loginPanel = new LoginPanel(ID_LOGIN_PANEL);
        add(loginPanel);

        add(new Label(ID_PAGE_TITLE, createPageTitleModel()));

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        add(feedbackContainer);

        MainFeedback feedback = new MainFeedback(ID_FEEDBACK);
        feedbackContainer.add(feedback);

        TempFeedback tempFeedback = new TempFeedback(ID_TEMP_FEEDBACK);
        feedbackContainer.add(tempFeedback);
    }

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }

    private void validateInjection(Object object, String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        }
    }

    public SessionStorage getSessionStorage() {
        MidPointAuthWebSession session = (MidPointAuthWebSession) getSession();
        return session.getSessionStorage();
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public abstract List<TopMenuItem> getTopMenuItems();

    public abstract List<BottomMenuItem> getBottomMenuItems();

    public PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
    }

    protected TaskManager getTaskManager() {
        return taskManager;
    }

    protected WorkflowManager getWorkflowManager() {
        return workflowManager;
    }

    protected WfDataAccessor getWorkflowDataAccessor() {
        return workflowManager.getDataAccessor();
    }

    protected IModel<String> createPageTitleModel() {
        return createStringResource("page.title");
    }

    public ModelService getModelService() {
        return modelService;
    }

    protected ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }

    protected ModelDiagnosticService getModelDiagnosticService() {
        return modelDiagnosticService;
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, new Model<String>(), resourceKey, objects);
    }

    public StringResourceModel createStringResource(Enum e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    public Task createSimpleTask(String operation, PrismObject<UserType> owner) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        if (owner == null) {
        	MidPointPrincipal user = SecurityUtils.getPrincipalUser();
            if (user == null) {
                return task;
            } else {
                owner = user.getUser().asPrismObject();
            }
        }

        task.setOwner(owner);
        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
    	MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        return createSimpleTask(operation, user != null ? user.getUser().asPrismObject() : null);
    }

    public void showResult(OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OpResult opResult = new OpResult(result);
        showResult(opResult, false);
    }

    public void showResultInSession(OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OpResult opResult = new OpResult(result);
        showResult(opResult, true);
    }

    private void showResult(OpResult opResult, boolean showInSession) {
        Validate.notNull(opResult, "Operation result must not be null.");
        Validate.notNull(opResult.getStatus(), "Operation result status must not be null.");

        switch (opResult.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                if (showInSession) {
                    getSession().error(opResult);
                } else {
                    error(opResult);
                }
                break;
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                if (showInSession) {
                    getSession().info(opResult);
                } else {
                    info(opResult);
                }
                break;
            case SUCCESS:
                if (showInSession) {
                    getSession().success(opResult);
                } else {
                    success(opResult);
                }
                break;
            case UNKNOWN:
            case WARNING:
            default:
                if (showInSession) {
                    getSession().warn(opResult);
                } else {
                    warn(opResult);
                }
        }
    }

    protected String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    /**
     * It's here only because of eclipse ide - it's not properly filtering resources during maven build.
     * "buildnumber" variable is not replaced.
     *
     * @return
     * @deprecated
     */
    @Deprecated
    public String getBuildNumber() {
        return getString("pageBase.unknownBuildNumber");
    }
}
