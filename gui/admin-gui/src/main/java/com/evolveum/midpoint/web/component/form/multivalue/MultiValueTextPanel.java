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
package com.evolveum.midpoint.web.component.form.multivalue;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  @author shood
 * */
public class MultiValueTextPanel<T extends Serializable> extends BasePanel<List<T>> {

    private static final String ID_PLACEHOLDER_CONTAINER = "placeholderContainer";
    private static final String ID_PLACEHOLDER_ADD = "placeholderAdd";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT = "input";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_PLUS = "plus";
    private static final String ID_MINUS = "minus";

    private static final String CSS_DISABLED = " disabled";

    public MultiValueTextPanel(String id, IModel<List<T>> value, NonEmptyModel<Boolean> readOnlyModel, boolean emptyStringToNull) {
        super(id, value);
        setOutputMarkupId(true);

        initLayout(readOnlyModel, emptyStringToNull);
    }

    @Override
    public IModel<List<T>> getModel(){
        if(super.getModel().getObject() == null){
            super.getModel().setObject(new ArrayList<T>());
        }

        return super.getModel();
    }

    private void initLayout(final NonEmptyModel<Boolean> readOnlyModel, final boolean emptyStringToNull) {
        WebMarkupContainer placeholderContainer = new WebMarkupContainer(ID_PLACEHOLDER_CONTAINER);
        placeholderContainer.setOutputMarkupPlaceholderTag(true);
        placeholderContainer.setOutputMarkupPlaceholderTag(true);
        placeholderContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return getModel().getObject().isEmpty();
            }
        });
        add(placeholderContainer);

        AjaxLink placeholderAdd = new AjaxLink(ID_PLACEHOLDER_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
		placeholderAdd.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        placeholderAdd.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if (buttonsDisabled()) {
                    return " " + CSS_DISABLED;
                }

                return "";
            }
        }));
        placeholderAdd.setOutputMarkupId(true);
        placeholderAdd.setOutputMarkupPlaceholderTag(true);
        placeholderContainer.add(placeholderAdd);

        ListView repeater = new ListView<T>(ID_REPEATER, getModel()){

            @Override
            protected void populateItem(final ListItem<T> item) {
                TextField text = new TextField<>(ID_TEXT, createTextModel(item.getModel()));
                text.add(new AjaxFormComponentUpdatingBehavior("blur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        T updatedValue = (T)text.getConvertedInput();
                        List<T> modelObject = MultiValueTextPanel.this.getModelObject();
                        modelObject.set(item.getIndex(), updatedValue);
                        modelObjectUpdatePerformed(target, modelObject);
                    }
                });
                text.add(AttributeAppender.replace("placeholder", createEmptyItemPlaceholder()));
				text.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
				text.setConvertEmptyInputStringToNull(emptyStringToNull);
                item.add(text);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                item.add(buttonGroup);
                initButtons(buttonGroup, item, readOnlyModel);
            }
        };
        repeater.setOutputMarkupId(true);
        repeater.setOutputMarkupPlaceholderTag(true);
        repeater.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return !getModel().getObject().isEmpty();
            }
        });
        add(repeater);
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item, NonEmptyModel<Boolean> readOnlyModel) {
        AjaxLink plus = new AjaxLink(ID_PLUS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        plus.add(new AttributeAppender("class", getPlusClassModifier(item)));
		plus.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        buttonGroup.add(plus);

        AjaxLink minus = new AjaxLink(ID_MINUS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target, item);
            }
        };
        minus.add(new AttributeAppender("class", getMinusClassModifier()));
		minus.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
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
        if(buttonsDisabled()){
            return CSS_DISABLED;
        }

        return "";
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
        modelObjectUpdatePerformed(target, objects);

        target.add(this);
    }

    protected T createNewEmptyItem(){
        return (T)"";
    }

    protected StringResourceModel createEmptyItemPlaceholder(){
        return createStringResource("TextField.universal.placeholder");
    }

    /**
     *  Override to provide the information about buttons enabled/disabled status
     * */
    protected boolean buttonsDisabled(){
        return false;
    }

    protected void modelObjectUpdatePerformed(AjaxRequestTarget target, List<T> modelObject){
    }

    protected void removeValuePerformed(AjaxRequestTarget target, ListItem<T> item){
        List<T> objects = getModelObject();
        Iterator<T> iterator = objects.iterator();
        while (iterator.hasNext()) {
            T object = iterator.next();
            if (object == null){
                continue;
            }
            if (object.equals(item.getModelObject())) {
                iterator.remove();
                break;
            }
        }
        modelObjectUpdatePerformed(target, objects);
        target.add(this);
    }
}
