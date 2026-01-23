/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.web.security.MidPointAuthWebSession;

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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.delta.DeltaScanner;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
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
        item.add(listItems);

        Changes changes = createChangedItems(rowModel);
        if (changes.deltas != null) {
            boolean showPath = changes.deltas.size() > 1;
            for (ItemDelta<?, ?> delta : changes.deltas) {
                DeltaColumnPanel panel = new DeltaColumnPanel(listItems.newChildId(), () -> delta);
                panel.setShowOldValues(getDisplayValueType() == DisplayValueType.ESTIMATED_OLD || getDisplayValueType() == DisplayValueType.ESTIMATED_OLD_AND_CHANGES);
                panel.setShowNewValues(getDisplayValueType() == DisplayValueType.CHANGES || getDisplayValueType() == DisplayValueType.ESTIMATED_OLD_AND_CHANGES);
                panel.setShowPath(showPath);

                listItems.add(panel);
            }
        } else if (changes.addObject != null) {
            listItems.add(new MultiLineLabel(listItems.newChildId(), () -> prettyPrintObjectSimple(changes.addObject)));
        }
    }

    private String prettyPrintObjectSimple(PrismObject<?> object) {
        UserFriendlyPrettyPrinterOptions opts = new UserFriendlyPrettyPrinterOptions()
                .locale(MidPointAuthWebSession.get().getLocale())
                .localizationService(getPageBase().getLocalizationService())
                .defaultUIIndentation();

        UserFriendlyPrettyPrinter printer = new UserFriendlyPrettyPrinter(opts);

        return printer.prettyPrintObjectSimple(object, 0);
    }

    private DisplayValueType getDisplayValueType() {
        DisplayValueType display = getCustomColumn().getDisplayValue() != null ? getCustomColumn().getDisplayValue() : DEFAULT_DISPLAY_VALUE;
        if (!ALLOWED_DISPLAY_VALUES.contains(display)) {
            return DEFAULT_DISPLAY_VALUE;
        }

        return display;
    }

    private Changes createChangedItems(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        ItemPath path = getPath() != null ? getPath() : ItemPath.EMPTY_PATH;

        return rowModel.getObject().getValue().getDelta().stream()
                .map(d -> d.getObjectDelta())
                .filter(Objects::nonNull)
                .map(d -> {
                    try {
                        ObjectDelta<? extends ObjectType> objectDelta = DeltaConvertor.createObjectDelta(d);

                        PrismObject<?> add = objectDelta.getObjectToAdd();
                        List deltas = null;
                        if (add == null || !ItemPath.EMPTY_PATH.equals(path)) {
                            DeltaScanner scanner = createDeltaScanner();
                            deltas = scanner.searchDelta(objectDelta, path).stream()
                                    .map(r -> r.toDelta())
                                    .filter(id -> !id.isOperational())
                                    .toList();
                        }

                        return new Changes(deltas, add);
                    } catch (SchemaException ex) {
                        LOGGER.debug("Cannot convert delta to object delta: {}", ex.getMessage(), ex);
                        return new Changes(null, null);
                    }
                })
                .findFirst()
                .orElse(new Changes(null, null));
    }

    private record Changes(List<ItemDelta<?, ?>> deltas, PrismObject<?> addObject) implements Serializable {
    }

    @Override
    public IModel<String> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        if (!useSimpleDeltaPanel()) {
            return super.getDataModel(rowModel);
        }

        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                Changes computedChanges = createChangedItems(rowModel);

                if (computedChanges.addObject != null) {
                    return prettyPrintObjectSimple(computedChanges.addObject);
                } else if (computedChanges.deltas != null) {
                    return computedChanges.deltas.stream()
                            .map(item -> {
                                String oldValues = "";
                                if (item.getEstimatedOldValues() != null) {
                                    oldValues = item.getEstimatedOldValues().stream()
                                            .map(v ->
                                                    createPrinterForData()
                                                            .prettyPrintValue(v, 0)
                                            )
                                            .filter(StringUtils::isNotBlank)
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

                                if (StringUtils.isBlank(oldValues) && StringUtils.isBlank(newValues)) {
                                    return "";
                                }

                                if (StringUtils.isEmpty(oldValues)) {
                                    return newValues;
                                }

                                return oldValues + " -> " + newValues;
                            })
                            .filter(StringUtils::isNotBlank)
                            .collect(Collectors.joining(getStringValueDelimiter()));
                }

                return "";
            }
        };
    }

    private DeltaScanner createDeltaScanner() {
        return new DeltaScanner()
                .allowPartialMatches(true);
    }

    private UserFriendlyPrettyPrinter createPrinterForData() {
        return new UserFriendlyPrettyPrinter(
                new UserFriendlyPrettyPrinterOptions()
                        .showOperational(false)
                        .locale(MidPointAuthWebSession.get().getLocale())
                        .localizationService(getPageBase().getLocalizationService())
                        .indentation("\t"));
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

        String valuesStr = values.stream()
                .map(v -> createPrinterForData().prettyPrintValue(v, 0))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(", "));

        if (StringUtils.isNotBlank(valuesStr)) {
            changes.add(operation + valuesStr);
        }
    }
}
