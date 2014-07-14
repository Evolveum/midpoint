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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.CapabilityDto;
import com.evolveum.midpoint.web.page.PageTest2;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ObjectFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 *  @author shood
 * */
public class CapabilityPanel extends SimplePanel{

    private static final Trace LOGGER = TraceManager.getTrace(CapabilityPanel.class);

    private static final String DOT_CLASS = CapabilityPanel.class.getName() + ".";
    private static final String OPERATION_SAVE_CAPABILITIES = DOT_CLASS + "saveCapabilities";

    public static ArrayList<Class<? extends CapabilityType>> capabilities;
    static{
        capabilities = new ArrayList<>(Arrays.asList(
                ActivationCapabilityType.class,
                ScriptCapabilityType.class,
                CredentialsCapabilityType.class,
                ReadCapabilityType.class,
                CreateCapabilityType.class,
                UpdateCapabilityType.class,
                LiveSyncCapabilityType.class,
                TestConnectionCapabilityType.class,
                DeleteCapabilityType.class));
    }

    private static final String ID_CAPABILITY_TABLE = "tableRows";
    private static final String ID_CAPABILITY_ROW = "capabilityRow";
    private static final String ID_CAPABILITY_NAME = "capabilityName";
    private static final String ID_CAPABILITY_LINK = "capabilityLink";
    private static final String ID_CAPABILITY_DELETE = "capabilityDelete";
    private static final String ID_CAPABILITY_ADD = "capabilityAdd";
    private static final String ID_CAPABILITY_CONFIG = "capabilityConfig";
    private static final String ID_BUTTON_SAVE = "saveButton";

    private static final String DIALOG_SELECT_CAPABILITY = "capabilitySelectPopup";

    private IModel<CapabilityStepDto> model;
    private IModel<PrismObject<ResourceType>> resourceModel;

    public CapabilityPanel(String componentId, IModel<PrismObject<ResourceType>> prismModel){
        super(componentId, prismModel);
    }

    private CapabilityStepDto loadModel(){
        PrismObject<ResourceType> resourcePrism = (PrismObject<ResourceType>)getModel().getObject();
        ResourceType resource = resourcePrism.asObjectable();
        CapabilityStepDto dto = new CapabilityStepDto();
        List<CapabilityDto> capabilityList = loadCapabilitiesFromResource(resource);
        dto.setCapabilities(capabilityList);
        return dto;
    }

    private List<CapabilityDto> loadCapabilitiesFromResource(ResourceType resource){
        List<CapabilityDto> capabilityList = new ArrayList<>();

        try {
            List<Object> objects = ResourceTypeUtil.getEffectiveCapabilities(resource);

            for(Object capability: objects){
                JAXBElement cap = (JAXBElement)capability;
                if(cap.getValue() instanceof ReadCapabilityType){
                    capabilityList.add(new CapabilityDto((ReadCapabilityType)cap.getValue(), "Read", true));
                } else if(cap.getValue() instanceof UpdateCapabilityType){
                    capabilityList.add(new CapabilityDto((UpdateCapabilityType)cap.getValue(), "Update", true));
                } else if(cap.getValue() instanceof CreateCapabilityType){
                    capabilityList.add(new CapabilityDto((CreateCapabilityType)cap.getValue(), "Create", true));
                } else if(cap.getValue() instanceof DeleteCapabilityType){
                    capabilityList.add(new CapabilityDto((DeleteCapabilityType)cap.getValue(), "Delete", true));
                } else if(cap.getValue() instanceof LiveSyncCapabilityType){
                    capabilityList.add(new CapabilityDto((LiveSyncCapabilityType)cap.getValue(), "Live Sync", true));
                } else if(cap.getValue() instanceof TestConnectionCapabilityType){
                    capabilityList.add(new CapabilityDto((TestConnectionCapabilityType)cap.getValue(), "Test Connection", true));
                } else if(cap.getValue() instanceof ActivationCapabilityType){
                    capabilityList.add(new CapabilityDto((ActivationCapabilityType)cap.getValue(), "Activation", true));
                } else if(cap.getValue() instanceof CredentialsCapabilityType){
                    capabilityList.add(new CapabilityDto((CredentialsCapabilityType)cap.getValue(), "Credentials", true));
                } else if(cap.getValue() instanceof ScriptCapabilityType){
                    capabilityList.add(new CapabilityDto((ScriptCapabilityType)cap.getValue(), "Script", true));
                }
            }

        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Couldn't load capabilities", e);
            //TODO - show this error to the user [erik]
        }

        return capabilityList;
    }

        @Override
    protected void initLayout(){

        model = new LoadableModel<CapabilityStepDto>() {

            @Override
            protected CapabilityStepDto load() {
                return loadModel();
            }
        };

        resourceModel = new LoadableModel<PrismObject<ResourceType>>() {

            @Override
            protected PrismObject<ResourceType> load() {
                return (PrismObject<ResourceType>)getModel().getObject();
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

                AjaxLink name = new AjaxLink(ID_CAPABILITY_LINK) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editCapabilityPerformed(target, dto);
                    }
                };
                Label label = new Label(ID_CAPABILITY_NAME, new PropertyModel<>(dto, CapabilityDto.F_VALUE));
                name.add(label);
                capabilityRow.add(name);


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

        AjaxLink save = new AjaxLink(ID_BUTTON_SAVE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                savePerformed(target);
            }
        };
        add(save);

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

    private void editCapabilityPerformed(final AjaxRequestTarget target, CapabilityDto capability){
        for(CapabilityDto dto: model.getObject().getCapabilities()){
            dto.setSelected(false);
        }

        WebMarkupContainer config = getConfigContainer();
        WebMarkupContainer newConfig;
        CapabilityType capType = capability.getCapability();

        if(capType instanceof ActivationCapabilityType){
            newConfig = new CapabilityActivationPanel(ID_CAPABILITY_CONFIG, new Model<>(capability)){

                @Override
                public IModel<List<QName>> createAttributeChoiceModel(){

                    return new LoadableModel<List<QName>>(false) {
                        @Override
                        protected List<QName> load() {
                            List<QName> choices = new ArrayList<>();

                            PrismObject<ResourceType> resourcePrism = resourceModel.getObject();

                            try {
                                ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resourcePrism, CapabilityPanel.this.getPageBase().getPrismContext());
                                ObjectClassComplexTypeDefinition def = schema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);

                                for(ResourceAttributeDefinition attribute: def.getAttributeDefinitions()){
                                    choices.add(attribute.getName());
                                }

                            } catch (Exception e){
                                LoggingUtils.logException(LOGGER, "Couldn't load resource schema attributes.", e);
                                getPageBase().error("Couldn't load resource schema attributes" + e);
                                //TODO - show this to the user
                            }
                            return choices;
                        }
                    };
                }
            };
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

    private void savePerformed(AjaxRequestTarget target){
        PrismObject<ResourceType> oldResource;
        PrismObject<ResourceType> newResource = (PrismObject<ResourceType>)getModel().getObject();
        OperationResult result = new OperationResult(OPERATION_SAVE_CAPABILITIES);
        ModelService modelService = getPageBase().getModelService();
        ObjectDelta delta = null;

        try{
            ResourceType resource = newResource.asObjectable();
            JAXBElement<? extends CapabilityType> jaxbCapability;
            ObjectFactory capabilityFactory = new ObjectFactory();

            List<CapabilityDto> oldCapabilities = loadCapabilitiesFromResource(((PrismObject<ResourceType>) getModel().getObject()).asObjectable());
            List<CapabilityDto> newCapabilities = new ArrayList<>();

            for(CapabilityDto dto: model.getObject().getCapabilities()){
                if(!oldCapabilities.contains(dto)){
                    newCapabilities.add(dto);
                }
            }

            for(CapabilityDto dto: newCapabilities){
                jaxbCapability = createJAXBCapability(dto.getCapability(), capabilityFactory);

                if(jaxbCapability != null){
                    resource.getCapabilities().getConfigured().getAny().add(jaxbCapability);
                }

            }

            oldResource = WebModelUtils.loadObject(ResourceType.class, newResource.getOid(), result, getPageBase());
            if(oldResource != null){
                delta = oldResource.diff(newResource);
            }

            if(delta != null){

                if(LOGGER.isTraceEnabled()){
                    LOGGER.trace(delta.debugDump());
                }

                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                modelService.executeChanges(deltas, null, getPageBase().createSimpleTask(OPERATION_SAVE_CAPABILITIES), result);
            }

        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Couldn't save capabilities", e);
            result.recordFatalError("Couldn't save capabilities", e);
        } finally {
            result.computeStatusIfUnknown();
        }

        if(WebMiscUtil.isSuccessOrHandledError(result)){
            getPageBase().showResultInSession(result);
            setResponsePage(PageTest2.class);
        } else {
            getPageBase().showResult(result);
            target.add(getPageBase().getFeedbackPanel());
        }

    }

    private JAXBElement<? extends CapabilityType> createJAXBCapability(CapabilityType capability, ObjectFactory factory){
        if(capability instanceof CreateCapabilityType){
            return factory.createCreate((CreateCapabilityType)capability);
        } else if(capability instanceof DeleteCapabilityType){
            return factory.createDelete((DeleteCapabilityType)capability);
        } else if(capability instanceof UpdateCapabilityType){
            return factory.createUpdate((UpdateCapabilityType)capability);
        } else if(capability instanceof ReadCapabilityType){
            return factory.createRead((ReadCapabilityType)capability);
        } else if(capability instanceof LiveSyncCapabilityType){
            return factory.createLiveSync((LiveSyncCapabilityType)capability);
        } else if(capability instanceof TestConnectionCapabilityType){
            return factory.createTestConnection((TestConnectionCapabilityType)capability);
        } else if(capability instanceof CredentialsCapabilityType){
            return factory.createCredentials((CredentialsCapabilityType)capability);
        } else if(capability instanceof ScriptCapabilityType){
            return factory.createScript((ScriptCapabilityType)capability);
        } else if(capability instanceof ActivationCapabilityType){
            return factory.createActivation((ActivationCapabilityType)capability);
        }

        return null;
    }
}
