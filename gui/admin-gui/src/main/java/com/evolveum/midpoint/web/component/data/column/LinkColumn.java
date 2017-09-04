/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class LinkColumn<T> extends AbstractColumn<T, String> implements IExportableColumn<T, String>  {
	private static final long serialVersionUID = 1L;

	private String propertyExpression;

    public LinkColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public LinkColumn(IModel<String> displayModel, String propertyExpression) {
        this(displayModel, null, propertyExpression);
    }

    public LinkColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
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
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {

    	IModel model = createLinkModel(rowModel);
        cellItem.add(new LinkPanel(componentId, model) {
        	private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                LinkColumn.this.onClick(target, rowModel);
            }

            @Override
            public boolean isEnabled() {
                return LinkColumn.this.isEnabled(rowModel);
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
        return new PropertyModel<String>(rowModel, propertyExpression);
    }

}
