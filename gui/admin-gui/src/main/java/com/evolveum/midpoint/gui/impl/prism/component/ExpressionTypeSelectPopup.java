/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.prism.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

        CheckBox shadowRefCheckbox = new CheckBox(ID_SHADOW_REF_CHECKBOX, shadowSelectModel);
        shadowRefCheckbox.setOutputMarkupId(true);
        shadowRefCheckbox.add(new EmptyOnBlurAjaxFormUpdatingBehaviour(){
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                shadowSelectModel.setObject(true);
                target.add(ExpressionTypeSelectPopup.this);
            }
        });
        shadowRefCheckbox.add(new EnableBehaviour(() -> !assocTargetSearchSelectModel.getObject()));
        add(shadowRefCheckbox);

        CheckBox associationTargetSearchCheckbox = new CheckBox(ID_ASSOCIATION_TARGET_SEARCH_CHECKBOX, assocTargetSearchSelectModel);
        associationTargetSearchCheckbox.setOutputMarkupId(true);
        associationTargetSearchCheckbox.add(new EmptyOnBlurAjaxFormUpdatingBehaviour(){
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                assocTargetSearchSelectModel.setObject(true);
                target.add(ExpressionTypeSelectPopup.this);
            }
        });
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
