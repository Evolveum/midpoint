/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.outbound.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attributeMapping.AttributeMappingsTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * @author lskublik
 */
public abstract class AssociationOutboundAttributeMappingsTable extends AttributeMappingsTable<AssociationConstructionExpressionEvaluatorType, AttributeOutboundMappingsDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationOutboundAttributeMappingsTable.class);

    public AssociationOutboundAttributeMappingsTable(
            String id, IModel<PrismContainerValueWrapper<AssociationConstructionExpressionEvaluatorType>> valueModel, ContainerPanelConfigurationType config) {
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
        return MappingDirection.ATTRIBUTE;
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

                FocusDefinitionsMappingProvider provider;
                if (isMappingForAssociation()) {
                    provider = new FocusDefinitionsMappingProvider((IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel) {
                        @Override
                        protected PrismContainerDefinition<? extends Containerable> getFocusTypeDefinition(ResourceObjectTypeDefinitionType resourceObjectType) {
                            return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);
                        }
                    };
                } else {
                    provider = new FocusDefinitionsMappingProvider((IModel<PrismPropertyWrapper<VariableBindingDefinitionType>>) rowModel);
                }
                return new Select2MultiChoiceColumnPanel<>(componentId, multiselectModel, provider);
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

        Model<PrismContainerDefinition<AttributeOutboundMappingsDefinitionType>> resourceAttributeDef =
                Model.of(PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(
                        AttributeOutboundMappingsDefinitionType.class));

        columns.add(createVirtualRefItemColumn(resourceAttributeDef, "col-xl-2 col-lg-2 col-md-3"));

        return columns;
    }

    private boolean isMappingForAssociation() {
        var associationParent = getValueModel().getObject().getParentContainerValue(ShadowAssociationDefinitionType.class);
        return associationParent != null;
    }

    @Override
    protected ItemName getItemNameOfRefAttribute() {
        return AttributeOutboundMappingsDefinitionType.F_REF;
    }

    @Override
    protected ItemPathType getAttributeRefAttributeValue(PrismContainerValueWrapper<AttributeOutboundMappingsDefinitionType> value) {
        return value.getRealValue().getRef();
    }

    @Override
    protected String getRefColumnPrefix() {
        return "OUTBOUND.";
    }
}
