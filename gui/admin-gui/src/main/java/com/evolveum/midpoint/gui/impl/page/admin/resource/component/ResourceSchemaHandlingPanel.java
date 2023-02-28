/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ResourceAttributePanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil.getObjectClassName;

@PanelType(name = "schemaHandling")
@PanelInstance(identifier = "schemaHandling", applicableForType = ResourceType.class,
        display = @PanelDisplay(label = "PageResource.tab.schemaHandling", icon = GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM, order = 90))
public class ResourceSchemaHandlingPanel extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final String ID_TABLE = "table";
    private static final String ID_FORM = "form";

    public ResourceSchemaHandlingPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        MultivalueContainerListPanelWithDetailsPanel<ResourceObjectTypeDefinitionType> objectTypesPanel = new MultivalueContainerListPanelWithDetailsPanel<>(ID_TABLE, ResourceObjectTypeDefinitionType.class) {

            @Override
            protected WebMarkupContainer getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> item) {
                return createMultivalueContainerDetailsPanel(ID_ITEM_DETAILS, item.getModel());
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return ResourceSchemaHandlingPanel.this.isCreateNewObjectVisible();
            }

            @Override
            protected IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_SCHEMA_HANDLING;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> createDefaultColumns() {
                List<IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String>> columns = new ArrayList<>();
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_KIND, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_INTENT, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_DEFAULT, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                columns.add(new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_DESCRIPTION, AbstractItemWrapperColumn.ColumnType.STRING, getPageBase()));
                List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
                columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getCssClass() {
                        return " mp-w-md-1 ";
                    }

                });
                return columns;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String idButton) {
                List<Component> buttons = super.createToolbarButtonsList(idButton);
                buttons.stream()
                        .filter(component -> component instanceof AjaxIconButton)
                        .forEach(button -> {
                            ((AjaxIconButton) button).showTitleAsLabel(true);
                            button.add(AttributeAppender.replace("class", "btn btn-primary btn-sm"));
                        });
                return buttons;
            }

            @Override
            protected String getIconForNewObjectButton() {
                return "fa fa-circle-plus";
            }

            @Override
            protected String getKeyOfTitleForNewObjectButton() {
                return "ResourceSchemaHandlingPanel.newObject";
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return new PrismPropertyWrapperColumn<>(getContainerModel(), ResourceObjectTypeDefinitionType.F_DISPLAY_NAME, AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model) {
                        itemDetailsPerformed(target, model);
                    }
                };
            }

            @Override
            public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> rowModel, List<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> listItems) {
                AbstractPageObjectDetails parent = findParent(AbstractPageObjectDetails.class);

                if (parent == null) {
                    super.editItemPerformed(target, rowModel, listItems);
                    return;
                }
                if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
                    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;
                    if (rowModel == null) {
                        valueModel = () -> listItems.iterator().next();
                    } else {
                        valueModel = rowModel;
                    }
                    onEditValue(valueModel, target);
                } else {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getPageBase().getFeedbackPanel());
                }
            }

            @Override
            protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSepc) {
                onNewValue(getContainerModel(), target);
            }
        };
        form.add(objectTypesPanel);
    }

    protected void onNewValue(
            IModel<PrismContainerWrapper<ResourceObjectTypeDefinitionType>> newWrapperModel, AjaxRequestTarget target) {
        getObjectDetailsModels().getPageResource().showObjectTypeWizard(target, newWrapperModel.getObject().getPath());
    }

    protected void onEditValue(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, AjaxRequestTarget target) {
        if (valueModel != null) {
            getObjectDetailsModels().getPageResource().showResourceObjectTypePreviewWizard(
                    target,
                    valueModel.getObject().getPath());
        }
    }

    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    private MultivalueContainerListPanelWithDetailsPanel<ResourceObjectTypeDefinitionType> getMultivalueContainerListPanel() {
        return ((MultivalueContainerListPanelWithDetailsPanel<ResourceObjectTypeDefinitionType>) get(getPageBase().createComponentPath(ID_FORM, ID_TABLE)));
    }

    private WebMarkupContainer createMultivalueContainerDetailsPanel(String panelId, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model) {
        return new MultivalueContainerDetailsPanel<>(panelId, model, true) {

            @Override
            protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
                if (itemWrapper instanceof PrismContainerWrapper) {
                    return ItemVisibility.HIDDEN;
                }
                return ItemVisibility.AUTO;
            }

            @Override
            protected @NotNull List<ITab> createTabs() {
                List<ITab> tabs = new ArrayList<>();
                tabs.add(new PanelTab(createStringResource("ResourceSchemaHandlingPanel.tab.attributes")) {

                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new ResourceAttributePanel(panelId, PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), ResourceObjectTypeDefinitionType.F_ATTRIBUTE), getPanelConfiguration());
                    }
                });
                return tabs;
            }

            @Override
            protected DisplayNamePanel<ResourceObjectTypeDefinitionType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())) {

                    @Override
                    protected IModel<String> createHeaderModel() {
                        return new ReadOnlyModel<>(() -> loadHeaderModel(getModelObject()));
                    }

                    @Override
                    protected IModel<List<String>> getDescriptionLabelsModel() {
                        return new ReadOnlyModel<>(() -> loadDescriptionModel(getModelObject()));
                    }

                };
            }
        };
    }

    private String loadHeaderModel(ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        if (resourceObjectTypeDefinitionType.getDisplayName() != null) {
            return resourceObjectTypeDefinitionType.getDisplayName();
        }
        return getString("SchemaHandlingType.objectType");
    }

    private List<String> loadDescriptionModel(ResourceObjectTypeDefinitionType resourceObjectTypeDefinitionType) {
        List<String> description = new ArrayList<>();
        if (resourceObjectTypeDefinitionType.getKind() != null) {
            description.add(getString("ResourceSchemaHandlingPanel.description.kind", resourceObjectTypeDefinitionType.getKind()));
        }
        if (resourceObjectTypeDefinitionType.getIntent() != null) {
            description.add(getString("ResourceSchemaHandlingPanel.description.intent", resourceObjectTypeDefinitionType.getIntent()));
        }
        QName objectClassName = getObjectClassName(resourceObjectTypeDefinitionType);
        if (objectClassName != null) {
            description.add(getString("ResourceSchemaHandlingPanel.description.objectClass", objectClassName.getLocalPart()));
        }
        return description;
    }

    public MultivalueContainerListPanelWithDetailsPanel getTable() {
        return (MultivalueContainerListPanelWithDetailsPanel) get(getPageBase().createComponentPath(ID_FORM, ID_TABLE));
    }
}
