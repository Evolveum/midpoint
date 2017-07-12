/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.DisplayableValue;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
abstract class SearchPopupPanel<T> extends BasePanel<T> {

    private static final String ID_REMOVE = "remove";
    private static final String ID_ADD = "add";

    public SearchPopupPanel(String id, IModel<T> model) {
        super(id, model);

        initButtons();
    }

    private void initButtons() {
        AjaxLink remove = new AjaxLink(ID_REMOVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                removePerformed(target);
            }
        };
        add(remove);

        AjaxLink add = new AjaxLink(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target);
            }
        };
        add(add);
    }

    protected void addPerformed(AjaxRequestTarget target) {
        SearchItemPanel panel = findParent(SearchItemPanel.class);

        SearchItemPopoverDto dto = panel.getPopoverModel().getObject();
        List<DisplayableValue> values = dto.getValues();
        values.add(new SearchValue());

        panel.updatePopupBody(target);
    }

    protected void removePerformed(AjaxRequestTarget target) {
        SearchItemPanel panel = findParent(SearchItemPanel.class);
        T val = getModelObject();

        SearchItemPopoverDto dto = panel.getPopoverModel().getObject();
        List<DisplayableValue> values = dto.getValues();

        values.remove(val);

        if (values.isEmpty()) {
            values.add(new SearchValue());
        }

        panel.updatePopupBody(target);
    }
}
