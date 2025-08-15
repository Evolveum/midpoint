/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
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

        WebMarkupContainer objectTypesPanel = createMultiValueListPanel();
        form.add(objectTypesPanel);
    }

    public <C extends Containerable> IModel<PrismContainerWrapper<C>> createContainerModel() {
        return PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), getTypesContainerPath());
    }

    private @NotNull MultivalueContainerListPanel<C> createMultiValueListPanel() {
        return new MultivalueContainerListPanel<C>(ID_TABLE, getSchemaHandlingObjectsType()) {
            @Override
            protected boolean isCreateNewObjectVisible() {
                return SchemaHandlingObjectsPanel.this.isCreateNewObjectVisible();
            }

            @Override
            protected void customProcessNewRowItem(Item<PrismContainerValueWrapper<C>> item, IModel<PrismContainerValueWrapper<C>> model) {
                super.customProcessNewRowItem(item, model);
                customizeNewRowItem(item, model);
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> getContainerModel() {
                return createContainerModel();
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
                List<InlineMenuItem> actions = getDefaultMenuActions();
                actions.add(createShowLifecycleStatesInlineMenu());
                return actions;
            }

            @Override
            protected String getInlineMenuCssClass() {
                return " mp-w-md-1 ";
            }

            @Override
            protected List<Component> createToolbarButtonsList(String idButton) {
                List<Component> bar = new ArrayList<>();
                createNewObjectPerformButton(idButton, bar);
                createSuggestObjectButton(idButton, bar);
                return bar;
            }

            @Override
            public boolean displayNoValuePanel() {
                return allowNoValuePanel() && hasNoValues();
            }

            private void createSuggestObjectButton(String idButton, @NotNull List<Component> bar) {
                AjaxIconButton suggestObjectButton = new AjaxIconButton(idButton,
                        Model.of(GuiStyleConstants.CLASS_ICON_WIZARD),
                        createStringResource("SchemaHandlingObjectsPanel.suggestNew")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        showSuggestConfirmDialog(getPageBase(),
                                createStringResource("SchemaHandlingObjectsPanel.suggestNew"), target);
                    }
                };
                suggestObjectButton.showTitleAsLabel(true);
                suggestObjectButton.add(AttributeAppender.replace("class", "btn btn-default btn-sm mr-2"));
                suggestObjectButton.add(new VisibleBehaviour(() -> isSuggestButtonVisible()));
                bar.add(suggestObjectButton);
            }

            private void createNewObjectPerformButton(String idButton, @NotNull List<Component> bar) {
                AjaxIconButton newObjectButton = new AjaxIconButton(idButton,
                        new Model<>(getIconForNewObjectButton()),
                        createStringResource(getKeyOfTitleForNewObjectButton())) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        newItemPerformed(target, null);
                    }
                };
                newObjectButton.showTitleAsLabel(true);
                newObjectButton.add(AttributeAppender.replace("class", "btn btn-primary btn-sm mr-2"));
                newObjectButton.add(new VisibleBehaviour(() -> isCreateNewObjectVisible()));
                bar.add(newObjectButton);
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
                    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<C>> rowModel) {

                        Component component = onNameColumnPopulateItem(cellItem, componentId, rowModel);
                        if (component != null) {
                            cellItem.add(component);
                            return;
                        }

                        super.populateItem(cellItem, componentId, rowModel);
                    }

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
            protected void newItemPerformed(PrismContainerValue<C> value, AjaxRequestTarget target, AssignmentObjectRelation relationSpec, boolean isDuplicate) {
                onNewValue(value, getContainerModel(), target, isDuplicate);
            }

        };
    }

    protected void customizeNewRowItem(
            Item<PrismContainerValueWrapper<C>> item, IModel<PrismContainerValueWrapper<C>> model) {
        // Default implementation does nothing. Override if needed.
    }
    /**
     * This method is called when the name column is populated.
     * It allows for custom behavior or additional components to be added
     * to the name column item.
     *
     * @param cellItem The item being populated.
     * @param componentId The ID of the component being populated.
     * @param rowModel The model for the row being populated.
     * @return A component to be added to the cell, or null if no custom component is needed.
     */
    protected @Nullable Component onNameColumnPopulateItem(
            Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<C>> rowModel) {
        // Default implementation does nothing. Override if needed.
        return null;
    }

    private InlineMenuItem createShowLifecycleStatesInlineMenu() {
        return new InlineMenuItem(createStringResource("SchemaHandlingObjectsPanel.button.showLifecycleStates")) {
            private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<PrismContainerValueWrapper<C>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        OnePanelPopupPanel popupPanel = new OnePanelPopupPanel(
                                getPageBase().getMainPopupBodyId(),
                                1100,
                                600,
                                createStringResource("ContainerWithLifecyclePanel.popup.title")) {
                            @Override
                            protected WebMarkupContainer createPanel(String id) {
                                return new ContainerWithLifecyclePanel<>(id, getRowModel());
                            }

                            @Override
                            protected void processHide(AjaxRequestTarget target) {
                                super.processHide(target);
                                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, getRowModel().getObject());
                            }
                        };

                        getPageBase().showMainPopup(popupPanel, target);

                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
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
            PrismContainerValue<C> value, IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target, boolean isDuplicate);

    protected abstract void onSuggestValue(
            PrismContainerValue<C> value, IModel<PrismContainerWrapper<C>> newWrapperModel, AjaxRequestTarget target);

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<C>> valueModel, AjaxRequestTarget target);

    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @SuppressWarnings("rawtypes")
    public MultivalueContainerListPanel getTable() {
        Component component = get(getPageBase().createComponentPath(ID_FORM, ID_TABLE));
        if (component instanceof MultivalueContainerListPanel) {
            return (MultivalueContainerListPanel) component;
        } else {
            return null;
        }
    }

    /**
     * Determines whether the panel should display a special UI component
     * (e.g. {@link NoValuePanel}) when there are no values
     * present in the container.
     */
    protected boolean allowNoValuePanel() {
        return false;
    }

    /**
     * Checks whether the container at the specified path has any values.
     * <p>
     * This method inspects the {@link PrismObjectWrapper} associated with the current resource
     * and attempts to retrieve the {@link PrismContainerWrapper} at the path defined by
     * {@link #getTypesContainerPath()}. If the container does not exist or contains no values,
     * the method returns {@code true}.
     * </p>
     *
     * <p>This is typically used to decide whether to display the "no values" fallback panel.</p>
     *
     * @return {@code true} if the container is null or contains no values; {@code false} otherwise.
     * @throws IllegalStateException if the container cannot be found due to schema issues.
     */
    protected boolean hasNoValues() {
        PrismObjectWrapper<ResourceType> object = getObjectWrapperModel().getObject();
        if (object == null) {
            return true;
        }

        PrismContainerWrapper<C> typesContainer;
        try {
            typesContainer = object.findContainer(getTypesContainerPath());
        } catch (SchemaException e) {
            throw new IllegalStateException("Cannot find container for path: " + getTypesContainerPath(), e);
        }
        if (typesContainer == null) {
            return true;
        }

        return typesContainer.getValues() == null || typesContainer.getValues().isEmpty();
    }

    protected void showSuggestConfirmDialog(
            @NotNull PageBase pageBase,
            StringResourceModel confirmModel, AjaxRequestTarget target) {
        SmartSuggestConfirmationPanel dialog = new SmartSuggestConfirmationPanel(pageBase.getMainPopupBodyId(), confirmModel) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                onSuggestValue(
                        null, createContainerModel(), target);
            }
        };
        pageBase.showMainPopup(dialog, target);
    }

    protected boolean isSuggestButtonVisible() {
        return true;
    }
}
