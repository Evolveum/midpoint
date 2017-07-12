/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author Viliam Repan (lazyman)
 */
public class MenuItem extends BaseMenuItem {

    public MenuItem(IModel<String> nameModel, Class<? extends WebPage> pageClass) {
        this(nameModel, pageClass, null, null);
    }

    public MenuItem(IModel<String> nameModel, Class<? extends WebPage> pageClass,
                    PageParameters params, VisibleEnableBehaviour visibleEnable, Class<? extends WebPage>... aliases) {
        super(nameModel, pageClass, params, visibleEnable, aliases);
    }
}
