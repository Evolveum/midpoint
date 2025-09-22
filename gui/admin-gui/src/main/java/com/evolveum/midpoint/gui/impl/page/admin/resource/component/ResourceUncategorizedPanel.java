/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.CollectionPanelType;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.authentication.CompiledShadowCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ResourceObjectClassChoiceRenderer;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.web.page.admin.shadows.ShadowTablePanel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.ResourceContentStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@PanelType(name = "resourceUncategorized")
@PanelInstance(identifier = "resourceUncategorized", applicableForOperation = OperationTypeType.MODIFY, applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageResource.tab.content.others", icon = GuiStyleConstants.CLASS_SHADOW_ICON_UNKNOWN, order = 80))
public class ResourceUncategorizedPanel extends AbstractResourceObjectPanel {

    private static final String ID_OBJECT_TYPE = "objectType";
    private static final String ID_TABLE = "table";
    private static final String ID_TITLE = "title";
    private static final String ID_TASKS = "tasks";

    public ResourceUncategorizedPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void addViewMenuItemsForCreateTaskButton(List<InlineMenuItem> items) {
        items.add(createTaskViewMenuItem(
                createStringResource("ResourceObjectsPanel.button.viewSimulatedTasks"),
                SystemObjectsType.ARCHETYPE_SHADOW_RECLASSIFICATION_TASK.value(),
                true));

        items.add(createTaskViewMenuItem(
                createStringResource("ResourceObjectsPanel.button.viewShadowReclassificationTasks"),
                SystemObjectsType.ARCHETYPE_SHADOW_RECLASSIFICATION_TASK.value(),
                false));
    }

    @Override
    protected TaskCreationPopup<?> createNewTaskPopup() {
        return new TaskCreationForUncategorizedObjectsPopup(getPageBase().getMainPopupBodyId()) {
            @Override
            protected void createNewTaskPerformed(SynchronizationTaskFlavor flavor, boolean simulate, AjaxRequestTarget target) {
                ResourceUncategorizedPanel.this.createNewTaskPerformed(flavor, simulate, target);
            }
        };
    }

    @Override
    protected QName getObjectClass() {
        return getSelectedObjectClass();
    }

    @Override
    protected ObjectQuery createQueryForTasks(boolean isSimulationTasks) {
        S_FilterExit filter = PrismContext.get()
                .queryFor(TaskType.class)
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_RESOURCE_OBJECTS,
                        BasicResourceObjectSetType.F_RESOURCE_REF))
                .ref(getObjectDetailsModels().getObjectType().getOid())
                .and()
                .item(ItemPath.create(
                        TaskType.F_AFFECTED_OBJECTS,
                        TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_RESOURCE_OBJECTS,
                        BasicResourceObjectSetType.F_OBJECTCLASS))
                .eq(getObjectClass());

        if (isSimulationTasks) {
            filter = addSimulationRule(
                    filter.and(),
                    false,
                    ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW);
        } else {
            filter = addSimulationRule(
                    filter.and(),
                    true,
                    ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW);
        }

        return filter.build();
    }

    @Override
    protected void initLayout() {
        createPanelTitle();
        createObjectTypeChoice();
        createTasksButton(ID_TASKS);
        createShadowTable();
    }

    private void createPanelTitle() {
        Label title = new Label(ID_TITLE, createStringResource("ResourceUncategorizedPanel.select.objectClass.title"));
        title.add(getTitleVisibleBehaviour());
        title.setOutputMarkupId(true);
        add(title);
    }

    protected VisibleEnableBehaviour getTitleVisibleBehaviour() {
        return VisibleBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    private void createObjectTypeChoice() {
        QName defaultObjectClass = getDefaultObjectClass();
        resetSearch(defaultObjectClass);

        boolean templateCategory = WebComponentUtil.isTemplateCategory(getObjectWrapperObject().asObjectable());

        var objectTypes = new DropDownChoicePanel<>(ID_OBJECT_TYPE,
                Model.of(defaultObjectClass),
                () -> {
                    List<QName> resourceObjectClassesDefinitions = getObjectDetailsModels().getResourceObjectClassesDefinitions();
                    return Objects.requireNonNullElseGet(resourceObjectClassesDefinitions, ArrayList::new);
                },
                new ResourceObjectClassChoiceRenderer(), templateCategory);
        objectTypes.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ShadowTablePanel table = getShadowTable();
                resetSearch(getSelectedObjectClass());
                table.getSearchModel().getObject();
                table.refreshTable(target);
                table.resetTable(target);
            }
        });
        objectTypes.add(new VisibleBehaviour(this::isObjectClassFieldVisible));
        objectTypes.setOutputMarkupId(true);
        add(objectTypes);
    }

    protected boolean isObjectClassFieldVisible() {
        return true;
    }

    private void resetSearch(QName currentObjectClass) {
        ResourceContentStorage storage = getPageBase().getSessionStorage().getResourceContentStorage(null);

        QName storedObjectClass = storage.getContentSearch().getObjectClass();
        String storedResourceOid = storage.getContentSearch().getResourceOid();
        String wrapperResourceOid = getObjectWrapper().getOid();
        if (storedObjectClass == null
                || !QNameUtil.match(currentObjectClass, storedObjectClass)
                || StringUtils.isEmpty(wrapperResourceOid)
                || !wrapperResourceOid.equals(storedResourceOid)) {
            storage.setSearch(null);
            storage.getContentSearch().setResourceOid(wrapperResourceOid);
            storage.getContentSearch().setObjectClass(currentObjectClass);
        }
    }

    protected QName getDefaultObjectClass() {
        ResourceContentStorage storage = getPageBase().getSessionStorage().getResourceContentStorage(null);
        if (getObjectWrapper().getOid() != null
                && getObjectWrapper().getOid().equals(storage.getContentSearch().getResourceOid())
                && storage.getContentSearch().getObjectClass() != null) {
            return storage.getContentSearch().getObjectClass();
        }
        return getObjectDetailsModels().getDefaultObjectClass();
    }

    private void createShadowTable() {
        ShadowTablePanel shadowTablePanel = new ShadowTablePanel(ID_TABLE, getPanelConfiguration()) {

            @Override
            protected boolean isDeleteOnlyRepoShadowAllow() {
                return false;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_RESOURCE_OBJECT_CLASS_PANEL;
            }

            @Override
            public PageStorage getPageStorage() {
                return getPageBase().getSessionStorage().getResourceContentStorage(null);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ShadowType>> createProvider() {
                SelectableBeanObjectDataProvider<ShadowType> provider = createSelectableBeanObjectDataProvider(() -> getResourceContentQuery(), null,
                        createSearchOptions());
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                provider.setTaskConsumer(createProviderSearchTaskCustomizer());
                return provider;
//                return ResourceUncategorizedPanel.this.createProvider(getSearchModel(), (CompiledShadowCollectionView) getObjectCollectionView());
            }

            @Override
            protected SearchContext createAdditionalSearchContext() {
                SearchContext searchContext = new SearchContext();
                searchContext.setPanelType(CollectionPanelType.RESOURCE_SHADOW);
                var objClassDef = getObjectDetailsModels().findResourceObjectClassDefinition(getSelectedObjectClass());
                searchContext.setResourceObjectDefinition(objClassDef);
                // MID-9569: selectedObjectDefinition has knowledge about detailed shadow type, so we can provide it
                // directly to search (since we are also adding coordinates to filter) so Axiom Query can access
                // additional attributes
                if (objClassDef != null) {
                    searchContext.setDefinitionOverride(objClassDef.getPrismObjectDefinition());
                }
                return searchContext;
            }

            @Override
            public CompiledObjectCollectionView getObjectCollectionView() {
                CompiledShadowCollectionView compiledView = findContainerPanelConfig();
                if (compiledView != null) {
                    return compiledView;
                }
                return super.getObjectCollectionView();
            }

            @Override
            protected boolean isShadowDetailsEnabled(IModel<SelectableBean<ShadowType>> rowModel) {
                return ResourceUncategorizedPanel.this.isShadowDetailsEnabled();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                if (isEnabledInlineMenu()) {
                    return super.createInlineMenu();
                }
                return List.of();
            }

            @Override
            protected boolean isFulltextEnabled() {
                return false;
            }

            @Override
            protected boolean isHeaderVisible() {
                return ResourceUncategorizedPanel.this.isHeaderVisible();
            }
        };
        shadowTablePanel.setOutputMarkupId(true);
        add(shadowTablePanel);
    }

    protected boolean isHeaderVisible() {
        return true;
    }

    protected Consumer<Task> createProviderSearchTaskCustomizer() {
        return null;
    }

    protected boolean isEnabledInlineMenu() {
        return true;
    }

    protected boolean isShadowDetailsEnabled() {
        return true;
    }

    private QName getSelectedObjectClass() {
        DropDownChoicePanel<QName> objectTypeSelector = getObjectTypeSelector();
        if (objectTypeSelector != null && objectTypeSelector.getModel() != null) {
            return objectTypeSelector.getModel().getObject();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private DropDownChoicePanel<QName> getObjectTypeSelector() {
        return (DropDownChoicePanel<QName>) get(ID_OBJECT_TYPE);
    }

    public ShadowTablePanel getShadowTable() {
        return (ShadowTablePanel) get(ID_TABLE);
    }

    private ObjectQuery getResourceContentQuery() {
        return ObjectQueryUtil.createResourceAndObjectClassQuery(getObjectWrapper().getOid(), getSelectedObjectClass());
    }

    private Collection<SelectorOptions<GetOperationOptions>> createSearchOptions() {
        GetOperationOptionsBuilder builder = getPageBase().getOperationOptionsBuilder()
                .item(ShadowType.F_ASSOCIATIONS).dontRetrieve();
        return builder.build();
    }

    @Override
    protected void customizeTaskCreator(ResourceTaskCreator creator, boolean isSimulation) {
        if (isSimulation) {
            creator.withExecutionMode(ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW);
        }
    }
}
