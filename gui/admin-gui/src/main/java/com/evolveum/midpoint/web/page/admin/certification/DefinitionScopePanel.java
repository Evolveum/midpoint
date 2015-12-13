/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationScopeType;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */

public class DefinitionScopePanel extends SimplePanel<DefinitionScopeDto> {

    IModel<DefinitionScopeDto> model;
    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";

    public DefinitionScopePanel(String id, IModel<DefinitionScopeDto> model) {
        super(id, model);
        this.model = model;
    }

    @Override
    protected void initLayout() {
        final TextField nameField = new TextField(ID_NAME, new PropertyModel<>(getModel(), DefinitionScopeDto.F_NAME));
        nameField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(nameField);

        final TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(getModel(), DefinitionScopeDto.F_DESCRIPTION));
        descriptionField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(descriptionField);

    }
}
