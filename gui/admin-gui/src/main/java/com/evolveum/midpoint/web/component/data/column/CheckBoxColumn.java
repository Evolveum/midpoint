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

import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.IAjaxCallListener;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CheckBoxColumn<T extends Serializable> extends AbstractColumn<T, String> {

    private String propertyExpression;
    private IModel<Boolean> enabled = new Model<Boolean>(true);

    public CheckBoxColumn(IModel<String> displayModel) {
        this(displayModel, Selectable.F_SELECTED);
    }

    public CheckBoxColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel);
        this.propertyExpression = propertyExpression;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
        IModel<Boolean> selected = new PropertyModel<Boolean>(rowModel, propertyExpression);

        CheckBoxPanel check = new CheckBoxPanel(componentId, selected, enabled) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                DataTable table = findParent(DataTable.class);
                onUpdateRow(target, table, rowModel);

                //updating table row
                target.add(cellItem.findParent(SelectableDataTable.SelectableRowItem.class));
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                // this will disable javascript event propagation from checkbox to parent dom components
                attributes.getAjaxCallListeners().add(
                        new AjaxCallListener().onBefore("\nattrs.event.stopPropagation();"));
            }
        };
        check.setOutputMarkupId(true);

        cellItem.add(check);
    }

    @Override
    public String getCssClass() {
        return "tableCheckbox";
    }

    protected IModel<Boolean> getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled.setObject(enabled);
    }

    protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<T> rowModel) {
    }

    protected String getPropertyExpression() {
        return propertyExpression;
    }
}
