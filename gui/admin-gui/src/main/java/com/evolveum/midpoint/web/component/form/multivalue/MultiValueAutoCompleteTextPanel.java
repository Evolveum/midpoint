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

package com.evolveum.midpoint.web.component.form.multivalue;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.Strings;
import org.apache.wicket.validation.IValidator;

import java.io.Serializable;
import java.util.*;

/**
 *  @author shood
 * */
public class MultiValueAutoCompleteTextPanel<T extends Serializable> extends SimplePanel<List<T>>{

    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT = "input";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "delete";

    private static final String CSS_DISABLED = " disabled";
    private static final Integer AUTO_COMPLETE_LIST_SIZE = 10;

    public MultiValueAutoCompleteTextPanel(String id, IModel<List<T>> model, boolean inputEnabled, boolean prepareModel){
        super(id, model);

        setOutputMarkupId(true);

        initLayout(inputEnabled, prepareModel);
    }

    private IModel<List<T>> prepareModel(boolean prepareModel){
        if(prepareModel){
            if(getModel().getObject() == null){
                getModel().setObject(new ArrayList<>(Arrays.asList(createNewEmptyItem())));
            } else if(getModel().getObject().isEmpty()){
                getModel().getObject().add(createNewEmptyItem());
            }
        }
        return getModel();
    }

    protected T createNewEmptyItem(){
        return null;
    }

    private void initLayout(final boolean inputEnabled, boolean prepareModel){

        ListView repeater = new ListView<T>(ID_REPEATER, prepareModel(prepareModel)){

            @Override
            protected void populateItem(final ListItem<T> item) {

                AutoCompleteSettings autoCompleteSettings = new AutoCompleteSettings();
                autoCompleteSettings.setShowListOnEmptyInput(true);
                AutoCompleteTextField<String> autoCompleteEditor = new AutoCompleteTextField<String>(ID_TEXT,
                        createTextModel(item.getModel()), autoCompleteSettings) {

                    @Override
                    protected Iterator<String> getChoices(String input) {
                        return createAutocompleteObjectList(input);
                    }
                };
                autoCompleteEditor.add(createAutoCompleteValidator());
                item.add(autoCompleteEditor);

                autoCompleteEditor.add(AttributeAppender.replace("placeholder", createEmptyItemPlaceholder()));

                if(!inputEnabled){
                    autoCompleteEditor.add(new AttributeModifier("disabled","disabled"));
                }
                item.add(autoCompleteEditor);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                item.add(buttonGroup);
                initButtons(buttonGroup, item);
            }
        };
        add(repeater);
    }

    private Iterator<String> createAutocompleteObjectList(String input) {
        List<T> list = createObjectList();
        List<String> choices = new ArrayList<>(AUTO_COMPLETE_LIST_SIZE);

        if(Strings.isEmpty(input)){
            for(T object: list){
                choices.add(createAutoCompleteObjectLabel(object));

                if(choices.size() == AUTO_COMPLETE_LIST_SIZE){
                    break;
                }
            }

            return choices.iterator();
        }

        for(T object: list){
            if(createAutoCompleteObjectLabel(object).toLowerCase().startsWith(input.toLowerCase())){
                choices.add(createAutoCompleteObjectLabel(object));

                if(choices.size() == AUTO_COMPLETE_LIST_SIZE){
                    break;
                }
            }
        }

        return choices.iterator();
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item) {
        AjaxLink add = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        add.add(new AttributeAppender("class", getPlusClassModifier(item)));
        buttonGroup.add(add);

        AjaxLink remove = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target, item);
            }
        };
        remove.add(new AttributeAppender("class", getMinusClassModifier()));
        buttonGroup.add(remove);
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
        int size = getModelObject().size();
        if (size > 1) {
            return "";
        }

        return CSS_DISABLED;
    }

    /**
     *  Creates a StringResourceModel containing a placeholder for editor textField when no value is present
     * */
    protected StringResourceModel createEmptyItemPlaceholder(){
        return createStringResource("TextField.universal.placeholder");
    }

    protected void addValuePerformed(AjaxRequestTarget target){
        List<T> objects = getModelObject();
        objects.add(createNewEmptyItem());

        target.add(this);
    }

    /**
     *  Creates IModel<String> - a value for label in main text field of editor
     * */
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
     *  Provides an IValidator<String> for auto-complete edit field
     * */
    protected IValidator<String> createAutoCompleteValidator(){
        return null;
    }

    /**
     *  Create a List<T> of objects that are shown in autoComplete drop-down list
     * */
    protected List<T> createObjectList(){
        return new ArrayList<>();
    }

    /**
     *  Creates label for item in autoComplete drop-down list. CAREFUL, this
     *  method is also used to create String that is used as compare value with
     *  users input to generate values for auto-complete drop-down
     * */
    protected String createAutoCompleteObjectLabel(T object){
        return object.toString();
    }
}
