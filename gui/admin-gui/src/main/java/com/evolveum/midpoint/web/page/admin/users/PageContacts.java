package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.admin.users.component.ContactDataProvider;
import com.evolveum.midpoint.web.page.admin.users.component.ContactOrgUnitPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ContactUserPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.ContactTreeDto;
import com.evolveum.midpoint.web.page.admin.users.dto.ContactUserDto;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/users/contacts", action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#contacts",
                label = "PageContacts.auth.contacts.label",
                description = "PageContacts.auth.contacts.description")})
public class PageContacts extends PageAdminUsers {

    private static final String ID_TABLE = "table";
    private static final String ID_TREE = "tree";

    public PageContacts() {
        initLayout();
    }

    private void initLayout() {
//        ITreeProvider<ContactTreeDto> provider = new ContactDataProvider();
//
//        NestedTree tree = new NestedTree<ContactTreeDto>(ID_TREE, provider) {
//
//            @Override
//            protected Component newContentComponent(String id, IModel model) {
//                ContactTreeDto dto = (ContactTreeDto) model.getObject();
//                if (dto instanceof ContactUserDto) {
//                    return new ContactUserPanel(id, model);
//                }
//
//                return new ContactOrgUnitPanel(id, model);
//            }
//        };
//        add(tree);
    }
}
