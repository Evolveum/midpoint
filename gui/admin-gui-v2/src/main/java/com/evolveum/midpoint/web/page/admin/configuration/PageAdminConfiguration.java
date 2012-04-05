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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import java.util.ArrayList;
import java.util.List;

/**
 * Marker page class for {@link com.evolveum.midpoint.web.component.menu.top.TopMenu}
 *
 * @author lazyman
 */
public class PageAdminConfiguration extends PageAdmin {

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem("pageAdminConfiguration.logging", PageLogging.class));
        items.add(new BottomMenuItem("pageAdminConfiguration.debugView", PageDebugView.class));
        items.add(new BottomMenuItem("pageAdminConfiguration.debugList", PageDebugList.class));
        items.add(new BottomMenuItem("pageAdminConfiguration.importFromXml", PageImportXml.class));
        items.add(new BottomMenuItem("pageAdminConfiguration.importFromFile", PageImportFile.class));
       

        return items;
    }
}
