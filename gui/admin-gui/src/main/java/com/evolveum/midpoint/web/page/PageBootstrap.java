package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowService;
import org.apache.commons.lang.Validate;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * @author lazyman
 */
public class PageBootstrap extends WebPage {

    private static final Trace LOGGER = TraceManager.getTrace(PageBootstrap.class);

    private static final String ID_TITLE = "title";
    private static final String ID_DEBUG_PANEL = "debugPanel";
    private static final String ID_TOP_MENU = "topMenu";
    private static final String ID_LOGIN_PANEL = "loginPanel";
    private static final String ID_PAGE_TITLE = "pageTitle";

    @SpringBean(name = "modelController")
    private ModelService modelService;
    @SpringBean(name = "modelController")
    private ModelInteractionService modelInteractionService;
    @SpringBean(name = "modelController")
    private TaskService taskService;
    @SpringBean(name = "modelDiagController")
    private ModelDiagnosticService modelDiagnosticService;
    @SpringBean(name = "taskManager")
    private TaskManager taskManager;
    @SpringBean(name = "workflowService")
    private WorkflowService workflowService;
    @SpringBean(name = "midpointConfiguration")
    private MidpointConfiguration midpointConfiguration;

    public PageBootstrap() {
        Injector.get().inject(this);
        Validate.notNull(modelService, "Model service was not injected.");
        Validate.notNull(taskManager, "Task manager was not injected.");
        initLayout();
    }

    private void initLayout() {
        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);
    }

    protected IModel<String> createPageTitleModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return "Evolveum :: MidPoint";
            }
        };
    }
}
