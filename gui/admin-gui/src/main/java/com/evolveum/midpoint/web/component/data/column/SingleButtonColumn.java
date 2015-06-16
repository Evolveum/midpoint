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

import com.evolveum.midpoint.web.component.data.SingleButtonPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * EXPERIMENTAL
 *
 *  @author shood
 *  @author mederly
 */
public class SingleButtonColumn<T extends Serializable> extends AbstractColumn<T, String> {

    public static final String BUTTON_BASE_CLASS = "btn";

    private SingleButtonPanel panel;

    private String caption;
    private IModel<T> rowModel;

    private String propertyExpression;

    public SingleButtonColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel);
        this.propertyExpression = propertyExpression;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel){
        this.rowModel = rowModel;

        panel = new SingleButtonPanel<T>(componentId, rowModel){

            @Override
            public String getButtonCssSizeClass(){
                return SingleButtonColumn.this.getButtonCssSizeClass();
            }

            @Override
            public String getButtonCssColorClass(){
                return SingleButtonColumn.this.getButtonCssColorClass();
            }

            @Override
            public String getCaption(){
                return SingleButtonColumn.this.getCaption();
            }

            @Override
            public void clickPerformed(AjaxRequestTarget target, IModel<T> model){
                SingleButtonColumn.this.clickPerformed(target, model);
            }

            @Override
            public boolean isEnabled(IModel<T> model){
                return isButtonEnabled(model);
            }

            @Override
            public boolean isVisible(IModel<T> model){
                return isButtonVisible(model);
            }
        };
        cellItem.add(panel);
    }

    public void clickPerformed(AjaxRequestTarget target, IModel<T> model){}

    public String getButtonCssSizeClass(){
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.SMALL.toString();
    }

    public String getButtonCssColorClass(){
        return DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    public String getCaption(){
        return caption;
    }

    public boolean isButtonEnabled(IModel<T> model){
        return true;
    }

    public boolean isButtonVisible(IModel<T> model){
        return true;
    }

    public SingleButtonPanel getButtonPanel(){
        return panel;
    }

    protected IModel<T> getRowModel(){
        return rowModel;
    }
}
