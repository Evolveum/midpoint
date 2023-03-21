/*
 * Copyright (C) 2022-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.prism.InputPanel;
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
