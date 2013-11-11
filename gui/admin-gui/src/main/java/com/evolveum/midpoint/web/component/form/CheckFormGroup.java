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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class CheckFormGroup extends SimplePanel<Boolean> {

    private static final String ID_CHECK = "check";
    private static final String ID_CHECK_WRAPPER = "checkWrapper";
    private static final String ID_LABEL = "label";

    public CheckFormGroup(String id, IModel<Boolean> value, IModel<String> label, String labelSize, String textSize) {
        super(id, value);

        initLayout(label, labelSize, textSize);
    }

    private void initLayout(IModel<String> label, String labelSize, String textSize) {
        Label l = new Label(ID_LABEL, label);
        if (StringUtils.isNotEmpty(labelSize)) {
            l.add(AttributeAppender.prepend("class", labelSize));
        }
        add(l);

        WebMarkupContainer checkWrapper = new WebMarkupContainer(ID_CHECK_WRAPPER);
        if (StringUtils.isNotEmpty(textSize)) {
            checkWrapper.add(AttributeAppender.prepend("class", textSize));
        }
        add(checkWrapper);


        CheckBox check = new CheckBox(ID_CHECK, getModel());
        checkWrapper.add(check);
    }
}
