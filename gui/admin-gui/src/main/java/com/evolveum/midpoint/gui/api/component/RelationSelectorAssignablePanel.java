/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * Created by honchar.
 */
public class RelationSelectorAssignablePanel<T extends ObjectType> extends TypedAssignablePanel{
    private static final long serialVersionUID = 1L;

    private static final String ID_RELATION = "relation";
    private IModel<RelationTypes> relationModel;

    public RelationSelectorAssignablePanel(String id, final Class<T> type, boolean multiselect, PageBase parentPage) {
        super(id, type, multiselect, parentPage);
    }

    @Override
    protected void initAssignmentParametersPanel(){
        super.initAssignmentParametersPanel();

        relationModel = Model.of(RelationTypes.MEMBER);
        DropDownChoicePanel relationSelector = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
                WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class), relationModel, RelationSelectorAssignablePanel.this, false);
        relationSelector.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        relationSelector.setOutputMarkupId(true);
        relationSelector.setOutputMarkupPlaceholderTag(true);
        add(relationSelector);
    }

    @Override
    protected void addPerformed(AjaxRequestTarget target, List selected) {
        super.addPerformed(target, selected);
        addPerformed(target, selected, relationModel.getObject());

    }

    protected void addPerformed(AjaxRequestTarget target, List selected, RelationTypes relation) {
    }

}
