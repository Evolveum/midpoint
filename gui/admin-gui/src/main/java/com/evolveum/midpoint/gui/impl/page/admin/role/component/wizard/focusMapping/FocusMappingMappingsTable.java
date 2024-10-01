/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.component.wizard.focusMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoicePanel;
import com.evolveum.midpoint.gui.impl.component.input.SourceOfFocusMappingProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping.AbstractMappingsTable;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

/**
 * @author lskublik
 */
public class FocusMappingMappingsTable extends AbstractMappingsTable<MappingsType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusMappingMappingsTable.class);

    public FocusMappingMappingsTable(
            String id,
            IModel<PrismContainerValueWrapper<MappingsType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
    }

    @Override
    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(getValueModel(), MappingsType.F_MAPPING);
    }

    @Override
    protected PrismContainerValueWrapper createNewValue(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        PrismContainerValueWrapper<MappingType> newValue = super.createNewValue(value, target);
        if (newValue != null && newValue.getRealValue() != null) {
            try {
                PrismPropertyWrapper<MappingStrengthType> property = newValue.findProperty(MappingType.F_STRENGTH);
                property.getValue().setRealValue(MappingStrengthType.STRONG);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find property for strength in " + newValue);
            }
        }
        return newValue;
    }

    @Override
    protected Collection<? extends IColumn<PrismContainerValueWrapper<MappingType>, String>> createCustomColumns() {

        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();

        columns.add(createSourceColumn(mappingTypeDef));

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

        columns.add(new
                PrismPropertyWrapperColumn(
                mappingTypeDef,
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

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_TARGET,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        return columns;
    }

    private IColumn<PrismContainerValueWrapper<MappingType>, String> createSourceColumn(IModel<PrismContainerDefinition<MappingType>> mappingTypeDef) {
        return new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_SOURCE,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {

                IModel<Collection<VariableBindingDefinitionType>> multiselectModel = createSourceMultiselectModel(rowModel);

                SourceOfFocusMappingProvider provider = new SourceOfFocusMappingProvider(
                        (IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel);
                return new Select2MultiChoicePanel<>(componentId, multiselectModel, provider);
            }

            @Override
            public String getCssClass() {
                return "col-xl-2 col-lg-2 col-md-2";
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_FOCUS_MAPPING_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "FocusMappingMappingsTable.newObject";
    }
}
