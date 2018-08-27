/*
 * Copyright (c) 2018 Evolveum
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

package com.evolveum.midpoint.gui.impl.component.form;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 * @author skublik
 */
public class TriStateFormGroup extends BasePanel<Boolean> {

	private static final long serialVersionUID = 1L;
	
	private static final String ID_VALUE = "value";
    private static final String ID_VALUE_WRAPPER = "valueWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";
    private static final String ID_REQUIRED = "required";
    private static final String ID_FEEDBACK = "feedback";

    public TriStateFormGroup(String id, IModel<Boolean> value, IModel<String> label, String labelCssClass, String textCssClass, boolean required) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required);
    }

    public TriStateFormGroup(String id, IModel<Boolean> value, IModel<String> label, String tooltipKey,
                          boolean isTooltipInModal, String labelCssClass, String textCssClass, boolean required) {
        super(id, value);

        initLayout(label, tooltipKey, isTooltipInModal, labelCssClass, textCssClass, required);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, boolean isTooltipInModal, String labelCssClass, String textCssClass, boolean required) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        add(labelContainer);
        Label l = new Label(ID_LABEL, label);

        if (StringUtils.isNotEmpty(labelCssClass)) {
            labelContainer.add(AttributeAppender.prepend("class", labelCssClass));
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
        tooltipLabel.add(new VisibleEnableBehaviour(){

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

        WebMarkupContainer valueWrapper = new WebMarkupContainer(ID_VALUE_WRAPPER);
        if (StringUtils.isNotEmpty(textCssClass)) {
            valueWrapper.add(AttributeAppender.prepend("class", textCssClass));
        }
        add(valueWrapper);
        
        TriStateComboPanel triStateCombo = new TriStateComboPanel(ID_VALUE, getModel());;
        valueWrapper.add(triStateCombo);
        
        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ComponentFeedbackMessageFilter(triStateCombo.getBaseFormComponent()));
        feedback.setOutputMarkupId(true);
        valueWrapper.add(feedback);
    }

    public CheckBox getValue(){
        return (CheckBox) get(ID_VALUE_WRAPPER + ":" + ID_VALUE);
    }
}
