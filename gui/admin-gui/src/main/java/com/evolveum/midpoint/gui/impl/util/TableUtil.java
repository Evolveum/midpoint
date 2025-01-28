package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.web.component.data.SelectableDataTable;

import com.evolveum.midpoint.web.component.util.SelectableBean;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisitor;

import java.util.ArrayList;
import java.util.List;

public class TableUtil {

    public static <T extends ObjectType> List<SelectableBean<T>> getSelectedModels(DataTable table) {
        List<SelectableBean<T>> objects = new ArrayList<>();
        table.visitChildren(SelectableDataTable.SelectableRowItem.class, (IVisitor<SelectableDataTable.SelectableRowItem<SelectableBean<T>>, Void>) (row, visit) -> {
            if (row.getModelObject().isSelected()) {
                objects.add(row.getModel().getObject());
            }
        });
        return objects;
    }

    public static void updateRows(DataTable dataTable, AjaxRequestTarget target) {
        dataTable.visitChildren(SelectableDataTable.SelectableRowItem.class, (IVisitor<SelectableDataTable.SelectableRowItem, Void>) (row, visit) -> {
            if (row.getOutputMarkupId()) {
                //we skip rows that doesn't have outputMarkupId set to true (it would fail)
                target.add(row);
            }
        });
    }

    public static <T>  List<IModel<T>> getAvailableData(DataTable table) {
        List<IModel<T>> objects = new ArrayList<>();
        table.visitChildren(SelectableDataTable.SelectableRowItem.class, (IVisitor<SelectableDataTable.SelectableRowItem, Void>) (row, visit) -> objects.add(row.getModel()));
        return objects;
    }
}
