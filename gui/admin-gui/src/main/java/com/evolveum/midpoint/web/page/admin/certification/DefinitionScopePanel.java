/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeObjectType;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */

public class DefinitionScopePanel extends SimplePanel<DefinitionScopeDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_OBJECT_TYPE_CHOOSER = "objectTypeChooser";
    private static final String ID_SEARCH_FILTER = "searchFilterEditor";
    private static final String ID_INCLUDE_RESOURCES = "includeResources";
    private static final String ID_INCLUDE_ROLES = "includeRoles";
    private static final String ID_INCLUDE_INDUCEMENTS = "includeInducements";
    private static final String ID_INCLUDE_ASSIGNMENTS = "includeAssignments";
    private static final String ID_INCLUDE_ORGS = "includeOrgs";
    private static final String ID_INCLUDE_ENABLED_ITEMS_ONLY = "includeEnabledItemsOnly";

    public DefinitionScopePanel(String id, IModel<DefinitionScopeDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        final TextField nameField = new TextField(ID_NAME, new PropertyModel<>(getModel(), DefinitionScopeDto.F_NAME));
        nameField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(nameField);

        final TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(getModel(), DefinitionScopeDto.F_DESCRIPTION));
        descriptionField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(descriptionField);

        DropDownChoicePanel objectTypeChooser = new DropDownChoicePanel(ID_OBJECT_TYPE_CHOOSER,
                new PropertyModel(getModel(), DefinitionScopeDto.F_OBJECT_TYPE),
                WebMiscUtil.createReadonlyModelFromEnum(DefinitionScopeObjectType.class),
                new IChoiceRenderer<DefinitionScopeObjectType>() {

                    @Override
                    public Object getDisplayValue(DefinitionScopeObjectType item) {
                        return item.name();
                    }

                    @Override
                    public String getIdValue(DefinitionScopeObjectType item, int index) {
                        return Integer.toString(index);
                    }
                });
        add(objectTypeChooser);

        TextArea filterTextArea = new TextArea(ID_SEARCH_FILTER, new PropertyModel<String>(getModel(), DefinitionScopeDto.F_SEARCH_FILTER_TEXT));
        filterTextArea.setOutputMarkupId(true);
        add(filterTextArea);

        add(new AjaxCheckBox(ID_INCLUDE_ASSIGNMENTS, new PropertyModel<Boolean>(getModel(), DefinitionScopeDto.F_INCLUDE_ASSIGNMENTS)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        add(new AjaxCheckBox(ID_INCLUDE_INDUCEMENTS, new PropertyModel<Boolean>(getModel(), DefinitionScopeDto.F_INCLUDE_INDUCEMENTS)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        add(new AjaxCheckBox(ID_INCLUDE_RESOURCES, new PropertyModel<Boolean>(getModel(), DefinitionScopeDto.F_INCLUDE_RESOURCES)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        add(new AjaxCheckBox(ID_INCLUDE_ROLES, new PropertyModel<Boolean>(getModel(), DefinitionScopeDto.F_INCLUDE_RESOURCES)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        add(new AjaxCheckBox(ID_INCLUDE_ORGS, new PropertyModel<Boolean>(getModel(), DefinitionScopeDto.F_INCLUDE_ORGS)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
        add(new AjaxCheckBox(ID_INCLUDE_ENABLED_ITEMS_ONLY, new PropertyModel<Boolean>(getModel(), DefinitionScopeDto.F_INCLUDE_ENABLED_ITEMS_ONLY)) {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        });
    }
}
