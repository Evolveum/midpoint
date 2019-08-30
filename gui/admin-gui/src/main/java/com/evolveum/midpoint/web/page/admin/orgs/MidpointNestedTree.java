/**
 * Copyright (c) 2015-2019 Evolveum
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
package com.evolveum.midpoint.web.page.admin.orgs;

import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.model.IModel;

import java.util.Set;

public class MidpointNestedTree extends NestedTree<TreeSelectableBean<OrgType>> {


    public MidpointNestedTree(String id, ITreeProvider<TreeSelectableBean<OrgType>> provider) {
        super(id, provider);
    }

    public MidpointNestedTree(String id, ITreeProvider<TreeSelectableBean<OrgType>> provider, IModel<? extends Set<TreeSelectableBean<OrgType>>> state) {
        super(id, provider, state);

    }

    @Override
    protected Component newContentComponent(String id, IModel<TreeSelectableBean<OrgType>> model) {
        return null;
    }

    @Override
    public Component newSubtree(String id, IModel<TreeSelectableBean<OrgType>> model) {
        return new MidpointSubtree(id, this, model);
    }

}
