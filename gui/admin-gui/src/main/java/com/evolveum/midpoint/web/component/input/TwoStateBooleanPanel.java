/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;

/**
 *  Use this component when two-state widget is needed (e.g. boolean attributes - true/false)
 *  Bear in mind that:
 *     - first button represents FALSE
 *     - second button represents TRUE
 *
 *  @author shood
 * */
public class TwoStateBooleanPanel extends BasePanel<Boolean>{

    private static final String ID_BUTTON_ONE = "optionOne";
    private static final String ID_BUTTON_TWO = "optionTwo";

    private static final String DEFAULT_BUTTON_CLASS = "btn-default";

    public TwoStateBooleanPanel(String id, IModel<Boolean> model){
        this(id, model, null, null, null);
    }

    public TwoStateBooleanPanel(String id, IModel<Boolean> model, String optionOneLabel,
                                  String optionTwoLabel, String buttonCssClass){
        super(id, model);

        setOutputMarkupId(true);
        initLayout(optionOneLabel, optionTwoLabel, buttonCssClass);
    }

    private void initLayout(final String optionOneLabel, final String optionTwoLabel, final String buttonCssClass){

        AjaxButton buttonFalse = new AjaxButton(ID_BUTTON_ONE, new IModel<String>() {

            @Override
            public String getObject() {
                if(optionOneLabel == null){
                    return getString("ThreeStateBooleanPanel.false");
                } else {
                    return getString(optionOneLabel);
                }
            }
        }) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stateChanged(Boolean.FALSE, target);
            }
        };
        buttonFalse.setOutputMarkupId(true);
        buttonFalse.add(prepareButtonCssClass(buttonCssClass));
        buttonFalse.add(prepareActiveButtonAppender(Boolean.FALSE));
        add(buttonFalse);

        AjaxButton buttonTrue = new AjaxButton(ID_BUTTON_TWO, new IModel<String>() {

            @Override
            public String getObject() {
                if(optionTwoLabel == null){
                    return getString("ThreeStateBooleanPanel.true");
                } else {
                    return getString(optionTwoLabel);
                }
            }
        }) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stateChanged(Boolean.TRUE, target);
            }
        };
        buttonTrue.setOutputMarkupId(true);
        buttonTrue.add(prepareButtonCssClass(buttonCssClass));
        buttonTrue.add(prepareActiveButtonAppender(Boolean.TRUE));
        add(buttonTrue);
    }

    private void stateChanged(Boolean newValue, AjaxRequestTarget target){
        getModel().setObject(newValue);
        onStateChanged(target, newValue);
        target.add(this);
    }

    private AttributeAppender prepareActiveButtonAppender(final Boolean value){
        return new AttributeAppender("class", new IModel<String>() {

            @Override
            public String getObject() {
                if(getModel() != null){
                    return getModel().getObject() == value ? " active" : null;
                }

                return null;
            }
        });
    }

    private AttributeAppender prepareButtonCssClass(String cssClass){
        if(cssClass == null){
            return new AttributeAppender("class", " " + DEFAULT_BUTTON_CLASS);
        } else {
            return new AttributeAppender("class", " " + cssClass);
        }
    }

    /**
     *  Override to provide custom action on change state event
     * */
    protected void onStateChanged(AjaxRequestTarget target, Boolean newValue){}

    public void setPanelEnabled(boolean isEnabled){
        get(ID_BUTTON_ONE).setEnabled(isEnabled);
        get(ID_BUTTON_TWO).setEnabled(isEnabled);
    }
}
