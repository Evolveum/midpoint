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

import com.evolveum.midpoint.web.component.data.MultiButtonPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

/**
 * Created by honchar.
 */
public class InlineMenuButtonColumn<T extends Serializable> extends MultiButtonColumn<T>{
    private static int numberOfDisplayedButtons = 3;
    private List<InlineMenuItem> menuItems;

    public InlineMenuButtonColumn(List<InlineMenuItem> menuItems){
        super(null, menuItems.size() < 3 ? menuItems.size() : numberOfDisplayedButtons);
        if (menuItems.size() < 3){
            numberOfDisplayedButtons = menuItems.size();
        }
        this.menuItems = menuItems;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
        this.rowModel = rowModel;
        cellItem.add(getPanel(componentId, this.rowModel));
    }

    @Override
    public Component getHeader(String componentId) {
        return getPanel(componentId, null);
    }

    private Component getPanel(String componentId, IModel<T> rowModel){
        panel = new MultiButtonPanel<T>(componentId, numberOfDisplayedButtons, rowModel) {

            @Override
            public String getCaption(int id) {
                return "";
            }

            @Override
            protected String getButtonCssClass(int id) {
                return InlineMenuButtonColumn.this.getButtonCssClass(id);
            }

            @Override
            protected int getButtonId(int id){
                for (InlineMenuItem menuItem : menuItems){
                    if (menuItem.getId() == id){
                        return menuItem.getId();
                    }
                }
                return id;
            }
            @Override
            public String getButtonSizeCssClass(int id) {
                return InlineMenuButtonColumn.this.getButtonSizeCssClass(id);
            }

            @Override
            public String getButtonColorCssClass(int id) {
                return InlineMenuButtonColumn.this.getButtonColorCssClass(id);
            }

            @Override
            public void clickPerformed(int id, AjaxRequestTarget target, IModel<T> model) {
                for (InlineMenuItem menuItem : menuItems){
                    if (menuItem.getAction() != null) {
                        ((ColumnMenuAction) menuItem.getAction()).setRowModel(rowModel);
                    }
                }
                InlineMenuButtonColumn.this.menuItemClickPerformed(id, target, model);
            }
        };
        return panel;
    }

    private void menuItemClickPerformed(int id, AjaxRequestTarget target, IModel<T> model){
        for (InlineMenuItem menuItem : menuItems){
            if (menuItem.getId() == id){
                if (menuItem.getAction() != null) {
                    menuItem.getAction().onClick(target);
                }
            }
        }
    }

    @Override
    public String getButtonColorCssClass(int id) {
        for (InlineMenuItem menuItem : menuItems){
            if (menuItem.getId() == id){
                return menuItem.getButtonColorCssClass();
            }
        }
        return DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    @Override
    public String getButtonSizeCssClass(int id) {
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.EXTRA_SMALL.toString();
    }

    @Override
    protected String getButtonCssClass(int id) {
        StringBuilder sb = new StringBuilder();
        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        sb.append(getButtonColorCssClass(id)).append(" ");
        sb.append(getButtonSizeCssClass(id)).append(" ");
        for (InlineMenuItem menuItem : menuItems){
            if (menuItem.getId() == id){
                sb.append(menuItem.getButtonIconCssClass()).append(" ");
            }
        }
        return sb.toString();
    }

//    @Override
//    public void clickPerformed(int id, AjaxRequestTarget target, IModel<SelectableBean<UserType>> model) {
//
//    }

}
