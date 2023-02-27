/*
 * Copyright (c) 2010-2022 Evolveum
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

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SimpleAceEditorPanel extends InputPanel {

    private static final String ID_EDITOR = "editor";

    private int minSize;

    public SimpleAceEditorPanel(String id, IModel<String> model, int minSize) {
        super(id);

        this.minSize = minSize;

        initLayout(model);
    }

    private void initLayout(IModel<String> model) {
        add(AttributeAppender.append("class", "border rounded"));

        AceEditor editor = createEditor(ID_EDITOR, model, minSize);
        add(editor);
    }

    protected AceEditor createEditor(String id, IModel<String> model, int minSize) {
        AceEditor editor = new AceEditor(id, model);
        editor.setReadonly(false);
        if (minSize > 0) {
            editor.setMinHeight(minSize);
        }
        editor.setResizeToMaxHeight(minSize == 0);
        add(editor);
        editor.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

        return editor;
    }

    public AceEditor getEditor() {
        return (AceEditor) get(ID_EDITOR);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return getEditor();
    }
}
