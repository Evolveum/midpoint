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

import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lazyman
 */
public class DropDownFormGroup<T> extends SimplePanel<T> {

    private static final String ID_SELECT = "select";
    private static final String ID_SELECT_WRAPPER = "selectWrapper";
    private static final String ID_LABEL = "label";
    private static final String ID_FEEDBACK = "feedback";

    public DropDownFormGroup(String id, IModel<T> value, IModel<List<T>> choices, IChoiceRenderer renderer,
                             IModel<String> label, String labelSize, String textSize, boolean required) {
        super(id, value);

        initLayout(choices, renderer, label, labelSize, textSize, required);
    }

    private void initLayout(IModel<List<T>> choices, IChoiceRenderer renderer, IModel<String> label,
                            String labelSize, String textSize, boolean required) {
        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelSize)) {
            l.add(AttributeAppender.prepend("class", labelSize));
        }
        add(l);

        WebMarkupContainer selectWrapper = new WebMarkupContainer(ID_SELECT_WRAPPER);
        if (StringUtils.isNotEmpty(textSize)) {
            selectWrapper.add(AttributeAppender.prepend("class", textSize));
        }
        add(selectWrapper);

        DropDownChoice select = createDropDown(ID_SELECT, choices, renderer, required);
        select.setLabel(label);
        selectWrapper.add(select);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(select));
        feedback.setOutputMarkupId(true);
        selectWrapper.add(feedback);
    }

    protected DropDownChoice createDropDown(String id, IModel<List<T>> choices, IChoiceRenderer renderer,
                                            boolean required) {
        DropDownChoice choice =  new DropDownChoice(id, getModel(), choices, renderer){

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return getString("DropDownChoicePanel.notDefined");
            }

            @Override
            protected String getNullValidDisplayValue() {
                return getString("DropDownChoicePanel.notDefined");
            }
        };
        choice.setNullValid(!required);
        choice.setRequired(required);
        return choice;
    }

    public DropDownChoice getInput() {
        return (DropDownChoice) get(createComponentPath(ID_SELECT_WRAPPER, ID_SELECT));
    }
}
