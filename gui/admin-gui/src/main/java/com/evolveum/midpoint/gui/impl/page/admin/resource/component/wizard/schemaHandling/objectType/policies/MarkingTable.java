/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */
public class MarkingTable extends AbstractWizardTable<ShadowMarkingConfigurationType, ResourceObjectTypeDefinitionType> {

    public MarkingTable(
            String id,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config, ShadowMarkingConfigurationType.class);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ShadowMarkingConfigurationType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ShadowMarkingConfigurationType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ShadowMarkingConfigurationType>> markingDef = getMarkingDefinition();
        columns.add(new PrismReferenceWrapperColumn<>(
                markingDef,
                ShadowMarkingConfigurationType.F_MARK_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-8";
            }
        });

//        columns.add(new PrismPropertyWrapperColumn<ShadowMarkingConfigurationType, String>(
//                markingDef,
//                ItemPath.create(
//                        ShadowMarkingConfigurationType.F_PATTERN,
//                        ResourceObjectPatternType.F_FILTER),
//                AbstractItemWrapperColumn.ColumnType.VALUE,
//                getPageBase()) {
//
//            @Override
//            public String getCssClass() {
//                return "col-6";
//            }
//        });

        columns.add(new PrismPropertyWrapperColumn<ShadowMarkingConfigurationType, String>(
                markingDef,
                ShadowMarkingConfigurationType.F_APPLICATION_TIME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-4";
            }
        });

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<ShadowMarkingConfigurationType>> getMarkingDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ShadowMarkingConfigurationType> load() {
                return getValueModel().getObject().getDefinition()
                        .findContainerDefinition(ResourceObjectTypeDefinitionType.F_MARKING);
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CORRELATION_ITEMS_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "MarkingTable.newObject.simple";
    }

    @Override
    protected IModel<PrismContainerWrapper<ShadowMarkingConfigurationType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(getValueModel(), ResourceObjectTypeDefinitionType.F_MARKING);
    }

    @Override
    protected boolean isDuplicationSupported() {
        return false;
    }
}
