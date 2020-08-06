/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
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

/**
 * @author shood
 */
public class ResourceCredentialsEditor extends BasePanel<ResourceCredentialsDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceCredentialsEditor.class);

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

    private final Map<String, String> passPolicyMap = new HashMap<>();

    public ResourceCredentialsEditor(String id, IModel<ResourceCredentialsDefinitionType> model, PageResourceWizard parentPage) {
        super(id, model);
        initLayout(parentPage);
    }

    @Override
    public IModel<ResourceCredentialsDefinitionType> getModel() {
        IModel<ResourceCredentialsDefinitionType> model = super.getModel();

        if (model.getObject() == null) {
            model.setObject(new ResourceCredentialsDefinitionType());
        }

        if (model.getObject().getPassword() == null) {
            model.getObject().setPassword(new ResourcePasswordDefinitionType());
        }

        return model;
    }

    protected void initLayout(final PageResourceWizard parentPage) {
        DropDownChoice<?> fetchStrategy = new DropDownChoice<>(ID_FETCH_STRATEGY,
                new PropertyModel<>(getModel(), "password.fetchStrategy"),
                WebComponentUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
                new EnumChoiceRenderer<>(this));
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
        TextField<?> outboundLabel = new TextField<>(ID_OUTBOUND_LABEL, (IModel<String>) () -> {
            ResourceCredentialsDefinitionType credentials = getModel().getObject();

            if (credentials == null || credentials.getPassword() == null) {
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
        });
        outboundLabel.setEnabled(false);
        outboundLabel.setOutputMarkupId(true);
        outboundLabel.add(showIfEditingOrOutboundExists);
        add(outboundLabel);

        AjaxSubmitLink outbound = new AjaxSubmitLink(ID_OUTBOUND_BUTTON) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                outboundEditPerformed(target);
            }
        };
        outbound.setOutputMarkupId(true);
        outbound.add(showIfEditingOrOutboundExists);
        add(outbound);

        MultiValueTextEditPanel<?> inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<>(getModel(), "password.inbound"), null, false, true, parentPage.getReadOnlyModel()) {

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
            protected MappingType createNewEmptyItem() {
                return WizardUtil.createEmptyMapping();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, MappingType object) {
                inboundEditPerformed(target, object);
            }
        };
        inbound.setOutputMarkupId(true);
        add(inbound);

        DropDownChoice<?> passwordPolicy = new DropDownChoice<>(ID_PASS_POLICY,
                new PropertyModel<>(getModel(), "password.passwordPolicyRef"),
                (IModel<List<ObjectReferenceType>>) () ->
                        WebModelServiceUtils.createObjectReferenceList(
                                ValuePolicyType.class, getPageBase(), passPolicyMap),
                new ObjectReferenceChoiceRenderer(passPolicyMap));
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

    private void initModals(NonEmptyModel<Boolean> readOnlyModel) {
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

    private void outboundEditPerformed(AjaxRequestTarget target) {
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

    private void inboundEditPerformed(AjaxRequestTarget target, MappingType mapping) {
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_INBOUND);
        window.updateModel(target, mapping, false);
        window.show(target);
    }
}
