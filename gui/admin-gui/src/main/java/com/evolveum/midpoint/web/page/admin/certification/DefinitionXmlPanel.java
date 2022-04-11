/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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
