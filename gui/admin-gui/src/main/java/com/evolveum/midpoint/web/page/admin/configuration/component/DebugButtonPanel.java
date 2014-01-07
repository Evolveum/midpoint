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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class DebugButtonPanel<T> extends SimplePanel<T> {

    private static final String ID_EXPORT = "export";
    private static final String ID_DELETE = "delete";

    public DebugButtonPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        AjaxButton export = new AjaxButton(ID_EXPORT, createStringResource("DebugButtonPanel.button.export")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                exportPerformed(target, DebugButtonPanel.this.getModel());
            }
        };
        add(export);

        AjaxButton delete = new AjaxButton(ID_DELETE, createStringResource("DebugButtonPanel.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target, DebugButtonPanel.this.getModel());
            }
        };
        add(delete);
    }

    public void deletePerformed(AjaxRequestTarget target, IModel<T> model) {

    }

    public void exportPerformed(AjaxRequestTarget target, IModel<T> model) {

    }
}
