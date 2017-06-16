/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.web.component.data.MultiButtonPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

/**
 *  @author shood
 *  @author mederly
 */
public class MultiButtonColumn<T extends Serializable> extends AbstractColumn<T, String> {

    protected MultiButtonPanel panel;

    private List<String> captions;
    protected IModel<T> rowModel;
    protected int numberOfButtons;

    public MultiButtonColumn(int numberOfButtons) {
        super(null);
        this.numberOfButtons = numberOfButtons;
    }

    public MultiButtonColumn(IModel<String> displayModel, int numberOfButtons) {
        super(displayModel);
        this.numberOfButtons = numberOfButtons;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {
        this.rowModel = rowModel;

        panel = new MultiButtonPanel<T>(componentId, numberOfButtons, rowModel) {

            @Override
            public String getCaption(int id) {
                return MultiButtonColumn.this.getCaption(id);
            }

            @Override
            public String getButtonTitle(int id) {
                return MultiButtonColumn.this.getButtonTitle(id);
            }

            @Override
            public boolean isButtonEnabled(int id, IModel<T> model) {
                return MultiButtonColumn.this.isButtonEnabled(id, model);
            }

            @Override
            public boolean isButtonVisible(int id, IModel<T> model) {
                return MultiButtonColumn.this.isButtonVisible(id, model);
            }

            @Override
            protected String getButtonCssClass(int id) {
                return MultiButtonColumn.this.getButtonCssClass(id);
            }

            @Override
            public String getButtonSizeCssClass(int id) {
                return MultiButtonColumn.this.getButtonSizeCssClass(id);
            }

            @Override
            public String getButtonColorCssClass(int id) {
                return MultiButtonColumn.this.getButtonColorCssClass(id);
            }

            @Override
            public void clickPerformed(int id, AjaxRequestTarget target, IModel<T> model) {
                MultiButtonColumn.this.clickPerformed(id, target, model);
            }


        };
        cellItem.add(panel);
    }

    public boolean isButtonEnabled(int id, IModel<T> model) {
        return true;
    }

    protected String getButtonCssClass(int id) {
        StringBuilder sb = new StringBuilder();
        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        sb.append(getButtonColorCssClass(id)).append(" ").append(getButtonSizeCssClass(id));
        if (!isButtonEnabled(id, getRowModel())) {
            sb.append(" disabled");
        }
        return sb.toString();
    }

    public boolean isButtonVisible(int id, IModel<T> model) {
        return true;
    }

    public String getCaption(int id) {
        return String.valueOf(id);
    }

    public void clickPerformed(int id, AjaxRequestTarget target, IModel<T> model) {
    }

    public String getButtonColorCssClass(int id) {
        return DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    public String getButtonSizeCssClass(int id) {
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.SMALL.toString();
    }

    public MultiButtonPanel getButtonPanel(){
        return panel;
    }

    public IModel<T> getRowModel(){
        return rowModel;
    }

    public String getButtonTitle(int id) {
        return "";
    }

}
