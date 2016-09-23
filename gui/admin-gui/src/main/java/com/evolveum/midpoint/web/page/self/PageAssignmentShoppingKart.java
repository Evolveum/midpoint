package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.assignment.AssignmentShoppingCartPanel;
import com.evolveum.midpoint.web.component.assignment.MultipleAssignmentSelectorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
@PageDescriptor(url = {"/self/assignmentShoppingCart"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENT_SHOP_KART_URL,
                label = "PageAssignmentShoppingKart.auth.requestAssignment.label",
                description = "PageAssignmentShoppingKart.auth.requestAssignment.description")})
public class PageAssignmentShoppingKart extends PageSelf{
    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_MAIN_FORM = "mainForm";

    public PageAssignmentShoppingKart(){
        initLayout();
    }
    private void initLayout(){
        Form mainForm = new org.apache.wicket.markup.html.form.Form(ID_MAIN_FORM);
        add(mainForm);
//TODO get oid of the catalog OrgType object
        AssignmentShoppingCartPanel panel = new AssignmentShoppingCartPanel(ID_MAIN_PANEL, new IModel<String>() {
            @Override
            public String getObject() {
                return "3ecc0a04-358a-4a83-9182-d7e4bcf71781";
            }

            @Override
            public void setObject(String s) {

            }

            @Override
            public void detach() {

            }
        });
        mainForm.add(panel);

    }
}
