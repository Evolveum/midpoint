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
package com.evolveum.midpoint.web.page.admin.valuePolicy.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * Created by matus on 9/18/2017.
 */
public class ValuePolicyBasicPanel extends BasePanel<ValuePolicyDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-8";

    public ValuePolicyBasicPanel(String id, IModel<ValuePolicyDto> model) {
        super(id, model);

        initializeBasicLayout();
    }

    private void initializeBasicLayout() {

        TextFormGroup nameField = new TextFormGroup(ID_NAME, new PropertyModel<>(getModel(), "valuePolicy.name"), createStringResource("ValuePolicyBasicPanel.valuePolicy.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        TextFormGroup descriptionField = new TextFormGroup(ID_DESCRIPTION, new PropertyModel<>(getModel(), "valuePolicy.description"), createStringResource("ValuePolicyBasicPanel.valuePolicy.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);

        add(nameField);
        add(descriptionField);

    }

}
