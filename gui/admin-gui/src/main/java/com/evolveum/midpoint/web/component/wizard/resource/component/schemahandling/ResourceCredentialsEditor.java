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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.input.ObjectReferenceChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardUtil;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.MappingEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
public class ResourceCredentialsEditor extends BasePanel<ResourceCredentialsDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceCredentialsEditor.class);

    private static final String DOT_CLASS = ResourceCredentialsEditor.class.getName() + ".";
    private static final String OPERATION_LOAD_PASSWORD_POLICIES = DOT_CLASS + "createPasswordPolicyList";

    private static final String ID_FETCH_STRATEGY = "fetchStrategy";
    private static final String ID_OUTBOUND_LABEL = "outboundLabel";
    private static final String ID_OUTBOUND_BUTTON = "outboundButton";
    private static final String ID_INBOUND = "inbound";
    private static final String ID_PASS_POLICY = "passPolicy";
    private static final String ID_MODAL_INBOUND = "inboundEditor";
    private static final String ID_MODAL_OUTBOUND = "outboundEditor";
    private static final String ID_T_FETCH = "fetchStrategyTooltip";
    private static final String ID_T_OUT = "outboundTooltip";
    private static final String ID_T_IN = "inboundTooltip";
    private static final String ID_T_PASS_POLICY = "passwordPolicyRefTooltip";

    final private Map<String, String> passPolicyMap = new HashMap<>();

    public ResourceCredentialsEditor(String id, IModel<ResourceCredentialsDefinitionType> model, PageResourceWizard parentPage) {
        super(id, model);
		initLayout(parentPage);
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

    protected void initLayout(final PageResourceWizard parentPage) {
        DropDownChoice fetchStrategy = new DropDownChoice<>(ID_FETCH_STRATEGY,
                new PropertyModel<AttributeFetchStrategyType>(getModel(), "password.fetchStrategy"),
                WebComponentUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
                new EnumChoiceRenderer<AttributeFetchStrategyType>(this));
		parentPage.addEditingEnabledBehavior(fetchStrategy);
        add(fetchStrategy);

		VisibleEnableBehaviour showIfEditingOrOutboundExists = new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				ResourceCredentialsDefinitionType credentials = getModel().getObject();
				if (credentials == null || credentials.getPassword() == null) {
					return !parentPage.isReadOnly();
				}
				return !parentPage.isReadOnly() || credentials.getPassword().getOutbound() != null;
			}
		};
        TextField outboundLabel = new TextField<>(ID_OUTBOUND_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceCredentialsDefinitionType credentials = getModel().getObject();

                if(credentials == null || credentials.getPassword() == null){
                    return null;
                }
                // TODO: support multiple password mappings
                MappingType outbound = null;
                List<MappingType> outbounds = credentials.getPassword().getOutbound();
                if (!outbounds.isEmpty()) {
                	outbound = outbounds.get(0);
                }
                return MappingTypeDto.createMappingLabel(outbound, LOGGER, getPageBase().getPrismContext(),
                        getString("MappingType.label.placeholder"), getString("MultiValueField.nameNotSpecified"));
            }
        });
        outboundLabel.setEnabled(false);
        outboundLabel.setOutputMarkupId(true);
		outboundLabel.add(showIfEditingOrOutboundExists);
        add(outboundLabel);

        AjaxSubmitLink outbound = new AjaxSubmitLink(ID_OUTBOUND_BUTTON) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                outboundEditPerformed(target);
            }
        };
        outbound.setOutputMarkupId(true);
		outbound.add(showIfEditingOrOutboundExists);
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<List<MappingType>>(getModel(), "password.inbound"), null, false, true, parentPage.getReadOnlyModel()) {

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
                return WizardUtil.createEmptyMapping();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, MappingType object){
                inboundEditPerformed(target, object);
            }
        };
        inbound.setOutputMarkupId(true);
        add(inbound);

        DropDownChoice passwordPolicy = new DropDownChoice<>(ID_PASS_POLICY,
                new PropertyModel<ObjectReferenceType>(getModel(), "password.passwordPolicyRef"),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                    	return WebModelServiceUtils.createObjectReferenceList(ValuePolicyType.class, getPageBase(), passPolicyMap);

                    }
                }, new ObjectReferenceChoiceRenderer(passPolicyMap));
		parentPage.addEditingEnabledBehavior(passwordPolicy);
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

        initModals(parentPage.getReadOnlyModel());
    }

    private void initModals(NonEmptyModel<Boolean> readOnlyModel){
        ModalWindow inboundEditor = new MappingEditorDialog(ID_MODAL_INBOUND, null, readOnlyModel) {

            @Override
            public void updateComponents(AjaxRequestTarget target) {
                target.add(ResourceCredentialsEditor.this.get(ID_INBOUND));
            }
        };
        add(inboundEditor);

        ModalWindow outboundEditor = new MappingEditorDialog(ID_MODAL_OUTBOUND, null, readOnlyModel) {

            @Override
            public void updateComponents(AjaxRequestTarget target) {
                target.add(ResourceCredentialsEditor.this.get(ID_OUTBOUND_LABEL), ResourceCredentialsEditor.this.get(ID_OUTBOUND_BUTTON));
            }
        };
        add(outboundEditor);
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
        } catch (CommonException |RuntimeException e) {
            result.recordFatalError("Couldn't load password policies.", e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load password policies", e);
        }

        // TODO - show error somehow
        // if(!result.isSuccess()){
        //    getPageBase().showResult(result);
        // }

        if(policies != null){
            ObjectReferenceType ref;

            for(PrismObject<ValuePolicyType> policy: policies){
                passPolicyMap.put(policy.getOid(), WebComponentUtil.getName(policy));
                ref = new ObjectReferenceType();
                ref.setType(ValuePolicyType.COMPLEX_TYPE);
                ref.setOid(policy.getOid());
                references.add(ref);
            }
        }

        return references;
    }

    private void outboundEditPerformed(AjaxRequestTarget target){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_OUTBOUND);
        IModel<ResourceCredentialsDefinitionType> model = getModel();
        List<MappingType> outboundMappingList = model == null ? null :
                (model.getObject() == null ? null :
                        (model.getObject().getPassword() == null ? null : model.getObject().getPassword().getOutbound()));
        MappingType mapping = outboundMappingList != null && outboundMappingList.size() > 0 ?
                outboundMappingList.get(0) : null;
        window.updateModel(target, mapping, false);
        window.show(target);
    }

    private void inboundEditPerformed(AjaxRequestTarget target, MappingType mapping){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_INBOUND);
        window.updateModel(target, mapping, false);
        window.show(target);
    }
}
