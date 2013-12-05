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

package com.evolveum.midpoint.web.component.form;

import com.evolveum.midpoint.web.component.util.PrismPropertyList;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

import java.util.List;

/**
 * todo not finished [lazyman]
 *
 * @author lazyman
 */
public class MultiValueTextFormGroup extends SimplePanel<List<String>> {

    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_LABEL = "label";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_ADD = "add";
    private static final String ID_REMOVE = "remove";

    public MultiValueTextFormGroup(String id, IModel<List<String>> value, IModel<String> label, String labelSize,
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

        ListView repeater = new ListView<String>(ID_REPEATER, getModel()) {

            @Override
            protected void populateItem(ListItem<String> item) {
                WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
                if (StringUtils.isNotEmpty(textSize)) {
                    textWrapper.add(AttributeAppender.prepend("class", textSize));
                }
                item.add(textWrapper);

                TextField text = new TextField(ID_TEXT, item.getModel());
                text.setRequired(required);
                text.add(AttributeAppender.replace("placeholder", label));
                text.setLabel(label);
                textWrapper.add(text);

                FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(text));
                textWrapper.add(feedback);

                initButtons(item);
            }
        };
        add(repeater);
    }

    private void initButtons(final ListItem<String> item) {
        AjaxLink add = new AjaxLink(ID_ADD) {

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
        item.add(add);

        AjaxLink remove = new AjaxLink(ID_REMOVE) {

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
        item.add(remove);
    }

    private boolean isAddButtonVisible(ListItem<String> item) {
        return true;
    }

    private boolean isRemoveButtonVisible(ListItem<String> item) {
        List object = getModelObject();
        if (!(object instanceof PrismPropertyList)) {
            return true;
        }

//        PrismPropertyList list =
        return true;
    }

    private void addValuePerformed(AjaxRequestTarget target, ListItem<String> item) {
        getModel().getObject().add("");

        target.add(this);
    }

    private void removeValuePerformed(AjaxRequestTarget target, ListItem<String> item) {
        target.add(this);
    }
}
