/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer.help;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.SmartMappingTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundAttributeMappingsTable.getMappingUsedIconColumn;

public abstract class AssociationSmartAttributeMappingsTable<C extends Containerable>
        extends SmartMappingTable<C> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationSmartAttributeMappingsTable.class);

    public AssociationSmartAttributeMappingsTable(
            @NotNull String id,
            @NotNull IModel<MappingDirection> mappingDirection,
            @NotNull IModel<Boolean> suggestionToggleModel,
            @NotNull IModel<PrismContainerValueWrapper<C>> refAttributeDefValue,
            @Nullable String resourceOid) {
        super(id, mappingDirection, suggestionToggleModel, refAttributeDefValue, resourceOid);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        if (isInboundRelated()) {
            return UserProfileStorage.TableId.PANEL_INBOUND_MAPPING_WIZARD;
        } else {
            return UserProfileStorage.TableId.PANEL_OUTBOUND_MAPPING_WIZARD;
        }
    }

    @Override
    protected ISortableDataProvider<PrismContainerValueWrapper<MappingType>, String> createDataProvider() {
        return new MultivalueContainerListDataProvider<>(
                this,
                Model.of(), //TODO search
                new PropertyModel<>(getContainerModel(), "values"));
    }

    protected abstract boolean isInboundRelated();

    private boolean isMappingForAssociation() {
        var value = getValueModel() != null ? getValueModel().getObject() : null;
        return value != null && value.getParentContainerValue(ShadowAssociationDefinitionType.class) != null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> getColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        if (isInboundRelated()) {
            columns.add(getMappingUsedIconColumn("tile-column-icon"));
        }

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                return GuiDisplayTypeUtil.getDisplayTypeForStrengthOfMapping(rowModel, "text-muted");
            }

            @Override
            public String getCssClass() {
                return "px-0 tile-column-icon";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                getMappingTypeDefinition(),
                MappingType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col header-border-right";
            }

            @Override
            public String getSortProperty() {
                return MappingType.F_NAME.getLocalPart();
            }
        });

        IModel<? extends PrismContainerDefinition<?>> refDefModel = isInboundRelated()
                ? Model.of(PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AttributeInboundMappingsDefinitionType.class))
                : Model.of(PrismContext.get().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(AttributeOutboundMappingsDefinitionType.class));

        columns.add(new PrismPropertyWrapperColumn(
                refDefModel,
                AbstractAttributeMappingsDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-2 header-border-right";
            }

            @Override
            protected Component createHeader(String componentId, IModel mainModel) {
                return super.createHeader(componentId, mainModel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<>(
                getMappingTypeDefinition(),
                MappingType.F_EXPRESSION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-2 header-border-right";
            }
        });

        if (!isInboundRelated()) {
            columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                    getMappingTypeDefinition(),
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
                                    .filter(value
                                            -> !ValueStatus.DELETED.equals(value.getStatus()) && value.getRealValue() != null)
                                    .map(PrismValueWrapperImpl::getRealValue)
                                    .collect(Collectors.toList());
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public void setObject(@NotNull Collection<VariableBindingDefinitionType> newValues) {

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
                                    .toList();
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
                    return "col-2 header-border-right";
                }
            });
        } else {
            columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                    getMappingTypeDefinition(),
                    MappingType.F_TARGET,
                    AbstractItemWrapperColumn.ColumnType.VALUE,
                    getPageBase()) {
                @Override
                public String getCssClass() {
                    return "col-2 header-border-right";
                }
            });
        }

        // lifecycle
        columns.add(new LifecycleStateColumn<>(getMappingTypeDefinition(), getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-auto";
            }
        });

        return columns;
    }

    @Override
    protected ItemName getItemNameOfRefAttribute() {
        return AbstractAttributeMappingsDefinitionType.F_REF;
    }
}
