/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.List;

/**
 * @author lazyman
 */
public class CheckBoxHeaderColumn<T extends Serializable> extends CheckBoxColumn {

    private static final Trace LOGGER = TraceManager.getTrace(CheckBoxHeaderColumn.class);

    public CheckBoxHeaderColumn() {
        super(null);
    }

    @Override
    public Component getHeader(String componentId) {
        final Model<Boolean> model = new Model<Boolean>();
        return new CheckBoxPanel(componentId, model, getEnabled()) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                DataTable table = findParent(DataTable.class);
                boolean selected = model.getObject() != null ? model.getObject() : false;
                onUpdateHeader(target, selected, table);
            }
        };
    }

    @Override
    public String getCssClass() {
        return "tableCheckbox";
    }

    public void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
        IDataProvider provider = table.getDataProvider();
        if (!(provider instanceof BaseSortableDataProvider)) {
            LOGGER.debug("Select all checkbox work only with {} provider type. Current provider is type of {}.",
                    new Object[]{BaseSortableDataProvider.class.getName(), provider.getClass().getName()});
        }

        BaseSortableDataProvider baseProvider = (BaseSortableDataProvider) provider;
        List<T> objects = baseProvider.getAvailableData();
        for (T object : objects) {
            if (object instanceof Selectable) {
                Selectable selectable = (Selectable) object;
                selectable.setSelected(selected);
            }
        }
    }
}
