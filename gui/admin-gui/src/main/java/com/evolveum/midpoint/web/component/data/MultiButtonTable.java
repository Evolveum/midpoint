package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.assignment.*;
import com.evolveum.midpoint.web.page.self.PageAssignmentDetails;
import com.evolveum.midpoint.web.session.UsersStorage;
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
    private static final String ID_ROW = "row";
    private static final String ID_CELL = "cell";
    private static final String ID_BUTTON_TYPE_ICON = "typeIcon";
    private static final String ID_BUTTON_LABEL = "buttonLabel";
    private static final String ID_BUTTON_PLUS_ICON = "plusIcon";
    private static final String ID_BUTTON = "assignmentButton";

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
        AjaxLink assignmentButton = new AjaxLink(ID_BUTTON) {
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
        Label plusLabel = new Label(ID_BUTTON_PLUS_ICON, "+");
//        plusLabel.add(new AttributeAppender("title", getPageBase().createStringResource("MultiButtonPanel.plusIconTitle")));
        plusLabel.add(new AjaxEventBehavior("click") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                addAssignmentPerformed(assignment, target);
            }
        });
        assignmentButton.add(plusLabel);

        WebMarkupContainer icon = new WebMarkupContainer(ID_BUTTON_TYPE_ICON);
        icon.add(new AttributeAppender("class", getIconClass(assignment.getType())));
        assignmentButton.add(icon);

        Label nameLabel = new Label(ID_BUTTON_LABEL, assignment.getName());
//        nameLabel.add(new AjaxEventBehavior("click") {
//            private static final long serialVersionUID = 1L;
//            @Override
//            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
//
//            }
//        });
        assignmentButton.add(nameLabel);
        cellContainer.add(assignmentButton);
    }

    protected void assignmentDetailsPerformed(AjaxRequestTarget target){
    }

    private String getIconClass(AssignmentEditorDtoType type){
        if (AssignmentEditorDtoType.ROLE.equals(type)){
            return "fa fa-street-view object-role-color";
        }else if (AssignmentEditorDtoType.SERVICE.equals(type)){
            return "fa fa-cloud object-service-color";
        }else if (AssignmentEditorDtoType.ORG_UNIT.equals(type)){
            return "fa fa-building object-org-color";
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
        AssignmentCatalogPanel parent = (AssignmentCatalogPanel)MultiButtonTable.this.getParent().getParent();
        parent.reloadCartButton(target);

    }

}
