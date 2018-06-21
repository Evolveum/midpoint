/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class InlineMenuColumn<T extends InlineMenuable> extends AbstractColumn<T, Void> {

    public InlineMenuColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                             IModel<T> rowModel) {

        cellItem.add(new com.evolveum.midpoint.web.component.menu.cog.InlineMenu(componentId,
                createMenuModel(rowModel)));
    }

    private IModel<List<InlineMenuItem>> createMenuModel(final IModel<T> rowModel) {
        return new LoadableModel<List<InlineMenuItem>>(false) {

            @Override
            public List<InlineMenuItem> load() {
                if (!(rowModel.getObject() instanceof InlineMenuable)) {
                    return new ArrayList<>();
                }

                T row = rowModel.getObject();
                if (row.getMenuItems() == null) {
                    return new ArrayList<>();
                }

                for (InlineMenuItem item : row.getMenuItems()) {
                    if (!(item.getAction() instanceof ColumnMenuAction)) {
                        continue;
                    }

                    ColumnMenuAction action = (ColumnMenuAction) item.getAction();
                    action.setRowModel(rowModel);
                }

                return row.getMenuItems();
            }
        };
    }

    @Override
    public String getCssClass() {
        return "cog";
    }
}
