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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.MappingEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class ResourceCredentialsEditor extends SimplePanel<ResourceCredentialsDefinitionType>{

    private static final Trace LOGGER = TraceManager.getTrace(ResourceCredentialsEditor.class);

    private static final String DOT_CLASS = ResourceCredentialsEditor.class.getName() + ".";
    private static final String OPERATION_LOAD_PASSWORD_POLICIES = DOT_CLASS + "createPasswordPolicyList";

    private static final String ID_FETCH_STRATEGY = "fetchStrategy";
    private static final String ID_OUTBOUND_LABEL = "outboundLabel";
    private static final String ID_OUTBOUND_BUTTON = "outboundButton";
    private static final String ID_INBOUND = "inbound";
    private static final String ID_PASS_POLICY = "passPolicy";
    private static final String ID_MODAL_MAPPING = "mappingEditor";
    private static final String ID_T_FETCH = "fetchStrategyTooltip";
    private static final String ID_T_OUT = "outboundTooltip";
    private static final String ID_T_IN = "inboundTooltip";
    private static final String ID_T_PASS_POLICY = "passwordPolicyRefTooltip";

    private Map<String, String> passPolicyMap = new HashMap<>();

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

        TextField outboundLabel = new TextField<>(ID_OUTBOUND_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceCredentialsDefinitionType credentials = getModel().getObject();

                if(credentials == null || credentials.getPassword() == null){
                    return null;
                }

                return MappingTypeDto.createMappingLabel(credentials.getPassword().getOutbound(), LOGGER, getPageBase().getPrismContext(),
                        getString("MappingType.label.placeholder"), getString("MultiValueField.nameNotSpecified"));
            }
        });
        outboundLabel.setEnabled(false);
        outboundLabel.setOutputMarkupId(true);
        add(outboundLabel);

        AjaxSubmitLink outbound = new AjaxSubmitLink(ID_OUTBOUND_BUTTON) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                outboundEditPerformed(target);
            }
        };
        outbound.setOutputMarkupId(true);
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<List<MappingType>>(getModel(), "password.inbound"), false, true){

            @Override
            protected IModel<String> createTextModel(final IModel<MappingType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        return MappingTypeDto.createMappingLabel(model.getObject(), LOGGER, getPageBase().getPrismContext(),
                                getString("MappingType.label.placeholder"), getString("MultiValueField.nameNotSpecified"));
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
        inbound.setOutputMarkupId(true);
        add(inbound);

        DropDownChoice passwordPolicy = new DropDownChoice<>(ID_PASS_POLICY,
                new PropertyModel<ObjectReferenceType>(getModel(), "password.passwordPolicyRef"),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                        return createPasswordPolicyList();
                    }
                }, new IChoiceRenderer<ObjectReferenceType>() {

            @Override
            public Object getDisplayValue(ObjectReferenceType object) {
                return passPolicyMap.get(object.getOid());
            }

            @Override
            public String getIdValue(ObjectReferenceType object, int index) {
                return Integer.toString(index);
            }
        });
        add(passwordPolicy);

        Label fetchTooltip = new Label(ID_T_FETCH);
        fetchTooltip.add(new InfoTooltipBehavior());
        add(fetchTooltip);

        Label outTooltip = new Label(ID_T_OUT);
        outTooltip.add(new InfoTooltipBehavior());
        add(outTooltip);

        Label inTooltip = new Label(ID_T_IN);
        inTooltip.add(new InfoTooltipBehavior());
        add(inTooltip);

        Label passPolicyTooltip = new Label(ID_T_PASS_POLICY);
        passPolicyTooltip.add(new InfoTooltipBehavior());
        add(passPolicyTooltip);

        initModals();
    }

    private void initModals(){
        ModalWindow mappingEditor = new MappingEditorDialog(ID_MODAL_MAPPING, null){

            @Override
            public void updateComponents(AjaxRequestTarget target) {
                target.add(ResourceCredentialsEditor.this.get(ID_INBOUND), ResourceCredentialsEditor.this.get(ID_OUTBOUND_BUTTON),
                        ResourceCredentialsEditor.this.get(ID_OUTBOUND_LABEL));
            }
        };
        add(mappingEditor);
    }

    private List<ObjectReferenceType> createPasswordPolicyList(){
        passPolicyMap.clear();
        OperationResult result = new OperationResult(OPERATION_LOAD_PASSWORD_POLICIES);
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_PASSWORD_POLICIES);
        List<PrismObject<ValuePolicyType>> policies = null;
        List<ObjectReferenceType> references = new ArrayList<>();

        try{
            policies = getPageBase().getModelService().searchObjects(ValuePolicyType.class, new ObjectQuery(), null, task, result);
            result.recomputeStatus();
        } catch (Exception e){
            result.recordFatalError("Couldn't load password policies.", e);
            LoggingUtils.logException(LOGGER, "Couldn't load password policies", e);
        }

        // TODO - show error somehow
        // if(!result.isSuccess()){
        //    getPageBase().showResult(result);
        // }

        if(policies != null){
            ObjectReferenceType ref;

            for(PrismObject<ValuePolicyType> policy: policies){
                passPolicyMap.put(policy.getOid(), WebMiscUtil.getName(policy));
                ref = new ObjectReferenceType();
                ref.setType(ValuePolicyType.COMPLEX_TYPE);
                ref.setOid(policy.getOid());
                references.add(ref);
            }
        }

        return references;
    }

    private void outboundEditPerformed(AjaxRequestTarget target){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_MAPPING);
        window.updateModel(target, new PropertyModel<MappingType>(getModel(), "password.outbound"));
        window.show(target);
    }

    private void mappingEditPerformed(AjaxRequestTarget target, MappingType mapping){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_MAPPING);
        window.updateModel(target, mapping);
        window.show(target);
    }
}
