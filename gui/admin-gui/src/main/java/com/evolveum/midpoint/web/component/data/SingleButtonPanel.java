/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.data.column.SingleButtonColumn;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 *  @author shood
 *  @author mederly
 */
public class SingleButtonPanel<T> extends BasePanel<T> {

    private static final String ID_BUTTON = "button";

    private String caption;

    public SingleButtonPanel(String id, IModel<T> model){
        super(id, model);
        initLayout();
    }

    private void initLayout(){
        AjaxButton button = new AjaxButton(ID_BUTTON, createButtonStringResource(getCaption())) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clickPerformed(target, SingleButtonPanel.this.getModel());
            }

            @Override
            public boolean isEnabled(){
                return SingleButtonPanel.this.isEnabled(SingleButtonPanel.this.getModel());
            }

            @Override
            public boolean isVisible() {
                return SingleButtonPanel.this.isVisible(SingleButtonPanel.this.getModel());
            }
        };
        button.add(new AttributeAppender("class", getButtonCssClass()));

        add(button);
    }

    private String getButtonCssClass(){
        StringBuilder sb = new StringBuilder();
        sb.append(SingleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        sb.append(getButtonCssColorClass()).append(" ").append(getButtonCssSizeClass());
        return sb.toString();
    }

    public String getButtonCssSizeClass(){
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.DEFAULT.toString();
    }

    public String getButtonCssColorClass(){
        return DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    public String getCaption(){
        return caption;
    }

    private StringResourceModel createButtonStringResource(String caption) {
        if (caption == null){
            return createStringResource("SingleButtonPanel.button.default");
        } else {
            return createStringResource(caption);
        }
    }

    public void clickPerformed(AjaxRequestTarget target, IModel<T> model){}

    public boolean isEnabled(IModel<T> model){
        return true;
    }

    public boolean isVisible(IModel<T> model){
        return true;
    }

    public AjaxButton getButton() {
        return (AjaxButton) get(ID_BUTTON);
    }
}
