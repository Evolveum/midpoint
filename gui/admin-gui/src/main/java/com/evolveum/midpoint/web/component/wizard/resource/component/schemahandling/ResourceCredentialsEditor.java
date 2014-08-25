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
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceCredentialsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourcePasswordDefinitionType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 *  @author shood
 * */
public class ResourceCredentialsEditor extends SimplePanel{

    private static final String ID_FETCH_STRATEGY = "fetchStrategy";
    private static final String ID_OUTBOUND_LABEL = "outboundLabel";
    private static final String ID_OUTBOUND_BUTTON = "outboundButton";
    private static final String ID_INBOUND = "inbound";
    private static final String ID_PASS_POLICY_LABEL = "passPolicyRefLabel";
    private static final String ID_PASS_POLICY_BUTTON = "passPolicyRefButton";

    public ResourceCredentialsEditor(String id, IModel<ResourceCredentialsDefinitionType> model){
        super(id, model);
    }

    @Override
    public IModel<ResourceCredentialsDefinitionType> getModel() {
        IModel<ResourceCredentialsDefinitionType> model = super.getModel();

        if(model.getObject() == null){
            model.setObject(new ResourceCredentialsDefinitionType());
        }

        if(model.getObject().getPassword() == null){
            model.getObject().setPassword(new ResourcePasswordDefinitionType());
        }

        return model;
    }

    @Override
    protected void initLayout(){
        DropDownChoice fetchStrategy = new DropDownChoice<>(ID_FETCH_STRATEGY,
                new PropertyModel<AttributeFetchStrategyType>(getModel(), "password.fetchStrategy"),
                WebMiscUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
                new EnumChoiceRenderer<AttributeFetchStrategyType>(this));
        add(fetchStrategy);

        //TODO - what should we display as Mapping label?
        TextField outboundLabel = new TextField<>(ID_OUTBOUND_LABEL,
                new PropertyModel<String>(getModel(), "password.outbound.name"));
        outboundLabel.setEnabled(false);
        add(outboundLabel);

        AjaxLink outbound = new AjaxLink(ID_OUTBOUND_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                outboundEditPerformed(target);
            }
        };
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<List<MappingType>>(getModel(), "password.inbound"), false, true){

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

        //TODO - load real name of PassPolicy from ObjectReference as model of passPolicyLabel
        TextField passPolicyLabel = new TextField<>(ID_PASS_POLICY_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceCredentialsDefinitionType credentials = (ResourceCredentialsDefinitionType)getModelObject();

                if(credentials != null && credentials.getPassword() != null
                        && credentials.getPassword().getPasswordPolicyRef() != null){
                    return credentials.getPassword().getPasswordPolicyRef().getOid();
                } else {
                    return null;
                }
            }
        });
        passPolicyLabel.setEnabled(false);
        add(passPolicyLabel);

        AjaxLink passPolicyButton = new AjaxLink(ID_PASS_POLICY_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                passwordPolicyRefEditPerformed(target);
            }
        };
        add(passPolicyButton);
    }

    private void outboundEditPerformed(AjaxRequestTarget target){
        //TODO - implement this
    }

    private void mappingEditPerformed(AjaxRequestTarget target, MappingType mapping){
        //TODO - implement (open ModalWindow here - MappingType editor)
    }

    private void passwordPolicyRefEditPerformed(AjaxRequestTarget target){
        //TODO - implement (open ModalWindow here - object chooser for PasswordPolicyType)
    }
}
