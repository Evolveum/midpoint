/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources;


import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.users.PageUser;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.string.StringValue;

/**
 * Marker page class for {@link com.evolveum.midpoint.web.component.menu.top.TopMenu}
 *
 * @author lazyman
 */
public class PageAdminResources extends PageAdmin {

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem("pageAdminResources.listResources", PageResources.class));
//        items.add(new BottomMenuItem("pageAdminResources.newResource", PageUser.class));
        items.add(new BottomMenuItem("pageAdminResources.detailsResource", PageResource.class, new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isEditingResource();
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        }));
        items.add(new BottomMenuItem("pageAdminResources.importResource", PageResourceImport.class, new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isImportResource();
            }

            @Override
            public boolean isEnabled() {
                return false;
            }
        }));
        return items;
    }
    private boolean isEditingResource() {
        StringValue resourceOid = getPageParameters().get(PageResource.PARAM_RESOURCE_ID);
        return resourceOid != null && StringUtils.isNotEmpty(resourceOid.toString());
    }
    
    private boolean isImportResource() {
        StringValue resourceOid = getPageParameters().get(PageResourceImport.PARAM_RESOURCE_IMPORT_ID);
        System.out.println(resourceOid +" >>>>>>> "+resourceOid.toString());
        return resourceOid != null && StringUtils.isNotEmpty(resourceOid.toString());
    }
}
