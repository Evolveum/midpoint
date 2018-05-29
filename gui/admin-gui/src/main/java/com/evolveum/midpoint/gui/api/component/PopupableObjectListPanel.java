/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;

import java.util.Collection;
import java.util.List;

/**
 * Created by honchar.
 */
public class PopupableObjectListPanel<O extends ObjectType> extends PopupObjectListPanel<O> implements Popupable {

    public PopupableObjectListPanel(String id, Class<? extends O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options,
                                    boolean multiselect, PageBase parentPage, List<O> selectedObjectsList) {
        super(id, defaultType, options, multiselect, parentPage, selectedObjectsList);
    }

    @Override
    public int getWidth() {
        return 900;
    }

    @Override
    public int getHeight() {
        return 700;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("ObjectBrowserPanel.chooseObject");
    }

    @Override
    public Component getComponent() {
        return this;
    }


}
