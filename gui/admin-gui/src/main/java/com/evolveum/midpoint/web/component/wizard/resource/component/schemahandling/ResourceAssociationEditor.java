/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling;

import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 *  @author shood
 * */
public class ResourceAssociationEditor extends SimplePanel{

    private static final String ID_LABEL = "label";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_DIRECTION = "direction";
    private static final String ID_ASSOCIATION_ATTRIBUTE = "associationAttribute";
    private static final String ID_VALUE_ATTRIBUTE = "valueAttribute";
    private static final String ID_EXPLICIT_REF_INTEGRITY = "explicitRefIntegrity";

    public ResourceAssociationEditor(String id, IModel<ResourceObjectAssociationType> model){
        super(id, model);
    }

    @Override
    protected void initLayout(){
        Label label = new Label(ID_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceObjectAssociationType association = (ResourceObjectAssociationType)getModelObject();

                if(association.getDisplayName() == null && association.getRef() == null){
                    return getString("ResourceAssociationEditor.label.new");
                } else {
                    return getString("ResourceAssociationEditor.label.edit", association.getDisplayName());
                }
            }
        });
        add(label);

        DropDownChoice kind = new DropDownChoice<>(ID_KIND,
                new PropertyModel<ShadowKindType>(getModel(), "kind"),
                WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                new EnumChoiceRenderer<ShadowKindType>(this));
        add(kind);

        MultiValueTextPanel intent = new MultiValueTextPanel<>(ID_INTENT,
                new PropertyModel<List<String>>(getModel(), "intent"));
        add(intent);

        DropDownChoice direction = new DropDownChoice<>(ID_DIRECTION,
                new PropertyModel<ResourceObjectAssociationDirectionType>(getModel(), "direction"),
                WebMiscUtil.createReadonlyModelFromEnum(ResourceObjectAssociationDirectionType.class),
                new EnumChoiceRenderer<ResourceObjectAssociationDirectionType>(this));
        add(direction);

        //TODO - figure out what associationAttribute is exactly and make this autoCompleteField with proper resource values + validator
        TextField associationAttribute = new TextField<>(ID_ASSOCIATION_ATTRIBUTE,
                new PropertyModel<String>(getModel(), "associationAttribute.localPart"));
        add(associationAttribute);

        //TODO - figure out what valueAttribute is exactly and make this autoCompleteField with proper resource values + validator
        TextField valueAttribute = new TextField<>(ID_VALUE_ATTRIBUTE,
                new PropertyModel<String>(getModel(), "valueAttribute.localPart"));
        add(valueAttribute);

        CheckBox explicitRefIntegrity = new CheckBox(ID_EXPLICIT_REF_INTEGRITY,
                new PropertyModel<Boolean>(getModel(), "explicitReferentialIntegrity"));
        add(explicitRefIntegrity);
    }
}
