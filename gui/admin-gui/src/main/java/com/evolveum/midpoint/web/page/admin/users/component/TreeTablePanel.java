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
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.markup.html.repeater.tree.ISortableTreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.CheckedFolder;
import org.apache.wicket.extensions.markup.html.repeater.tree.theme.WindowsTheme;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
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
        ISortableTreeProvider provider = new OrgTreeProvider(this, getModel());
        NestedTree<OrgTreeDto> tree = new NestedTree<OrgTreeDto>(ID_TREE, provider) {

            @Override
            protected Component newContentComponent(String id, IModel<OrgTreeDto> model) {
                return new CheckedFolder<OrgTreeDto>(id, this, model) {

                    @Override
                    protected IModel<Boolean> newCheckBoxModel(final IModel<OrgTreeDto> model) {
                        return new PropertyModel<Boolean>(model, Selectable.F_SELECTED);
                    }

                    @Override
                    protected IModel<?> newLabelModel(final IModel<OrgTreeDto> model) {
                        return new AbstractReadOnlyModel<String>() {

                            @Override
                            public String getObject() {
                                OrgTreeDto dto = model.getObject();
                                if (StringUtils.isNotEmpty(dto.getDisplayName())) {
                                    return dto.getDisplayName();
                                }
                                return dto.getName();
                            }
                        };
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
        tree.add(new WindowsTheme());
        add(tree);

        add(new Label(ID_TABLE, "asdf"));
    }
}
