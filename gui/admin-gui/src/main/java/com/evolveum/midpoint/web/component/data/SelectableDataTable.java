package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import java.util.List;

public class SelectableDataTable<T> extends DataTable<T, String> {

    public SelectableDataTable(String id, List<IColumn<T, String>> columns, IDataProvider<T> dataProvider, int rowsPerPage) {
        super(id, columns, dataProvider, rowsPerPage);
    }

    @Override
    protected Item<T> newRowItem(String id, int index, IModel<T> model) {
        final Item<T> rowItem = new Item<T>(id, index, model);

        rowItem.add(new AjaxEventBehavior("onclick") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                T object = rowItem.getModel().getObject();
                if (!(object instanceof Selectable)) {
                    //todo I dont' get it, but what else to do? [lazyman]
                    return;
                }

                Selectable selectable = (Selectable) object;

                boolean enabled = !selectable.isSigned();
                selectable.setSelected(enabled);
                selectable.setSigned(enabled);
            }
        });

        rowItem.setOutputMarkupId(true);
        return rowItem;
    }
}
