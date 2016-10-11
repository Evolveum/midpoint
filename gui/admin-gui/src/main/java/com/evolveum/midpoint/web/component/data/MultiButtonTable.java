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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.page.self.PageAssignmentDetails;
import com.evolveum.midpoint.web.session.UsersStorage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.navigation.paging.IPageableItems;
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
    private static final String ID_INNER = "inner";
    private static final String ID_INNER_LABEL = "innerLabel";
    private static final String ID_TYPE_ICON = "typeIcon";
    private static final String ID_ADD_TO_CART_LINK = "addToCartLink";
    private static final String ID_DETAILS_LINK = "detailsLink";

    private long itemsCount = 0;
    private long itemsPerRow = 0;

    private boolean plusIconClicked = false;

    public MultiButtonTable (String id){
        super(id);
    }

    public MultiButtonTable (String id, long itemsPerRow, IModel<List<AssignmentEditorDto>> model){
        super(id, model);
        this.itemsPerRow = itemsPerRow;

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

                    populateCell(colContainer, assignmentsList.get(index));
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
        };
        cellContainer.add(inner);
        
        Label nameLabel = new Label(ID_INNER_LABEL, assignment.getName());
        inner.add(nameLabel);
        
        AjaxLink detailsLink = new AjaxLink(ID_DETAILS_LINK) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				// TODO
			}
        	
        };
        cellContainer.add(detailsLink);
        
        AjaxLink addToCartLink = new AjaxLink(ID_ADD_TO_CART_LINK) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				addAssignmentPerformed(assignment, target);
			}
        	
        };
        cellContainer.add(addToCartLink);
        

        WebMarkupContainer icon = new WebMarkupContainer(ID_TYPE_ICON);
        icon.add(new AttributeAppender("class", getIconClass(assignment.getType())));
        cellContainer.add(icon);

    }

    protected void assignmentDetailsPerformed(AjaxRequestTarget target){
    }

    private String getIconClass(AssignmentEditorDtoType type){
    	// TODO: switch to icon constants
        if (AssignmentEditorDtoType.ROLE.equals(type)){
            return "fa fa-street-view";
        }else if (AssignmentEditorDtoType.SERVICE.equals(type)){
            return "fa fa-cloud";
        }else if (AssignmentEditorDtoType.ORG_UNIT.equals(type)){
            return "fa fa-building";
        } else {
            return "";
        }
    }

    private void addAssignmentPerformed(AssignmentEditorDto assignment, AjaxRequestTarget target){
        plusIconClicked = true;
        UsersStorage storage = getPageBase().getSessionStorage().getUsers();
        if (storage.getAssignmentShoppingCart() == null){
            storage.setAssignmentShoppingCart(new ArrayList<AssignmentEditorDto>());
        }
        List<AssignmentEditorDto> assignmentsToAdd = storage.getAssignmentShoppingCart();
        assignmentsToAdd.add(assignment);
        storage.setAssignmentShoppingCart(assignmentsToAdd);
        CatalogItemsPanel parent = MultiButtonTable.this.findParent(CatalogItemsPanel.class);
        parent.reloadCartButton(target);

    }

}
