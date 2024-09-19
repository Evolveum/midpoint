/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.Badge;

/**
 * @author lazyman
 */
public class AjaxLinkWithBadgesColumn<T> extends AbstractColumn<T, String> implements IExportableColumn<T, String> {

    private static final long serialVersionUID = 1L;

    private String propertyExpression;

    public AjaxLinkWithBadgesColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public AjaxLinkWithBadgesColumn(IModel<String> displayModel, String propertyExpression) {
        this(displayModel, null, propertyExpression);
    }

    public AjaxLinkWithBadgesColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
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
        AjaxLinkWithBadgesPanel<T> panel = new AjaxLinkWithBadgesPanel(componentId, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkWithBadgesColumn.this.onClick(target, rowModel);
            }

            @Override
            public boolean isEnabled() {
                return AjaxLinkWithBadgesColumn.this.isEnabled(rowModel);
            }

            @Override
            protected IModel<List<Badge>> createBadgesModel() {
                return AjaxLinkWithBadgesColumn.this.createBadgesModel(rowModel);
            }
        };
        panel.add(AttributeModifier.replace("class", "d-flex gap-1"));
        cellItem.add(panel);
    }

    public boolean isEnabled(IModel<T> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<T> rowModel) {
    }

    protected IModel<List<Badge>> createBadgesModel(IModel<T> rowModel) {
        return () -> List.of();
    }

    @Override
    public IModel<String> getDataModel(IModel<T> rowModel) {
        return new PropertyModel<>(rowModel, propertyExpression);
    }
}
