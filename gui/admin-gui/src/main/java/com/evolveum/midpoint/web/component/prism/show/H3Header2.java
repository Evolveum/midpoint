/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class H3Header2 extends SimplePanel<DataSceneDto> {

    private static final String ID_STATUS = "status";
    private static final String ID_SHOW_MORE = "showMore";
    public static final String ID_TITLE = "title";

    public H3Header2(String id, IModel<DataSceneDto> model) {
        super(id, model);

        add(AttributeModifier.append("class", "h3-header"));
    }

    @Override
    protected void initLayout() {
        Label title = new Label(ID_TITLE, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getDisplayName();
            }
        });
        add(title);

        final IModel<List<InlineMenuItem>> items = new Model((Serializable) createMenuItems());

    }

    private void showResult(OperationResult result) {
        PageBase page = getPageBase();
        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            page.showResult(result);
        }
    }

    private String getDisplayName() {
        if (getModelObject().getScene().getName() == null) {
            return "";
        }
        String key = getModelObject().getScene().getName().getDisplayName();
        if (key == null) {
            return "";
        }
        
        return PageBase.createStringResourceStatic(getPage(), key).getString();

    }

    protected List<InlineMenuItem> createMenuItems() {
        return new ArrayList<InlineMenuItem>();
    }
}
