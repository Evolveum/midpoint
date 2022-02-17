/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
        return DoubleButtonColumn.ButtonSizeClass.DEFAULT.toString();
    }

    public String getButtonCssColorClass(){
        return DoubleButtonColumn.ButtonColorClass.DEFAULT.toString();
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
