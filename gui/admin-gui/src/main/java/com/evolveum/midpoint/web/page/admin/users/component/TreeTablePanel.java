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

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.CheckedFolder;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class TreeTablePanel extends SimplePanel {

    private static final String ID_TREE = "tree";
    private static final String ID_TABLE = "table";

    public TreeTablePanel(String id, IModel model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        ISortableTreeProvider provider = new OrgTreeProvider(this);
        NestedTree<OrgTreeDto> tree = new NestedTree<OrgTreeDto>(ID_TREE, provider) {

            @Override
            protected Component newContentComponent(String id, IModel<OrgTreeDto> model) {
                return new CheckedFolder<OrgTreeDto>(id, this, model) {

                    @Override
                    protected IModel<Boolean> newCheckBoxModel(final IModel<OrgTreeDto> model) {
                        return new PropertyModel<Boolean>(model, Selectable.F_SELECTED);
                    }

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
//                        OrgTreeDto dto = getModelObject();
//
//                        // search first ancestor with quux not set
//                        while (!dto.isSelected() && dto.getParent() != null) {
//                            dto = dto.getParent();
//                        }
//
//                        tree.updateBranch(dto, target);
                        //todo do something...
                    }

                    @Override
                    protected boolean isClickable() {
                        return true;
                    }

                    @Override
                    protected boolean isSelected() {
                        OrgTreeDto dto = getModelObject();
                        return dto.isSelected();
                    }
                };
            }
        };
        add(tree);

        add(new Label(ID_TABLE, "asdf"));
    }
}
