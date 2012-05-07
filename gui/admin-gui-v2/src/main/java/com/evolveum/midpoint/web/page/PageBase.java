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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.component.login.LoginPanel;
import com.evolveum.midpoint.web.component.menu.left.LeftMenu;
import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenu;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.message.MainFeedback;
import com.evolveum.midpoint.web.component.message.OpResult;
import com.evolveum.midpoint.web.security.MidPointApplication;
import org.apache.commons.lang.Validate;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageBase extends WebPage {

    @SpringBean(name = "modelController")
    private ModelService modelService;
    @SpringBean(name = "cacheRepositoryService")
    private RepositoryService cacheRepositoryService;
    @SpringBean(name = "taskManager")
    private TaskManager taskManager;

    public PageBase() {
        Injector.get().inject(this);
        validateInjection(modelService, "Model service was not injected.");
        validateInjection(cacheRepositoryService, "Cache repository service was not injected.");
        validateInjection(taskManager, "Task manager was not injected.");

        initLayout();
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();

        getSession().getFeedbackMessages().clear();
    }

    private void initLayout() {
        DebugBar debugPanel = new DebugBar("debugPanel");
        add(debugPanel);

        List<TopMenuItem> topMenuItems = getTopMenuItems();
        Validate.notNull(topMenuItems, "Top menu item list must not be null.");

        List<BottomMenuItem> bottomMenuItems = getBottomMenuItems();
        Validate.notNull(bottomMenuItems, "Bottom menu item list must not be null.");

        add(new TopMenu("topMenu2", topMenuItems, bottomMenuItems));
        add(new LeftMenu("leftMenu", getLeftMenuItems()));

        LoginPanel loginPanel = new LoginPanel("loginPanel");

        /*if(loginPanel.getIsAdminLoggedIn()){
            add(loginPanel);
        }*/
        add(loginPanel);

        add(new Label("pageTitle", createPageTitleModel()));
        add(new MainFeedback("feedback"));
    }

    protected MainFeedback getFeedbackPanel() {
        return (MainFeedback) get("feedback");
    }

    private void validateInjection(Object object, String message) {
        if (object == null) {
            throw new IllegalStateException(message);
        }
    }

    protected MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public abstract List<TopMenuItem> getTopMenuItems();

    public abstract List<BottomMenuItem> getBottomMenuItems();

    public abstract List<LeftMenuItem> getLeftMenuItems();

    protected RepositoryService getCacheRepositoryService() {
        return cacheRepositoryService;
    }

    protected PrismContext getPrismContext() {
        return getMidpointApplication().getPrismContext();
    }

    protected TaskManager getTaskManager() {
        return taskManager;
    }

    protected IModel<String> createPageTitleModel() {
        return createStringResource("page.title");
    }

    protected ModelService getModelService() {
        return modelService;
    }

    protected StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, null, objects);
    }

    protected StringResourceModel createStringResource(Enum e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    public void showResult(OpResult opResult) {
        Validate.notNull(opResult, "Operation result must not be null.");
        Validate.notNull(opResult.getStatus(), "Operation result status must not be null.");

        switch (opResult.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                error(opResult);
                break;
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                info(opResult);
                break;
            case SUCCESS:
                success(opResult);
                break;
            case UNKNOWN:
            case WARNING:
            default:
                warn(opResult);
        }
    }

    public void showResult(OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OpResult opResult = new OpResult(result);
        showResult(opResult);
    }

    public void showResultInSession(OperationResult result) {
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        OpResult opResult = new OpResult(result);
        showResultInSession(opResult);
    }

    public void showResultInSession(OpResult opResult) {
        Validate.notNull(opResult, "Operation result must not be null.");
        Validate.notNull(opResult.getStatus(), "Operation result status must not be null.");

        switch (opResult.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                getSession().error(opResult);
                break;
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                getSession().info(opResult);
                break;
            case SUCCESS:
                getSession().success(opResult);
                break;
            case UNKNOWN:
            case WARNING:
            default:
                getSession().warn(opResult);
        }
    }
}
