/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.services.PageService;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.self.PageAssignmentDetails;
import com.evolveum.midpoint.web.page.self.PageAssignmentShoppingKart;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class RoleCatalogItemButton extends BasePanel<AssignmentEditorDto>{
    private static final long serialVersionUID = 1L;

    private static final String ID_ITEM_BUTTON_CONTAINER = "itemButtonContainer";
    private static final String ID_INNER = "inner";
    private static final String ID_INNER_LABEL = "innerLabel";
    private static final String ID_INNER_DESCRIPTION = "innerDescription";
    private static final String ID_TYPE_ICON = "typeIcon";
    private static final String ID_ALREADY_ASSIGNED_ICON = "alreadyAssignedIcon";
    private static final String ID_ADD_TO_CART_LINK = "addToCartLink";
    private static final String ID_ADD_TO_CART_LINK_LABEL = "addToCartLinkLabel";
    private static final String ID_ADD_TO_CART_LINK_ICON = "addToCartLinkIcon";
    private static final String ID_DETAILS_LINK = "detailsLink";
    private static final String ID_DETAILS_LINK_LABEL = "detailsLinkLabel";
    private static final String ID_DETAILS_LINK_ICON = "detailsLinkIcon";

    private boolean plusIconClicked = false;

    public RoleCatalogItemButton(String id, IModel<AssignmentEditorDto> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        WebMarkupContainer itemButtonContainer = new WebMarkupContainer(ID_ITEM_BUTTON_CONTAINER);
        itemButtonContainer.setOutputMarkupId(true);
        itemButtonContainer.add(new AttributeAppender("class", getBackgroundClass(getModelObject())));
        add(itemButtonContainer);

        AjaxLink inner = new AjaxLink(ID_INNER) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                targetObjectDetailsPerformed(RoleCatalogItemButton.this.getModelObject(), ajaxRequestTarget);
            }
        };
        inner.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(RoleCatalogItemButton.this.getModelObject());
            }
        });
        inner.add(new AttributeAppender("title", getModelObject().getName()));
        itemButtonContainer.add(inner);

        Label nameLabel = new Label(ID_INNER_LABEL, getModelObject().getName());
        inner.add(nameLabel);

        Label descriptionLabel = new Label(ID_INNER_DESCRIPTION, getModelObject().getTargetRef() != null ?
                getModelObject().getTargetRef().getDescription() : "");
        descriptionLabel.setOutputMarkupId(true);
        inner.add(descriptionLabel);

        AjaxLink detailsLink = new AjaxLink(ID_DETAILS_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                assignmentDetailsPerformed(RoleCatalogItemButton.this.getModelObject(), ajaxRequestTarget);
            }
        };
        detailsLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(getModelObject());
            }
        });
        itemButtonContainer.add(detailsLink);

        Label detailsLinkLabel = new Label(ID_DETAILS_LINK_LABEL, createStringResource("MultiButtonPanel.detailsLink"));
        detailsLinkLabel.setRenderBodyOnly(true);
        detailsLink.add(detailsLinkLabel);

        AjaxLink detailsLinkIcon = new AjaxLink(ID_DETAILS_LINK_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

        };
        detailsLinkIcon.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(getModelObject());
            }
        });
        detailsLink.add(detailsLinkIcon);

        AjaxLink addToCartLink = new AjaxLink(ID_ADD_TO_CART_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addAssignmentPerformed(RoleCatalogItemButton.this.getModelObject(), ajaxRequestTarget);
            }
        };
        addToCartLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(getModelObject());
            }
        });
        itemButtonContainer.add(addToCartLink);

        AjaxLink addToCartLinkIcon = new AjaxLink(ID_ADD_TO_CART_LINK_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

        };
        addToCartLinkIcon.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return isMultiUserRequest() || canAssign(getModelObject());
            }
        });
        addToCartLink.add(addToCartLinkIcon);

        WebMarkupContainer icon = new WebMarkupContainer(ID_TYPE_ICON);
        icon.add(new AttributeAppender("class", WebComponentUtil.createDefaultBlackIcon(getModelObject().getType().getQname())));
        itemButtonContainer.add(icon);

        WebMarkupContainer alreadyAssignedIcon = new WebMarkupContainer(ID_ALREADY_ASSIGNED_ICON);
        alreadyAssignedIcon.add(new AttributeAppender("title", getAlreadyAssignedIconTitleModel(getModelObject())));
        alreadyAssignedIcon.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return !isMultiUserRequest() && getModelObject().isAlreadyAssigned();
            }
        });
        itemButtonContainer.add(alreadyAssignedIcon);

    }

    private String getBackgroundClass(AssignmentEditorDto dto){
        if (!isMultiUserRequest() && !canAssign(dto)){
            return GuiStyleConstants.CLASS_DISABLED_OBJECT_ROLE_BG;
        } else if (AssignmentEditorDtoType.ROLE.equals(dto.getType())){
            return GuiStyleConstants.CLASS_OBJECT_ROLE_BG;
        }else if (AssignmentEditorDtoType.SERVICE.equals(dto.getType())){
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_BG;
        }else if (AssignmentEditorDtoType.ORG_UNIT.equals(dto.getType())){
            return GuiStyleConstants.CLASS_OBJECT_ORG_BG;
        } else {
            return "";
        }
    }

    private IModel<String> getAlreadyAssignedIconTitleModel(AssignmentEditorDto dto) {
        return new LoadableModel<String>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                List<RelationTypes> assignedRelations = dto.getAssignedRelationsList();
                String relations = "";
                if (assignedRelations != null && assignedRelations.size() > 0) {
                    relations = createStringResource("MultiButtonPanel.alreadyAssignedIconTitle").getString() + " ";
                    for (RelationTypes relation : assignedRelations) {
                        String relationName = createStringResource(relation).getString();
                        if (!relations.contains(relationName)) {
                            if (assignedRelations.indexOf(relation) > 0) {
                                relations = relations + ", ";
                            }
                            relations = relations + createStringResource(relation).getString();
                        }
                    }
                }
                return relations;
            }
        };
    }

    private void assignmentDetailsPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        if (!plusIconClicked) {
            assignment.setMinimized(false);
            assignment.setSimpleView(true);
            getPageBase().navigateToNext(new PageAssignmentDetails(Model.of(assignment)));
        } else {
            plusIconClicked = false;
        }
    }

    private void targetObjectDetailsPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        if (assignment.getTargetRef() == null || assignment.getTargetRef().getOid() == null){
            return;
        }
        if (!plusIconClicked) {
            PageParameters parameters = new PageParameters();
            parameters.add(OnePageParameterEncoder.PARAMETER, assignment.getTargetRef().getOid());

            if (AssignmentEditorDtoType.ORG_UNIT.equals(assignment.getType())){
                getPageBase().navigateToNext(PageOrgUnit.class, parameters);
            } else if (AssignmentEditorDtoType.ROLE.equals(assignment.getType())){
                getPageBase().navigateToNext(PageRole.class, parameters);
            } else if (AssignmentEditorDtoType.SERVICE.equals(assignment.getType())){
                getPageBase().navigateToNext(PageService.class, parameters);
            }
        } else {
            plusIconClicked = false;
        }
    }

    private boolean isMultiUserRequest(){
        return getPageBase().getSessionStorage().getRoleCatalog().isMultiUserRequest();
    }

    private boolean canAssign(AssignmentEditorDto assignment) {
        return assignment.isAssignable();
    }

    private void addAssignmentPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        plusIconClicked = true;
        RoleCatalogStorage storage = getPageBase().getSessionStorage().getRoleCatalog();
        if (storage.getAssignmentShoppingCart() == null){
            storage.setAssignmentShoppingCart(new ArrayList<AssignmentEditorDto>());
        }
        AssignmentEditorDto dto = assignment.clone();
        dto.setDefaultRelation();
        storage.getAssignmentShoppingCart().add(dto);

        assignmentAddedToShoppingCartPerformed(target);
    }

    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
    }
}
