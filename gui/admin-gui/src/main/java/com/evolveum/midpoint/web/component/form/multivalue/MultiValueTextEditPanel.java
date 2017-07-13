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
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 *  @author shood
 * */
public class MultiValueTextEditPanel<T extends Serializable> extends BasePanel<List<T>> {

    private static final String ID_PLACEHOLDER_CONTAINER = "placeholderContainer";
    private static final String ID_PLACEHOLDER_ADD = "placeholderAdd";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT = "input";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "delete";
    private static final String ID_EDIT = "edit";

    private static final String CSS_DISABLED = " disabled";

	@Nullable private final IModel<T> selectedModel;			// holding the selected item

    public MultiValueTextEditPanel(String id, IModel<List<T>> model, IModel<T> selectedModel, boolean inputEnabled, boolean showPlaceholder,
			NonEmptyModel<Boolean> readOnlyModel) {
        super(id, model);
        setOutputMarkupId(true);
		this.selectedModel = selectedModel;

        initLayout(inputEnabled, showPlaceholder, readOnlyModel);
    }

    private void initLayout(final boolean inputEnabled, final boolean showPlaceholder, final NonEmptyModel<Boolean> readOnlyModel) {
        WebMarkupContainer placeholderContainer = new WebMarkupContainer(ID_PLACEHOLDER_CONTAINER);
        placeholderContainer.setOutputMarkupPlaceholderTag(true);
        placeholderContainer.setOutputMarkupPlaceholderTag(true);
        placeholderContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return showPlaceholder && (getModel().getObject() == null || getModel().getObject().isEmpty());
            }
        });
        add(placeholderContainer);

        AjaxLink placeholderAdd = new AjaxLink(ID_PLACEHOLDER_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        placeholderAdd.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if (buttonsDisabled()) {
                    return " " + CSS_DISABLED;
                }

                return "";
            }
        }));
		placeholderAdd.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        placeholderAdd.setOutputMarkupId(true);
        placeholderAdd.setOutputMarkupPlaceholderTag(true);
        placeholderContainer.add(placeholderAdd);

        ListView repeater = new ListView<T>(ID_REPEATER, getModel()) {

            @Override
            protected void populateItem(final ListItem<T> item) {
                TextField text = new TextField<>(ID_TEXT, createTextModel(item.getModel()));
                text.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                text.add(AttributeAppender.replace("placeholder", createEmptyItemPlaceholder()));
				if (selectedModel != null && item.getModelObject() == selectedModel.getObject()) {
					text.add(AttributeAppender.append("style", "background-color: #FFFFD0;"));			// TODO color constant
				}

				if (!inputEnabled) {
					text.add(new AttributeModifier("disabled", "disabled"));
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
                return getModel().getObject() != null && !getModel().getObject().isEmpty();
            }
        });
        add(repeater);
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item, NonEmptyModel<Boolean> readOnlyModel) {
        AjaxSubmitLink edit = new AjaxSubmitLink(ID_EDIT) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                editPerformed(target, item.getModelObject());
            }

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getPageBase().getFeedbackPanel());
			}
		};
        edit.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if (buttonsDisabled()) {
                    return " " + CSS_DISABLED;
                }

                return "";
            }
        }));
        buttonGroup.add(edit);

        AjaxLink add = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        add.add(new AttributeAppender("class", getPlusClassModifier(item)));
		add.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        buttonGroup.add(add);

        AjaxLink remove = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target, item);
            }
        };
        remove.add(new AttributeAppender("class", getMinusClassModifier()));
		remove.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
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
        if(buttonsDisabled()){
            return CSS_DISABLED;
        }

        return "";
    }

    protected T createNewEmptyItem(){
        return (T)"";
    }

    protected StringResourceModel createEmptyItemPlaceholder(){
        return createStringResource("TextField.universal.placeholder");
    }

    protected void addValuePerformed(AjaxRequestTarget target){
        List<T> objects = getModelObject();
		T added = createNewEmptyItem();
		objects.add(added);
		performAddValueHook(target, added);
		editPerformed(target, added);
		target.add(this);
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

        performRemoveValueHook(target, item);
        target.add(this);
    }

    /**
     *  Override to provide handling of edit event (edit button clicked)
     * */
    protected void editPerformed(AjaxRequestTarget target, T object){}

    /**
     *  Override to provide the information about buttons enabled/disabled status
     * */
    protected boolean buttonsDisabled(){
        return false;
    }

    /**
     *  Override to provide custom hook when adding new value
     * */
    protected void performAddValueHook(AjaxRequestTarget target, T added){}

    /**
     *  Override to provide custom hook when removing value from list
     * */
    protected void performRemoveValueHook(AjaxRequestTarget target, ListItem<T> item){}
}
