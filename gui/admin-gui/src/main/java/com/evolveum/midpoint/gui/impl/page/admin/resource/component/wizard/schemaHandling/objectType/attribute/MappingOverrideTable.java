/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LifecycleStateColumn;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AttributeMappingValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.input.TwoStateBooleanPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lskublik
 */
public abstract class MappingOverrideTable<C extends Containerable> extends AbstractWizardTable<ResourceAttributeDefinitionType, C> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingOverrideTable.class);

    public MappingOverrideTable(
            String id,
            IModel<PrismContainerValueWrapper<C>> valueModel,
            ContainerPanelConfigurationType config) {
        super(id, valueModel, config, ResourceAttributeDefinitionType.class);
    }

    protected PrismContainerValueWrapper<?> createNewValue(PrismContainerValue<ResourceAttributeDefinitionType> value, AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> mappingAttributeContainer =
                    getValueModel().getObject().findContainer(getItemNameOfContainerWithMappings());
            PrismContainerValue<ResourceAttributeDefinitionType> newMapping = value;
            if (newMapping == null) {
                newMapping = mappingAttributeContainer.getItem().createNewValue();
            }

            AttributeMappingValueWrapper<?> newAttributeMappingWrapper =
                    WebPrismUtil.createNewValueWrapper(mappingAttributeContainer, newMapping, getPageBase(), target);
            newAttributeMappingWrapper.addAttributeMappingType(MappingDirection.OVERRIDE);

            return newAttributeMappingWrapper;
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new attribute mapping");
        }
        return null;
    }

    protected abstract ItemName getItemNameOfContainerWithMappings();

    @Override
    protected IModel<PrismContainerWrapper<ResourceAttributeDefinitionType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(getValueModel(), getItemNameOfContainerWithMappings());
    }

    @Override
    protected ISelectableDataProvider<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> createProvider() {
        return new MultivalueContainerListDataProvider<>(MappingOverrideTable.this, getSearchModel(), new PropertyModel<>(getContainerModel(), "values")) {

            @Override
            protected List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> searchThroughList() {
                List<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> list = super.searchThroughList();
                return list.stream().filter(value -> {
                    if (value instanceof AttributeMappingValueWrapper) {
                        return ((AttributeMappingValueWrapper<?>) value).getAttributeMappingTypes()
                                .contains(MappingDirection.OVERRIDE);
                    }
                    return true;
                }).collect(Collectors.toList());
            }
        };
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ResourceAttributeDefinitionType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ResourceAttributeDefinitionType>> attributeDef = getAttributeDefinition();
        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
                attributeDef,
                ResourceAttributeDefinitionType.F_REF,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
                attributeDef,
                ResourceAttributeDefinitionType.F_DISPLAY_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("MappingOverrideTable.column.description")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> iModel) {
                Label help = new Label(id);
                IModel<String> helpModel = () -> iModel.getObject().getRealValue().getDescription();
                help.add(AttributeModifier.replace("title", createStringResource(helpModel.getObject() != null ? helpModel.getObject() : "")));
                help.add(new InfoTooltipBehavior() {
                    @Override
                    public String getCssClass() {
                        String cssClass = "fa fa-info-circle";
                        cssClass = cssClass + (helpModel.getObject() != null ? " text-info" : " text-secondary");
                        return cssClass;
                    }
                });
                item.add(help);
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("MappingOverrideTable.column.mandatoryField")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel) {

                IModel<Boolean> mandatoryModel =
                        createModelForOccurs(rowModel, PropertyLimitationsType.F_MIN_OCCURS, "1", "0", true);
                TwoStateBooleanPanel panel = new TwoStateBooleanPanel(id, mandatoryModel);
                item.add(panel);
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("MappingOverrideTable.column.multiValueField")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<PrismContainerValueWrapper<ResourceAttributeDefinitionType>>> item,
                    String id,
                    IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel) {

                IModel<Boolean> multivalueModel =
                        createModelForOccurs(rowModel, PropertyLimitationsType.F_MAX_OCCURS, "unbounded", "1", false);
                TwoStateBooleanPanel panel = new TwoStateBooleanPanel(id, multivalueModel);
                item.add(panel);
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
                attributeDef,
                ResourceAttributeDefinitionType.F_TOLERANT,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

//        columns.add(new PrismPropertyWrapperColumn<ResourceAttributeDefinitionType, String>(
//                attributeDef,
//                ItemPath.create(ResourceAttributeDefinitionType.F_CORRELATOR, ItemCorrelatorDefinitionType.),
//                AbstractItemWrapperColumn.ColumnType.VALUE,
//                getPageBase()));

        columns.add(new LifecycleStateColumn<>(getContainerModel(), getPageBase()));

        return columns;
    }

    /**
     * Creates a boolean model for limitation properties (minOccurs or maxOccurs) that are
     * represented in the UI as checkboxes.
     *
     * <p>The model resolves the effective value of the limitation as follows:
     * <ul>
     *   <li>If a limitation override exists in the attribute definition, its value is used.</li>
     *   <li>If no override exists, the default value from the referenced schema definition is used.</li>
     * </ul>
     *
     * <p>The checkbox is considered {@code true} if the effective value equals {@code trueValue}.
     *
     * <p>When setting the value:
     * <ul>
     *   <li>If the limitations container or property does not exist, it is created.</li>
     *   <li>The property value is set to {@code trueValue} or {@code falseValue}
     *       according to the provided boolean.</li>
     * </ul>
     *
     * <p><b>Important:</b> Limitation values are always explicitly written once modified,
     * even if they match the schema default. They are not automatically removed when equal
     * to the default value. This prevents accidental loss of user intent in case the
     * referenced schema changes in the future and its default values differ.
     *
     * @param rowModel          model of the attribute definition row
     * @param path              item name of the limitation property (e.g. minOccurs or maxOccurs)
     * @param trueValue         string value representing {@code true} (e.g. "1" or "unbounded")
     * @param falseValue        string value representing {@code false}
     * @param isMandatoryCheck  {@code true} if the checkbox represents mandatory (minOccurs),
     *                          {@code false} if it represents multi-value (maxOccurs)
     * @return boolean model backing the checkbox component
     */
    private @NotNull IModel<Boolean> createModelForOccurs(
            IModel<PrismContainerValueWrapper<ResourceAttributeDefinitionType>> rowModel,
            ItemName path,
            String trueValue,
            String falseValue,
            boolean isMandatoryCheck) {
        return new IModel<>() {
            @Override
            public Boolean getObject() {
                Boolean isTrueOccurs = null;
                PrismContainerValueWrapper<ResourceAttributeDefinitionType> attributeDefVW = rowModel.getObject();

                try {
                    PrismContainerWrapper<PropertyLimitationsType> limitation =
                            attributeDefVW.findContainer(ResourceAttributeDefinitionType.F_LIMITATIONS);
                    if (limitation != null && !limitation.getValues().isEmpty()) {
                        PrismPropertyWrapper<String> occurs = limitation.getValues().iterator().next().findProperty(path);
                        if (occurs != null) {
                            isTrueOccurs = trueValue.equals(occurs.getValue().getRealValue());
                        }
                    }
                } catch (SchemaException | RuntimeException e) {
                    LOGGER.error("Couldn't create new value for limitation container", e);
                }

                if (isTrueOccurs == null) {
                    isTrueOccurs = trueValue.equals(getSchemaRefAttributeDefaultValue(attributeDefVW, isMandatoryCheck));
                }

                return isTrueOccurs;
            }

            private String getSchemaRefAttributeDefaultValue(
                    PrismContainerValueWrapper<ResourceAttributeDefinitionType> attributeDefVW,
                    boolean isMandatoryCheck) {
                try {
                    ResourceAttributeDefinitionType attrDef = attributeDefVW.getRealValue();
                    CompleteResourceSchema refinedSchema = getResourceDetailsModel().getRefinedSchema();
                    ShadowKindType kind = getObjectTypeDefinition().getKind();
                    String intent = getObjectTypeDefinition().getIntent();
                    ResourceObjectTypeDefinition objDef = refinedSchema.getObjectTypeDefinition(kind, intent);

                    if (objDef == null) {
                        return falseValue;
                    }

                    PrismPropertyDefinition<?> propDef =
                            objDef.findPropertyDefinition(attrDef.getRef().getItemPath());

                    if (propDef == null) {
                        return falseValue;
                    }

                    if (isMandatoryCheck) {
                        return propDef.isMandatory() ? trueValue : falseValue;
                    } else {
                        return !propDef.isSingleValue() ? trueValue : falseValue;
                    }

                } catch (SchemaException | RuntimeException | ConfigurationException e) {
                    LOGGER.error("Couldn't resolve refined schema attribute definition", e);
                    return falseValue;
                }
            }

            @Override
            public void setObject(Boolean isTrueOccurs) {
                if (isTrueOccurs == null) {
                    return;
                }
                try {
                    PrismContainerWrapper<PropertyLimitationsType> limitation =
                            rowModel.getObject().findContainer(ResourceAttributeDefinitionType.F_LIMITATIONS);
                    if (limitation.getValues().isEmpty()) {
                        PrismContainerValue<PropertyLimitationsType> newItem = limitation.getItem().createNewValue();
                        PrismContainerValueWrapper<PropertyLimitationsType> newItemWrapper = WebPrismUtil.createNewValueWrapper(
                                limitation, newItem, getPageBase());
                        limitation.getValues().add(newItemWrapper);
                    }
                    PrismContainerValueWrapper<PropertyLimitationsType> value = limitation.getValues().iterator().next();
                    PrismPropertyWrapper<Object> minOccurs = value.findProperty(path);
                    Object rv = minOccurs.getValue().getRealValue();
                    String actualValue = rv != null ? String.valueOf(rv) : null;
                    String desired = isTrueOccurs ? trueValue : falseValue;

                    if (!Objects.equals(actualValue, desired)) {
                        minOccurs.getValue().setRealValue(desired);
                    }

                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create new value for limitation container", e);
                }
            }
        };
    }

    /**
     * Returns the parent {@link ResourceObjectTypeDefinitionType}
     * of the current container value.
     *
     * @return the associated object type definition
     */
    protected @NotNull ResourceObjectTypeDefinitionType getObjectTypeDefinition() {
        PrismContainerValueWrapper<C> value = getValueModel().getObject();
        var parentContainerValue = value.getParentContainerValue(ResourceObjectTypeDefinitionType.class);
        return parentContainerValue.getRealValue();
    }

    protected LoadableModel<PrismContainerDefinition<ResourceAttributeDefinitionType>> getAttributeDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ResourceAttributeDefinitionType> load() {
                return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(ResourceAttributeDefinitionType.class);
            }
        };
    }

    protected abstract ResourceDetailsModel getResourceDetailsModel();

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_MAPPING_OVERRIDE_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "MappingOverrideTable.newObject";
    }
}
