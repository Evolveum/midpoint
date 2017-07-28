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
package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

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
    private ValueWrapper valueWrapper;

    public LockoutStatusPanel(String id){
        this(id, null, null);
    }

    public LockoutStatusPanel(String id, ValueWrapper valueWrapper, IModel<LockoutStatusType> model){
        super(id);
        initialValue = model.getObject();
        this.valueWrapper = valueWrapper;
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
                PrismPropertyValue oldValue = (PrismPropertyValue)valueWrapper.getOldValue();
                if (!isInitialState){
                    model.setObject(initialValue);
                    oldValue.setValue(initialValue);
                    valueWrapper.setStatus(ValueStatus.NOT_CHANGED);
                } else {
                    model.setObject(LockoutStatusType.NORMAL);
                    if (oldValue.getValue() != null) {
                        oldValue.setValue(null);
                    }
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
