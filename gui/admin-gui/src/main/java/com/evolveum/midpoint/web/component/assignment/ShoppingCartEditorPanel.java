/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.assignment;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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


    public ShoppingCartEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);
    }

    @Override
    protected void initHeaderRow(){
        WebMarkupContainer box = new WebMarkupContainer(ID_BOX);
        headerRow.add(box);

        box.add(new AttributeModifier("class", BOX_CSS_CLASS + " " + getBoxAdditionalCssClass()));

        box.add(new Label(ID_DISPLAY_NAME, new PropertyModel<AssignmentEditorDto>(getModel(), AssignmentEditorDto.F_NAME)));
        box.add(new Label(ID_DESCRIPTION, new IModel<String>() {
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
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return "";
            }
        };
    }

    @Override
    protected boolean isRelationEditable(){
        return false;
    }
}
