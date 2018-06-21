/**
 * Copyright (c) 2015-2018 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.ExpressionValuePanel;
import com.evolveum.midpoint.web.component.input.QNameEditorPanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * Created by honchar
 */
public class AssociationDetailsPanel extends BasePanel<ContainerValueWrapper<ResourceObjectAssociationType>>{
    private static final long serialVersionUID = 1L;

    private static final String ID_REF_FIELD = "refField";
    private static final String ID_EXPRESSION_PANEL = "expressionPanel";
    private static final String ID_REMOVE_ASSOCIATION = "removeAssociation";

    private ConstructionType construction;

    public AssociationDetailsPanel(String id, IModel<ContainerValueWrapper<ResourceObjectAssociationType>> associationWrapperModel,
                                   ConstructionType construction){
        super(id, associationWrapperModel);
        this.construction = construction;
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        QNameEditorPanel refField = new QNameEditorPanel(ID_REF_FIELD, WebComponentUtil.createPrismPropertySingleValueModel(getModel(), ResourceObjectAssociationType.F_REF),
                null, null, false, false){
            private static final long serialVersionUID = 1L;
            @Override
            protected AttributeAppender getSpecificLabelStyleAppender() {
                return AttributeAppender.append("style", "font-weight: normal !important;");
            }
        };
        refField.setOutputMarkupId(true);
        add(refField);

        ResourceObjectAssociationType resourceObjectAssociationType = getModelObject().getContainerValue().asContainerable();
        MappingType outbound = resourceObjectAssociationType.getOutbound();
        ExpressionValuePanel expressionValuePanel = new ExpressionValuePanel(ID_EXPRESSION_PANEL,
            new PropertyModel<>(outbound, MappingType.F_EXPRESSION.getLocalPart()),
                construction, getPageBase());
        expressionValuePanel.setOutputMarkupId(true);
        add(expressionValuePanel);

        ToggleIconButton removeAssociationButton = new ToggleIconButton(ID_REMOVE_ASSOCIATION,
                GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER, GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
//                isChildContainersSelectorPanelVisible = true;
//                target.add(PrismContainerValueHeaderPanel.this);
            }

            @Override
            public boolean isOn() {
                return true;
            }
        };
        add(removeAssociationButton);


    }
}
