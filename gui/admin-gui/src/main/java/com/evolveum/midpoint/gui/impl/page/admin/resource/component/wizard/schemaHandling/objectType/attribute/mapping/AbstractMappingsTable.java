/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
public abstract class AbstractMappingsTable<P extends Containerable> extends AbstractWizardTable<MappingType, P> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractMappingsTable.class);

    public AbstractMappingsTable(
            String id,
            IModel<PrismContainerValueWrapper<P>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config, MappingType.class);
    }

    @Override
    protected String getInlineMenuCssClass() {
        return "";
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<MappingType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();

        IColumn<PrismContainerValueWrapper<MappingType>, String> iconColumns = createUsedIconColumn();
        Optional.ofNullable(iconColumns).ifPresent(column -> columns.add(column));

        IColumn<PrismContainerValueWrapper<MappingType>, String> strengthColumns = createStrengthIconColumn();
        Optional.ofNullable(strengthColumns).ifPresent(column -> columns.add(column));

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @Override
            public String getCssClass() {
                return "col-xl-2 col-lg-2 col-md-2";
            }
        });

        columns.addAll(createCustomColumns());

        columns.add(new LifecycleStateColumn<>(getContainerModel(), getPageBase()));

        return columns;
    }

    protected IColumn<PrismContainerValueWrapper<MappingType>, String> createStrengthIconColumn() {
        return new IconColumn<>(Model.of()) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
                cellItem.add(AttributeAppender.append("class", "text-center"));
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return GuiDisplayTypeUtil.getDisplayTypeForStrengthOfMapping(rowModel);
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        };
    }

    protected IColumn<PrismContainerValueWrapper<MappingType>, String> createUsedIconColumn() {
        return null;
    }

    protected abstract Collection<? extends IColumn<PrismContainerValueWrapper<MappingType>, String>> createCustomColumns();

    protected final <IW extends ItemWrapper> IModel<Collection<VariableBindingDefinitionType>> createSourceMultiselectModel(IModel<IW> rowModel) {
        return new IModel<>() {

            @Override
            public Collection<VariableBindingDefinitionType> getObject() {

                return ((PrismPropertyWrapper<VariableBindingDefinitionType>) rowModel.getObject())
                        .getValues().stream()
                        .filter(value -> !ValueStatus.DELETED.equals(value.getStatus()) && value.getRealValue() != null)
                        .map(PrismValueWrapperImpl::getRealValue)
                        .collect(Collectors.toList());
            }

            @Override
            public void setObject(Collection<VariableBindingDefinitionType> newValues) {

                PrismPropertyWrapper<VariableBindingDefinitionType> sourceItem =
                        ((PrismPropertyWrapper<VariableBindingDefinitionType>) rowModel.getObject());
                List<PrismPropertyValueWrapper<VariableBindingDefinitionType>> toRemoveValues
                        = sourceItem.getValues().stream()
                        .filter(v -> v.getRealValue() != null).collect(Collectors.toList());

                newValues.forEach(newValue -> {
                    if (newValue.getPath() == null) {
                        return;
                    }
                    Optional<PrismPropertyValueWrapper<VariableBindingDefinitionType>> found = sourceItem.getValues().stream()
                            .filter(actualValue -> actualValue.getRealValue() != null
                                    && actualValue.getRealValue().getPath() != null
                                    && newValue.getPath().getItemPath().stripVariableSegment()
                                    .equals(actualValue.getRealValue().getPath().getItemPath().stripVariableSegment()))
                            .findFirst();
                    if (found.isPresent()) {
                        toRemoveValues.remove(found.get());
                        if (ValueStatus.DELETED.equals(found.get().getStatus())) {
                            found.get().setStatus(ValueStatus.NOT_CHANGED);
                        }
                    } else {
                        try {
                            PrismPropertyValue<VariableBindingDefinitionType> newPrismValue
                                    = getPrismContext().itemFactory().createPropertyValue();
                            newPrismValue.setValue(newValue);
                            sourceItem.add(newPrismValue, getPageBase());
                        } catch (SchemaException e) {
                            LOGGER.error("Couldn't initialize new value for Source item", e);
                        }
                    }
                });

                toRemoveValues.forEach(toRemoveValue -> {
                    try {
                        sourceItem.remove(toRemoveValue, getPageBase());
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't remove old value for Source item", e);
                    }
                });

                List<PrismPropertyValueWrapper<VariableBindingDefinitionType>> undeletedValues = sourceItem.getValues().stream()
                        .filter(value -> value.getStatus() != ValueStatus.DELETED)
                        .collect(Collectors.toList());
                if (undeletedValues.stream().filter(value -> value.getRealValue() != null).count() > 0) {
                    sourceItem.getValues().removeIf(value -> value.getRealValue() == null);
                } else if (undeletedValues.isEmpty()) {
                    try {
                        PrismPropertyValue<VariableBindingDefinitionType> newPrismValue
                                = getPrismContext().itemFactory().createPropertyValue();
                        newPrismValue.setValue(null);
                        sourceItem.add(newPrismValue, getPageBase());
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't initialize new null value for Source item", e);
                    }
                }

            }
        };
    }

    protected final LoadableModel<PrismContainerDefinition<MappingType>> getMappingTypeDefinition() {
        return WebComponentUtil.getContainerDefinitionModel(MappingType.class);
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = new ArrayList<>();
        AjaxIconButton newObjectButton = new AjaxIconButton(
                idButton,
                new Model<>("fa fa-circle-plus"),
                createStringResource(getKeyOfTitleForNewObjectButton())) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                newItemPerformed(target, null);
            }
        };
        newObjectButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm ml-3"));
        newObjectButton.add(new VisibleBehaviour(this::isCreateNewObjectVisible));
        newObjectButton.showTitleAsLabel(true);
        buttons.add(newObjectButton);

        return buttons;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> items = new ArrayList<>();

        ButtonInlineMenuItem changeLifecycle = new ButtonInlineMenuItem(
                createStringResource("AttributeMappingsTable.button.changeLifecycle")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-heart-pulse");
            }

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> isHeader;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createChangeLifecycleColumnAction();
            }
        };
        items.add(changeLifecycle);

        items.addAll(super.createInlineMenu());

        InlineMenuItem changeMappingName = new InlineMenuItem(
                createStringResource("AttributeMappingsTable.button.changeMappingName")) {
            private static final long serialVersionUID = 1L;

            @Override
            public VisibilityChecker getVisibilityChecker() {
                return (rowModel, isHeader) -> {
                    if (isHeader) {
                        return false;
                    }

                    if(rowModel == null || rowModel.getObject() == null) {
                        return false;
                    }

                    try {
                        PrismPropertyWrapper<String> property =
                                ((PrismContainerValueWrapper<MappingType>) rowModel.getObject()).findProperty(MappingType.F_NAME);
                        return property.isReadOnly();
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't find name property in " + rowModel.getObject());
                    }

                    return false;
                };
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createChangeNameColumnAction();
            }
        };
        items.add(changeMappingName);
        return items;
    }

    private InlineMenuItemAction createChangeLifecycleColumnAction() {
        return new InlineMenuItemAction() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<List<PrismContainerValueWrapper<MappingType>>> selected = () -> getSelectedItems();
                if (selected.getObject().isEmpty()) {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getFeedbackPanel());
                    return;
                }
                ChangeLifecycleSelectedMappingsPopup changePopup = new ChangeLifecycleSelectedMappingsPopup(
                        getPageBase().getMainPopupBodyId(),
                        selected) {
                    @Override
                    protected void applyChanges(AjaxRequestTarget target) {
                        super.applyChanges(target);
                        refreshTable(target);
                    }
                };

                getPageBase().showMainPopup(changePopup, target);
            }
        };
    }

    private InlineMenuItemAction createChangeNameColumnAction() {
        return new ColumnMenuAction() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<PrismContainerValueWrapper<MappingType>> selected = getRowModel();
                if (getRowModel().getObject() == null) {
                    warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
                    target.add(getFeedbackPanel());
                    return;
                }
                ChangeMappingNamePopup changePopup = new ChangeMappingNamePopup(
                        getPageBase().getMainPopupBodyId(),
                        selected) {
                    @Override
                    protected void applyChanges(AjaxRequestTarget target) {
                        super.applyChanges(target);
                        refreshTable(target);
                    }
                };

                getPageBase().showMainPopup(changePopup, target);
            }
        };
    }
}
