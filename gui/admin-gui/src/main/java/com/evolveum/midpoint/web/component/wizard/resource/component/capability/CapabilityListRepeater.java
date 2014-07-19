/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.web.component.wizard.resource.component.capability;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityListRepeater<T extends Serializable> extends SimplePanel<List<T>> {

    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT = "input";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_PLUS = "plus";
    private static final String ID_MINUS = "minus";

    private static final String CSS_DISABLED = " disabled";

    public CapabilityListRepeater(String id, IModel<List<T>> value){
        super(id, value);
        setOutputMarkupId(true);

        initLayout(true);
    }

    private void initLayout(boolean unused){

        if(getModel().getObject() == null){
            getModel().setObject(new ArrayList<>(Arrays.asList(createNewEmptyItem())));
        } else if(getModel().getObject().isEmpty()){
            getModel().getObject().add(createNewEmptyItem());
        }

        ListView repeater = new ListView<T>(ID_REPEATER, getModel()){

            @Override
            protected void populateItem(final ListItem<T> item) {
                TextField text = new TextField<>(ID_TEXT, createTextModel(item.getModel()));
                text.add(new AjaxFormComponentUpdatingBehavior("onblur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {}
                });
                text.add(AttributeAppender.replace("placeholder", createEmptyItemPlaceholder()));
                item.add(text);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                item.add(buttonGroup);
                initButtons(buttonGroup, item);
            }
        };
        add(repeater);
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item) {
        AjaxLink plus = new AjaxLink(ID_PLUS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        plus.add(new AttributeAppender("class", getPlusClassModifier(item)));
        buttonGroup.add(plus);

        AjaxLink minus = new AjaxLink(ID_MINUS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target, item);
            }
        };
        minus.add(new AttributeAppender("class", getMinusClassModifier()));
        buttonGroup.add(minus);
    }

    protected String getPlusClassModifier(ListItem<T> item){
        int size = getModelObject().size();
        if (size <= 1) {
            return "";
        }
        if (item.getIndex() == size - 1) {
            return "";
        }

        return CSS_DISABLED;
    }

    protected String getMinusClassModifier(){
        int size = getModelObject().size();
        if (size > 1) {
            return "";
        }

        return CSS_DISABLED;
    }

    protected IModel<String> createTextModel(final IModel<T> model) {
        return new IModel<String>() {
            @Override
            public String getObject() {
                T obj = model.getObject();
                return obj != null ? obj.toString() : null;
            }

            @Override
            public void setObject(String object) {
                model.setObject((T) object);
            }

            @Override
            public void detach() {
            }
        };
    }

    protected void addValuePerformed(AjaxRequestTarget target){
        List<T> objects = getModelObject();
        objects.add(createNewEmptyItem());

        target.add(this);
    }

    protected T createNewEmptyItem(){
        return (T)"";
    }

    protected StringResourceModel createEmptyItemPlaceholder(){
        return createStringResource("capabilityListRepeater.label.text.placeholder");
    }

    protected void removeValuePerformed(AjaxRequestTarget target, ListItem<T> item){
        List<T> objects = getModelObject();
        Iterator<T> iterator = objects.iterator();
        while (iterator.hasNext()) {
            T object = iterator.next();
            if (object.equals(item.getModelObject())) {
                iterator.remove();
                break;
            }
        }

        target.add(this);
    }
}
