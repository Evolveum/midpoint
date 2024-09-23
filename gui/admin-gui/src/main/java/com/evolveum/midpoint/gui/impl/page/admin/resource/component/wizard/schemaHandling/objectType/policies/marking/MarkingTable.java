/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.policies.marking;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
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

import org.apache.wicket.model.Model;

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

        columns.add(new IconColumn<>(Model.of()) {

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ShadowMarkingConfigurationType>>> cellItem, String componentId, IModel<PrismContainerValueWrapper<ShadowMarkingConfigurationType>> rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
                cellItem.add(AttributeAppender.append("class", "text-center"));
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<ShadowMarkingConfigurationType>> rowModel) {
                PrismContainerValueWrapper<ShadowMarkingConfigurationType> marking = rowModel.getObject();
                ShadowMarkingConfigurationType markingType = marking.getRealValue();

                List<ResourceObjectPatternType> patterns = markingType.getPattern();
                if (!patterns.isEmpty()) {
                    String tooltip = null;
                    if (patterns.size() == 1 && patterns.get(0).getFilter() != null) {
                        tooltip = LocalizationUtil.translate("MarkingTable.filter", new Object[] { patterns.get(0).getFilter().getText() });
                    } else if (patterns.stream().anyMatch(pattern -> pattern.getFilter() != null)) {
                        tooltip = LocalizationUtil.translate(
                                "MarkingTable.filters",
                                new Object[] { patterns.stream().filter(pattern -> pattern.getFilter() != null).count() });
                    }

                    if (tooltip != null) {
                        return new DisplayType()
                                .tooltip(tooltip)
                                .beginIcon()
                                .cssClass("fa fa-filter text-info")
                                .end();
                    }
                }
                return new DisplayType();
            }

            @Override
            public String getCssClass() {
                return "px-0";
            }
        });

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
        return UserProfileStorage.TableId.PANEL_MARKING_WIZARD;
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
