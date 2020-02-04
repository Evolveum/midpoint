package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.refresh.AutoRefreshDto;
import com.evolveum.midpoint.web.component.refresh.Refreshable;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoExecutionStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/task", matchUrlForSecurity = "/admin/task")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_URL,
                        label = "PageUser.auth.user.label",
                        description = "PageUser.auth.user.description")
        })
public class PageTask extends PageAdminObjectDetails<TaskType> implements Refreshable {
    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(PageTask.class);

    private static final String DOT_CLASS = PageTask.class.getName() + ".";

    private static final int REFRESH_INTERVAL_IF_RUNNING = 2000;
    private static final int REFRESH_INTERVAL_IF_RUNNABLE = 60000;
    private static final int REFRESH_INTERVAL_IF_SUSPENDED = 60000;
    private static final int REFRESH_INTERVAL_IF_WAITING = 60000;
    private static final int REFRESH_INTERVAL_IF_CLOSED = 60000;

    public PageTask() {
        initialize(null);
    }

    public PageTask(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> buildGetOptions() {
        return getOperationOptionsBuilder()
                // retrieve
                .item(TaskType.F_SUBTASK).retrieve()
                .item(TaskType.F_NODE_AS_OBSERVED).retrieve()
                .item(TaskType.F_NEXT_RUN_START_TIMESTAMP).retrieve()
                .item(TaskType.F_NEXT_RETRY_TIMESTAMP).retrieve()
                .item(TaskType.F_RESULT).retrieve()         // todo maybe only when it is to be displayed
                .build();
    }

    public PageTask(final PrismObject<TaskType> taskToEdit) {
        initialize(taskToEdit);
    }

    public PageTask(final PrismObject<TaskType> taskToEdit, boolean isNewObject)  {
        initialize(taskToEdit, isNewObject);
    }

    @Override
    public Class<TaskType> getCompileTimeClass() {
        return TaskType.class;
    }

    @Override
    protected TaskType createNewObject() {
        return new TaskType();
    }

    @Override
    protected ObjectSummaryPanel<TaskType> createSummaryPanel() {
        return new TaskSummaryPanelNew(ID_SUMMARY_PANEL, createSummaryPanelModel(), this, this);
    }

    private IModel<TaskType> createSummaryPanelModel() {
        return isEditingFocus() ?

                new LoadableModel<TaskType>(true) {
                    @Override
                    protected TaskType load() {
                        PrismObjectWrapper<TaskType> taskWrapper = getObjectWrapper();
                        if (taskWrapper == null) {
                            return null;
                        }
                        return taskWrapper.getObject().asObjectable();
                    }
                } : Model.of();
    }

    @Override
    protected AbstractObjectMainPanel<TaskType> createMainPanel(String id) {

        return new AbstractObjectMainPanel<TaskType>(id, getObjectModel(), this) {

            @Override
            protected List<ITab> createTabs(PageAdminObjectDetails<TaskType> parentPage) {
                List<ITab> tabs = new ArrayList<>();
                tabs.add(new AbstractTab(createStringResource("pageTask.basic.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, getObjectModel());
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.schedule.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_SCHEDULE));
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.subtasks.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_SCHEDULE));
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.operationStats.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskOperationStatisticsPanel(panelId, getObjectModel());
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.internalPerformane.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskInternalPerformanceTabPanelNew(panelId, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_OPERATION_STATS));
                    }
                });

                tabs.add(new AbstractTab(createStringResource("pageTask.result.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskResultTabPanelNew(panelId, getObjectModel());
                    }
                });


                tabs.add(new AbstractTab(createStringResource("pageTask.errors.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return new TaskErrorsTabPanelNew(panelId, getObjectModel());
                    }
                });

                return tabs;
            }
        };
    }



    private <C extends Containerable> Panel createContainerPanel(String id, QName typeName, IModel<? extends PrismContainerWrapper<C>> model) {
            try {
                ItemPanelSettingsBuilder builder = new ItemPanelSettingsBuilder()
                        .visibilityHandler(wrapper -> getVisibility(wrapper.getPath()))
                        .showOnTopLevel(true);
                Panel panel = initItemPanel(id, typeName, model, builder.build());
                return panel;
            } catch (SchemaException e) {
                LOGGER.error("Cannot create panel for {}, {}", typeName, e.getMessage(), e);
                getSession().error("Cannot create panel for " + typeName); // TODO opertion result? localization?
            }

            return null;
    }

    private ItemVisibility getVisibility(ItemPath path) {
        return ItemVisibility.AUTO;
    }

    @Override
    protected Class<? extends Page> getRestartResponsePage() {
        return PageTasks.class;
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {

    }

    @Override
    public int getRefreshInterval() {
        TaskType task = getObjectWrapper().getObject().asObjectable();
        TaskDtoExecutionStatus exec = TaskDtoExecutionStatus.fromTaskExecutionStatus(task.getExecutionStatus(), task.getNodeAsObserved() != null);
        if (exec == null) {
            return REFRESH_INTERVAL_IF_CLOSED;
        }
        switch (exec) {
            case RUNNING:
            case SUSPENDING: return REFRESH_INTERVAL_IF_RUNNING;
            case RUNNABLE:return REFRESH_INTERVAL_IF_RUNNABLE;
            case SUSPENDED: return REFRESH_INTERVAL_IF_SUSPENDED;
            case WAITING: return REFRESH_INTERVAL_IF_WAITING;
            case CLOSED: return REFRESH_INTERVAL_IF_CLOSED;
        }
        return REFRESH_INTERVAL_IF_RUNNABLE;
    }

    @Override
    public Component getRefreshingBehaviorParent() {
        return null; //nothing to do, this method will be removed
    }

    public void refresh(AjaxRequestTarget target) {

        target.add(getSummaryPanel());

        for (Component component : getMainPanel().getTabbedPanel()) {
            if (component instanceof TaskTabPanel) {

                for (Component c : ((TaskTabPanel) component).getComponentsToUpdate()) {
                    getObjectModel().reset();
                    target.add(c);
                }
            }
//            target.add(component);
        }


//        TabbedPanel<ITab> tabbedPanel = getMainPanel().getTabbedPanel();
//        int selected = tabbedPanel.getSelectedTab();
//        ITab selectedTab = tabbedPanel.getTabs().getObject().get(selected);
//        if (selectedTab instanceof TaskTabPanel) {
//            for (Component c : ((TaskTabPanel) selectedTab).getComponentsToUpdate()) {
//                target.add(c);
//            }
//        }

//        target.add(getMainPanel());
    }
}
