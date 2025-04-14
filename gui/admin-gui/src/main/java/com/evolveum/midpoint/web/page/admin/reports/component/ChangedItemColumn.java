/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.UserFriendlyPrettyPrinter;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.delta.ItemTreeDelta;
import com.evolveum.midpoint.schema.delta.ItemTreeDeltaValue;
import com.evolveum.midpoint.schema.delta.ObjectTreeDelta;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ChangedItemColumn extends AbstractExportableColumn<SelectableBean<AuditEventRecordType>, String> {

    private static final Trace LOGGER = TraceManager.getTrace(ChangedItemColumn.class);

    private final GuiObjectColumnType guiObjectColumn;

    private final ExpressionType expression;

    private final ModelServiceLocator modelServiceLocator;

    private final IModel<Search<AuditEventRecordType>> searchModel;

    public ChangedItemColumn(
            IModel<String> displayModel,
            GuiObjectColumnType guiObjectColumn,
            ExpressionType expressionType,
            IModel<Search<AuditEventRecordType>> searchModel,
            ModelServiceLocator modelServiceLocator) {

        super(displayModel);

        this.guiObjectColumn = guiObjectColumn;
        this.expression = expressionType;
        this.modelServiceLocator = modelServiceLocator;
        this.searchModel = searchModel;
    }

    @Override
    public IModel<String> getDisplayModel() {
        DisplayableValue<ItemPathType> value = getSearchChangedItemValue();
        if (value == null) {
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

        IModel<List<ChangedItem>> model = new LoadableDetachableModel<>() {

            @Override
            protected List<ChangedItem> load() {
                return createChangedItems(rowModel);
            }
        };

        RepeatingView listItems = new RepeatingView(componentId);
        for (ChangedItem data : model.getObject()) {
            listItems.add(new ChangedItemPanel(listItems.newChildId(), () -> data));
        }
        item.add(listItems);
    }

    private List<ChangedItem> createChangedItems(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        ItemPath path = getPath();
        if (path == null) {
            return List.of();
        }

        List<ItemTreeDelta> deltas = rowModel.getObject().getValue().getDelta().stream()
                .map(d -> d.getObjectDelta())
                .filter(Objects::nonNull)
                .map(d -> {
                    try {
                        ObjectTreeDelta<? extends ObjectType> delta = ObjectTreeDelta.fromItemDelta(DeltaConvertor.createObjectDelta(d));
                        return delta.findItemDelta(path, ItemTreeDelta.class);
                    } catch (SchemaException ex) {
                        LOGGER.debug("Cannot convert delta to object delta: {}", ex.getMessage(), ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return deltas.stream()
                .map(delta -> {
                    List<? extends ItemTreeDeltaValue> values = delta.getValues();
                    List<PrismValue> estimatedOldValues = delta.getEstimatedOldValues();

                    List<ChangedItemValue> newValues = values.stream()
                            .map(itdv -> new ChangedItemValue(itdv.getModificationType(), itdv.getValue()))
                            .toList();

                    if (estimatedOldValues == null) {
                        estimatedOldValues = List.of();
                    }
                    if (newValues == null) {
                        newValues = List.of();
                    }

                    return new ChangedItem(path, estimatedOldValues, newValues);
                })
                .toList();
    }

    @Override
    public IModel<List<String>> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<String> load() {
                List<ChangedItem> items = createChangedItems(rowModel);
                return items.stream()
                        .map(item -> {
                            String oldValues = item.oldValues().stream()
                                    .map(v ->
                                            new UserFriendlyPrettyPrinter()
                                                    .indent("\t")
                                                    .prettyPrintValue(v, 0))
                                    .collect(Collectors.joining(", "));

                            String newValues = item.newValues().stream()
                                    .map(v ->
                                            createChange(v.modificationType()) + " " +
                                                    new UserFriendlyPrettyPrinter()
                                                            .indent("\t")
                                                            .prettyPrintValue(v.value(), 0))
                                    .collect(Collectors.joining(", "));

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

    private String createChange(ModificationType modificationType) {
        if (modificationType == null) {
            return "";
        }

        return switch (modificationType) {
            case ADD -> "(+)";
            case DELETE -> "(-)";
            case REPLACE -> "(=)";
        };
    }
}
