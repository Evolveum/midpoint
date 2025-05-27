/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.UserFriendlyPrettyPrinter;
import com.evolveum.midpoint.common.UserFriendlyPrettyPrinterOptions;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.ConfigurableExpressionColumn;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.delta.ItemTreeDelta;
import com.evolveum.midpoint.schema.delta.ObjectTreeDelta;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class DeltaColumn extends ConfigurableExpressionColumn<SelectableBean<AuditEventRecordType>, AuditEventRecordType> {

    private static final Trace LOGGER = TraceManager.getTrace(DeltaColumn.class);

    private static final List<DisplayValueType> ALLOWED_DISPLAY_VALUES = List.of(
            DisplayValueType.ESTIMATED_OLD,
            DisplayValueType.CHANGES,
            DisplayValueType.ESTIMATED_OLD_AND_CHANGES
    );

    private static final DisplayValueType DEFAULT_DISPLAY_VALUE = DisplayValueType.CHANGES;

    private final IModel<Search<AuditEventRecordType>> searchModel;

    public DeltaColumn(
            IModel<String> displayModel,
            GuiObjectColumnType guiObjectColumn,
            IModel<Search<AuditEventRecordType>> searchModel,
            SerializableSupplier<VariablesMap> variablesSupplier, ExpressionType expressionType, PageBase modelServiceLocator) {

        super(displayModel != null ? displayModel : Model.of(), null, guiObjectColumn, variablesSupplier, expressionType, modelServiceLocator);

        this.searchModel = searchModel;
    }

    @Override
    protected Component createLabel(String componentId, IModel<?> model) {
        IModel<String> labelModel = new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                Object obj = model.getObject();
                String str = obj != null ? obj.toString() : "";
                return DeltaColumnPanel.escapePrettyPrintedValue(str);
            }
        };

        MultiLineLabel label = new MultiLineLabel(componentId, labelModel);
        label.setEscapeModelStrings(false);

        return label;
    }

    @Override
    protected String getStringValueDelimiter() {
        return "\n";
    }

    @Override
    public IModel<String> getDisplayModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                String label = DeltaColumn.super.getDisplayModel().getObject();
                if (StringUtils.isNotEmpty(label)) {
                    return label;
                }

                DisplayableValue<ItemPathType> value = getSearchChangedItemValue();
                if (value != null && value.getValue() != null) {
                    return LocalizationUtil.translate("ChangedItemColumn.header", value.getLabel());
                }

                return "";
            }
        };
    }

    private DisplayableValue<ItemPathType> getSearchChangedItemValue() {
        if (searchModel.getObject() == null) {
            return null;
        }

        // noinspection unchecked
        PropertySearchItemWrapper<ItemPathType> wrapper = searchModel.getObject()
                .findPropertySearchItem(AuditEventRecordType.F_CHANGED_ITEM);
        if (wrapper == null) {
            return null;
        }

        return wrapper.getValue();
    }

    private ItemPath getPath() {
        DisplayableValue<ItemPathType> value = getSearchChangedItemValue();
        if (value == null) {
            return null;
        }
        ItemPathType itemPathType = value.getValue();
        return itemPathType != null ? itemPathType.getItemPath() : null;
    }

    private boolean useSimpleDeltaPanel() {
        return getExpression() == null;
    }

    @Override
    public void populateItem(
            Item<ICellPopulator<SelectableBean<AuditEventRecordType>>> item,
            String componentId,
            IModel<SelectableBean<AuditEventRecordType>> rowModel) {

        if (!useSimpleDeltaPanel()) {
            super.populateItem(item, componentId, rowModel);
            return;
        }

        RepeatingView listItems = new RepeatingView(componentId);
        for (ItemDelta<?, ?> delta : createChangedItems(rowModel)) {
            DeltaColumnPanel panel = new DeltaColumnPanel(listItems.newChildId(), () -> delta);
            panel.setShowOldValues(getDisplayValueType() == DisplayValueType.ESTIMATED_OLD || getDisplayValueType() == DisplayValueType.ESTIMATED_OLD_AND_CHANGES);
            panel.setShowNewValues(getDisplayValueType() == DisplayValueType.CHANGES || getDisplayValueType() == DisplayValueType.ESTIMATED_OLD_AND_CHANGES);

            listItems.add(panel);
        }
        item.add(listItems);
    }

    private DisplayValueType getDisplayValueType() {
        DisplayValueType display = getCustomColumn().getDisplayValue() != null ? getCustomColumn().getDisplayValue() : DEFAULT_DISPLAY_VALUE;
        if (!ALLOWED_DISPLAY_VALUES.contains(display)) {
            return DEFAULT_DISPLAY_VALUE;
        }

        return display;
    }

    private List<ItemDelta<?, ?>> createChangedItems(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        ItemPath path = getPath();

        // noinspection unchecked
        return (List) rowModel.getObject().getValue().getDelta().stream()
                .map(d -> d.getObjectDelta())
                .filter(Objects::nonNull)
                .map(d -> {
                    try {
                        ObjectTreeDelta<? extends ObjectType> delta = ObjectTreeDelta.fromItemDelta(DeltaConvertor.createObjectDelta(d));
                        ItemTreeDelta partial = delta.findItemDelta(path, ItemTreeDelta.class);
                        if (partial == null || partial instanceof ObjectTreeDelta<?>) {
                            return null;
                        }
                        return partial;
                    } catch (SchemaException ex) {
                        LOGGER.debug("Cannot convert delta to object delta: {}", ex.getMessage(), ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(delta -> delta.toDelta())
//                .flatMap(List::stream)
                .toList();
    }

    @Override
    public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        if (!useSimpleDeltaPanel()) {
            return super.getDataModel(rowModel);
        }

        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                List<ItemDelta<?, ?>> deltas = createChangedItems(rowModel);
                return deltas.stream()
                        .map(item -> {
                            if (item instanceof ObjectTreeDelta<?> od) {
                                try {
                                    return PrettyPrinter.prettyPrint(od.toObjectDelta());
                                } catch (SchemaException ex) {
                                    LOGGER.trace("Cannot convert delta to object delta: {}", ex.getMessage(), ex);
                                    return "";
                                }
                            }

                            String oldValues = "";
                            if (item.getEstimatedOldValues() != null) {
                                oldValues = item.getEstimatedOldValues().stream()
                                        .map(v ->
                                                new UserFriendlyPrettyPrinter(
                                                        new UserFriendlyPrettyPrinterOptions()
                                                                .indentation("\t"))
                                                        .prettyPrintValue(v, 0)
                                        )
                                        .collect(Collectors.joining(", "));
                            }

                            List<String> changes = new ArrayList<>();
                            // noinspection unchecked
                            addChanges(ModificationType.ADD, (List<PrismValue>) item.getValuesToAdd(), changes);
                            // noinspection unchecked
                            addChanges(ModificationType.DELETE, (List<PrismValue>) item.getValuesToDelete(), changes);
                            // noinspection unchecked
                            addChanges(ModificationType.REPLACE, (List<PrismValue>) item.getValuesToReplace(), changes);

                            String newValues = StringUtils.joinWith(", ", changes);

                            if (StringUtils.isEmpty(oldValues)) {
                                return newValues;
                            }

                            return oldValues + " -> " + newValues;
                        })
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.joining(getStringValueDelimiter()));
            }
        };
    }

    private void addChanges(ModificationType modificationType, List<PrismValue> values, List<String> changes) {
        if (modificationType == null || values == null || values.isEmpty()) {
            return;
        }

        String operation = switch (modificationType) {
            case ADD -> "(+)";
            case DELETE -> "(-)";
            case REPLACE -> "(=)";
        };

        changes.add(operation +
                values.stream()
                        .map(v -> new UserFriendlyPrettyPrinter().prettyPrintValue(v, 0))
                        .collect(Collectors.joining(", ")));
    }
}
