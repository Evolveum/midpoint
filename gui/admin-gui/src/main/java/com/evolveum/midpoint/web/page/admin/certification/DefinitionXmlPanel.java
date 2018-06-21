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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */

public class DefinitionXmlPanel extends BasePanel<CertDefinitionDto> {

    private static final String ID_ACE_EDITOR = "aceEditor1";

    public DefinitionXmlPanel(String id, IModel<CertDefinitionDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {
        AceEditor editor = new AceEditor(ID_ACE_EDITOR, new PropertyModel<>(getModel(), CertDefinitionDto.F_XML));
        //TODO for now it is only readonly
        editor.setReadonly(true);
        add(editor);
    }
}
