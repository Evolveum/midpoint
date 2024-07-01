/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.List;


public abstract class SchemaHandlingObjectsPanel<C extends Containerable> extends AbstractObjectMainPanel<ResourceType, ResourceDetailsModel> {

    private static final String ID_TABLE = "table";
    private static final String ID_FORM = "form";

    public SchemaHandlingObjectsPanel(String id, ResourceDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    protected void initLayout() {
        MidpointForm<?> form = new MidpointForm<>(ID_FORM);
        add(form);

        MultivalueContainerListPanel<C> objectTypesPanel = new MultivalueContainerListPanel<C>(ID_TABLE, getSchemaHandlingObjectsType()) {
            @Override
            protected boolean isCreateNewObjectVisible() {
                return SchemaHandlingObjectsPanel.this.isCreateNewObjectVisible();
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return SchemaHandlingObjectsPanel.this.getTableId();
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<C>, String>> createDefaultColumns() {
                return SchemaHandlingObjectsPanel.this.createColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return getDefaultMenuActions();
            }

            @Override
            protected String getInlineMenuCssClass() {
                return " mp-w-md-1 ";
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
                return SchemaHandlingObjectsPanel.this.getKeyOfTitleForNewObjectButton();
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected IColumn<PrismContainerValueWrapper<C>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return new PrismPropertyWrapperColumn<>(getContainerModel(), getPathForDisplayName(), AbstractItemWrapperColumn.ColumnType.LINK, getPageBase()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> model) {
                        editItemPerformed(target, model, null);
                    }
                };
            }

            @Override
            public void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel, List<PrismContainerValueWrapper<C>> listItems) {
                AbstractPageObjectDetails parent = findParent(AbstractPageObjectDetails.class);

                if (parent == null) {
                    getParentPage().warn("SchemaHandlingObjectsPanel.message.couldnOpenWizard");
                    return;
                }
                if ((listItems != null && !listItems.isEmpty()) || rowModel != null) {
                    IModel<PrismContainerValueWrapper<C>> valueModel;
                    if (rowModel == null) {
                        valueModel = () -> listItems.iterator().next();
                    } else {
                        valueModel = rowModel;
                    }
                    if (valueModel != null) {
                        onEditValue(valueModel, target);
                    }
                } else {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getPageBase().getFeedbackPanel());
                }
            }

            @Override
            protected void newItemPerformed(PrismContainerValue<C> value, AjaxRequestTarget target, AssignmentObjectRelation relationSepc) {
                onNewValue(value, getContainerModel(), target);
            }
        };
        form.add(objectTypesPanel);
    }

    protected ItemPath getPathForDisplayName() {
        return ResourceObjectTypeDefinitionType.F_DISPLAY_NAME;
    }

    protected abstract ItemPath getTypesContainerPath();

    protected abstract UserProfileStorage.TableId getTableId();

    protected abstract String getKeyOfTitleForNewObjectButton();

    protected abstract List<IColumn<PrismContainerValueWrapper<C>, String>> createColumns();

    protected abstract Class<C> getSchemaHandlingObjectsType();

    protected abstract void onNewValue(
            PrismContainerValue<C> value, IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target);

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<C>> valueModel, AjaxRequestTarget target);

    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    public MultivalueContainerListPanel getTable() {
        return (MultivalueContainerListPanel) get(getPageBase().createComponentPath(ID_FORM, ID_TABLE));
    }
}
