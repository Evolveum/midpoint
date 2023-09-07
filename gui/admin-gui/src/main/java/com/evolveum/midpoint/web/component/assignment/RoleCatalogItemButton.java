/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.service.PageService;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.PageAssignmentDetails;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
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

    private static final String DOT_CLASS = RoleCatalogItemButton.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_LOAD_RELATION_DEFINITION_LIST = DOT_CLASS + "loadRelationDefinitionList";
    private static final Trace LOGGER = TraceManager.getTrace(RoleCatalogItemButton.class);

    public RoleCatalogItemButton(String id, IModel<AssignmentEditorDto> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        WebMarkupContainer itemButtonContainer = new WebMarkupContainer(ID_ITEM_BUTTON_CONTAINER);
        itemButtonContainer.setOutputMarkupId(true);
        itemButtonContainer.add(new AttributeAppender("class", getBackgroundClass(getModelObject())));
        add(itemButtonContainer);

        AjaxLink<Void> inner = new AjaxLink<Void>(ID_INNER) {
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
                return isAuthorizedForTargetObjectDetailsPage(getModelObject())
                        && (isMultiUserRequest() || canAssign(RoleCatalogItemButton.this.getModelObject()));
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

        AjaxLink<Void> detailsLink = new AjaxLink<Void>(ID_DETAILS_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                assignmentDetailsPerformed(RoleCatalogItemButton.this.getModelObject(), ajaxRequestTarget);
            }
        };
        detailsLink.add(getAssignmentDetailsLinkVisibleBehavior());
        detailsLink.add(AttributeAppender.append("title",
                AssignmentsUtil.getShoppingCartAssignmentsLimitReachedTitleModel(getPageBase())));
        detailsLink.add(AttributeAppender.append("class", new LoadableModel<String>() {
            @Override
            protected String load() {
                return detailsLink.isEnabled() ?  "shopping-cart-item-button-details" :  "shopping-cart-item-button-details-disabled";
            }
        }));
        itemButtonContainer.add(detailsLink);

        Label detailsLinkLabel = new Label(ID_DETAILS_LINK_LABEL, createStringResource("MultiButtonPanel.detailsLink"));
        detailsLinkLabel.setRenderBodyOnly(true);
        detailsLink.add(detailsLinkLabel);

        AjaxLink<Void> detailsLinkIcon = new AjaxLink<Void>(ID_DETAILS_LINK_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

        };
        detailsLinkIcon.add(getAssignmentDetailsLinkVisibleBehavior());
        detailsLink.add(detailsLinkIcon);

        AjaxLink<Void> addToCartLink = new AjaxLink<Void>(ID_ADD_TO_CART_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addAssignmentPerformed(RoleCatalogItemButton.this.getModelObject(), ajaxRequestTarget);
            }
        };
        addToCartLink.add(getFooterLinksEnableBehaviour());
        addToCartLink.add(AttributeAppender.append("title",
                AssignmentsUtil.getShoppingCartAssignmentsLimitReachedTitleModel(getPageBase())));
        addToCartLink.add(AttributeAppender.append("class", new LoadableModel<String>() {
            @Override
            protected String load() {
                return addToCartLink.isEnabled() ?  "shopping-cart-item-button-add" :  "shopping-cart-item-button-add-disabled";
            }
        }));
        itemButtonContainer.add(addToCartLink);

        AjaxLink<Void> addToCartLinkIcon = new AjaxLink<Void>(ID_ADD_TO_CART_LINK_ICON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
            }

        };
        addToCartLinkIcon.add(getFooterLinksEnableBehaviour());
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
        ActivationStatusType activation = getItemEffectiveStatusType(dto.getOldValue());
        if (!isMultiUserRequest() && !canAssign(dto) || ActivationStatusType.ARCHIVED.equals(activation) || ActivationStatusType.DISABLED.equals(activation)){
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

    private ActivationStatusType getItemEffectiveStatusType(PrismContainerValue<AssignmentType> assignmentValue){
        if (assignmentValue == null || assignmentValue.asContainerable() == null){
            return null;
        }
        AssignmentType assignment = assignmentValue.asContainerable();
        PrismObject target = assignment.getTargetRef().asReferenceValue().getObject();
        if (target == null || !(target.canRepresent(AbstractRoleType.class))) {
            return null;
        }
        ActivationType activation = ((AbstractRoleType)target.asObjectable()).getActivation();
        return activation != null ? activation.getEffectiveStatus() : null;
    }

    private IModel<String> getAlreadyAssignedIconTitleModel(AssignmentEditorDto dto) {
                List<QName> assignedRelations = dto.getAssignedRelationsList();
                StringBuilder relations = new StringBuilder();
                if (assignedRelations != null && assignedRelations.size() > 0) {
                    List<RelationDefinitionType> defs = WebComponentUtil.getRelationDefinitions(RoleCatalogItemButton.this.getPageBase());
                    for (QName relation : assignedRelations) {
                        RelationDefinitionType def = ObjectTypeUtil.findRelationDefinition(defs, relation);
                        String relationLabel;
                        if (def == null || def.getDisplay() == null || def.getDisplay().getLabel() == null){
                            relationLabel = relation.getLocalPart();
                        } else {
                            relationLabel = createStringResource(def.getDisplay().getLabel()).getString();
                        }
                        if (!relations.toString().contains(relationLabel)){
                            if (!relations.toString().isEmpty()){
                                relations.append(", ");
                            }
                            relations.append(relationLabel);
                        }
                    }
                }
                return createStringResource("MultiButtonPanel.alreadyAssignedIconTitle", relations.toString());
    }

    private VisibleEnableBehaviour getFooterLinksEnableBehaviour() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                int assignmentsLimit = getRoleCatalogStorage().getAssignmentRequestLimit();
                return !AssignmentsUtil.isShoppingCartAssignmentsLimitReached(assignmentsLimit, RoleCatalogItemButton.this.getPageBase())
                        && (isMultiUserRequest() || canAssign(getModelObject()));
            }
        };
    }

    private VisibleBehaviour getAssignmentDetailsLinkVisibleBehavior(){
        return new VisibleBehaviour(() -> {
            int assignmentsLimit = getRoleCatalogStorage().getAssignmentRequestLimit();
            boolean isAuthorized = WebComponentUtil.isAuthorized(PageSelf.AUTH_SELF_ALL_URI, AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENT_DETAILS_URL);
            return isAuthorized && !AssignmentsUtil.isShoppingCartAssignmentsLimitReached(assignmentsLimit, RoleCatalogItemButton.this.getPageBase())
                    && (isMultiUserRequest() || canAssign(getModelObject()));
        });
    }

    private void assignmentDetailsPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        if (!plusIconClicked) {
            assignment.setMinimized(false);
            assignment.setSimpleView(true);
            assignment.getTargetRef().setRelation(getNewAssignmentRelation());
            getPageBase().navigateToNext(new PageAssignmentDetails(Model.of(assignment)));
        } else {
            plusIconClicked = false;
        }
    }

    private boolean isAuthorizedForTargetObjectDetailsPage(AssignmentEditorDto assignment){
        if (assignment.getTargetRef() == null || assignment.getTargetRef().getOid() == null){
            return false;
        }
        if (AssignmentEditorDtoType.ORG_UNIT.equals(assignment.getType())){
            return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ORG_ALL_URL, AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL);
        } else if (AssignmentEditorDtoType.ROLE.equals(assignment.getType())){
            return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL, AuthorizationConstants.AUTZ_UI_ROLE_URL, AuthorizationConstants.AUTZ_UI_ROLE_DETAILS_URL);
        } else if (AssignmentEditorDtoType.SERVICE.equals(assignment.getType())){
            return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL, AuthorizationConstants.AUTZ_UI_SERVICE_URL);
        }
        return false;
    }

    private void targetObjectDetailsPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        if (assignment.getTargetRef() == null || assignment.getTargetRef().getOid() == null){
            return;
        }
        if (!plusIconClicked) {
            OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT);
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_OBJECT);

            if (AssignmentEditorDtoType.ORG_UNIT.equals(assignment.getType())){
                PrismObject<OrgType> object = WebModelServiceUtils.loadObject(OrgType.class, assignment.getTargetRef().getOid(),
                        getPageBase(), task, result);

                navigateToDetails(new PageOrg(object));
            } else if (AssignmentEditorDtoType.ROLE.equals(assignment.getType())){
                PrismObject<RoleType> object = WebModelServiceUtils.loadObject(RoleType.class, assignment.getTargetRef().getOid(),
                        getPageBase(), task, result);

                navigateToDetails(new PageRole(object));
            } else if (AssignmentEditorDtoType.SERVICE.equals(assignment.getType())){
                PrismObject<ServiceType> object = WebModelServiceUtils.loadObject(ServiceType.class, assignment.getTargetRef().getOid(),
                        getPageBase(), task, result);

                navigateToDetails(new PageService(object));
            }
        } else {
            plusIconClicked = false;
        }
    }

    private void navigateToDetails(PageFocusDetails next) {
        next.setReadonlyOverride(true);
        getPageBase().navigateToNext(next);
    }

    private boolean isMultiUserRequest(){
        return getRoleCatalogStorage().isMultiUserRequest();
    }

    private RoleCatalogStorage getRoleCatalogStorage(){
        return getPageBase().getSessionStorage().getRoleCatalog();
    }

    private boolean canAssign(AssignmentEditorDto assignment) {
        return assignment.isAssignable(getRoleCatalogStorage().getSelectedRelation());
    }

    private void addAssignmentPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        plusIconClicked = true;
        RoleCatalogStorage storage = getPageBase().getSessionStorage().getRoleCatalog();
        AssignmentEditorDto dto = assignment.clone();
        dto.getTargetRef().setRelation(getNewAssignmentRelation());
        storage.getAssignmentShoppingCart().add(dto);

        assignmentAddedToShoppingCartPerformed(target);
    }

    protected void assignmentAddedToShoppingCartPerformed(AjaxRequestTarget target){
    }

    protected QName getNewAssignmentRelation() {
        return WebComponentUtil.getDefaultRelationOrFail();
    }
}
