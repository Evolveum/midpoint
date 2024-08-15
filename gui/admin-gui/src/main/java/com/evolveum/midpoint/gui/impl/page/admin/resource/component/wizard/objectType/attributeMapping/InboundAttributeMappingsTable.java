/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class InboundAttributeMappingsTable<P extends Containerable> extends AttributeMappingsTable<P>{
    public InboundAttributeMappingsTable(
            String id, IModel<PrismContainerValueWrapper<P>> valueModel,
            ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
    }

    enum UsedFor {
        CORRELATION(InboundMappingUseType.CORRELATION,
                "text-warning fa fa-code-branch",
                "UsedFor.CORRELATION"),
        SYNCHRONIZATION(InboundMappingUseType.SYNCHRONIZATION,
                "text-warning fa fa-rotate",
                "UsedFor.SYNCHRONIZATION"),
        ALL(InboundMappingUseType.ALL,
                "text-info fa fa-retweet",
                "UsedFor.ALL");

        private final InboundMappingUseType type;
        private final String icon;

        private final String tooltip;

        UsedFor(InboundMappingUseType type, String icon, String tooltip) {
            this.type = type;
            this.icon = icon;
            this.tooltip = tooltip;
        }
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_INBOUND_MAPPING_WIZARD;
    }

    @Override
    protected MappingDirection getMappingType() {
        return MappingDirection.INBOUND;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "InboundAttributeMappingsTable.newObject";
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<MappingType>, String> createUsedIconColumn() {
        return new IconColumn<>(Model.of()) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<MappingType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
                cellItem.add(AttributeAppender.append("class", "text-center"));
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                PrismContainerValueWrapper<MappingType> mapping = rowModel.getObject();
                MappingType mappingBean = mapping.getRealValue();

                InboundMappingUseType mappingUsed = ((InboundMappingType) mappingBean).getUse();
                if (mappingUsed == null) {
                    mappingUsed = InboundMappingUseType.ALL;
                }
                for (UsedFor usedFor : Arrays.stream(UsedFor.values()).toList()) {
                    if (usedFor.type.equals(mappingUsed)) {
                        return new DisplayType()
                                .tooltip(usedFor.tooltip)
                                .beginIcon()
                                .cssClass(usedFor.icon)
                                .end();
                    }
                }
                return new DisplayType();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        };
    }

    @Override
    protected Collection<? extends IColumn<PrismContainerValueWrapper<MappingType>, String>> createCustomColumns() {

        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        Model<PrismContainerDefinition<ResourceAttributeDefinitionType>> resourceAttributeDef =
                Model.of(PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(
                        ResourceAttributeDefinitionType.class));

        columns.add(createVirtualRefItemColumn(resourceAttributeDef, null));

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return new DisplayType().beginIcon().cssClass("fa fa-minus text-secondary").end();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                getMappingTypeDefinition(),
                MappingType.F_EXPRESSION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return new DisplayType().beginIcon().cssClass("fa fa-arrow-right-long text-secondary").end();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_TARGET,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        return columns;
    }

    @Override
    protected boolean isHeaderVisible() {
        return true;
    }

    @Override
    protected Component createHeader(String headerId) {
        DropDownChoicePanel<UsedFor> dropdown = WebComponentUtil.createEnumPanel(
                headerId,
                WebComponentUtil.createReadonlyModelFromEnum(UsedFor.class),
                Model.of(),
                InboundAttributeMappingsTable.this,
                true,
                getString("InboundAttributeMappingsTable.allMappings"));
        dropdown.getBaseFormComponent().add(AttributeAppender.append("style", "width: 220px;"));
        dropdown.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                refreshTable(target);
            }
        });
        return dropdown;
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<MappingType>> createProvider() {
        return new MultivalueContainerListDataProvider<>(
                InboundAttributeMappingsTable.this,
                getSearchModel(),
                new PropertyModel<>(getContainerModel(), "values")) {

            @Override
            protected List<PrismContainerValueWrapper<MappingType>> searchThroughList() {
                List<PrismContainerValueWrapper<MappingType>> list = super.searchThroughList();

                if (list == null || list.isEmpty()) {
                    return null;
                }

                UsedFor usedFor = getSelectedTypeOfMappings();
                if (usedFor == null) {
                    return list;
                }

                list.removeIf(valueWrapper -> {
                    InboundMappingType realValue = (InboundMappingType) valueWrapper.getRealValue();
                    InboundMappingUseType valueUse = realValue.getUse();
                    if (valueUse == null) {
                        valueUse = InboundMappingUseType.ALL;
                    }
                    UsedFor valueUsedFor = UsedFor.valueOf(valueUse.name());

                    return !usedFor.equals(valueUsedFor);
                });
                return list;
            }
        };
    }

    private UsedFor getSelectedTypeOfMappings() {
        DropDownChoicePanel<UsedFor> header = (DropDownChoicePanel<UsedFor>) getTable().getHeader();
        return header.getModel().getObject();
    }
}
