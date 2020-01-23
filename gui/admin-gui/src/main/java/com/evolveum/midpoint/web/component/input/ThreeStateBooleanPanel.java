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
 *  Use this component when three-state widget is needed (e.g. Boolean attributes - true/false/undefined)
 *  Bear in mind that:
 *     - first button represents TRUE
 *     - second button represents UNDEFINED
 *     - third button represents FALSE
 *
 * @deprecated Don't use this component, it will be gradually removed from gui.
 * It can be easily replaced with TriStateComboPanel. [honchar]
 *
 *  @author shood
 */
@Deprecated
public class ThreeStateBooleanPanel extends BasePanel<Boolean>{

    private static final String ID_BUTTON_ONE = "optionOne";
    private static final String ID_BUTTON_TWO = "optionTwo";
    private static final String ID_BUTTON_THREE = "optionThree";

    private static final String DEFAULT_BUTTON_CLASS = "btn-default";

    public ThreeStateBooleanPanel(String id, IModel<Boolean> model){
        this(id, model, null, null, null, null);
    }

    public ThreeStateBooleanPanel(String id, IModel<Boolean> model, String optionOneLabel,
                                  String optionTwoLabel, String optionThreeLabel, String buttonCssClass){
        super(id, model);

        setOutputMarkupId(true);
        initLayout(optionOneLabel, optionTwoLabel, optionThreeLabel, buttonCssClass);
    }

    private void initLayout(final String optionOneLabel, final String optionTwoLabel,
                              final String optionThreeLabel, final String buttonCssClass){

        AjaxButton buttonTrue = new AjaxButton(ID_BUTTON_ONE, new IModel<String>() {

            @Override
            public String getObject() {
                if(optionOneLabel == null){
                    return getString("ThreeStateBooleanPanel.true");
                } else {
                    return getString(optionOneLabel);
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

        AjaxButton buttonUndef = new AjaxButton(ID_BUTTON_TWO, new IModel<String>() {

            @Override
            public String getObject() {
                if(optionTwoLabel == null){
                    return getString("ThreeStateBooleanPanel.undef");
                } else {
                    return getString(optionTwoLabel);
                }
            }
        }) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                stateChanged(null, target);
            }
        };
        buttonUndef.setOutputMarkupId(true);
        buttonUndef.add(prepareButtonCssClass(buttonCssClass));
        buttonUndef.add(prepareActiveButtonAppender(null));
        add(buttonUndef);

        AjaxButton buttonFalse = new AjaxButton(ID_BUTTON_THREE, new IModel<String>() {

            @Override
            public String getObject() {
                if(optionThreeLabel == null){
                    return getString("ThreeStateBooleanPanel.false");
                } else {
                    return getString(optionThreeLabel);
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
    }

    private void stateChanged(Boolean newValue, AjaxRequestTarget target){
        getModel().setObject(newValue);
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
}
