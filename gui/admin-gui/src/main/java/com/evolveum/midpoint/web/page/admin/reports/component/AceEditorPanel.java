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
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AceEditor;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * @author shood
 */
public class AceEditorPanel extends BasePanel<String> {

    private static final String ID_TITLE = "title";
    private static final String ID_EDITOR = "editor";

    private IModel<String> title;

    public AceEditorPanel(String id, IModel<String> title, IModel<String> data) {
        super(id, data);

        this.title = title;
        initLayout(0);
    }
    
    public AceEditorPanel(String id, IModel<String> title, IModel<String> data, int minSize) {
        super(id, data);

        this.title = title;
        initLayout(minSize);
    }


    private void initLayout(int minSize) {
        Label title = new Label(ID_TITLE, this.title);
        add(title);

        AceEditor editor = new AceEditor(ID_EDITOR, getModel());
        editor.setReadonly(false);
        if (minSize > 0) {
            editor.setMinHeight(minSize);
        }
        add(editor);
    }

    public AceEditor getEditor(){
        return (AceEditor)get(ID_EDITOR);
    }
}
