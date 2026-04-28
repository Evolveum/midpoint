/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.table;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.AbstractMappingsTable.createSourceMultiselectModel;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundAttributeMappingsTable.getMappingUsedIconColumn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.PreviewMappingPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Builds column definitions for {@link SmartMappingTable}.
 *
 * <p>Encapsulates all column-related UI logic, including ordering and
 * direction-specific columns (inbound/outbound).
 *
 * <p>Does not handle data loading or actions.
 */
record SmartMappingColumns<P extends Containerable>(SmartMappingTable<P> table) implements Serializable {

    @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> getColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        if (table.isInbound()) {
            columns.add(getMappingUsedIconColumn("tile-column-icon"));
        }

        columns.add(createStrengthIconColumn());
        columns.add(createNameColumn());

        if (table.isOutbound()) {
            columns.add(createSourceColumn());
            columns.add(createExpressionColumn());
            columns.add(createRefColumn());
        } else {
            columns.add(createRefColumn());
            columns.add(createExpressionColumn());
            columns.add(createTargetColumn());
        }

        columns.add(createLifecycleColumn());
        return columns;
    }

    private IColumn<PrismContainerValueWrapper<MappingType>, String> createStrengthIconColumn() {
        return new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(
                    IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return GuiDisplayTypeUtil.getDisplayTypeForStrengthOfMapping(rowModel, "text-muted");
            }

            @Override
            public String getCssClass() {
                return "px-0 tile-column-icon";
            }
        };
    }

    private IColumn<PrismContainerValueWrapper<MappingType>, String> createNameColumn() {
        return new PrismPropertyWrapperColumn<>(
                table.getMappingTypeDefinition(),
                MappingType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                table.getPageBase()) {

            @Override
            public @NotNull String getCssClass() {
                return "col-2 header-border-right";
            }

            @Override
            public String getSortProperty() {
                return MappingType.F_NAME.getLocalPart();
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private IColumn<PrismContainerValueWrapper<MappingType>, String> createRefColumn() {
        return new PrismPropertyWrapperColumn(
                table.getRefColumnDefinitionModel(),
                ResourceAttributeDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                table.getPageBase()) {

            @Override
            protected Component createHeader(String componentId, IModel mainModel) {
                return createPropertyHeader(componentId, itemName,
                        table.getMappingDirectionType().name() + "." + ResourceAttributeDefinitionType.F_REF,
                        mainModel);
            }

            @Override
            public @NotNull String getCssClass() {
                return "col-2 header-border-right";
            }
        };
    }

    private IColumn<PrismContainerValueWrapper<MappingType>, String> createExpressionColumn() {
        return new PrismPropertyWrapperColumn<>(
                table.getMappingTypeDefinition(),
                MappingType.F_EXPRESSION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                table.getPageBase()) {

            @Override
            public @NotNull String getCssClass() {
                return "col-2 header-border-right";
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected <IW extends ItemWrapper> @NotNull Component createColumnPanel(
                    String componentId,
                    IModel<IW> rowModel) {

                Component panel = super.createColumnPanel(componentId, rowModel);
                panel.setOutputMarkupId(true);

                if (isMappingTypeRealValue(rowModel)) {

                    //noinspection unchecked
                    PrismContainerValueWrapper<MappingType> mappingWrapper =
                            (PrismContainerValueWrapper<MappingType>) rowModel.getObject().getParent();

                    if (table.getStatusInfo(mappingWrapper) != null) {
                        panel.add(AttributeModifier.append("class", "btn-link cursor-pointer"));
                        panel.add(new AjaxEventBehavior("click") {
                            @Override
                            protected void onEvent(AjaxRequestTarget target) {
                                PreviewMappingPanel preview =
                                        table.getActions().buildPreviewMappingPanelPopup(() -> mappingWrapper);
                                table.getPageBase().showMainPopup(preview, target);
                            }
                        });
                    }
                }

                return panel;
            }
        };
    }

    private static <IW extends ItemWrapper<?, ?>> boolean isMappingTypeRealValue(IModel<IW> rowModel) {
        return rowModel != null
                && rowModel.getObject() != null
                && rowModel.getObject().getParent() != null
                && rowModel.getObject().getParent().getRealValue() instanceof MappingType;
    }

    private IColumn<PrismContainerValueWrapper<MappingType>, String> createSourceColumn() {
        return new PrismPropertyWrapperColumn<MappingType, String>(
                table.getMappingTypeDefinition(),
                MappingType.F_SOURCE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                table.getPageBase()) {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(
                    String componentId,
                    IModel<IW> rowModel) {

                if (rowModel.getObject().isReadOnly()) {
                    return super.createColumnPanel(componentId, rowModel);
                }

                IModel<Collection<VariableBindingDefinitionType>> multiselectModel =
                        createSourceMultiselectModel(rowModel, table.getPageBase());
                FocusDefinitionsMappingProvider provider =
                        new FocusDefinitionsMappingProvider(
                                (IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel);

                return new Select2MultiChoiceColumnPanel<>(componentId, multiselectModel, provider);
            }

            @Override
            protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<MappingType>> mainModel) {
                return createPropertyHeader(componentId, itemName, "SmartMappingColumns.midPoint.property", mainModel);
            }

            @Override
            public String getCssClass() {
                return "col-2 header-border-right";
            }
        };
    }

    private IColumn<PrismContainerValueWrapper<MappingType>, String> createTargetColumn() {
        return new PrismPropertyWrapperColumn<MappingType, String>(
                table.getMappingTypeDefinition(),
                MappingType.F_TARGET,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                table.getPageBase()) {

            @Override
            protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<MappingType>> mainModel) {
                return createPropertyHeader(componentId, itemName, "SmartMappingColumns.midPoint.property", mainModel);
            }

            @Override
            public String getCssClass() {
                return "col-2 header-border-right";
            }
        };
    }

    private @NotNull PrismPropertyHeaderPanel<ItemPathType> createPropertyHeader(
            String componentId,
            ItemPath itemName,
            String headerLabelKey,
            IModel<? extends PrismContainerDefinition<MappingType>> mainModel) {
        return new PrismPropertyHeaderPanel<>(
                componentId,
                new PrismPropertyWrapperHeaderModel<>(mainModel, itemName, table.getPageBase())) {

            @Override
            protected boolean isAddButtonVisible() {
                return false;
            }

            @Override
            protected boolean isButtonEnabled() {
                return false;
            }

            @Override
            protected Component createTitle(IModel<String> label) {
                return super.createTitle(table.getPageBase()
                        .createStringResource(headerLabelKey));
            }
        };
    }

    private @NotNull IColumn<PrismContainerValueWrapper<MappingType>, String> createLifecycleColumn() {
        return new LifecycleStateColumn<>(table.getMappingTypeDefinition(), table.getPageBase()) {
            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                if (isMappingTypeRealValue(rowModel)) {
                    //noinspection unchecked
                    PrismContainerValueWrapper<MappingType> mappingWrapper =
                            (PrismContainerValueWrapper<MappingType>) rowModel.getObject().getParent();

                    if (table.getStatusInfo(mappingWrapper) != null) {
                        return new EmptyPanel(componentId);
                    }
                }
                return super.createColumnPanel(componentId, rowModel);
            }

            @Override
            public String getCssClass() {
                return "col-auto";
            }
        };
    }
}
