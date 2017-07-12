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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.page.PageBase;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class ShoppingCartEditorPanel extends AssignmentEditorPanel {
	private static final long serialVersionUID = 1L;

	protected static final String ID_BOX = "shoppingCartDetailsBox";
    protected static final String ID_DESCRIPTION = "description";
    protected static final String ID_ICON_BOX = "shoppingCartIconBox";
    protected static final String ID_ICON = "shoppingCartIcon";
    protected static final String ID_DISPLAY_NAME = "shoppingCartDisplayName";
    protected static final String BOX_CSS_CLASS = "info-box";
    protected static final String ICON_BOX_CSS_CLASS = "info-box-icon";


    public ShoppingCartEditorPanel(String id, IModel<AssignmentEditorDto> model, PageBase pageBase) {
        super(id, model, pageBase);
    }

    @Override
    protected void initHeaderRow(){
        WebMarkupContainer box = new WebMarkupContainer(ID_BOX);
        headerRow.add(box);

        box.add(new AttributeModifier("class", BOX_CSS_CLASS + " " + getBoxAdditionalCssClass()));

        box.add(new Label(ID_DISPLAY_NAME, new PropertyModel<AssignmentEditorDto>(getModel(), AssignmentEditorDto.F_NAME)));
        box.add(new Label(ID_DESCRIPTION, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject(){
                return getModelObject().getTargetRef() != null
                        && getModelObject().getTargetRef().getDescription() != null ?
                        getModelObject().getTargetRef().getDescription() : "";
            }
        }));

        WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
        box.add(iconBox);

        if (getIconBoxAdditionalCssClass() != null) {
            iconBox.add(new AttributeModifier("class", ICON_BOX_CSS_CLASS + " " + getIconBoxAdditionalCssClass()));
        }

        Label icon = new Label(ID_ICON, "");
        icon.add(new AttributeModifier("class", getIconCssClass()));
        iconBox.add(icon);
    }

    private String getBoxAdditionalCssClass(){
        AssignmentEditorDtoType type = getModel().getObject().getType();
        if (AssignmentEditorDtoType.ORG_UNIT.equals(type)){
            return "summary-panel-org";
        } else if (AssignmentEditorDtoType.ROLE.equals(type)){
            return "summary-panel-role";
        } else if (AssignmentEditorDtoType.SERVICE.equals(type)){
            return "summary-panel-service";
        }
        return "";
    }

    private String getIconBoxAdditionalCssClass(){
        AssignmentEditorDtoType type = getModel().getObject().getType();
        if (AssignmentEditorDtoType.ORG_UNIT.equals(type)){
            return "summary-panel-org";
        } else if (AssignmentEditorDtoType.ROLE.equals(type)){
            return "summary-panel-role";
        } else if (AssignmentEditorDtoType.SERVICE.equals(type)){
            return "summary-panel-service";
        }
        return "";
    }

    private String getIconCssClass(){
        return createImageTypeModel(getModel()).getObject();
    }

    @Override
    protected IModel<String> createHeaderClassModel(final IModel<AssignmentEditorDto> model) {
        return new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return "";
            }
        };
    }
}
