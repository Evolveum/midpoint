package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
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
public class PageTask extends PageAdminObjectDetails<TaskType> {
    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(PageTask.class);

    public PageTask() {
        initialize(null);
    }

    public PageTask(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
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
        return new TaskSummaryPanelNew(ID_SUMMARY_PANEL, isEditingFocus() ?
                Model.of(getObjectModel().getObject().getObject().asObjectable()) : Model.of(), this);
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

                tabs.add(new AbstractTab(createStringResource("pageTask.basic.title")) {
                    @Override
                    public WebMarkupContainer getPanel(String panelId) {
                        return createContainerPanel(panelId, TaskType.COMPLEX_TYPE, PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), TaskType.F_SCHEDULE));
                    }
                });
                return tabs;
            }
        };
    }

    private <C extends Containerable, T extends Containerable> Panel createContainerPanel(String id, QName typeName, IModel<? extends PrismContainerWrapper<C>> model) {
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
}
