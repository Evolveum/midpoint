/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingValueWrapper;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.input.TwoStateBooleanPanel;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.ValidatorAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
public abstract class MappingOverrideTable extends MultivalueContainerListPanel<ResourceAttributeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingOverrideTable.class);

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public MappingOverrideTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, ResourceAttributeDefinitionType.class);
        this.valueModel = valueModel;
    }

//    @Override
//    protected void onBeforeRender() {
//        super.onBeforeRender();
//
//        getTable().setShowAsCard(false);
//    }

    @Override
    protected boolean isHeaderVisible() {
        return false;
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = super.createToolbarButtonsList(idButton);
        buttons.forEach(button -> {
            if (button instanceof AjaxIconButton) {
                ((AjaxIconButton) button).showTitleAsLabel(true);
            }
        });
        return buttons;
    }

    @Override
    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec) {
        createNewOverride(target);
        refreshTable(target);
    }

    protected PrismContainerValueWrapper createNewOverride(AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> mappingAttributeContainer =
                    valueModel.getObject().findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
            PrismContainerValue<ResourceAttributeDefinitionType> newMapping
                    = mappingAttributeContainer.getItem().createNewValue();

            ResourceAttributeMappingValueWrapper newAttributeMappingWrapper =
                    WebPrismUtil.createNewValueWrapper(mappingAttributeContainer, newMapping, getPageBase(), target);
            newAttributeMappingWrapper.addAttributeMappingType(WrapperContext.AttributeMappingType.OVERRIDE);

            return newAttributeMappingWrapper;
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new attribute mapping");
        }
        return null;
    }

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {

        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditColumnAction();
            }
        };
        items.add(item);

        item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        };
        items.add(item);
        return items;
    }

    @Override
    protected IModel<PrismContainerWrapper<ResourceAttributeDefinitionType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(valueModel, ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> createProvider() {
        return new MultivalueContainerListDataProvider<>(MappingOverrideTable.this, getSearchModel(), new PropertyModel<>(getContainerModel(), "values")) {

            @Override
            protected PageStorage getPageStorage() {
                return MappingOverrideTable.this.getPageStorage();
            }

            @Override
            protected List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> searchThroughList() {
                List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> list = super.searchThroughList();
                return list.stream().filter(value -> {
                    if (value instanceof ResourceAttributeMappingValueWrapper) {
                        return ((ResourceAttributeMappingValueWrapper) value).getAttributeMappingTypes()
                                .contains(WrapperContext.AttributeMappingType.OVERRIDE);
                    }
                    return true;
                }).collect(Collectors.toList());
            }
        };
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ResourceAttributeDefinitionType>> attributeDef = getAttributeDefinition();
        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
                attributeDef,
                ResourceAttributeDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
                attributeDef,
                ResourceAttributeDefinitionType.F_DISPLAY_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("MappingOverrideTable.column.description")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> iModel) {
                Label help = new Label(id);
                IModel<String> helpModel = () -> iModel.getObject().getRealValue().getDescription();
                help.add(AttributeModifier.replace("title", createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
                help.add(new InfoTooltipBehavior() {
                    @Override
                    public String getCssClass() {
                        String cssClass = "fa fa-info-circle";
                        cssClass = cssClass + (helpModel.getObject() != null ? " text-info" : " text-secondary");
                        return cssClass;
                    }
                });
                item.add(help);
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("MappingOverrideTable.column.mandatoryField")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel) {

                IModel<Boolean> mandatoryModel =
                        createModelForOccurs(rowModel, PropertyLimitationsType.F_MIN_OCCURS, "1", "0");
                TwoStateBooleanPanel panel = new TwoStateBooleanPanel(id, mandatoryModel);
                item.add(panel);
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("MappingOverrideTable.column.multiValueField")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel) {

                IModel<Boolean> multivalueModel =
                        createModelForOccurs(rowModel, PropertyLimitationsType.F_MAX_OCCURS, "unbounded", "1");
                TwoStateBooleanPanel panel = new TwoStateBooleanPanel(id, multivalueModel);
                item.add(panel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
                attributeDef,
                ResourceAttributeDefinitionType.F_TOLERANT,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

//        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
//                attributeDef,
//                ItemPath.create(ResourceAttributeDefinitionType.F_CORRELATOR, ItemCorrelatorDefinitionType.),
//                AbstractItemWrapperColumn.ColumnType.VALUE,
//                getPageBase()));

        return columns;
    }

    private IModel<Boolean> createModelForOccurs(IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel, ItemName path, String trueValue, String falseValue) {
        return new IModel<>() {
            @Override
            public Boolean getObject() {
                Boolean isTrueOccurs = null;
                try {
                    PrismContainerWrapper<PropertyLimitationsType> limitation =
                            rowModel.getObject().findContainer(ResourceAttributeDefinitionType.F_LIMITATIONS);
                    if (limitation != null && !limitation.getValues().isEmpty()){
                        PrismPropertyWrapper<String> occurs = limitation.getValues().iterator().next().findProperty(path);
                        if (occurs != null) {
                            isTrueOccurs = trueValue.equals(occurs.getValue().getRealValue());
                        }
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create new value for limitation container", e);
                }
                return Boolean.TRUE.equals(isTrueOccurs);
            }

            @Override
            public void setObject(Boolean isTrueOccurs) {
                if (isTrueOccurs == null) {
                    return;
                }
                try {
                    PrismContainerWrapper<PropertyLimitationsType> limitation =
                            rowModel.getObject().findContainer(ResourceAttributeDefinitionType.F_LIMITATIONS);
                    if (limitation.getValues().isEmpty()) {
                        if (!isTrueOccurs) {
                            return;
                        }
                        PrismContainerValue<PropertyLimitationsType> newItem = limitation.getItem().createNewValue();
                        PrismContainerValueWrapper<PropertyLimitationsType> newItemWrapper = WebPrismUtil.createNewValueWrapper(
                                limitation, newItem, getPageBase());
                        limitation.getValues().add(newItemWrapper);
                    }
                    PrismContainerValueWrapper<PropertyLimitationsType> value = limitation.getValues().iterator().next();
                    PrismPropertyWrapper<Object> minOccurs = value.findProperty(path);
                    String actualValue = (String) minOccurs.getValue().getRealValue();
                    if (isTrueOccurs) {
                        minOccurs.getValue().setRealValue(trueValue);
                    } else if (actualValue != null){
                        minOccurs.getValue().setRealValue(falseValue);
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create new value for limitation container", e);
                }
            }
        };
    }

    protected LoadableModel<PrismContainerDefinition<ResourceAttributeDefinitionType>> getAttributeDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ResourceAttributeDefinitionType> load() {
                return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ResourceAttributeDefinitionType.class);
            }
        };
    }

//    @Override
//    public void refreshTable(AjaxRequestTarget target) {
//        getContainerModel().detach();
//        clearCache();
//        super.refreshTable(target);
//    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_MAPPING_OVERRIDE_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "MappingOverrideTable.newObject";
    }

    public boolean isValidFormComponents(AjaxRequestTarget target) {
        AtomicReference<Boolean> valid = new AtomicReference<>(true);
        getTable().visitChildren(SelectableDataTable.SelectableRowItem.class, (row, object) -> {
            ((SelectableDataTable.SelectableRowItem) row).visitChildren(FormComponent.class, (baseFormComponent, object2) -> {
                baseFormComponent.getBehaviors().stream()
                        .filter(behaviour -> behaviour instanceof ValidatorAdapter
                                && ((ValidatorAdapter)behaviour).getValidator() instanceof NotNullValidator)
                        .map(adapter -> ((ValidatorAdapter)adapter).getValidator())
                        .forEach(validator -> ((NotNullValidator)validator).setUseModel(true));
                ((FormComponent)baseFormComponent).validate();
                if (baseFormComponent.hasErrorMessage()) {
                    valid.set(false);
                    if (target != null) {
                        target.add(baseFormComponent);
                        InputPanel inputParent = baseFormComponent.findParent(InputPanel.class);
                        if (inputParent != null && inputParent.getParent() != null) {
                            target.addChildren(inputParent.getParent(), FeedbackLabels.class);
                        }
                    }
                }
            });
        });
        return valid.get();
    }
}
