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
package com.evolveum.midpoint.web.component.wizard.resource.component.capability;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityPanel extends SimplePanel{

    //private Trace LOGGER = TraceManager.getTrace(CapabilityPanel.class);

    private static ArrayList<CapabilityDto> capabilities;
    static{
        capabilities = new ArrayList<CapabilityDto>(Arrays.asList(
                new CapabilityDto(new ReadCapabilityType(), "Read", true),
                new CapabilityDto(new UpdateCapabilityType(), "Update", true),
                new CapabilityDto(new CreateCapabilityType(), "Create", true),
                new CapabilityDto(new DeleteCapabilityType(), "Delete", true),
                new CapabilityDto(new LiveSyncCapabilityType(), "Live Sync", true),
                new CapabilityDto(new TestConnectionCapabilityType(), "Test Connection", true),
                new CapabilityDto(new ActivationCapabilityType(), "Activation", true),
                new CapabilityDto(new CredentialsCapabilityType(), "Credentials", true),
                new CapabilityDto(new ScriptCapabilityType(), "Script", true)
        ));
    }

    private static final String ID_CAPABILITY_TABLE = "tableRows";
    private static final String ID_CAPABILITY_ROW = "capabilityRow";
    private static final String ID_CAPABILITY_NAME = "capabilityName";
    private static final String ID_CAPABILITY_DELETE = "capabilityDelete";
    private static final String ID_CAPABILITY_ADD = "capabilityAdd";

    private static final String DIALOG_SELECT_CAPABILITY = "capabilitySelectPopup";

//    private IModel<PrismObject<ResourceType>> model;
    private CapabilityStepDto model;

    public CapabilityPanel(String componentId, IModel<PrismObject<ResourceType>> model){
        super(componentId, model);
    }

    private CapabilityStepDto loadModel(){
//        PrismObject<ResourceType> resourcePrism = (PrismObject<ResourceType>)getModel().getObject();
//        ResourceType resource = resourcePrism.asObjectable();
//        CapabilityStepDto dto = new CapabilityStepDto();
//
//        dto.setCapabilities(resource.getCapabilities().getNative().getAny());
//        dto.getCapabilities().addAll(resource.getCapabilities().getConfigured().getAny());

        CapabilityStepDto dto = new CapabilityStepDto();
        dto.setCapabilities(capabilities);

        return dto;
    }

    @Override
    protected void initLayout(){
        this.model = loadModel();

        final ListDataProvider<CapabilityDto> capabilityProvider = new ListDataProvider<CapabilityDto>(this,
                new PropertyModel<List<CapabilityDto>>(model, CapabilityStepDto.F_CAPABILITIES));

        WebMarkupContainer tableBody = new WebMarkupContainer(ID_CAPABILITY_TABLE);
        tableBody.setOutputMarkupId(true);
        add(tableBody);

        DataView<CapabilityDto> capabilityDataView = new DataView<CapabilityDto>(ID_CAPABILITY_ROW, capabilityProvider) {

            @Override
            protected void populateItem(final Item<CapabilityDto> capabilityRow) {
                final CapabilityDto dto = capabilityRow.getModelObject();

                Label label = new Label(ID_CAPABILITY_NAME, new PropertyModel<>(dto, CapabilityDto.F_VALUE));
                capabilityRow.add(label);

                AjaxLink deleteLink = new AjaxLink(ID_CAPABILITY_DELETE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteCapabilityPerformed(target, dto);
                    }
                };
                capabilityRow.add(deleteLink);

                capabilityRow.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
                        if(capabilityRow.getModelObject().isSelected()){
                            return "success";
                        }

                        return null;
                    }
                }));
            }
        };
        tableBody.add(capabilityDataView);

        AjaxLink addLink = new AjaxLink(ID_CAPABILITY_ADD) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                addCapabilityPerformed(target);
            }
        };
        add(addLink);

        //TODO - continue here in 2.4
//        ModalWindow dialog = new ChooseTypeDialog<CapabilityDto>(DIALOG_SELECT_CAPABILITY, CapabilityDto.class){
//
//            @Override
//            protected void chooseOperationPerformed(AjaxRequestTarget target, CapabilityDto capability){
//                choosePerformed(target, capability);
//            }
//
//            @Override
//            public BaseSortableDataProvider getDataProvider(){
//                return capabilityProvider;
//            }
//        };
//        add(dialog);
    }

    private void deleteCapabilityPerformed(AjaxRequestTarget target, CapabilityDto rowModel){}

    private void addCapabilityPerformed(AjaxRequestTarget target){
//        ModalWindow window = (ModalWindow)get(DIALOG_SELECT_CAPABILITY);
//        window.show(target);
    }

    private void choosePerformed(AjaxRequestTarget target, CapabilityDto capability){}
}
