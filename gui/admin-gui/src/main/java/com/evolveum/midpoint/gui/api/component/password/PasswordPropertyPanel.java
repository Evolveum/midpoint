/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.api.component.password;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class PasswordPropertyPanel  extends PrismPropertyPanel<ProtectedStringType>{
    private static final long serialVersionUID = 1L;

    private static final String ID_PASSWORD_PANEL= "passwordPanel";

    public PasswordPropertyPanel(String id, IModel<PrismPropertyWrapper<ProtectedStringType>> model, ItemPanelSettings settings){
        super(id, model, settings);
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<ProtectedStringType>> item, GuiComponentFactory factory, ItemVisibilityHandler visibilityHandler) {

        PasswordPanel passwordPanel;
        if (!(getPageBase() instanceof PageUser)) {
            passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new ItemRealValueModel<>(item.getModel()),
                    getModelObject() != null && getModelObject().isReadOnly(), true);

        } else {

            passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new ItemRealValueModel<>(item.getModel()),
                    getModelObject() != null && getModelObject().isReadOnly(),
                    item.getModelObject() == null || item.getModelObject().getRealValue() == null );
        }
        passwordPanel.setOutputMarkupId(true);
        item.add(passwordPanel);
        return passwordPanel;



    }

    @Override
    protected void createButtons(ListItem<PrismPropertyValueWrapper<ProtectedStringType>> item) {
        //nothing to do
    }

}
