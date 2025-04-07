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

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.PropertySearchItemWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.delta.ItemTreeDelta;
import com.evolveum.midpoint.schema.delta.ObjectTreeDelta;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ChangedItemColumn extends AbstractExportableColumn<SelectableBean<AuditEventRecordType>, String> {

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

    private ItemPath getPath() {
        if (searchModel.getObject() == null) {
            return null;
        }

        // noinspection unchecked
        PropertySearchItemWrapper<ItemPathType> wrapper = searchModel.getObject()
                .findPropertySearchItem(AuditEventRecordType.F_CHANGED_ITEM);
        if (wrapper == null) {
            return null;
        }

        DisplayableValue<ItemPathType> value = wrapper.getValue();
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

        IModel<List<String>> itemModel = getDataModel(rowModel);
        RepeatingView listItems = new RepeatingView(componentId);
        for (String data : itemModel.getObject()) {
            listItems.add(new Label(listItems.newChildId(), () -> data));
        }
        item.add(listItems);
    }

    @Override
    public IModel<List<String>> getDataModel(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<String> load() {
                return createItemModelValue(rowModel);
            }
        };
    }

    private List<String> createItemModelValue(IModel<SelectableBean<AuditEventRecordType>> rowModel) {
        // todo handle expressions
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
                        // todo error handling
                        ex.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return deltas.stream()
                .map(delta -> {
                    if (delta.getValues().isEmpty()) {
                        return null;
                    }

                    return delta.getValues().stream()
                            .map(Object::toString)
                            .collect(Collectors.joining(", "));
                })
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }
}
