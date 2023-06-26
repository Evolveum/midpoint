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
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoicePanel;
import com.evolveum.midpoint.gui.impl.component.input.SourceMappingProvider;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author lskublik
 */
public abstract class OutboundAttributeMappingsTable<P extends Containerable> extends AttributeMappingsTable<P> {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundAttributeMappingsTable.class);

    public OutboundAttributeMappingsTable(
            String id, IModel<PrismContainerValueWrapper<P>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
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
    protected MappingDirection getMappingType() {
        return MappingDirection.OUTBOUND;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "OutboundAttributeMappingsTable.newObject";
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
                getPageBase()) {

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {

                IModel<Collection<VariableBindingDefinitionType>> multiselectModel = new IModel<>() {

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

                    }
                };

                return new Select2MultiChoicePanel<>(componentId, multiselectModel,
                        new SourceMappingProvider((IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel));
            }

            @Override
            public String getCssClass() {
                return "col-xl-2 col-lg-2 col-md-2";
            }
        });

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

        columns.add(new PrismPropertyWrapperColumn(
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

        Model<PrismContainerDefinition<ResourceAttributeDefinitionType>> resourceAttributeDef =
                Model.of(PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(
                        ResourceAttributeDefinitionType.class));
        columns.add(new PrismPropertyWrapperColumn(
                resourceAttributeDef,
                ResourceAttributeDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()){

            @Override
            public String getCssClass() {
                return "col-xl-2 col-lg-2 col-md-3";
            }

        });

        return columns;
    }
}
