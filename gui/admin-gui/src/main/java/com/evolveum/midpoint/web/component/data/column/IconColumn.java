/*
 * Copyright (c) 2010-2018 Evolveum
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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class IconColumn<T> extends AbstractColumn<T, String> implements IExportableColumn<T, String> {
    private static final long serialVersionUID = 1L;

    public IconColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public String getCssClass() {
        IModel<String> display = getDisplayModel();
        return StringUtils.isNoneEmpty(display.getObject()) ? null : "icon";
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        cellItem.add(new ImagePanel(componentId, createIconModel(rowModel), createTitleModel(rowModel)));
    }

    protected IModel<String> createTitleModel(final IModel<T> rowModel) {
        return null;
    }

    protected IModel<String> createIconModel(final IModel<T> rowModel) {
        throw new UnsupportedOperationException("Not implemented, please implement in your column.");
    }

    @Override
    public IModel<String> getDataModel(IModel<T> rowModel) {
        return Model.of("");
    }
}
