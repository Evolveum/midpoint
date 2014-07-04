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
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypeDialog;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityPanel extends SimplePanel{

    //private Trace LOGGER = TraceManager.getTrace(CapabilityPanel.class);

    public static ArrayList<CapabilityDto> capabilities;
    static{
        capabilities = new ArrayList<>(Arrays.asList(
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
    private static final String ID_CAPABILITY_EDIT = "capabilityEdit";
    private static final String ID_CAPABILITY_ADD = "capabilityAdd";
    private static final String ID_CAPABILITY_CONFIG = "capabilityConfig";

    private static final String DIALOG_SELECT_CAPABILITY = "capabilitySelectPopup";

//    private IModel<PrismObject<ResourceType>> model;
    private IModel<CapabilityStepDto> model;

    public CapabilityPanel(String componentId, IModel<PrismObject<ResourceType>> prismModel){
        super(componentId, prismModel);
    }

    private CapabilityStepDto loadModel(){
//        PrismObject<ResourceType> resourcePrism = (PrismObject<ResourceType>)getModel().getObject();
//        ResourceType resource = resourcePrism.asObjectable();
//        CapabilityStepDto dto = new CapabilityStepDto();
//
//        dto.setCapabilities(resource.getCapabilities().getNative().getAny());
//        dto.getCapabilities().addAll(resource.getCapabilities().getConfigured().getAny());

        CapabilityStepDto dto = new CapabilityStepDto();
        //TODO - remove this cloning when capabilities will be loaded from resource
        dto.setCapabilities((List<CapabilityDto>) capabilities.clone());

        //TODO - Preparing some mock object, remove this when finished
        ScriptCapabilityType script = (ScriptCapabilityType)dto.getCapabilities().get(8).getCapability();
        ScriptCapabilityType.Host onConnectorHost = new ScriptCapabilityType.Host();
        onConnectorHost.setType(ProvisioningScriptHostType.CONNECTOR);
        onConnectorHost.getLanguage().add("java");
        onConnectorHost.getLanguage().add("sql");

        ScriptCapabilityType.Host onResourceHost = new ScriptCapabilityType.Host();
        onResourceHost.setType(ProvisioningScriptHostType.RESOURCE);
        onResourceHost.getLanguage().add("erlang");
        onResourceHost.getLanguage().add("javascript");
        onResourceHost.getLanguage().add("groovy");
        script.getHost().add(onConnectorHost);
        script.getHost().add(onResourceHost);

        ActivationCapabilityType activation = (ActivationCapabilityType)dto.getCapabilities().get(6).getCapability();
        ActivationStatusCapabilityType enableDisable = new ActivationStatusCapabilityType();
        enableDisable.setIgnoreAttribute(true);
        enableDisable.getEnableValue().addAll(new ArrayList<>(Arrays.asList("4","5","6")));
        enableDisable.getDisableValue().addAll(new ArrayList<>(Arrays.asList("1","2","3")));
        activation.setEnableDisable(enableDisable);
        activation.setEnabled(true);

        ActivationValidityCapabilityType validFrom = new ActivationValidityCapabilityType();
        validFrom.setReturnedByDefault(true);
        activation.setValidFrom(validFrom);

        return dto;
    }

    @Override
    protected void initLayout(){

        model = new LoadableModel<CapabilityStepDto>() {

            @Override
            protected CapabilityStepDto load() {
                return loadModel();
            }
        };

        final ListDataProvider<CapabilityDto> capabilityProvider = new ListDataProvider<>(this,
                new PropertyModel<List<CapabilityDto>>(model.getObject(), CapabilityStepDto.F_CAPABILITIES));

        WebMarkupContainer tableBody = new WebMarkupContainer(ID_CAPABILITY_TABLE);
        tableBody.setOutputMarkupId(true);
        add(tableBody);

        WebMarkupContainer configBody = new WebMarkupContainer(ID_CAPABILITY_CONFIG);
        configBody.setOutputMarkupId(true);
        add(configBody);

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

                AjaxLink editLink = new AjaxLink(ID_CAPABILITY_EDIT) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editCapabilityPerformed(target, dto);
                    }
                };
                capabilityRow.add(editLink);

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

        ModalWindow dialog = new AddCapabilityDialog(DIALOG_SELECT_CAPABILITY, model){

            @Override
            protected void addPerformed(AjaxRequestTarget target){
                addCapabilitiesPerformed(target, getSelectedData());
            }
        };
        add(dialog);
    }

    private WebMarkupContainer getTable(){
        return (WebMarkupContainer)get(ID_CAPABILITY_TABLE);
    }

    private WebMarkupContainer getConfigContainer(){
        return (WebMarkupContainer)get(ID_CAPABILITY_CONFIG);
    }

    private void deleteCapabilityPerformed(AjaxRequestTarget target, CapabilityDto rowModel){
        for(CapabilityDto dto: model.getObject().getCapabilities()){
            dto.setSelected(false);
        }

        model.getObject().getCapabilities().remove(rowModel);
        target.add(getConfigContainer().replaceWith(new WebMarkupContainer(ID_CAPABILITY_CONFIG)));
        target.add(getTable());
    }

    private void addCapabilitiesPerformed(AjaxRequestTarget target, List<CapabilityDto> selected){
        for(CapabilityDto dto: selected){
            dto.setSelected(false);
            model.getObject().getCapabilities().add(dto);
        }
        target.add(getTable());
        AddCapabilityDialog window = (AddCapabilityDialog)get(DIALOG_SELECT_CAPABILITY);
        window.close(target);
    }

    private void addCapabilityPerformed(AjaxRequestTarget target){
        AddCapabilityDialog window = (AddCapabilityDialog)get(DIALOG_SELECT_CAPABILITY);
        window.updateTable(target, model);
        window.show(target);
    }

    private void editCapabilityPerformed(AjaxRequestTarget target, CapabilityDto capability){
        for(CapabilityDto dto: model.getObject().getCapabilities()){
            dto.setSelected(false);
        }

        WebMarkupContainer config = getConfigContainer();
        WebMarkupContainer newConfig;
        CapabilityType capType = capability.getCapability();

        if(capType instanceof ActivationCapabilityType){
            newConfig = new CapabilityActivationPanel(ID_CAPABILITY_CONFIG, new Model<>(capability));
        } else if(capType instanceof ScriptCapabilityType){
            newConfig = new CapabilityScriptPanel(ID_CAPABILITY_CONFIG, new Model<>(capability));
        } else if(capType instanceof CredentialsCapabilityType){
            newConfig = new CapabilityCredentialsPanel(ID_CAPABILITY_CONFIG, new Model<>(capability));
        } else {
            newConfig = new CapabilityValuePanel(ID_CAPABILITY_CONFIG, new Model<>(capability));
         }

        newConfig.setOutputMarkupId(true);
        config.replaceWith(newConfig);

        target.add(newConfig);
        capability.setSelected(true);
        target.add(getTable());
    }
}
