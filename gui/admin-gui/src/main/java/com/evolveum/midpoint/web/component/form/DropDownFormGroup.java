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

package com.evolveum.midpoint.web.component.form;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author lazyman
 */
public class DropDownFormGroup<T> extends BasePanel<T> {

    private static final String ID_SELECT = "select";
    private static final String ID_SELECT_WRAPPER = "selectWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";
    private static final String ID_REQUIRED = "required";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_ADDITIONAL_INFO = "additionalInfo";

    public DropDownFormGroup(String id, IModel<T> value, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
                             IModel<String> label, String labelSize, String textSize, boolean required) {
        this(id, value, choices, renderer, label, null, false, labelSize, textSize, required);
    }

    public DropDownFormGroup(String id, IModel<T> value, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
                             IModel<String> label, String tooltipKey, boolean isTooltipInModal,  String labelSize, String textSize, boolean required) {
        super(id, value);

        initLayout(choices, renderer, label, tooltipKey, isTooltipInModal, labelSize, textSize, required);
    }

    private void initLayout(IModel<List<T>> choices, IChoiceRenderer<T> renderer, IModel<String> label, final String tooltipKey,
                            boolean isTooltipInModal, String labelSize, String textSize, final boolean required) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        add(labelContainer);

        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelSize)) {
            labelContainer.add(AttributeAppender.prepend("class", labelSize));
        }
        labelContainer.add(l);

		Label tooltipLabel = new Label(ID_TOOLTIP, new Model<>());
		tooltipLabel.add(new AttributeAppender("data-original-title", new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				return getString(tooltipKey);
			}
		}));
		tooltipLabel.add(new InfoTooltipBehavior(isTooltipInModal));
		tooltipLabel.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return tooltipKey != null;
			}
		});
		tooltipLabel.setOutputMarkupId(true);
		tooltipLabel.setOutputMarkupPlaceholderTag(true);
		labelContainer.add(tooltipLabel);

		WebMarkupContainer requiredContainer = new WebMarkupContainer(ID_REQUIRED);
		requiredContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return required;
			}
		});
		labelContainer.add(requiredContainer);

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

		Component additionalInfo = createAdditionalInfoComponent(ID_ADDITIONAL_INFO);
		if (additionalInfo == null) {
			additionalInfo = new Label(ID_ADDITIONAL_INFO, "");
		}
		selectWrapper.add(additionalInfo);
    }

	protected Component createAdditionalInfoComponent(String id) {
		return null;
	}

	public Component getAdditionalInfoComponent() {
		return get(createComponentPath(ID_SELECT_WRAPPER, ID_ADDITIONAL_INFO));
	}

	protected DropDownChoice<T> createDropDown(String id, IModel<List<T>> choices, IChoiceRenderer<T> renderer,
                                            boolean required) {
        DropDownChoice choice = new DropDownChoice<T>(id, getModel(), choices, renderer){

            @Override
            protected String getNullValidDisplayValue() {
                return getString("DropDownChoicePanel.empty");
            }
        };
        choice.setNullValid(!required);
        choice.setRequired(required);
        return choice;
    }

    public DropDownChoice<T> getInput() {
        return (DropDownChoice<T>) get(createComponentPath(ID_SELECT_WRAPPER, ID_SELECT));
    }
}
