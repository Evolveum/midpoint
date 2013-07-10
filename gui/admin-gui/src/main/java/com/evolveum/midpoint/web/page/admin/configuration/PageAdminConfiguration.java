/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.util.string.StringValue;

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

        items.add(new BottomMenuItem(
                createStringResource("pageAdminConfiguration.debugList"), PageDebugList.class));
        items.add(new BottomMenuItem(
                createStringResource("pageAdminConfiguration.debugView"), PageDebugView.class,
                new VisibleEnableBehaviour() {

                    @Override
                    public boolean isEnabled() {
                        return false;
                    }

                    @Override
                    public boolean isVisible() {
                        return isEditingObject();
                    }
                }));
        items.add(new BottomMenuItem(
                createStringResource("pageAdminConfiguration.importObject"), PageImportObject.class));
        items.add(new BottomMenuItem(
                createStringResource("pageAdminConfiguration.logging"), PageLogging.class));
        items.add(new BottomMenuItem(
                createStringResource("pageAdminConfiguration.timeTest"), PageTimeTest.class));
//        items.add(new BottomMenuItem(
//                createStringResource("pageAdminConfiguration.systemConfiguration"), PageSystemConfiguration.class));

        return items;
    }

    private boolean isEditingObject() {
        StringValue objectOid = getPageParameters().get(PageDebugView.PARAM_OBJECT_ID);
        return objectOid != null && StringUtils.isNotEmpty(objectOid.toString());
    }
}
