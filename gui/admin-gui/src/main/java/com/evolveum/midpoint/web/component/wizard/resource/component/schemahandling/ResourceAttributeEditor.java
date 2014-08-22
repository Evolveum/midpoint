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

import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 *  @author shood
 * */
public class ResourceAttributeEditor extends SimplePanel{

    private static final String ID_LABEL = "label";
    private static final String ID_REFERENCE = "reference";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXCLUSIVE_STRONG = "exclusiveStrong";
    private static final String ID_TOLERANT = "tolerant";
    private static final String ID_TOLERANT_VP = "tolerantValuePattern";
    private static final String ID_INTOLERANT_VP = "intolerantValuePattern";
    private static final String ID_FETCH_STRATEGY = "fetchStrategy";
    private static final String ID_MATCHING_RULE = "matchingRule";
    private static final String ID_INBOUND = "inbound";
    private static final String ID_OUTBOUND_LABEL = "outboundLabel";
    private static final String ID_BUTTON_OUTBOUND = "buttonOutbound";
    private static final String ID_BUTTON_LIMITATIONS = "buttonLimitations";

    public ResourceAttributeEditor(String id, IModel<ResourceAttributeDefinitionType> model){
        super(id, model);
    }

    @Override
    protected void initLayout(){
        Label label = new Label(ID_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceAttributeDefinitionType attribute = (ResourceAttributeDefinitionType)getModelObject();

                if(attribute.getDisplayName() == null && attribute.getRef() == null){
                    return getString("ResourceAttributeEditor.label.new");
                } else {
                    return getString("ResourceAttributeEditor.label.edit", attribute.getDisplayName());
                }
            }
        });
        add(label);

        //TODO - figure out what ref is exactly and make this autoCompleteField with proper resource values + validator
        TextField ref = new TextField<>(ID_REFERENCE, new PropertyModel<String>(getModel(), "ref.localPart"));
        add(ref);

        TextField displayName = new TextField<>(ID_DISPLAY_NAME, new PropertyModel<String>(getModel(), "displayName"));
        add(displayName);

        TextArea description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(getModel(), "description"));
        add(description);

        AjaxLink limitations = new AjaxLink(ID_BUTTON_LIMITATIONS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                limitationsEditPerformed(target);
            }
        };
        add(limitations);

        CheckBox exclusiveStrong = new CheckBox(ID_EXCLUSIVE_STRONG, new PropertyModel<Boolean>(getModel(), "exclusiveStrong"));
        add(exclusiveStrong);

        CheckBox tolerant = new CheckBox(ID_TOLERANT, new PropertyModel<Boolean>(getModel(), "tolerant"));
        add(tolerant);

        MultiValueTextPanel tolerantVP = new MultiValueTextPanel<>(ID_TOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "tolerantValuePattern"));
        add(tolerantVP);

        MultiValueTextPanel intolerantVP = new MultiValueTextPanel<>(ID_INTOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "intolerantValuePattern"));
        add(intolerantVP);

        DropDownChoice fetchStrategy = new DropDownChoice<>(ID_FETCH_STRATEGY,
                new PropertyModel<AttributeFetchStrategyType>(getModel(), "fetchStrategy"),
                WebMiscUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
                new EnumChoiceRenderer<AttributeFetchStrategyType>(this));
        add(fetchStrategy);

        //TODO - figure out what matchingRule is exactly and make this autoCompleteField with proper resource values + validator
        TextField matchingRule = new TextField<>(ID_MATCHING_RULE, new PropertyModel<String>(getModel(), "matchingRule.localPart"));
        add(matchingRule);

        //TODO - what should we display as Mapping label?
        TextField outboundLabel = new TextField<>(ID_OUTBOUND_LABEL,
                new PropertyModel<String>(getModel(), "outbound.name"));
        outboundLabel.setEnabled(false);
        add(outboundLabel);

        AjaxLink outbound = new AjaxLink(ID_BUTTON_OUTBOUND) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                outboundEditPerformed(target);
            }
        };
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<List<MappingType>>(getModel(), "inbound"), false, true){

            @Override
            protected IModel<String> createTextModel(final IModel<MappingType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        //TODO - what should we display as Mapping label?
                        return model.getObject().getName();
                    }
                };
            }

            @Override
            protected MappingType createNewEmptyItem(){
                return new MappingType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, MappingType object){
                mappingEditPerformed(target, object);
            }
        };
        add(inbound);
    }

    private void limitationsEditPerformed(AjaxRequestTarget target){
        //TODO - implement (open ModalWindow here - limitations editor)
    }

    private void outboundEditPerformed(AjaxRequestTarget target){
        //TODO - implement (open ModalWindow here - MappingType editor)
    }

    private void mappingEditPerformed(AjaxRequestTarget target, MappingType mapping){
        //TODO - implement (open ModalWindow here - MappingType editor)
    }
}
