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
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.UserFriendlyPrettyPrinter;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.delta.ItemTreeDelta;
import com.evolveum.midpoint.schema.delta.ObjectTreeDelta;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class DeltaColumn extends AbstractExportableColumn<SelectableBean<AuditEventRecordType>, String> {

    private static final Trace LOGGER = TraceManager.getTrace(DeltaColumn.class);

    private static final List<DisplayValueType> ALLOWED_DISPLAY_VALUES = List.of(
            DisplayValueType.OLD_VALUE,
            DisplayValueType.NEW_VALUE,
            DisplayValueType.OLD_NEW_VALUE
    );

    private final GuiObjectColumnType guiObjectColumn;

    private final IModel<Search<AuditEventRecordType>> searchModel;

    public DeltaColumn(
            IModel<String> displayModel,
            GuiObjectColumnType guiObjectColumn,
            IModel<Search<AuditEventRecordType>> searchModel) {

        super(displayModel != null ? displayModel : Model.of());

        this.guiObjectColumn = guiObjectColumn;
        this.searchModel = searchModel;
    }

    @Override
    public String getSortProperty() {
        return null;
    }

    @Override
    public IModel<String> getDisplayModel() {
        DisplayableValue<ItemPathType> value = getSearchChangedItemValue();
        if (value == null || value.getValue() == null) {
            return super.getDisplayModel();
        }

        return PageBase.createStringResourceStatic("ChangedItemColumn.header", value.getLabel(), value.getLabel());
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

    @Override
    public String getCssClass() {
        if (guiObjectColumn == null) {
            return super.getCssClass();
        }
        return guiObjectColumn.getDisplay() != null ? guiObjectColumn.getDisplay().getCssClass() : null;
    }

    @Override
    public void populateItem(
            Item<ICellPopulator<SelectableBean<AuditEventRecordType>>> item,
            String componentId,
            IModel<SelectableBean<AuditEventRecordType>> rowModel) {

        IModel<List<ItemDelta<?, ?>>> model = new LoadableDetachableModel<>() {

            @Override
            protected List<ItemDelta<?, ?>> load() {
                return createChangedItems(rowModel);
            }
        };

        RepeatingView listItems = new RepeatingView(componentId);
        for (ItemDelta<?, ?> delta : model.getObject()) {
            listItems.add(new DeltaColumnPanel(listItems.newChildId(), () -> delta));
        }
        item.add(listItems);
    }

    private DisplayValueType getDisplayValueType() {
        DisplayValueType display = guiObjectColumn.getDisplayValue() != null ? guiObjectColumn.getDisplayValue() : DisplayValueType.NEW_VALUE;
        if (!ALLOWED_DISPLAY_VALUES.contains(display)) {
            display = DisplayValueType.NEW_VALUE;
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
                        if (path == null) {
                            return delta;
                        }
                        return delta.findItemDelta(path, ItemTreeDelta.class);
                    } catch (SchemaException ex) {
                        LOGGER.debug("Cannot convert delta to object delta: {}", ex.getMessage(), ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(delta -> delta.toDelta())
                .toList();
    }

    @Override
    public IModel<List<String>> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<String> load() {
                List<ItemDelta<?, ?>> deltas = createChangedItems(rowModel);
                return deltas.stream()
                        .map(item -> {
                            String oldValues = "";
                            if (item.getEstimatedOldValues() != null) {
                                oldValues = item.getEstimatedOldValues().stream()
                                        .map(v ->
                                                new UserFriendlyPrettyPrinter()
                                                        .indent("\t")
                                                        .prettyPrintValue(v, 0))
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

                            if (StringUtils.isNotEmpty(oldValues)) {
                                return newValues;
                            }

                            return oldValues + " -> " + newValues;
                        })
                        .filter(StringUtils::isNotBlank)
                        .toList();
            }
        };
    }

    private void addChanges(ModificationType modificationType, List<PrismValue> values, List<String> changes) {
        if (modificationType == null) {
            return;
        }

        String operation = switch (modificationType) {
            case ADD -> "(+)";
            case DELETE -> "(-)";
            case REPLACE -> "(=)";
        };

        changes.add(operation);
    }

    private String createChange(ModificationType modificationType, PrismValue value) {
        if (modificationType == null) {
            return "";
        }

        String operation = switch (modificationType) {
            case ADD -> "(+)";
            case DELETE -> "(-)";
            case REPLACE -> "(=)";
        };

        return operation + new UserFriendlyPrettyPrinter()
                .indent("\t")
                .prettyPrintValue(value, 0);
    }
}
