package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelDiagnosticService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.TaskService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.top2.MenuBarItem;
import com.evolveum.midpoint.web.component.menu.top2.MenuItem;
import com.evolveum.midpoint.web.component.menu.top2.TopMenuBar;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.admin.users.PageOrgStruct;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.wf.api.WorkflowService;
import org.apache.commons.lang.Validate;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.ArrayList;
import java.util.List;

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
        DebugBar debugPanel = new DebugBar(ID_DEBUG_PANEL);
        add(debugPanel);

        TopMenuBar topMenu = new TopMenuBar(ID_TOP_MENU, createMenuItems());
        add(topMenu);
    }

    private List<MenuBarItem> createMenuItems() {
        List<MenuBarItem> items = new ArrayList<MenuBarItem>();

        MenuBarItem home = new MenuBarItem(createStringResource("PageBootstrap.menu.top.home"), PageDashboard.class);
        items.add(home);

        MenuBarItem users = new MenuBarItem(createStringResource("PageBootstrap.menu.top.users"), null);
        users.addMenuItem(new MenuItem(createStringResource("PageBootstrap.menu.top.users.list"), PageUsers.class));
        users.addMenuItem(new MenuItem(createStringResource("PageBootstrap.menu.top.users.find"), PageUsers.class));
        users.addMenuItem(new MenuItem(createStringResource("PageBootstrap.menu.top.users.new"), PageUser.class));
        users.addMenuItem(new MenuItem(null));
        users.addMenuItem(new MenuItem(createStringResource("PageBootstrap.menu.top.users.org"), true, null, null));
        users.addMenuItem(new MenuItem(createStringResource("PageBootstrap.menu.top.users.org.tree"), PageOrgStruct.class));
        users.addMenuItem(new MenuItem(createStringResource("PageBootstrap.menu.top.users.org.new"), PageOrgStruct.class));

        items.add(users);

        return items;
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

    protected IModel<String> createPageTitleModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return "Evolveum :: MidPoint";
            }
        };
    }
}
