package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.gui.impl.model.SelectableObjectModel;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;

import com.evolveum.midpoint.web.component.util.SelectableBean;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TableUtil {

    public static <T extends ObjectType> List<SelectableBean<T>> getSelectedModels(DataTable table) {
        List<SelectableBean<T>> objects = new ArrayList<>();
        table.visitChildren(SelectableDataTable.SelectableRowItem.class, new IVisitor<SelectableDataTable.SelectableRowItem<SelectableBean<T>>, Void>() {

            @Override
            public void component(SelectableDataTable.SelectableRowItem<SelectableBean<T>> row, IVisit<Void> visit) {
                if (row.getModelObject().isSelected()) {
                    objects.add(row.getModel().getObject());
                }
//                SelectableObjectModel<T> model = (SelectableObjectModel<T>) row.getModel();
//                if (model.isSelected()) {
//                    objects.add((SelectableObjectModel<T>) row.getModel());
//                }
            }
        });
        return objects;
    }
}
