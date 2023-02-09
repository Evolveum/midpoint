/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form.multivalue;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.AbstractTextComponent;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

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
            super.getModel().setObject(new ArrayList<>());
        }

        return super.getModel();
    }

    private void initLayout(final NonEmptyModel<Boolean> readOnlyModel, final boolean emptyStringToNull) {
        WebMarkupContainer placeholderContainer = new WebMarkupContainer(ID_PLACEHOLDER_CONTAINER);
        placeholderContainer.setOutputMarkupPlaceholderTag(true);
        placeholderContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return getModel().getObject().isEmpty();
            }
        });
        add(placeholderContainer);

        AjaxLink<Void> placeholderAdd = new AjaxLink<Void>(ID_PLACEHOLDER_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        placeholderAdd.add(getAddButtonVisibleBehavior(readOnlyModel));
        placeholderAdd.add(new AttributeAppender("class", new IModel<String>() {

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
                InputPanel text = createTextPanel(ID_TEXT, createTextModel(item.getModel()));
                FormComponent formComponent = text.getBaseFormComponent();
                formComponent.add(new AjaxFormComponentUpdatingBehavior("blur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        T updatedValue = (T)text.getBaseFormComponent().getConvertedInput();
                        List<T> modelObject = MultiValueTextPanel.this.getModelObject();
                        modelObject.set(item.getIndex(), updatedValue);
                        modelObjectUpdatePerformed(target, modelObject);
                    }
                });
                formComponent.add(AttributeAppender.replace("placeholder", createEmptyItemPlaceholder()));
                formComponent.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
                if (formComponent instanceof AbstractTextComponent) {
                    ((AbstractTextComponent)formComponent).setConvertEmptyInputStringToNull(emptyStringToNull);
                }
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

    protected InputPanel createTextPanel(String id, IModel<String> model) {
        return new TextPanel<>(id, model);
    }

    protected Behavior getAddButtonVisibleBehavior(NonEmptyModel<Boolean> readOnlyModel) {
        return WebComponentUtil.visibleIfFalse(readOnlyModel);
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item, NonEmptyModel<Boolean> readOnlyModel) {
        AjaxLink<Void> plus = new AjaxLink<Void>(ID_PLUS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        plus.add(new AttributeAppender("class", getPlusClassModifier(item)));
        plus.add(getAddButtonVisibleBehavior(readOnlyModel));
        buttonGroup.add(plus);

        AjaxLink<Void> minus = new AjaxLink<Void>(ID_MINUS) {

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
        return new IModel<>() {
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

    protected IModel<String> createEmptyItemPlaceholder(){
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
