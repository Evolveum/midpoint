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
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class TextAreaFormGroup extends BasePanel<String> {

    private static final String ID_TEXT = "text";
    private static final String ID_TEXT_WRAPPER = "textWrapper";
    private static final String ID_LABEL_CONTAINER = "labelContainer";
    private static final String ID_LABEL = "label";
    private static final String ID_TOOLTIP = "tooltip";

    private static final int DEFAULT_NUMBER_OF_ROWS = 2;

    public TextAreaFormGroup(String id, IModel<String> value, IModel<String> label, String labelSize, String textSize) {
        this(id, value, label, labelSize, textSize, false);
    }

    public TextAreaFormGroup(String id, IModel<String> value, IModel<String> label, String labelSize, String textSize,
                             boolean required) {
        this(id, value, label, labelSize, textSize, required, DEFAULT_NUMBER_OF_ROWS);
    }

    public TextAreaFormGroup(String id, IModel<String> value, IModel<String> label, String labelSize, String textSize,
                             boolean required, int rowNumber){
        this(id, value, label, null, false, labelSize, textSize, required, rowNumber);
    }

    public TextAreaFormGroup(String id, IModel<String> value, IModel<String> label, String tooltipKey,
                             boolean isTooltipInModal, String labelSize, String textSize, boolean required, int rowNumber){
        super(id, value);

        initLayout(label, tooltipKey, isTooltipInModal, labelSize, textSize, required, rowNumber);
    }

    private void initLayout(IModel<String> label, final String tooltipKey, boolean isTooltipInModal, String labelSize,
                            String textSize, boolean required, int rowNumber) {
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

        WebMarkupContainer textWrapper = new WebMarkupContainer(ID_TEXT_WRAPPER);
        if (StringUtils.isNotEmpty(textSize)) {
            textWrapper.add(AttributeAppender.prepend("class", textSize));
        }
        add(textWrapper);

        TextArea text = new TextArea<>(ID_TEXT, getModel());
        text.add(new AttributeModifier("rows", rowNumber));
        text.setOutputMarkupId(true);
        text.setRequired(required);
        text.setLabel(label);
        text.add(AttributeAppender.replace("placeholder", label));
        textWrapper.add(text);
    }

    public void setRows(int rows) {
        TextArea area = (TextArea) get(createComponentPath(ID_TEXT_WRAPPER, ID_TEXT));
        area.add(AttributeModifier.replace("rows", rows));
    }
}
