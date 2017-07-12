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

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 *  @author shood
 * */
public class DoubleButtonPanel<T> extends BasePanel<T>{

    private static final String ID_BUTTON_FIRST = "first";
    private static final String ID_BUTTON_SECOND = "second";

    private String firstCaption;
    private String secondCaption;

    public DoubleButtonPanel(String id, IModel<T> model){
        super(id, model);
        initLayout();
    }

    private void initLayout(){
        AjaxButton firstButton = new AjaxButton(ID_BUTTON_FIRST, createButtonStringResource(getFirstCaption())) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                firstPerformed(target, DoubleButtonPanel.this.getModel());
            }

            @Override
            public boolean isEnabled(){
                return isFirstEnabled(DoubleButtonPanel.this.getModel());
            }

        };
        firstButton.add(new AttributeAppender("class", getFirstCssClass()));

        AjaxButton secondButton = new AjaxButton(ID_BUTTON_SECOND,createButtonStringResource(getSecondCaption())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                secondPerformed(target, DoubleButtonPanel.this.getModel());
            }

            @Override
            public boolean isEnabled(){
                return isSecondEnabled(DoubleButtonPanel.this.getModel());
            }
        };
        secondButton.add(new AttributeAppender("class", getSecondCssClass()));

        add(firstButton);
        add(secondButton);
    }

    private String getFirstCssClass(){
        StringBuilder sb = new StringBuilder();
        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        sb.append(getFirstCssColorClass()).append(" ").append(getFirstCssSizeClass());
        return sb.toString();
    }

    private String getSecondCssClass(){
        StringBuilder sb = new StringBuilder();
        sb.append(DoubleButtonColumn.BUTTON_BASE_CLASS).append(" ");
        sb.append(getSecondCssColorClass()).append(" ").append(getSecondCssSizeClass());
        return sb.toString();
    }

    public String getFirstCssSizeClass(){
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.DEFAULT.toString();
    }

    public String getSecondCssSizeClass(){
        return DoubleButtonColumn.BUTTON_SIZE_CLASS.DEFAULT.toString();
    }

    public String getFirstCssColorClass(){
        return DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    public String getSecondCssColorClass(){
        return DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString();
    }

    public String getFirstCaption(){
        return firstCaption;
    }

    public String getSecondCaption(){
        return secondCaption;
    }

    private StringResourceModel createButtonStringResource(String caption){
        if(caption == null){
            return createStringResource("DoubleButtonPanel.button.default");
        } else {
            return createStringResource(caption);
        }
    }

    public void firstPerformed(AjaxRequestTarget target, IModel<T> model){}
    public void secondPerformed(AjaxRequestTarget target, IModel<T> model){}

    public boolean isFirstEnabled(IModel<T> model){
        return true;
    }

    public boolean isSecondEnabled(IModel<T> model){
        return true;
    }

    public AjaxButton getFirstButton(){
        return (AjaxButton) get(ID_BUTTON_FIRST);
    }

    public AjaxButton getSecondButton(){
        return (AjaxButton) get(ID_BUTTON_SECOND);
    }
}
