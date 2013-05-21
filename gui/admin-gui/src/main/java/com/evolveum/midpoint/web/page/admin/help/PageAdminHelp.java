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

package com.evolveum.midpoint.web.page.admin.help;

import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.PageDisabledVisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mserbak
 */
public class PageAdminHelp extends PageAdmin {

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        List<BottomMenuItem> items = new ArrayList<BottomMenuItem>();

        items.add(new BottomMenuItem(createStringResource("pageAdminHelp.about"), PageAbout.class,
                new PageDisabledVisibleBehaviour(this, PageAbout.class)));
        items.add(new BottomMenuItem(createStringResource("pageAdminHelp.system"), PageSystem.class,
                new PageDisabledVisibleBehaviour(this, PageSystem.class)));

        return items;
    }
}
