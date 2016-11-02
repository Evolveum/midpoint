/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/users/contacts", action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_USERS_ALL,
                label = PageAdminUsers.AUTH_USERS_ALL_LABEL,
                description = PageAdminUsers.AUTH_USERS_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONTACTS_URL,
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
