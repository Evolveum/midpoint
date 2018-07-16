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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
public abstract class MultiTypesMemberPopupTabPanel<O extends ObjectType> extends MemberPopupTabPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final String ID_TYPE_SELECT_PANEL = "typeSelectPanel";
    private static final String ID_TYPE = "type";

    private ObjectTypes defaultObjectType = ObjectTypes.OBJECT_COLLECTION;

    public MultiTypesMemberPopupTabPanel(String id, List<RelationTypes> supportedRelationsList){
        super(id, supportedRelationsList);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        WebMarkupContainer typePanel = new WebMarkupContainer(ID_TYPE_SELECT_PANEL);
        typePanel.setOutputMarkupId(true);
        add(typePanel);

        DropDownChoice<ObjectTypes> typeSelect = new DropDownChoice<>(ID_TYPE, new LoadableModel<ObjectTypes>() {
            @Override
            protected ObjectTypes load() {
                return defaultObjectType;
            }
        },
                getSupportedTypesList(), new EnumChoiceRenderer<>(this));
        typeSelect.add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                defaultObjectType = typeSelect.getModelObject();
                MultiTypesMemberPopupTabPanel.this.addOrReplace(initObjectListPanel());
                target.add(MultiTypesMemberPopupTabPanel.this);
            }
        });
        typePanel.add(typeSelect);

        add(typePanel);
    }

    protected List<ObjectTypes> getSupportedTypesList(){
        List<ObjectTypes> supportedTypes = new ArrayList<>(Arrays.asList(ObjectTypes.values()));
        supportedTypes.remove(ObjectTypes.USER);
        supportedTypes.remove(ObjectTypes.ROLE);
        supportedTypes.remove(ObjectTypes.SERVICE);
        supportedTypes.remove(ObjectTypes.ORG);
        supportedTypes.remove(ObjectTypes.FOCUS_TYPE);
        supportedTypes.remove(ObjectTypes.ABSTRACT_ROLE);
        supportedTypes.remove(ObjectTypes.NODE);
        supportedTypes.remove(ObjectTypes.SHADOW);

        return supportedTypes;
    }

    @Override
    protected ObjectTypes getObjectType(){
        return defaultObjectType;
    }
}
