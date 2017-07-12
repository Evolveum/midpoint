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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 *  This is a generic component that server to edit various types
 *  of Serializable objects in GUI. It is aimed for multi-value
 *  objects and it requires to add custom modal window as an
 *  editor for object.
 *
 *  @author shood
 * */
public class GenericMultiValueLabelEditPanel <T extends Serializable> extends BasePanel<List<T>> {

    private static final Trace LOGGER = TraceManager.getTrace(GenericMultiValueLabelEditPanel.class);

    private static final String ID_LABEL = "label";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_TEXT = "text";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_ADD_FIRST_CONTAINER = "addFirstContainer";
    private static final String ID_ADD_FIRST = "addFirst";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "remove";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_EDIT = "edit";

//    protected static final String ID_MODAL_EDITOR = "modalEditor";

    private static final String CLASS_MULTI_VALUE = "multivalue-form";

    private boolean isMultiple;

    public GenericMultiValueLabelEditPanel(String id, IModel<List<T>> value, IModel<String> label,
                                           String labelSize, String textSize, boolean isMultiple){
        super(id, value);
        this.isMultiple = isMultiple;
        setOutputMarkupId(true);

        initLayout(label, labelSize, textSize);
    }

    private void initLayout(final IModel<String> label, final String labelSize, final String textSize){

        Label l = new Label(ID_LABEL, label);
        l.setVisible(getLabelVisibility());
        if(StringUtils.isNotEmpty(labelSize)){
            l.add(AttributeAppender.prepend("class", labelSize));
        }
        add(l);

        WebMarkupContainer addFirstContainer = new WebMarkupContainer(ID_ADD_FIRST_CONTAINER);
        addFirstContainer.setOutputMarkupId(true);
        addFirstContainer.setOutputMarkupPlaceholderTag(true);
        addFirstContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return getModelObject().isEmpty();
            }
        });
        add(addFirstContainer);

        AjaxLink addFirst = new AjaxLink(ID_ADD_FIRST) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addFirstPerformed(target);
            }
        };
        addFirstContainer.add(addFirst);

        ListView repeater = new ListView<T>(ID_REPEATER, getModel()) {

            @Override
            protected void populateItem(final ListItem<T> listItem) {
                WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
                textWrapper.add(AttributeAppender.prepend("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();
                        if(StringUtils.isNotEmpty(textSize)){
                            sb.append(textSize).append(' ');
                        }
                        if(listItem.getIndex() > 0 && StringUtils.isNotEmpty(getOffsetClass())){
                            sb.append(getOffsetClass()).append(' ');
                            sb.append(CLASS_MULTI_VALUE);
                        }
                        return sb.toString();
                    }
                }));
                listItem.add(textWrapper);

                TextField text = new TextField<>(ID_TEXT, createTextModel(listItem.getModel()));
                text.add(new AjaxFormComponentUpdatingBehavior("blur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {}
                });
                text.setEnabled(false);
                text.add(AttributeAppender.replace("placeholder", label));
                text.setLabel(label);
                textWrapper.add(text);

                FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(text));
                textWrapper.add(feedback);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                buttonGroup.add(AttributeAppender.append("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if(listItem.getIndex() > 0 && StringUtils.isNotEmpty(labelSize)){
                            return CLASS_MULTI_VALUE;
                        }

                        return null;
                    }
                }));

                AjaxLink edit = new AjaxLink(ID_EDIT) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editValuePerformed(target, listItem.getModel());
                    }
                };
                textWrapper.add(edit);

                listItem.add(buttonGroup);

                initButtons(buttonGroup, listItem);
            }
        };

        add(repeater);
    }

    protected void showDialog(Popupable dialogContent, AjaxRequestTarget target){
        getPageBase().showMainPopup(dialogContent, target);
    }


    /**
     * @return css class for off-setting other values (not first, left to the first there is a label)
     */
    protected String getOffsetClass() {
        return "col-md-offset-4";
    }

    protected IModel<String> createTextModel(final IModel<T> model) {
        return new IModel<String>() {
            @Override
            public String getObject() {
                T obj = model.getObject();
                return obj != null ? obj.toString() : null;
            }

            @Override
            public void setObject(String object) {}

            @Override
            public void detach() {
            }
        };
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item) {
        AjaxLink add = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target);
            }
        };
        add.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddButtonVisible(item);
            }
        });
        buttonGroup.add(add);

        AjaxLink remove = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target, item);
            }
        };
        remove.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible();
            }
        });
        buttonGroup.add(remove);
    }

    protected boolean isAddButtonVisible(ListItem<T> item) {
        if (isMultiple) {
            int size = getModelObject().size();
            if (size <= 1) {
                return true;
            }
            if (item.getIndex() == size - 1) {
                return true;
            }
        }
        return false;
    }

    /**
     *  Override to provide call-back to edit button click event
     * */
    protected void editValuePerformed(AjaxRequestTarget target, IModel<T> rowModel){}

    protected boolean isRemoveButtonVisible() {
        int size = getModelObject().size();
        if (size > 0) {
            return true;
        }

        return false;
    }

    protected void addValuePerformed(AjaxRequestTarget target) {
        List<T> objects = getModelObject();
        objects.add(createNewEmptyItem());

        target.add(this);
    }

    /**
     *  Override to provide a special handling for addition of first
     *  value to attribute.
     * */
    protected void addFirstPerformed(AjaxRequestTarget target){
        List<T> objects = getModelObject();
        objects.add(createNewEmptyItem());

        target.add(this);
    }

    /**
     *  Override to provide creation of a new empty item
     * */
    protected T createNewEmptyItem() {
        return null;
    }

    protected void removeValuePerformed(AjaxRequestTarget target, ListItem<T> item) {
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

    public void closeModalWindow(AjaxRequestTarget target){
        getPageBase().hideMainPopup(target);
    }

    protected boolean getLabelVisibility(){
        return true;
    }

    protected boolean getAddButtonVisibility(){
        return true;
    }

}
