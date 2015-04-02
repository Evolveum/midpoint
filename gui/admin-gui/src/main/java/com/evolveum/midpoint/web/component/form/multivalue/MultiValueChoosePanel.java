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

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypeDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.Iterator;
import java.util.List;

/**
 *  TODO - not finished, work in progress
 *  @author shood
 * */
public class MultiValueChoosePanel <T extends ObjectType> extends SimplePanel<List<T>>{

    private static final Trace LOGGER = TraceManager.getTrace(MultiValueChoosePanel.class);

    private static final String ID_LABEL = "label";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_TEXT = "text";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "remove";
    private static final String ID_BUTTON_GROUP = "buttonGroup";
    private static final String ID_EDIT = "edit";

    protected static final String MODAL_ID_CHOOSE_PANEL = "showPopup";

    private static final String CLASS_MULTI_VALUE = "multivalue-form";

    public MultiValueChoosePanel(String id, IModel<List<T>> value, IModel<String> label, String labelSize,
                                 String textSize, boolean required, Class<T> type){
        super(id, value);
        setOutputMarkupId(true);

        initLayout(label, labelSize, textSize, required, type);
    }

    private void initLayout(final IModel<String> label, final String labelSize, final String textSize,
                            final boolean required, Class<T> type){

        Label l = new Label(ID_LABEL, label);

        if(StringUtils.isNotEmpty(labelSize)){
            l.add(AttributeAppender.prepend("class", labelSize));
        }
        add(l);

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
                text.add(new AjaxFormComponentUpdatingBehavior("onblur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {}
                });
                text.setRequired(required);
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
                        editValuePerformed(target);
                    }
                };
                textWrapper.add(edit);

                listItem.add(buttonGroup);

                initButtons(buttonGroup, listItem);
            }
        };

        initDialog(type);
        add(repeater);
    }

    protected void initDialog(Class<T> type){
        ModalWindow dialog = new ChooseTypeDialog(MODAL_ID_CHOOSE_PANEL, type){

            @Override
            protected void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object){
                choosePerformed(target, (T)object);
            }

            @Override
            protected ObjectQuery getDataProviderQuery(){
                return createChooseQuery();
            }
        };
        add(dialog);
    }

    protected ObjectQuery createChooseQuery(){
        return null;
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
        int size = getModelObject().size();
        if (size <= 1) {
            return true;
        }
        if (item.getIndex() == size - 1) {
            return true;
        }

        return false;
    }

    protected void editValuePerformed(AjaxRequestTarget target){
        ModalWindow window = (ModalWindow) get(MODAL_ID_CHOOSE_PANEL);
        ChooseTypeDialog dialog = (ChooseTypeDialog)window;
        dialog.updateTablePerformed(target, createChooseQuery());
        window.show(target);
    }

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

    protected T createNewEmptyItem() {
        return null;
    }

    /*
     * TODO - this method contains check, if chosen object already is not in selected values array
     *  This is a temporary solution until we well be able to create "already-chosen" query
     *
     */
    protected void choosePerformed(AjaxRequestTarget target, T object){
        choosePerformedHook(target, object);
        ModalWindow window = (ModalWindow)get(MODAL_ID_CHOOSE_PANEL);
        window.close(target);

        if(isObjectUnique(object)){
            replaceIfEmpty(object);
        }

        if(LOGGER.isTraceEnabled()){
            LOGGER.trace("New object instance has been added to the model.");
        }

        target.add(this);
    }

    protected void replaceIfEmpty(Object object){
        List<T> objects = getModelObject();
        objects.add((T)object);
    }

    protected boolean isObjectUnique(Object object){

        for(T o: getModelObject()){
            if(o.equals(object)){
                return false;
            }
        }
        return true;
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

        if(objects.size() == 0){
            objects.add(createNewEmptyItem());
        }

        target.add(this);
    }

    /**
     *  A custom code in form of hook that can be run on event of
     *  choosing new object with this chooser component
     * */
    protected void choosePerformedHook(AjaxRequestTarget target, T object){}
}
