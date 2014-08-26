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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 *  @author shood
 * */
public class ResourceActivationEditor extends SimplePanel{

    private static final String ID_EXISTENCE_FS = "existenceFetchStrategy";
    private static final String ID_EXISTENCE_OUT = "existenceOutbound";
    private static final String ID_EXISTENCE_IN = "existenceInbound";
    private static final String ID_ADM_STATUS_FS = "admStatusFetchStrategy";
    private static final String ID_ADM_STATUS_OUT = "admStatusOutbound";
    private static final String ID_ADM_STATUS_IN = "admStatusInbound";
    private static final String ID_VALID_FROM_FS = "validFromFetchStrategy";
    private static final String ID_VALID_FROM_OUT = "validFromOutbound";
    private static final String ID_VALID_FROM_IN = "validFromInbound";
    private static final String ID_VALID_TO_FS = "validToFetchStrategy";
    private static final String ID_VALID_TO_OUT = "validToOutbound";
    private static final String ID_VALID_TO_IN = "validToInbound";

    public ResourceActivationEditor(String id, IModel<ResourceActivationDefinitionType> model){
        super(id, model);
    }

    @Override
    public IModel<ResourceActivationDefinitionType> getModel() {
        IModel<ResourceActivationDefinitionType> activationModel = super.getModel();

        if(activationModel.getObject() == null){
            activationModel.setObject(new ResourceActivationDefinitionType());
        }

        ResourceActivationDefinitionType activation = activationModel.getObject();

        if(activation.getExistence() == null){
            activation.setExistence(new ResourceBidirectionalMappingType());
        }

        if(activation.getAdministrativeStatus() == null){
            activation.setAdministrativeStatus(new ResourceBidirectionalMappingType());
        }

        if(activation.getValidFrom() == null){
            activation.setValidFrom(new ResourceBidirectionalMappingType());
        }

        if(activation.getValidTo() == null){
            activation.setValidTo(new ResourceBidirectionalMappingType());
        }

        return activationModel;
    }

    @Override
    protected void initLayout(){
        prepareActivationPanelBody(ResourceActivationDefinitionType.F_EXISTENCE.getLocalPart(), ID_EXISTENCE_FS,
                ID_EXISTENCE_OUT, ID_EXISTENCE_IN);

        prepareActivationPanelBody(ResourceActivationDefinitionType.F_ADMINISTRATIVE_STATUS.getLocalPart(), ID_ADM_STATUS_FS,
                ID_ADM_STATUS_OUT, ID_ADM_STATUS_IN);

        prepareActivationPanelBody(ResourceActivationDefinitionType.F_VALID_FROM.getLocalPart(), ID_VALID_FROM_FS,
                ID_VALID_FROM_OUT, ID_VALID_FROM_IN);

        prepareActivationPanelBody(ResourceActivationDefinitionType.F_VALID_TO.getLocalPart(), ID_VALID_TO_FS,
                ID_VALID_TO_OUT, ID_VALID_TO_IN);
    }

    private void prepareActivationPanelBody(String containerValue, String fetchStrategyId, String outboundId, String inboundId){
        DropDownChoice fetchStrategy = new DropDownChoice<>(fetchStrategyId,
                new PropertyModel<AttributeFetchStrategyType>(getModel(), containerValue + ".fetchStrategy"),
                WebMiscUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
                new EnumChoiceRenderer<AttributeFetchStrategyType>(this));
        add(fetchStrategy);

        MultiValueTextEditPanel outbound = new MultiValueTextEditPanel<MappingType>(outboundId,
                new PropertyModel<List<MappingType>>(getModel(), containerValue + ".outbound"), false, true){

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
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(inboundId,
                new PropertyModel<List<MappingType>>(getModel(), containerValue + ".inbound"), false, true){

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

    private void mappingEditPerformed(AjaxRequestTarget target, MappingType mapping){
        //TODO - open ModalWindow here - some MappingType editor
    }
}
