/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

public class PasswordHintPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_HINT= "hint";
    private final IModel<ProtectedStringType> hintModel;
    private final IModel<ProtectedStringType> passwordModel;
    private boolean isReadonly;

    public PasswordHintPanel(String id, IModel<ProtectedStringType> hintModel, IModel<ProtectedStringType> passwordModel, boolean isReadonly){
        super(id);
        this.hintModel = hintModel;
        this.passwordModel = passwordModel;
        this.isReadonly = isReadonly;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        final TextField<String> hint = new TextField<>(ID_HINT, new ProtectedStringModel(hintModel));
        hint.setOutputMarkupId(true);
        hint.add(new EnableBehaviour(() -> !isReadonly));
        add(hint);
    }

    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_HINT);
    }

}
