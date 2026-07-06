/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.table.SmartMappingTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createNewVirtualMappingValue;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingUtils.createVirtualMappingContainerModel;
import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.InboundAttributeMappingsTable.getMappingUsedIconColumn;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsType.F_MAPPING;

public abstract class AssociationAttributeMappingsTable<C extends Containerable>
        extends SmartMappingTable<C> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationAttributeMappingsTable.class);

    public AssociationAttributeMappingsTable(
            @NotNull String id,
            @NotNull IModel<MappingDirection> mappingDirection,
            @NotNull IModel<Boolean> suggestionToggleModel,
            @NotNull IModel<PrismContainerValueWrapper<C>> refAttributeDefValue,
            @NotNull String resourceOid) {
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
    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {

        IModel<PrismContainerWrapper<MappingType>> virtualAttributeMappingContainerModel =
                createVirtualMappingContainerModel(
                        getPageBase(),
                        getValueModel(),
                        AssociationConstructionExpressionEvaluatorType.F_ATTRIBUTE,
                        AbstractAttributeMappingsDefinitionType.F_REF,
                        MappingDirection.ATTRIBUTE);

        IModel<PrismContainerWrapper<MappingType>> virtualObjectRefMappingContainerModel =
                createVirtualMappingContainerModel(
                        getPageBase(),
                        getValueModel(),
                        AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF,
                        AbstractAttributeMappingsDefinitionType.F_REF,
                        MappingDirection.OBJECTS);

        return new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerWrapper<MappingType> load() {
                PrismContainerWrapper<MappingType> attributeContainer =
                        virtualAttributeMappingContainerModel.getObject();
                PrismContainerWrapper<MappingType> objectRefContainer =
                        virtualObjectRefMappingContainerModel.getObject();

                if (attributeContainer == null) {
                    return objectRefContainer;
                }
                if (objectRefContainer == null) {
                    return attributeContainer;
                }

                try {
                    PrismContainerWrapper<MappingType> merged =
                            cloneEmptyContainer(attributeContainer);

                    merged.getValues().clear();

                    mergeValues(merged, attributeContainer);
                    mergeValues(merged, objectRefContainer);

                    return merged;

                } catch (SchemaException e) {
                    throw new IllegalStateException("Couldn't merge virtual mapping containers", e);
                }
            }

            @Override
            protected void onDetach() {
                super.onDetach();
                virtualAttributeMappingContainerModel.detach();
                virtualObjectRefMappingContainerModel.detach();
            }
        };
    }

    private @NotNull PrismContainerWrapper<MappingType> cloneEmptyContainer(
            @NotNull PrismContainerWrapper<MappingType> template) throws SchemaException {

        Task task = getPageBase().createSimpleTask("Create merged virtual mappings");
        OperationResult result = task.getResult();

        PrismContainer<MappingType> prismContainer = template.getItem().clone();
        prismContainer.clear();

        PrismContainerWrapper<MappingType> wrapper =
                getPageBase().createItemWrapper(
                        prismContainer,
                        ItemStatus.ADDED,
                        new WrapperContext(task, result));

        wrapper.getValues().clear();
        return wrapper;
    }

    private void mergeValues(
            @NotNull PrismContainerWrapper<MappingType> target,
            @NotNull PrismContainerWrapper<MappingType> source) {

        for (PrismContainerValueWrapper<MappingType> value : source.getValues()) {
            target.getValues().add(value);
        }
    }

    @Override
    protected @Nullable PrismContainerValueWrapper<MappingType> createNewValue(
            PrismContainerValue<MappingType> oldMappingValue,
            AjaxRequestTarget target) {

        if (!isAttributeVisible()) {
            createNewAssociationMappingValue(target, AssociationMappingTypeChoicePanelPopup.AssociationMappingKind.OBJECT_REF,
                    oldMappingValue);
            return null;
        }

        AssociationMappingTypeChoicePanelPopup popup =
                new AssociationMappingTypeChoicePanelPopup(getPageBase().getMainPopupBodyId(), this) {
                    @Override
                    protected void onAssociationMappingKindChosen(
                            AjaxRequestTarget target,
                            AssociationMappingKind value) {
                        createNewAssociationMappingValue(target, value, oldMappingValue);
                        getPageBase().hideMainPopup(target);
                        refreshAndDetach(target);
                    }
                };

        getPageBase().showMainPopup(popup, target);
        return null;
    }

    private void createNewAssociationMappingValue(
            AjaxRequestTarget target,
            AssociationMappingTypeChoicePanelPopup.AssociationMappingKind kind, PrismContainerValue<MappingType> oldMappingValue) {

        MappingDirection associationType =
                kind == AssociationMappingTypeChoicePanelPopup.AssociationMappingKind.OBJECT_REF
                        ? MappingDirection.OBJECTS
                        : MappingDirection.ATTRIBUTE;

        ItemName associationContainerName =
                kind == AssociationMappingTypeChoicePanelPopup.AssociationMappingKind.OBJECT_REF
                        ? AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF
                        : AssociationConstructionExpressionEvaluatorType.F_ATTRIBUTE;

        createNewVirtualMappingValue(
                oldMappingValue,
                getValueModel(),
                associationType,
                associationContainerName,
                AbstractAttributeMappingsDefinitionType.F_REF,
                getPageBase(),
                target);
    }

    @Override
    protected void resolveDeletedItem(@NotNull PrismContainerValueWrapper<MappingType> value) {
        try {
            if (value.getStatus() == ValueStatus.ADDED) {
                boolean removed = removeAssociationObjectRefMapping(value);
                if (!removed) {
                    removed = removeAssociationAttributeMapping(value);
                }

                if (!removed) {
                    LOGGER.debug("Couldn't physically remove added association mapping {}, marking as deleted instead.", value);
                    value.setStatus(ValueStatus.DELETED);
                }
            } else {
                value.setStatus(ValueStatus.DELETED);
            }
        } catch (SchemaException e) {
            getPageBase().error("Couldn't delete mapping: " + e.getMessage());
        } finally {
            value.setSelected(false);
        }
    }

    private boolean removeAssociationAttributeMapping(
            @NotNull PrismContainerValueWrapper<MappingType> value) throws SchemaException {
        return removeAssociationMapping(value, AssociationConstructionExpressionEvaluatorType.F_ATTRIBUTE);
    }

    private boolean removeAssociationObjectRefMapping(
            @NotNull PrismContainerValueWrapper<MappingType> value) throws SchemaException {
        return removeAssociationMapping(value, AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF);
    }

    private boolean removeAssociationMapping(
            @NotNull PrismContainerValueWrapper<MappingType> value,
            @NotNull ItemName associationContainerName) throws SchemaException {

        PrismContainerWrapper<? extends Containerable> container =
                getValueModel().getObject().findContainer(associationContainerName);

        if (container == null) {
            return false;
        }

        for (PrismContainerValueWrapper<? extends Containerable> associationValue : container.getValues()) {
            PrismContainerWrapper<MappingType> mappingContainer =
                    associationValue.findContainer(F_MAPPING);

            if (mappingContainer != null && mappingContainer.getValues().removeIf(v -> v.equals(value))) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void createDuplicateValuePerform(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        createNewVirtualMappingValue(
                value,
                getValueModel(),
                isAttributeRefMapping(value)
                        ? MappingDirection.ATTRIBUTE
                        : MappingDirection.OBJECTS,
                isAttributeRefMapping(value)
                        ? AssociationConstructionExpressionEvaluatorType.F_ATTRIBUTE
                        : AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF,
                AbstractAttributeMappingsDefinitionType.F_REF,
                getPageBase(),
                target);
        refreshAndDetach(target);
    }

    protected abstract boolean isAttributeVisible();

    @Override
    protected boolean isGroupedSuggestion() {
        return false;
    }

    @Override
    protected boolean isAssociationMappingTable() {
        return true;
    }

    protected abstract boolean isInboundRelated();

    private boolean isMappingForAssociation() {
        var value = getValueModel() != null ? getValueModel().getObject() : null;
        return value != null && value.getParentContainerValue(ShadowAssociationDefinitionType.class) != null;
    }

    protected boolean isAttributeRefMapping(@NotNull PrismContainerValueWrapper<MappingType> row) {
        return !row.getPath().containsNameExactly(AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF);
    }

    protected boolean isAttributeRefMapping(@NotNull PrismContainerValue<MappingType> row) {
        return !row.getPath().containsNameExactly(AssociationConstructionExpressionEvaluatorType.F_OBJECT_REF);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected @NotNull List<IColumn<PrismContainerValueWrapper<MappingType>, String>> getColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(Model.of()) {
            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<MappingType>> rowModel) {
                PrismContainerValueWrapper<MappingType> row = rowModel.getObject();

                if (isAttributeRefMapping(row)) {
                    return GuiDisplayTypeUtil.createDisplayType(
                            "fa-solid fa-tags text-muted",
                            "text-primary",
                            "Association attribute mapping");
                }

                return GuiDisplayTypeUtil.createDisplayType(
                        "fa-solid fa-link text-muted",
                        "text-info",
                        "Association object reference mapping");
            }

            @Override
            public String getCssClass() {
                return "px-0 tile-column-icon";
            }
        });

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
                return "col header-border-end";
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
                return "col-2 header-border-end";
            }

        });

        columns.add(new PrismPropertyWrapperColumn<>(
                getMappingTypeDefinition(),
                MappingType.F_EXPRESSION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-2 header-border-end";
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
                    return "col-2 header-border-end";
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
                    return "col-2 header-border-end";
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
    protected boolean isSuggestionSwitchSupported() {
        return false;
    }

    @Override
    protected boolean isSimulationSupported() {
        return false;
    }

    @Override
    protected String getNewObjectButtonCssClass() {
        return "btn btn-outline-primary";
    }

}
