/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form.multivalue;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * todo not finished [lazyman]
 *
 * @author lazyman
 */
public class MultiValueTextFormGroup<T extends Serializable> extends BasePanel<List<T>> {

    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_LABEL = "label";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "remove";
    private static final String ID_BUTTON_GROUP = "buttonGroup";

    private static final String CLASS_MULTI_VALUE = "multivalue-form";

    public MultiValueTextFormGroup(String id, IModel<List<T>> value, IModel<String> label, String labelSize,
            String textSize, boolean required) {
        super(id, value);
        setOutputMarkupId(true);

        initLayout(label, labelSize, textSize, required);
    }

    private void initLayout(final IModel<String> label, final String labelSize, final String textSize,
            final boolean required) {
        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelSize)) {
            l.add(AttributeAppender.prepend("class", labelSize));
        }
        add(l);

        ListView repeater = new ListView<T>(ID_REPEATER, getModel()) {

            @Override
            protected void populateItem(final ListItem<T> item) {
                WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
                textWrapper.add(AttributeAppender.prepend("class", new IModel<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();
                        if (StringUtils.isNotEmpty(textSize)) {
                            sb.append(textSize).append(' ');
                        }
                        if (item.getIndex() > 0 && StringUtils.isNotEmpty(getOffsetClass())) {
                            sb.append(getOffsetClass()).append(' ');
                            sb.append(CLASS_MULTI_VALUE);
                        }

                        return sb.toString();
                    }
                }));
                item.add(textWrapper);

                TextField<?> text = new TextField<>(ID_TEXT, createTextModel(item.getModel()));
                text.add(new AjaxFormComponentUpdatingBehavior("blur") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                    }
                });
                text.setRequired(required);
                text.add(AttributeAppender.replace("placeholder", label));
                text.setLabel(label);
                textWrapper.add(text);

                FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(text));
                textWrapper.add(feedback);

                WebMarkupContainer buttonGroup = new WebMarkupContainer(ID_BUTTON_GROUP);
                buttonGroup.add(AttributeAppender.append("class", new IModel<String>() {

                    @Override
                    public String getObject() {
                        if (item.getIndex() > 0 && StringUtils.isNotEmpty(labelSize)) {
                            return CLASS_MULTI_VALUE;
                        }

                        return null;
                    }
                }));
                item.add(buttonGroup);

                initButtons(buttonGroup, item);
            }
        };
        add(repeater);
    }

    /**
     * @return css class for offseting other values (not first, left to the first there is a label)
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
            public void setObject(String object) {
                model.setObject((T) object);
            }

            @Override
            public void detach() {
            }
        };
    }

    private void initButtons(WebMarkupContainer buttonGroup, final ListItem<T> item) {
        AjaxLink<Void> add = new AjaxLink<Void>(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addValuePerformed(target, item);
            }
        };
        add.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isAddButtonVisible(item);
            }
        });
        buttonGroup.add(add);

        AjaxLink<Void> remove = new AjaxLink<Void>(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeValuePerformed(target, item);
            }
        };
        remove.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isRemoveButtonVisible(item);
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

    protected boolean isRemoveButtonVisible(ListItem<T> item) {
        int size = getModelObject().size();
        if (size > 1) {
            return true;
        }

        return false;
    }

    protected void addValuePerformed(AjaxRequestTarget target, ListItem<T> item) {
        List<T> objects = getModelObject();
        objects.add(createNewEmptyItem());

        target.add(this);
    }

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
}
