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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class TextFormGroup extends BasePanel<String> {

    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";
	private static final String ID_REQUIRED = "required";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_PROPERTY_LABEL = "propertyLabel";
    private static final String ID_ROW = "row";

    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String labelCssClass, String textCssClass,
            boolean required, boolean isSimilarAsPropertyPanel) {
    	this(id, value, label, null, false, labelCssClass, textCssClass, required, required, isSimilarAsPropertyPanel);
    }
    
    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String labelCssClass, String textCssClass,
                         boolean required) {
        this(id, value, label, null, false, labelCssClass, textCssClass, required, required, false);
    }
    
    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String tooltipKey, boolean isTooltipInModel, String labelCssClass,
            String textCssClass, boolean required, boolean markAsRequired) {
    	this(id, value, label, null, false, labelCssClass, textCssClass, required, markAsRequired, false);
    }

    public TextFormGroup(String id, IModel<String> value, IModel<String> label, String tooltipKey, boolean isTooltipInModel, String labelCssClass,
                         String textCssClass, boolean required, boolean markAsRequired, boolean isSimilarAsPropertyPanel) {
        super(id, value);

        initLayout(label, tooltipKey, isTooltipInModel, labelCssClass, textCssClass, required, markAsRequired, isSimilarAsPropertyPanel);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, boolean isTooltipInModal, String labelCssClass, String textCssClass, final boolean required,
			final boolean markAsRequired, boolean isSimilarAsPropertyPanel) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        add(labelContainer);

        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelCssClass)) {
            labelContainer.add(AttributeAppender.prepend("class", labelCssClass));
        }
        if(isSimilarAsPropertyPanel) {
        	labelContainer.add(AttributeAppender.prepend("class", " col-xs-2 prism-property-label "));
        } else {
        	labelContainer.add(AttributeAppender.prepend("class", " control-label "));
        }
        labelContainer.add(l);

        Label tooltipLabel = new Label(ID_TOOLTIP, new Model<>());
        tooltipLabel.add(new AttributeAppender("data-original-title", new IModel<String>() {

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
				return markAsRequired;
			}
		});
		labelContainer.add(requiredContainer);
		WebMarkupContainer propertyLabel = new WebMarkupContainer(ID_PROPERTY_LABEL);
		WebMarkupContainer rowLabel = new WebMarkupContainer(ID_ROW);
		WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
        if (StringUtils.isNotEmpty(textCssClass)) {
            textWrapper.add(AttributeAppender.prepend("class", textCssClass));
        }
        if(isSimilarAsPropertyPanel) {
        	propertyLabel.add(AttributeAppender.prepend("class", " col-md-10 prism-property-value "));
        	rowLabel.add(AttributeAppender.prepend("class", " row "));
        }
        propertyLabel.add(rowLabel);
		rowLabel.add(textWrapper);
        add(propertyLabel);

        TextField text = createText(getModel(), label, required);
        text.setLabel(label);
        textWrapper.add(text);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK, new ContainerFeedbackMessageFilter(this));
        feedback.setOutputMarkupId(true);
        textWrapper.add(feedback);
    }

    protected TextField createText(IModel<String> model, IModel<String> label, boolean required) {
        TextField text = new TextField(ID_TEXT, model);
        text.setRequired(required);
//        text.add(AttributeAppender.replace("placeholder", label));

        return text;
    }

    public TextField getField(){
        return (TextField) get(createComponentPath(ID_PROPERTY_LABEL, ID_ROW, ID_TEXT_WRAPPER, ID_TEXT));
    }
}
