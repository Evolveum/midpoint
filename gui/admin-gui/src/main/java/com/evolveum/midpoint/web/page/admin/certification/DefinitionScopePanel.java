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

import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.page.admin.certification.dto.DefinitionScopeDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AEPlevel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentReviewScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationObjectBasedScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */

public class DefinitionScopePanel extends SimplePanel<DefinitionScopeDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_OBJECT_TYPE_CHOOSER = "objectTypeChooser";

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

        final TextField descriptionField = new TextField(ID_DESCRIPTION, new PropertyModel<>(getModel(), DefinitionScopeDto.F_DESCRIPTION));
        descriptionField.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isEnabled() {
                return true;
            }
        });
        add(descriptionField);

//        DropDownChoice<ObjectType> aepLevel = new DropDownChoice<>(ID_OBJECT_TYPE_CHOOSER,
//                new PropertyModel<ObjectType>(getModel(), DefinitionScopeDto.F_OBJECT_TYPE),
//                WebMiscUtil.createReadonlyModelFromEnum(ObjectType.class),
//                new EnumChoiceRenderer<ObjectType>(DefinitionScopePanel.this));
//        aepLevel.setOutputMarkupId(true);
//        if(aepLevel.getModel().getObject() == null){
//            aepLevel.getModel().setObject(null);
//        }
//        aepLevel.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
//        add(aepLevel);
    }
}
