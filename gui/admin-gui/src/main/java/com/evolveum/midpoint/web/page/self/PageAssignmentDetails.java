package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentDetailsPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/self/assignmentDetails", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENT_DETAILS_URL,
                label = "PageAssignmentShoppingKart.auth.assignmentDetails.label",
                description = "PageAssignmentShoppingKart.auth.assignmentDetails.description")})
public class PageAssignmentDetails extends PageBase{
    private static final String ID_FORM = "mainForm";
    private static final String ID_DETAILS_PANEL = "detailsPanel";
    private static final String ID_BACK = "back";
    private static final String ID_ADD_TO_CART = "addToCart";

    public PageAssignmentDetails (){
        initLayout(null);
    }

    public PageAssignmentDetails(IModel<AssignmentEditorDto> assignmentModel){
        initLayout(assignmentModel);
    }

    public void initLayout(final IModel<AssignmentEditorDto> assignmentModel) {
        setOutputMarkupId(true);

        Form mainForm = new Form(ID_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        AssignmentDetailsPanel detailsPanel = new AssignmentDetailsPanel(ID_DETAILS_PANEL, assignmentModel, PageAssignmentDetails.this);
        detailsPanel.setOutputMarkupId(true);
        mainForm.add(detailsPanel);


        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageAssignmentDetails.backButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }

        };
        mainForm.add(back);

        AjaxButton addToCart = new AjaxButton(ID_ADD_TO_CART, createStringResource("PageAssignmentDetails.addToCartButton")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleCatalogStorage storage = getSessionStorage().getRoleCatalog();
                if (storage.getAssignmentShoppingCart() == null){
                    storage.setAssignmentShoppingCart(new ArrayList<AssignmentEditorDto>());
                }
                AssignmentEditorDto dto = assignmentModel.getObject();
                storage.getAssignmentShoppingCart().add(dto);

                redirectBack();
            }

        };
        mainForm.add(addToCart);
    }

    @Override
    public boolean canRedirectBack(){
        return true;
    }

}
