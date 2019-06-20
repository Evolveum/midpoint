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

package com.evolveum.midpoint.web.component.wizard.resource.component;

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
        add(editor);
    }
}
