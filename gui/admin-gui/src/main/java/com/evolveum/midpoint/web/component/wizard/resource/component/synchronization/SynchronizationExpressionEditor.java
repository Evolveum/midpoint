/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.wizard.resource.component.synchronization;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.input.ExpressionEditorPanel;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 *  @author shood
 * */
public class SynchronizationExpressionEditor extends BasePanel<ExpressionType> {

    private static final String ID_LABEL = "label";
    private static final String ID_EXPRESSION_EDITOR = "expressionPanel";

    public SynchronizationExpressionEditor(String id, IModel<ExpressionType> model, PageResourceWizard parentPage) {
        super(id, model);
        initLayout(parentPage);
    }

    @Override
    public IModel<ExpressionType> getModel(){
        IModel<ExpressionType> model = super.getModel();

        if(model.getObject() == null){
            model.setObject(new ExpressionType());
        }

        return model;
    }

    protected void initLayout(PageResourceWizard parentPage){
        Label label = new Label(ID_LABEL, new IModel<String>() {

            @Override
            public String getObject() {
                return getString(getLabel());
            }
        });
        add(label);

        ExpressionEditorPanel expressionEditor = new ExpressionEditorPanel(ID_EXPRESSION_EDITOR, getModel(), parentPage);
        add(expressionEditor);
    }

    public String getLabel(){
        return null;
    }
}
