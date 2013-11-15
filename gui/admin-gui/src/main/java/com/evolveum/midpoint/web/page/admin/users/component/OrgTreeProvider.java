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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableTreeProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Iterator;

/**
 * @author lazyman
 */
public class OrgTreeProvider extends SortableTreeProvider<OrgTreeDto, String> {

    private Component component;

    public OrgTreeProvider(Component component) {
        this.component = component;
    }

    private ModelService getModelService() {
        PageBase page = (PageBase) component.getPage();
        return page.getModelService();
    }

    @Override
    public Iterator<? extends OrgTreeDto> getChildren(OrgTreeDto node) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Iterator<? extends OrgTreeDto> getRoots() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasChildren(OrgTreeDto node) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public IModel<OrgTreeDto> model(OrgTreeDto object) {
        return new Model<OrgTreeDto>(object);
    }
}
