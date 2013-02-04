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

import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.login.LoginPanel;
import com.evolveum.midpoint.web.component.menu.left.LeftMenu;
import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenu;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.message.MainFeedback;
import com.evolveum.midpoint.web.component.message.OpResult;
import com.evolveum.midpoint.web.component.message.TempFeedback;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.wf.WfDataAccessor;
import com.evolveum.midpoint.wf.WorkflowManager;
import org.apache.commons.lang.Validate;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageBase extends WebPage {

    private static final Trace LOGGER = TraceManager.getTrace(PageBase.class);
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
        Label title = new Label("title", createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);

        DebugBar debugPanel = new DebugBar("debugPanel");
        add(debugPanel);

        List<TopMenuItem> topMenuItems = getTopMenuItems();
        Validate.notNull(topMenuItems, "Top menu item list must not be null.");

        List<BottomMenuItem> bottomMenuItems = getBottomMenuItems();
        Validate.notNull(bottomMenuItems, "Bottom menu item list must not be null.");

        add(new TopMenu("topMenu2", topMenuItems, bottomMenuItems));
        add(new LeftMenu("leftMenu", getLeftMenuItems()));

        LoginPanel loginPanel = new LoginPanel("loginPanel");
        add(loginPanel);

        add(new Label("pageTitle", createPageTitleModel()));

        WebMarkupContainer feedbackContainer = new WebMarkupContainer("feedbackContainer");
        feedbackContainer.setOutputMarkupId(true);
        add(feedbackContainer);

        MainFeedback feedback = new MainFeedback("feedback");
        feedbackContainer.add(feedback);

        TempFeedback tempFeedback = new TempFeedback("tempFeedback");
        feedbackContainer.add(tempFeedback);
    }

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get("feedbackContainer");
    }

    private void validateInjection(Object object, String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        }
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public abstract List<TopMenuItem> getTopMenuItems();

    public abstract List<BottomMenuItem> getBottomMenuItems();

    public abstract List<LeftMenuItem> getLeftMenuItems();

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

    public Task createSimpleTask(String operation) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        PrincipalUser user = SecurityUtils.getPrincipalUser();
        if (user == null) {
            return task;
        }
        task.setOwner(user.getUser().asPrismObject());

        return task;
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
