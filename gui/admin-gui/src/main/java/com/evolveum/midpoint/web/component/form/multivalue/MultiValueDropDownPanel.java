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
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.*;

/**
 *  @author shood
 * */
public class MultiValueDropDownPanel<T extends Serializable> extends BasePanel<List<T>>{

    private static final String ID_PLACEHOLDER_CONTAINER = "placeholderContainer";
    private static final String ID_PLACEHOLDER_ADD = "placeholderAdd";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_INPUT = "input";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "delete";

    private static final String CSS_DISABLED = " disabled";

    public MultiValueDropDownPanel(String id, IModel<List<T>> model, boolean nullValid, NonEmptyModel<Boolean> readOnlyModel) {
        super(id, model);
        setOutputMarkupId(true);

        initLayout(nullValid, readOnlyModel);
    }

    private void initLayout(final boolean nullValid, final NonEmptyModel<Boolean> readOnlyModel) {
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

        AjaxLink<Void> placeholderAdd = new AjaxLink<Void>(ID_PLACEHOLDER_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
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
        placeholderAdd.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        placeholderContainer.add(placeholderAdd);

        ListView repeater = new ListView<T>(ID_REPEATER, getModel()){

            @Override
            protected void populateItem(final ListItem<T> item) {

                DropDownChoice choice = new DropDownChoice<>(ID_INPUT, createDropDownItemModel(item.getModel()),
                        createChoiceList(), createRenderer());
                choice.setNullValid(nullValid);
                choice.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
                item.add(choice);

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

    protected IModel<T> createDropDownItemModel(final IModel<T> model) {
        return new IModel<T>() {
            @Override
            public T getObject() {
                T obj = model.getObject();
                return obj != null ? obj : null;
            }

            @Override
            public void setObject(T object) {
                model.setObject(object);
            }

            @Override
            public void detach() {
            }
        };
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item, NonEmptyModel<Boolean> readOnlyModel) {
        AjaxLink<Void> add = new AjaxLink<Void>(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        add.add(new AttributeAppender("class", getPlusClassModifier(item)));
        add.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        buttonGroup.add(add);

        AjaxLink<Void> remove = new AjaxLink<Void>(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target, item);
            }
        };
        remove.add(new AttributeAppender("class", getMinusClassModifier()));
        remove.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        buttonGroup.add(remove);
    }

    protected T createNewEmptyItem(){
        return null;
    }

    protected String getPlusClassModifier(ListItem<T> item){
        if(buttonsDisabled()){
            return CSS_DISABLED;
        }

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

    protected void addValuePerformed(AjaxRequestTarget target){
        List<T> objects = getModelObject();
        objects.add(createNewEmptyItem());

        target.add(this);
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

    /**
     *  Provide a function to determine if buttons of editor are disabled/enabled
     * */
    protected boolean buttonsDisabled(){
        return false;
    }

    /**
     *  Provides list of choices for drop-down component
     * */
    protected IModel<List<T>> createChoiceList(){
        return new IModel<List<T>>() {

            @Override
            public List<T> getObject() {
                return new ArrayList<>();
            }
        };
    }

    /**
     *  Provides an instance of IChoiceRenderer needed to render choices in drop-down component
     * */
    protected IChoiceRenderer<T> createRenderer(){
        return new IChoiceRenderer<T>() {

            @Override
            public T getObject(String id, IModel<? extends List<? extends T>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }

            @Override
            public Object getDisplayValue(T object) {
                return object.toString();
            }

            @Override
            public String getIdValue(T object, int index) {
                return Integer.toString(index);
            }
        };
    }
}
