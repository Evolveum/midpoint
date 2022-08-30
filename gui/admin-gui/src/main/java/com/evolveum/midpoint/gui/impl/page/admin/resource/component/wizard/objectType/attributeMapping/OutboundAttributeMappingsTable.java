/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public abstract class OutboundAttributeMappingsTable extends AttributeMappingsTable{
    public OutboundAttributeMappingsTable(String id, ResourceDetailsModel model, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model, valueModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_OUTBOUND_MAPPING_WIZARD;
    }

    @Override
    protected WrapperContext.AttributeMappingType getMappingType() {
        return WrapperContext.AttributeMappingType.OUTBOUND;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "OutboundAttributeMappingsTable.newObject.simple";
    }

    @Override
    protected Collection<? extends IColumn<PrismContainerValueWrapper<MappingType>, String>> createCustomColumns() {

        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_SOURCE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

//        columns.add(new IconColumn<>(Model.of()) {
//            @Override
//            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
//                return new DisplayType().beginIcon().cssClass("fa fa-minus text-secondary").end();
//            }
//
//            @Override
//            public String getCssClass() {
//                return "";
//            }
//        });

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return new DisplayType().beginIcon().cssClass("fa fa-arrow-right-long text-secondary").end();
            }

            @Override
            public String getCssClass() {
                return "";
            }
        });

        Model<PrismContainerDefinition<ResourceAttributeDefinitionType>> resourceAttributeDef =
                Model.of(PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(
                        ResourceAttributeDefinitionType.class));
        columns.add(new PrismPropertyWrapperColumn(
                resourceAttributeDef,
                ResourceAttributeDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        return columns;
    }
}
