/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.basic.Label;
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
    private IModel<Boolean> enabled = new Model<>(true);

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
        IModel<Boolean> selected = getCheckBoxValueModel(rowModel);

        IsolatedCheckBoxPanel check = new IsolatedCheckBoxPanel(componentId, selected, getEnabled(rowModel)) {

            @Override
            public void onUpdate(AjaxRequestTarget target) {
                DataTable table = findParent(DataTable.class);
                onUpdateRow(cellItem, target, table, rowModel, selected);

                //updating table row
//                target.add(cellItem.findParent(SelectableDataTable.SelectableRowItem.class));
            }
        };
        check.getPanelComponent().add(AttributeAppender.append("aria-label", () -> {
            if (getDisplayModel() == null || StringUtils.isBlank(getDisplayModel().getObject())) {
                return LocalizationUtil.translate("CheckBoxColumn.header");
            }
            return getDisplayModel().getObject();
        }));
        check.setOutputMarkupId(true);
        processBehaviourOfCheckBox(check, rowModel);

        cellItem.add(check);
    }

    protected void processBehaviourOfCheckBox(IsolatedCheckBoxPanel check, IModel<T> rowModel) {
    }

    protected IModel<Boolean> getCheckBoxValueModel(IModel<T> rowModel){
        return new PropertyModel<>(rowModel, propertyExpression);
    }

    @Override
    public String getCssClass() {
        IModel<String> display = getDisplayModel();
        if (display != null && StringUtils.isNotEmpty(display.getObject())) {
            return null;
        }

        return "icon";
    }

    protected IModel<Boolean> getEnabled(IModel<T> rowModel) {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled.setObject(enabled);
    }

    protected void onUpdateRow(Item<ICellPopulator<T>> cellItem, AjaxRequestTarget target, DataTable table, IModel<T> rowModel, IModel<Boolean> selected) {
    }

    protected String getPropertyExpression() {
        return propertyExpression;
    }

    @Override
    public Component getHeader(String componentId) {
        if (getDisplayModel() == null || StringUtils.isBlank(getDisplayModel().getObject())) {
            Label label = new Label(componentId, () -> LocalizationUtil.translate("CheckBoxColumn.header"));
            label.add(AttributeAppender.append("class", "sr-only"));
        }
        return super.getHeader(componentId);
    }
}
