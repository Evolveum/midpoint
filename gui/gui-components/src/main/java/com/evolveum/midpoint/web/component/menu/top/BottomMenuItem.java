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

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class BottomMenuItem implements Serializable {

    private IModel<String> label;
    private Class<? extends Page> page;
    private VisibleEnableBehaviour visible;

    public BottomMenuItem(IModel<String> label, Class<? extends Page> page) {
        this(label, page, null);
    }

    public BottomMenuItem(IModel<String> label, Class<? extends Page> page, VisibleEnableBehaviour visible) {
        this.label = label;
        this.page = page;
        this.visible = visible;
    }

    public IModel<String> getLabel() {
        return label;
    }

    public Class<? extends Page> getPage() {
        return page;
    }

    public VisibleEnableBehaviour getVisible() {
        return visible;
    }
}
