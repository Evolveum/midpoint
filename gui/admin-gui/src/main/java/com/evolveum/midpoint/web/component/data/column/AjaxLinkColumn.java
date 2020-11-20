/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class AjaxLinkColumn<T> extends AbstractColumn<T, String> implements IExportableColumn<T, String>  {
    private static final long serialVersionUID = 1L;

    private String propertyExpression;

    public AjaxLinkColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public AjaxLinkColumn(IModel<String> displayModel, String propertyExpression) {
        this(displayModel, null, propertyExpression);
    }

    public AjaxLinkColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty);
        this.propertyExpression = propertyExpression;
    }

    protected String getPropertyExpression() {
        return propertyExpression;
    }

    protected IModel createLinkModel(IModel<T> rowModel) {
        return new PropertyModel<String>(rowModel, propertyExpression);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, final IModel<T> rowModel) {

        IModel model = createLinkModel(rowModel);
        cellItem.add(new AjaxLinkPanel(componentId, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkColumn.this.onClick(target, rowModel);
            }

            @Override
            public boolean isEnabled() {
                return AjaxLinkColumn.this.isEnabled(rowModel);
            }
        });
    }

    public boolean isEnabled(IModel<T> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<T> rowModel) {
    }

    @Override
    public IModel<String> getDataModel(IModel<T> rowModel) {
        return new PropertyModel<>(rowModel, propertyExpression);
    }

}
