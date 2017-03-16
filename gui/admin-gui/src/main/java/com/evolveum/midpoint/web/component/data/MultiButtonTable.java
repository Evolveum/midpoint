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
package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.PageAssignmentDetails;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentMultiplicityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created honchar.
 */
public class MultiButtonTable extends BasePanel<List<AssignmentEditorDto>> {
	private static final long serialVersionUID = 1L;

	private static final String ID_ROW = "row";
    private static final String ID_CELL = "cell";
    private static final String ID_ITEM_BUTTON_CONTAINER = "itemButtonContainer";
    private static final String ID_INNER = "inner";
    private static final String ID_INNER_LABEL = "innerLabel";
    private static final String ID_TYPE_ICON = "typeIcon";
    private static final String ID_ADD_TO_CART_LINK = "addToCartLink";
    private static final String ID_ADD_TO_CART_LINK_LABEL = "addToCartLinkLabel";
    private static final String ID_ADD_TO_CART_LINK_ICON = "addToCartLinkIcon";
    private static final String ID_DETAILS_LINK = "detailsLink";
    private static final String ID_DETAILS_LINK_LABEL = "detailsLinkLabel";
    private static final String ID_DETAILS_LINK_ICON = "detailsLinkIcon";

    private static final String DOT_CLASS = AssignmentCatalogPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(AssignmentCatalogPanel.class);
    private static final String OPERATION_LOAD_ASSIGNMENT_MULTIPLICITY = DOT_CLASS + "loadAssignmentMultiplicity";

    private String addToCartLinkIcon = "fa fa-times-circle fa-lg text-danger";
    private String detailsLinkIcon = "fa fa-arrow-circle-right";
    private long itemsCount = 0;
    private long itemsPerRow = 0;
    private PageBase pageBase;

    private boolean plusIconClicked = false;

    public MultiButtonTable (String id){
        super(id);
    }

    public MultiButtonTable (String id, long itemsPerRow, IModel<List<AssignmentEditorDto>> model, PageBase pageBase){
        super(id, model);
        this.itemsPerRow = itemsPerRow;
        this.pageBase = pageBase;

         initLayout();
    }

    private void initLayout(){

        itemsCount = getModel() != null ? (getModel().getObject() != null ? getModel().getObject().size() : 0) : 0;
        RepeatingView rows = new RepeatingView(ID_ROW);
        rows.setOutputMarkupId(true);
        if (itemsCount > 0 && itemsPerRow > 0){
            int index = 0;
            List<AssignmentEditorDto> assignmentsList = getModelObject();
            long rowCount = itemsCount % itemsPerRow == 0 ? (itemsCount / itemsPerRow) : (itemsCount / itemsPerRow + 1);
            for (int rowNumber = 0; rowNumber < rowCount; rowNumber++){
                WebMarkupContainer rowContainer = new WebMarkupContainer(rows.newChildId());
                rows.add(rowContainer);
                RepeatingView columns = new RepeatingView(ID_CELL);
                columns.setOutputMarkupId(true);
                rowContainer.add(columns);
                for (int colNumber = 0; colNumber < itemsPerRow; colNumber++){
                    WebMarkupContainer colContainer = new WebMarkupContainer(columns.newChildId());
                    columns.add(colContainer);

                    WebMarkupContainer itemButtonContainer = new WebMarkupContainer(ID_ITEM_BUTTON_CONTAINER);
                    itemButtonContainer.setOutputMarkupId(true);
                    itemButtonContainer.add(new AttributeAppender("class", getBackgroundClass(assignmentsList.get(index))));
                    colContainer.add(itemButtonContainer);
                    populateCell(itemButtonContainer, assignmentsList.get(index));
                    index++;
                    if (index >= assignmentsList.size()){
                        break;
                    }

                }
            }
        }
        add(rows);
    }

    protected void populateCell(WebMarkupContainer cellContainer, final AssignmentEditorDto assignment){
        AjaxLink inner = new AjaxLink(ID_INNER) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                assignmentDetailsPerformed(assignment, ajaxRequestTarget);
            }
        };
        inner.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return !(AssignmentMultiplicityType.SINGLE.equals(getAssignmentMultiplicity())
                        && assignment.isAlreadyAssigned());
            }
        });
        cellContainer.add(inner);
        
        Label nameLabel = new Label(ID_INNER_LABEL, assignment.getName());
        inner.add(nameLabel);

        AjaxLink detailsLink = new AjaxLink(ID_DETAILS_LINK) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                assignmentDetailsPerformed(assignment, ajaxRequestTarget);
            }
        };
        detailsLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return !(AssignmentMultiplicityType.SINGLE.equals(getAssignmentMultiplicity())
                        && assignment.isAlreadyAssigned());
            }
        });
        cellContainer.add(detailsLink);

        Label detailsLinkLabel = new Label(ID_DETAILS_LINK_LABEL, pageBase.createStringResource("MultiButtonPanel.detailsLink"));
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
                return !(AssignmentMultiplicityType.SINGLE.equals(getAssignmentMultiplicity())
                        && assignment.isAlreadyAssigned());
            }
        });
        detailsLink.add(detailsLinkIcon);

        AjaxLink addToCartLink = new AjaxLink(ID_ADD_TO_CART_LINK) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                addAssignmentPerformed(assignment, ajaxRequestTarget);
            }
        };
        addToCartLink.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return !(AssignmentMultiplicityType.SINGLE.equals(getAssignmentMultiplicity())
                        && assignment.isAlreadyAssigned());
            }
        });
        cellContainer.add(addToCartLink);

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
                return !(AssignmentMultiplicityType.SINGLE.equals(getAssignmentMultiplicity())
                        && assignment.isAlreadyAssigned());
            }
        });
        addToCartLink.add(addToCartLinkIcon);

        WebMarkupContainer icon = new WebMarkupContainer(ID_TYPE_ICON);
        icon.add(new AttributeAppender("class", getIconClass(assignment.getType())));
        cellContainer.add(icon);

    }

    private void assignmentDetailsPerformed(final AssignmentEditorDto assignment, AjaxRequestTarget target){
        if (!plusIconClicked) {
            IModel<AssignmentEditorDto> assignmentModel = new IModel<AssignmentEditorDto>() {
                @Override
                public AssignmentEditorDto getObject() {
                    assignment.setMinimized(false);
                    assignment.setSimpleView(true);
                    return assignment;
                }

                @Override
                public void setObject(AssignmentEditorDto assignmentEditorDto) {

                }

                @Override
                public void detach() {

                }
            };
            setResponsePage(new PageAssignmentDetails(assignmentModel));
        } else {
            plusIconClicked = false;
        }
    }

    private String getIconClass(AssignmentEditorDtoType type){
    	// TODO: switch to icon constants
        if (AssignmentEditorDtoType.ROLE.equals(type)){
            return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
        }else if (AssignmentEditorDtoType.SERVICE.equals(type)){
            return GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON;
        }else if (AssignmentEditorDtoType.ORG_UNIT.equals(type)){
            return GuiStyleConstants.CLASS_OBJECT_ORG_ICON;
        } else {
            return "";
        }
    }

    private String getBackgroundClass(AssignmentEditorDto dto){
        if (dto.isAlreadyAssigned()){
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

    private void addAssignmentPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        plusIconClicked = true;
        RoleCatalogStorage storage = getPageBase().getSessionStorage().getRoleCatalog();
        if (storage.getAssignmentShoppingCart() == null){
            storage.setAssignmentShoppingCart(new ArrayList<AssignmentEditorDto>());
        }
        List<AssignmentEditorDto> assignmentsToAdd = storage.getAssignmentShoppingCart();
        assignmentsToAdd.add(assignment);
        storage.setAssignmentShoppingCart(assignmentsToAdd);
        AssignmentCatalogPanel parent = MultiButtonTable.this.findParent(AssignmentCatalogPanel.class);
        parent.reloadCartButton(target);

    }

    private AssignmentMultiplicityType getAssignmentMultiplicity(){
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_MULTIPLICITY);
        SystemConfigurationType systemConfig = null;
        try {
            systemConfig = pageBase.getModelInteractionService().getSystemConfiguration(result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
            return AssignmentMultiplicityType.SINGLE;
        }
        if (systemConfig != null && systemConfig.getRoleManagement() != null &&
                systemConfig.getRoleManagement().getDefaultAssignmentMultiplicity() != null &&
                !systemConfig.getRoleManagement().getDefaultAssignmentMultiplicity().toString().equals("")){
            return systemConfig.getRoleManagement().getDefaultAssignmentMultiplicity();
        }
        return AssignmentMultiplicityType.SINGLE;
    }
}
