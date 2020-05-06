/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;


/**
 * Created by honchar
 */
public abstract class ExpressionTypeSelectPopup extends BasePanel implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final String ID_SHADOW_REF_CHECKBOX = "shadowRefCheckBox";
    private static final String ID_ASSOCIATION_TARGET_SEARCH_CHECKBOX = "associationTargetSearchCheckBox";
    private static final String ID_ADD_BUTTON = "addButton";

    public ExpressionTypeSelectPopup(String id){
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        IModel<Boolean> shadowSelectModel = Model.of(false);
        IModel<Boolean> assocTargetSearchSelectModel = Model.of(false);

        AjaxCheckBox shadowRefCheckbox = new AjaxCheckBox(ID_SHADOW_REF_CHECKBOX, shadowSelectModel){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(ExpressionTypeSelectPopup.this);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
            }
        };
        shadowRefCheckbox.setOutputMarkupId(true);
        shadowRefCheckbox.add(new EnableBehaviour(() -> !assocTargetSearchSelectModel.getObject()));
        add(shadowRefCheckbox);

        AjaxCheckBox associationTargetSearchCheckbox = new AjaxCheckBox(ID_ASSOCIATION_TARGET_SEARCH_CHECKBOX, assocTargetSearchSelectModel){
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
               target.add(ExpressionTypeSelectPopup.this);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
            }
        };
        associationTargetSearchCheckbox.setOutputMarkupId(true);
        associationTargetSearchCheckbox.add(new EnableBehaviour(() -> !shadowSelectModel.getObject()));
        add(associationTargetSearchCheckbox);

        AjaxButton select = new AjaxButton(ID_ADD_BUTTON, new StringResourceModel("ExpressionTypeSelectPopup.addButton")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ExpressionValueTypes expressionType = getShadowRefCheckbox().getModelObject() ? ExpressionValueTypes.SHADOW_REF_EXPRESSION :
                        (getAssociationTargetSearchCheckbox().getModelObject() ? ExpressionValueTypes.ASSOCIATION_TARGET_SEARCH_EXPRESSION : null);
                addExpressionPerformed(target, expressionType);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
            }
        };
        add(select);

    }

    private CheckBox getShadowRefCheckbox(){
        return (CheckBox)get(ID_SHADOW_REF_CHECKBOX);
    }

    private CheckBox getAssociationTargetSearchCheckbox(){
        return (CheckBox)get(ID_ASSOCIATION_TARGET_SEARCH_CHECKBOX);
    }

    protected abstract void addExpressionPerformed(AjaxRequestTarget target, ExpressionValueTypes expressionType);

    @Override
    public int getWidth() {
        return 30;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("ExpressionTypeSelectPopup.title");
    }

    @Override
    public Component getComponent() {
        return this;
    }

}
