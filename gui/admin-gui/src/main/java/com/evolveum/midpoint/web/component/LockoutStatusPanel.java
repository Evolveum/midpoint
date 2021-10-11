/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;

/**
 * Created by honchar
 */
public class LockoutStatusPanel extends Panel {
    private static final String ID_CONTAINER = "container";
    private static final String ID_LABEL = "label";
    private static final String ID_BUTTON = "button";
    private static final String ID_FEEDBACK = "feedback";
    private boolean isInitialState = true;
    private LockoutStatusType initialValue;


    public LockoutStatusPanel(String id){
        this(id, null);
    }

    public LockoutStatusPanel(String id, IModel<LockoutStatusType> model){
        super(id);
        initialValue = model.getObject(); //TODO: clone
        initLayout(model);
    }

    private void initLayout(final IModel<LockoutStatusType> model){
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        add(container);

        Label label = new Label(ID_LABEL, new IModel<String>() {
            @Override
            public String getObject() {
                LockoutStatusType object = model != null ? model.getObject() : null;

                String labelValue = object == null ?
                        ((PageBase)getPage()).createStringResource("LockoutStatusType.UNDEFINED").getString()
                        : WebComponentUtil.createLocalizedModelForEnum(object, getLabel()).getObject();
                if (!isInitialState){
                    labelValue += " " + ((PageBase) getPage()).createStringResource("LockoutStatusPanel.changesSaving").getString();
                }
                return labelValue;
            }

            @Override
            public void setObject(String s) {
            }

            @Override
            public void detach() {

            }
        });
        label.setOutputMarkupId(true);
        container.add(label);

        AjaxButton button = new AjaxButton(ID_BUTTON, getButtonModel()) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (!isInitialState){
                    model.setObject(initialValue);
                } else {
                    model.setObject(LockoutStatusType.NORMAL);
                }
                isInitialState = !isInitialState;
                ajaxRequestTarget.add(getButton());
                ajaxRequestTarget.add(getLabel());
            }
        };
        button.add(new VisibleEnableBehaviour(){
            @Override
        public boolean isVisible(){
                return true;
            }
        });
        button.setOutputMarkupId(true);
        container.add(button);
    }

    private IModel<String> getButtonModel(){
        return new IModel<String>() {
            @Override
            public String getObject() {
                if (isInitialState){
                    return ((PageBase)getPage()).createStringResource("LockoutStatusPanel.unlockButtonLabel").getString();
                } else {
                    return ((PageBase)getPage()).createStringResource("LockoutStatusPanel.undoButtonLabel").getString();
                }
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        };
    }

    private Component getButton(){
        return get(ID_CONTAINER).get(ID_BUTTON);
    }

    private Component getLabel(){
        return get(ID_CONTAINER).get(ID_LABEL);
    }
}
