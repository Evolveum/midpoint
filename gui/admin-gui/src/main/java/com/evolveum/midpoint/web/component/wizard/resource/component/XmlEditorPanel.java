/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.component;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AceEditor;

/**
 * @author lazyman
 */
public class XmlEditorPanel extends BasePanel<String> {

    private static final String ID_ACE_EDITOR = "aceEditor";

    public XmlEditorPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }


    protected void initLayout() {
        AceEditor editor = new AceEditor(ID_ACE_EDITOR, getModel());
        editor.setReadonly(!isEditEnabled());
        add(editor);
    }

    protected boolean isEditEnabled() {
        return true;
    }
}
