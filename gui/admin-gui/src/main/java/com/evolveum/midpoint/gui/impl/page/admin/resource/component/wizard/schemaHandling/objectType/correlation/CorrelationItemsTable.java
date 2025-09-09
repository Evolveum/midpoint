/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumnPanel;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoiceColumnPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.StringTextChoiceProvider;

/**
 * @author lskublik
 */
public abstract class CorrelationItemsTable extends AbstractWizardTable<ItemsSubCorrelatorType, CorrelationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItemsTable.class);

    public CorrelationItemsTable(
            String id,
            IModel<PrismContainerValueWrapper<CorrelationDefinitionType>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config, ItemsSubCorrelatorType.class);
    }

    @Override
    protected IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> getContainerModel() {
        return PrismContainerWrapperModel.fromContainerValueWrapper(
                getValueModel(),
                ItemPath.create(
                        CorrelationDefinitionType.F_CORRELATORS,
                        CompositeCorrelatorType.F_ITEMS));
    }

    @Override
    public boolean displayNoValuePanel() {
        PrismContainerValueWrapper<CorrelationDefinitionType> wrapper = getValueModel().getObject();
        if (wrapper == null || wrapper.getRealValue() == null) {
            return true;
        }

        CompositeCorrelatorType correlators = wrapper.getRealValue().getCorrelators();
        if (correlators == null) {
            return true;
        }

        List<?> items = correlators.getItems();
        return items == null || items.isEmpty();
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<ItemsSubCorrelatorType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<ItemsSubCorrelatorType>> reactionDef = getCorrelationItemsDefinition();
        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_DESCRIPTION,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                ItemsSubCorrelatorType realValue = (ItemsSubCorrelatorType) rowModel.getObject().getParent().getRealValue();
                StringBuilder items = new StringBuilder();
                String prefix = "";
                for (CorrelationItemType item : realValue.getItem()) {
                    if (item != null && item.getRef() != null) {
                        items.append(prefix).append(item.getRef().toString());
                    }
                    prefix = ", ";
                }

                PrismPropertyWrapperColumnPanel panel = new PrismPropertyWrapperColumnPanel<>(
                        componentId, (IModel<PrismPropertyWrapper<String>>) rowModel, getColumnType()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onBeforeRender() {
                        super.onBeforeRender();
                        visitChildren(FormComponent.class, (formComponent, object) -> {
                            formComponent.add(AttributeAppender.append(
                                    "placeholder",
                                    () -> {
                                        if (items.length() == 0) {
                                            return "";
                                        } else {
                                            return getPageBase().createStringResource(
                                                    "CorrelationItemsTable.column.description.placeholder",
                                                    items.toString()).getString();
                                        }
                                    }));
                        });
                    }

                };
                return panel;
            }

            @Override
            public String getCssClass() {
                return "col-3";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_WEIGHT),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-1";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_TIER),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-1";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemPath.create(
                        ItemsSubCorrelatorType.F_COMPOSITION,
                        CorrelatorCompositionDefinitionType.F_IGNORE_IF_MATCHED_BY),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
                IModel<Collection<String>> multiselectModel = new IModel<>() {

                    @Override
                    public Collection<String> getObject() {

                        return ((PrismPropertyWrapper<String>) rowModel.getObject())
                                .getValues().stream()
                                .filter(value -> !ValueStatus.DELETED.equals(value.getStatus()) && value.getRealValue() != null)
                                .map(PrismValueWrapperImpl::getRealValue)
                                .collect(Collectors.toList());
                    }

                    @Override
                    public void setObject(Collection<String> newValues) {

                        PrismPropertyWrapper<String> ignoreIfMatchedByItem =
                                ((PrismPropertyWrapper<String>) rowModel.getObject());
                        List<PrismPropertyValueWrapper<String>> toRemoveValues
                                = ignoreIfMatchedByItem.getValues().stream()
                                .filter(v -> v.getRealValue() != null).collect(Collectors.toList());

                        newValues.forEach(newValue -> {
                            if (StringUtils.isEmpty(newValue)) {
                                return;
                            }
                            Optional<PrismPropertyValueWrapper<String>> found = ignoreIfMatchedByItem.getValues().stream()
                                    .filter(actualValue -> StringUtils.isNotEmpty(actualValue.getRealValue())
                                            && newValue.equals(actualValue.getRealValue()))
                                    .findFirst();
                            if (found.isPresent()) {
                                toRemoveValues.remove(found.get());
                                if (ValueStatus.DELETED.equals(found.get().getStatus())) {
                                    found.get().setStatus(ValueStatus.NOT_CHANGED);
                                }
                            } else {
                                try {
                                    PrismPropertyValue<String> newPrismValue
                                            = getPrismContext().itemFactory().createPropertyValue();
                                    newPrismValue.setValue(newValue);
                                    ignoreIfMatchedByItem.add(newPrismValue, getPageBase());
                                } catch (SchemaException e) {
                                    LOGGER.error("Couldn't initialize new value for Source item", e);
                                }
                            }
                        });

                        toRemoveValues.forEach(toRemoveValue -> {
                            try {
                                ignoreIfMatchedByItem.remove(toRemoveValue, getPageBase());
                            } catch (SchemaException e) {
                                LOGGER.error("Couldn't remove old value for Source item", e);
                            }
                        });

                    }
                };

                return new Select2MultiChoiceColumnPanel<>(componentId, multiselectModel, new StringTextChoiceProvider() {
                    @Override
                    public void query(String input, int i, Response<String> response) {
                        response.addAll(getContainerModel().getObject().getValues().stream()
                                .map(value -> value.getRealValue().getName())
                                .filter(StringUtils::isNotEmpty)
                                .filter(name -> StringUtils.isEmpty(input) || name.startsWith(input))
                                .collect(Collectors.toList()));
                    }
                });
            }

            @Override
            public String getCssClass() {
                return "col-3";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<ItemsSubCorrelatorType, String>(
                reactionDef,
                ItemsSubCorrelatorType.F_ENABLED,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            public String getCssClass() {
                return "col-1";
            }
        });

        return columns;
    }

    protected LoadableModel<PrismContainerDefinition<ItemsSubCorrelatorType>> getCorrelationItemsDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<ItemsSubCorrelatorType> load() {
                return getValueModel().getObject().getDefinition().findContainerDefinition(
                        ItemPath.create(
                                CorrelationDefinitionType.F_CORRELATORS,
                                CompositeCorrelatorType.F_ITEMS));
            }
        };
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PANEL_CORRELATION_ITEMS_WIZARD;
    }

    @Override
    protected String getKeyOfTitleForNewObjectButton() {
        return "CorrelationItemsTable.newObject.simple";
    }
}
