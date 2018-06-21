/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.wizard.WizardUtil;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.MappingEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 *  @author shood
 * */
public class ResourceActivationEditor extends BasePanel<ResourceActivationDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceActivationEditor.class);

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
    private static final String ID_MODAL_MAPPING = "mappingEditor";
    private static final String ID_T_EX_FETCH = "existenceFetchStrategyTooltip";
    private static final String ID_T_EX_OUT = "existenceOutboundTooltip";
    private static final String ID_T_EX_IN = "existenceInboundTooltip";
    private static final String ID_T_ADM_FETCH = "admStatusFetchStrategyTooltip";
    private static final String ID_T_ADM_OUT = "admStatusOutboundTooltip";
    private static final String ID_T_ADM_IN = "admStatusInboundTooltip";
    private static final String ID_T_VALID_F_FETCH = "validFromFetchStrategyTooltip";
    private static final String ID_T_VALID_F_OUT = "validFromOutboundTooltip";
    private static final String ID_T_VALID_F_IN = "validFromInboundTooltip";
    private static final String ID_T_VALID_T_FETCH = "validToFetchStrategyTooltip";
    private static final String ID_T_VALID_T_OUT = "validToOutboundTooltip";
    private static final String ID_T_VALID_T_IN = "validToInboundTooltip";

    //Default mapping inbound/outbound sources/targets
    public static final String EXISTENCE_DEFAULT_SOURCE = "&" + ExpressionConstants.VAR_LEGAL.getLocalPart();
    public static final String ADM_STATUS_OUT_SOURCE_DEFAULT = "&" + ExpressionConstants.VAR_FOCUS.getLocalPart() + "/activation/administrativeStatus";
    public static final String ADM_STATUS_OUT_TARGET_DEFAULT = "&" + ExpressionConstants.VAR_PROJECTION.getLocalPart() + "/activation/administrativeStatus";
    public static final String ADM_STATUS_IN_SOURCE_DEFAULT = "&" + ExpressionConstants.VAR_PROJECTION.getLocalPart() + "/activation/administrativeStatus";
    public static final String ADM_STATUS_IN_TARGET_DEFAULT = "&" + ExpressionConstants.VAR_FOCUS.getLocalPart() + "/activation/administrativeStatus";
    public static final String VALID_TO_OUT_SOURCE_DEFAULT = "&" + ExpressionConstants.VAR_FOCUS.getLocalPart() + "/activation/validTo";
    public static final String VALID_TO_OUT_TARGET_DEFAULT = "&" + ExpressionConstants.VAR_PROJECTION.getLocalPart() + "/activation/validTo";
    public static final String VALID_TO_IN_SOURCE_DEFAULT = "&" + ExpressionConstants.VAR_PROJECTION.getLocalPart() + "/activation/validTo";
    public static final String VALID_TO_IN_TARGET_DEFAULT = "&" + ExpressionConstants.VAR_FOCUS.getLocalPart() + "/activation/validTo";
    public static final String VALID_FROM_OUT_SOURCE_DEFAULT = "&" + ExpressionConstants.VAR_FOCUS.getLocalPart() + "/activation/validFrom";
    public static final String VALID_FROM_OUT_TARGET_DEFAULT = "&" + ExpressionConstants.VAR_PROJECTION.getLocalPart() + "/activation/validFrom";
    public static final String VALID_FROM_IN_SOURCE_DEFAULT = "&" + ExpressionConstants.VAR_PROJECTION.getLocalPart() + "/activation/validFrom";
    public static final String VALID_FROM_IN_TARGET_DEFAULT = "&" + ExpressionConstants.VAR_FOCUS.getLocalPart() + "/activation/validFrom";

    private boolean isInitialized = false;

    public ResourceActivationEditor(String id, IModel<ResourceActivationDefinitionType> model, NonEmptyModel<Boolean> readOnlyModel) {
        super(id, model);
		initLayout(readOnlyModel);
    }

    @Override
    public IModel<ResourceActivationDefinitionType> getModel() {
        IModel<ResourceActivationDefinitionType> activationModel = super.getModel();

        if(activationModel.getObject() == null){
            activationModel.setObject(new ResourceActivationDefinitionType());
        }

        if(!isInitialized){
            prepareActivationObject(activationModel.getObject());
            isInitialized = true;
        }

        return activationModel;
    }

    private void prepareActivationObject(ResourceActivationDefinitionType activation){
        if(activation.getExistence() == null){
            activation.setExistence(new ResourceBidirectionalMappingType());
        } else {
            for(MappingType mapping: activation.getExistence().getInbound()){
                if(mapping.equals(new MappingType())){
                	VariableBindingDefinitionType source = new VariableBindingDefinitionType();
                    source.setPath(new ItemPathType(EXISTENCE_DEFAULT_SOURCE));
                    mapping.getSource().add(source);
                }
            }
        }

        if(activation.getAdministrativeStatus() == null){
            activation.setAdministrativeStatus(new ResourceBidirectionalMappingType());
        } else {
            for(MappingType outbound: activation.getAdministrativeStatus().getOutbound()){
                if(outbound.equals(new MappingType())){
                	VariableBindingDefinitionType source = new VariableBindingDefinitionType();
                    source.setPath(new ItemPathType(ADM_STATUS_OUT_SOURCE_DEFAULT));
                    outbound.getSource().add(source);

                    VariableBindingDefinitionType target = new VariableBindingDefinitionType();
                    target.setPath(new ItemPathType(ADM_STATUS_OUT_TARGET_DEFAULT));
                    outbound.setTarget(target);
                }
            }

            for(MappingType inbound: activation.getAdministrativeStatus().getInbound()){
                if(inbound.equals(new MappingType())){
                    VariableBindingDefinitionType source = new VariableBindingDefinitionType();
                    source.setPath(new ItemPathType(ADM_STATUS_IN_SOURCE_DEFAULT));
                    inbound.getSource().add(source);

                    VariableBindingDefinitionType target = new VariableBindingDefinitionType();
                    target.setPath(new ItemPathType(ADM_STATUS_IN_TARGET_DEFAULT));
                    inbound.setTarget(target);
                }
            }
        }

        if(activation.getValidFrom() == null){
            activation.setValidFrom(new ResourceBidirectionalMappingType());
        } else {
            for(MappingType outbound: activation.getValidFrom().getOutbound()){
                if(outbound.equals(new MappingType())){
                    VariableBindingDefinitionType source = new VariableBindingDefinitionType();
                    source.setPath(new ItemPathType(VALID_FROM_OUT_SOURCE_DEFAULT));
                    outbound.getSource().add(source);

                    VariableBindingDefinitionType target = new VariableBindingDefinitionType();
                    target.setPath(new ItemPathType(VALID_FROM_OUT_TARGET_DEFAULT));
                    outbound.setTarget(target);
                }
            }

            for(MappingType inbound: activation.getValidFrom().getInbound()){
                if(inbound.equals(new MappingType())){
                    VariableBindingDefinitionType source = new VariableBindingDefinitionType();
                    source.setPath(new ItemPathType(VALID_FROM_IN_SOURCE_DEFAULT));
                    inbound.getSource().add(source);

                    VariableBindingDefinitionType target = new VariableBindingDefinitionType();
                    target.setPath(new ItemPathType(VALID_FROM_IN_TARGET_DEFAULT));
                    inbound.setTarget(target);
                }
            }
        }

        if(activation.getValidTo() == null){
            activation.setValidTo(new ResourceBidirectionalMappingType());
        } else {
            for(MappingType outbound: activation.getValidTo().getOutbound()){
                if(outbound.equals(new MappingType())){
                    VariableBindingDefinitionType source = new VariableBindingDefinitionType();
                    source.setPath(new ItemPathType(VALID_TO_OUT_SOURCE_DEFAULT));
                    outbound.getSource().add(source);

                    VariableBindingDefinitionType target = new VariableBindingDefinitionType();
                    target.setPath(new ItemPathType(VALID_TO_OUT_TARGET_DEFAULT));
                    outbound.setTarget(target);
                }
            }

            for(MappingType inbound: activation.getValidTo().getInbound()){
                if(inbound.equals(new MappingType())){
                    VariableBindingDefinitionType source = new VariableBindingDefinitionType();
                    source.setPath(new ItemPathType(VALID_TO_IN_SOURCE_DEFAULT));
                    inbound.getSource().add(source);

                    VariableBindingDefinitionType target = new VariableBindingDefinitionType();
                    target.setPath(new ItemPathType(VALID_TO_IN_TARGET_DEFAULT));
                    inbound.setTarget(target);
                }
            }
        }
    }

    protected void initLayout(NonEmptyModel<Boolean> readOnlyModel) {
        prepareActivationPanelBody(ResourceActivationDefinitionType.F_EXISTENCE.getLocalPart(), ID_EXISTENCE_FS,
                ID_EXISTENCE_OUT, ID_EXISTENCE_IN, readOnlyModel);

        prepareActivationPanelBody(ResourceActivationDefinitionType.F_ADMINISTRATIVE_STATUS.getLocalPart(), ID_ADM_STATUS_FS,
                ID_ADM_STATUS_OUT, ID_ADM_STATUS_IN, readOnlyModel);

        prepareActivationPanelBody(ResourceActivationDefinitionType.F_VALID_FROM.getLocalPart(), ID_VALID_FROM_FS,
                ID_VALID_FROM_OUT, ID_VALID_FROM_IN, readOnlyModel);

        prepareActivationPanelBody(ResourceActivationDefinitionType.F_VALID_TO.getLocalPart(), ID_VALID_TO_FS,
                ID_VALID_TO_OUT, ID_VALID_TO_IN, readOnlyModel);

        Label exFetchTooltip = new Label(ID_T_EX_FETCH);
        exFetchTooltip.add(new InfoTooltipBehavior());
        add(exFetchTooltip);

        Label exOutTooltip = new Label(ID_T_EX_OUT);
        exOutTooltip.add(new InfoTooltipBehavior());
        add(exOutTooltip);

        Label exInTooltip = new Label(ID_T_EX_IN);
        exInTooltip.add(new InfoTooltipBehavior());
        add(exInTooltip);

        Label admFetchTooltip = new Label(ID_T_ADM_FETCH);
        admFetchTooltip.add(new InfoTooltipBehavior());
        add(admFetchTooltip);

        Label admOutTooltip = new Label(ID_T_ADM_OUT);
        admOutTooltip.add(new InfoTooltipBehavior());
        add(admOutTooltip);

        Label admInTooltip = new Label(ID_T_ADM_IN);
        admInTooltip.add(new InfoTooltipBehavior());
        add(admInTooltip);

        Label validFromFetchTooltip = new Label(ID_T_VALID_F_FETCH);
        validFromFetchTooltip.add(new InfoTooltipBehavior());
        add(validFromFetchTooltip);

        Label validFromOutTooltip = new Label(ID_T_VALID_F_OUT);
        validFromOutTooltip.add(new InfoTooltipBehavior());
        add(validFromOutTooltip);

        Label validFromInTooltip = new Label(ID_T_VALID_F_IN);
        validFromInTooltip.add(new InfoTooltipBehavior());
        add(validFromInTooltip);

        Label validToFetchTooltip = new Label(ID_T_VALID_T_FETCH);
        validToFetchTooltip.add(new InfoTooltipBehavior());
        add(validToFetchTooltip);

        Label validToOutTooltip = new Label(ID_T_VALID_T_OUT);
        validToOutTooltip.add(new InfoTooltipBehavior());
        add(validToOutTooltip);

        Label validToInTooltip = new Label(ID_T_VALID_T_IN);
        validToInTooltip.add(new InfoTooltipBehavior());
        add(validToInTooltip);

        initModals(readOnlyModel);
    }

    private void prepareActivationPanelBody(String containerValue, String fetchStrategyId, String outboundId, String inboundId,
			NonEmptyModel<Boolean> readOnlyModel){
        DropDownChoice fetchStrategy = new DropDownChoice<>(fetchStrategyId,
            new PropertyModel<>(getModel(), containerValue + ".fetchStrategy"),
                WebComponentUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
            new EnumChoiceRenderer<>(this));
        fetchStrategy.setNullValid(true);
		fetchStrategy.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(fetchStrategy);

		MultiValueTextEditPanel outbound = new MultiValueTextEditPanel<MappingType>(outboundId,
            new PropertyModel<>(getModel(), containerValue + ".outbound"), null, false, true,
				readOnlyModel) {

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
                mappingEditPerformed(target, object, false);
            }
        };
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(inboundId,
            new PropertyModel<>(getModel(), containerValue + ".inbound"), null, false, true, readOnlyModel) {

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
                mappingEditPerformed(target, object, true);
            }
        };
        inbound.setOutputMarkupId(true);
        add(inbound);
    }

    private void initModals(NonEmptyModel<Boolean> readOnlyModel) {
        ModalWindow mappingEditor = new MappingEditorDialog(ID_MODAL_MAPPING, null, readOnlyModel) {

            @Override
            public void updateComponents(AjaxRequestTarget target){
                target.add(ResourceActivationEditor.this);
            }
        };
        add(mappingEditor);
    }

    private void mappingEditPerformed(AjaxRequestTarget target, MappingType mapping, boolean isInbound){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_MAPPING);
        window.updateModel(target, mapping, isInbound);
        window.show(target);
    }
}
