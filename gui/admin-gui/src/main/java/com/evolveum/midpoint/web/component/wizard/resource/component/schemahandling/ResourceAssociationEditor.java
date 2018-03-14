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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaImpl;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.input.QNameEditorPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardUtil;
import com.evolveum.midpoint.web.component.wizard.resource.SchemaHandlingStep;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.LimitationsEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.MappingEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class ResourceAssociationEditor extends BasePanel<ResourceObjectAssociationType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAssociationEditor.class);

    private static final String ID_LABEL = "label";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_DIRECTION = "direction";
    private static final String ID_ASSOCIATION_ATTRIBUTE = "associationAttribute";
    private static final String ID_VALUE_ATTRIBUTE = "valueAttribute";
    private static final String ID_EXPLICIT_REF_INTEGRITY = "explicitRefIntegrity";

    private static final String ID_ASSOCIATION_ATTRIBUTE_PANEL = "associationAttributePanel";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXCLUSIVE_STRONG = "exclusiveStrong";
    private static final String ID_TOLERANT = "tolerant";
    private static final String ID_TOLERANT_VP = "tolerantValuePattern";
    private static final String ID_INTOLERANT_VP = "intolerantValuePattern";
    private static final String ID_FETCH_STRATEGY = "fetchStrategy";
    private static final String ID_INBOUND = "inbound";
    private static final String ID_OUTBOUND_LABEL = "outboundLabel";
    private static final String ID_BUTTON_OUTBOUND = "buttonOutbound";
    private static final String ID_DELETE_OUTBOUND = "deleteOutbound";
    private static final String ID_BUTTON_LIMITATIONS = "buttonLimitations";
    private static final String ID_MODAL_LIMITATIONS = "limitationsEditor";
    private static final String ID_MODAL_INBOUND = "inboundEditor";
    private static final String ID_MODAL_OUTBOUND = "outboundEditor";
    private static final String ID_T_LIMITATIONS = "limitationsTooltip";
    private static final String ID_T_EXCLUSIVE_STRONG = "exclusiveStrongTooltip";
    private static final String ID_T_TOLERANT = "tolerantTooltip";
    private static final String ID_T_TOLERANT_VP = "tolerantVPTooltip";
    private static final String ID_T_INTOLERANT_VP = "intolerantVPTooltip";
    private static final String ID_T_FETCH = "fetchStrategyTooltip";
    private static final String ID_T_MATCHING_RULE = "matchingRuleTooltip";
    private static final String ID_T_OUTBOUND = "outboundTooltip";
    private static final String ID_T_INBOUND = "inboundTooltip";
    private static final String ID_T_KIND = "kindTooltip";
    private static final String ID_T_INTENT = "intentTooltip";
    private static final String ID_T_DIRECTION = "directionTooltip";
    private static final String ID_T_ASSOCIATION_ATTRIBUTE = "associationAttributeTooltip";
    private static final String ID_T_VALUE_ATTRIBUTE = "valueAttributeTooltip";
    private static final String ID_T_EXPLICIT_REF_INTEGRITY = "explicitRefIntegrityTooltip";

    private PrismObject<ResourceType> resource;
    private ResourceObjectTypeDefinitionType objectType;
	@NotNull final private SchemaHandlingStep parentStep;

	public ResourceAssociationEditor(String id, IModel<ResourceObjectAssociationType> model,
			ResourceObjectTypeDefinitionType objectType, PrismObject<ResourceType> resource, SchemaHandlingStep parentStep,
			NonEmptyModel<Boolean> readOnlyModel) {
        super(id, model);

        this.resource = resource;
        this.objectType = objectType;
		this.parentStep = parentStep;
		initLayout(readOnlyModel);
    }

    protected void initLayout(NonEmptyModel<Boolean> readOnlyModel) {
        Label label = new Label(ID_LABEL, new ResourceModel("ResourceAssociationEditor.label.edit"));
		label.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(label);

        DropDownChoice kind = new DropDownChoice<>(ID_KIND,
            new PropertyModel<>(getModel(), "kind"),
                WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class),
            new EnumChoiceRenderer<>(this));
        kind.setNullValid(false);
		kind.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(kind);

        MultiValueTextPanel intent = new MultiValueTextPanel<>(ID_INTENT,
                new PropertyModel<List<String>>(getModel(), "intent"), readOnlyModel, true);
		intent.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(intent);

        DropDownChoice direction = new DropDownChoice<>(ID_DIRECTION,
            new PropertyModel<>(getModel(), "direction"),
                WebComponentUtil.createReadonlyModelFromEnum(ResourceObjectAssociationDirectionType.class),
            new EnumChoiceRenderer<>(this));
        direction.setNullValid(true);
		direction.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(direction);

        DropDownChoice associationAttribute = new DropDownChoice<>(ID_ASSOCIATION_ATTRIBUTE,
            new PropertyModel<>(getModel(), "associationAttribute"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return loadObjectReferences(false);
                    }
                }, new QNameChoiceRenderer(true));
        associationAttribute.setNullValid(true);
		associationAttribute.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(associationAttribute);

        DropDownChoice valueAttribute = new DropDownChoice<>(ID_VALUE_ATTRIBUTE,
            new PropertyModel<>(getModel(), "valueAttribute"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return loadObjectReferences(false);
                    }
                }, new QNameChoiceRenderer(true));
        valueAttribute.setNullValid(true);
		valueAttribute.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(valueAttribute);

        CheckBox explicitRefIntegrity = new CheckBox(ID_EXPLICIT_REF_INTEGRITY,
            new PropertyModel<>(getModel(), "explicitReferentialIntegrity"));
		explicitRefIntegrity.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(explicitRefIntegrity);

        QNameEditorPanel nonSchemaRefPanel = new QNameEditorPanel(ID_ASSOCIATION_ATTRIBUTE_PANEL, new PropertyModel<>(getModel(), "ref"),
                "SchemaHandlingStep.association.tooltip.associationLocalPart", "SchemaHandlingStep.association.tooltip.associationNamespace",
                true, true)  {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(parentStep.getAssociationList());
				((PageResourceWizard) getPageBase()).refreshIssues(target);
			}
		};
        nonSchemaRefPanel.setOutputMarkupId(true);
        nonSchemaRefPanel.setOutputMarkupPlaceholderTag(true);
		nonSchemaRefPanel.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(nonSchemaRefPanel);

        TextField displayName = new TextField<>(ID_DISPLAY_NAME, new PropertyModel<String>(getModel(), "displayName"));
		displayName.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				target.add(parentStep.getAssociationList());
			}
		});
		displayName.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(displayName);

        TextArea description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(getModel(), "description"));
		description.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(description);

        AjaxLink limitations = new AjaxLink(ID_BUTTON_LIMITATIONS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                limitationsEditPerformed(target);
            }
        };
        add(limitations);

        CheckBox exclusiveStrong = new CheckBox(ID_EXCLUSIVE_STRONG, new PropertyModel<>(getModel(), "exclusiveStrong"));
		exclusiveStrong.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(exclusiveStrong);

        CheckBox tolerant = new CheckBox(ID_TOLERANT, new PropertyModel<>(getModel(), "tolerant"));
		tolerant.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(tolerant);

        MultiValueTextPanel tolerantVP = new MultiValueTextPanel<>(ID_TOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "tolerantValuePattern"), readOnlyModel, true);
		tolerantVP.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(tolerantVP);

        MultiValueTextPanel intolerantVP = new MultiValueTextPanel<>(ID_INTOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "intolerantValuePattern"), readOnlyModel, true);
		intolerantVP.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(intolerantVP);

        DropDownChoice fetchStrategy = new DropDownChoice<>(ID_FETCH_STRATEGY,
            new PropertyModel<>(getModel(), "fetchStrategy"),
                WebComponentUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
            new EnumChoiceRenderer<>(this));
        fetchStrategy.setNullValid(true);
		fetchStrategy.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(fetchStrategy);

        AttributeEditorUtils.addMatchingRuleFields(this, readOnlyModel);

		VisibleEnableBehaviour showIfEditingOrOutboundExists = AttributeEditorUtils.createShowIfEditingOrOutboundExists(getModel(), readOnlyModel);
        TextField outboundLabel = new TextField<>(ID_OUTBOUND_LABEL,
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        ResourceObjectAssociationType association = getModel().getObject();

                        if(association == null){
                            return null;
                        }

                        return MappingTypeDto.createMappingLabel(association.getOutbound(), LOGGER, getPageBase().getPrismContext(),
                                getString("MappingType.label.placeholder"), getString("MultiValueField.nameNotSpecified"));
                    }
                });
        outboundLabel.setOutputMarkupId(true);
        outboundLabel.setEnabled(false);
		outboundLabel.add(showIfEditingOrOutboundExists);
        add(outboundLabel);

        AjaxSubmitButton outbound = new AjaxSubmitButton(ID_BUTTON_OUTBOUND) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                outboundEditPerformed(target);
            }

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(parentStep.getPageBase().getFeedbackPanel());
			}
		};
        outbound.setOutputMarkupId(true);
		outbound.add(showIfEditingOrOutboundExists);
        add(outbound);

		AjaxSubmitLink deleteOutbound = new AjaxSubmitLink(ID_DELETE_OUTBOUND) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				deleteOutboundPerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(parentStep.getPageBase().getFeedbackPanel());
			}
		};
		deleteOutbound.setOutputMarkupId(true);
		deleteOutbound.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
		add(deleteOutbound);

		MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
            new PropertyModel<>(getModel(), "inbound"), null, false, true, readOnlyModel) {

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
			protected void performAddValueHook(AjaxRequestTarget target, MappingType added) {
				target.add(parentStep.getAssociationList());
				target.add(parentStep.getAttributeList());		// because of marking duplicates
				((PageResourceWizard) getPageBase()).refreshIssues(target);
			}

			@Override
			protected void performRemoveValueHook(AjaxRequestTarget target, ListItem<MappingType> item) {
				target.add(parentStep.getAssociationList());
				target.add(parentStep.getAttributeList());		// because of marking duplicates
				((PageResourceWizard) getPageBase()).refreshIssues(target);
			}

			@Override
            protected void editPerformed(AjaxRequestTarget target, MappingType object){
                inboundEditPerformed(target, object);
            }
        };
        inbound.setOutputMarkupId(true);
		add(inbound);

        Label kindTooltip = new Label(ID_T_KIND);
        kindTooltip.add(new InfoTooltipBehavior());
        add(kindTooltip);

        Label intentTooltip = new Label(ID_T_INTENT);
        intentTooltip.add(new InfoTooltipBehavior());
        add(intentTooltip);

        Label directionTooltip = new Label(ID_T_DIRECTION);
        directionTooltip.add(new InfoTooltipBehavior());
        add(directionTooltip);

        Label assAttributeTooltip = new Label(ID_T_ASSOCIATION_ATTRIBUTE);
        assAttributeTooltip.add(new InfoTooltipBehavior());
        add(assAttributeTooltip);

        Label valueAttributeTooltip = new Label(ID_T_VALUE_ATTRIBUTE);
        valueAttributeTooltip.add(new InfoTooltipBehavior());
        add(valueAttributeTooltip);

        Label integrityTooltip = new Label(ID_T_EXPLICIT_REF_INTEGRITY);
        integrityTooltip.add(new InfoTooltipBehavior());
        add(integrityTooltip);

        Label limitationsTooltip = new Label(ID_T_LIMITATIONS);
        limitationsTooltip.add(new InfoTooltipBehavior());
        add(limitationsTooltip);

        Label exclusiveStrongTooltip = new Label(ID_T_EXCLUSIVE_STRONG);
        exclusiveStrongTooltip.add(new InfoTooltipBehavior());
        add(exclusiveStrongTooltip);

        Label tolerantTooltip = new Label(ID_T_TOLERANT);
        tolerantTooltip.add(new InfoTooltipBehavior());
        add(tolerantTooltip);

        Label tolerantVPTooltip = new Label(ID_T_TOLERANT_VP);
        tolerantVPTooltip.add(new InfoTooltipBehavior());
        add(tolerantVPTooltip);

        Label intolerantVPTooltip = new Label(ID_T_INTOLERANT_VP);
        intolerantVPTooltip.add(new InfoTooltipBehavior());
        add(intolerantVPTooltip);

        Label fetchTooltip = new Label(ID_T_FETCH);
        fetchTooltip.add(new InfoTooltipBehavior());
        add(fetchTooltip);

        Label matchingRuleTooltip = new Label(ID_T_MATCHING_RULE);
        matchingRuleTooltip.add(new InfoTooltipBehavior());
        add(matchingRuleTooltip);

        Label outboundTooltip = new Label(ID_T_OUTBOUND);
        outboundTooltip.add(new InfoTooltipBehavior());
        add(outboundTooltip);

        Label inboundTooltip = new Label(ID_T_INBOUND);
        inboundTooltip.add(new InfoTooltipBehavior());
        add(inboundTooltip);

        initModals(readOnlyModel);
    }

    private void initModals(NonEmptyModel<Boolean> readOnlyModel) {
        ModalWindow limitationsEditor = new LimitationsEditorDialog(ID_MODAL_LIMITATIONS,
            new PropertyModel<>(getModel(), "limitations"), readOnlyModel);
        add(limitationsEditor);

        ModalWindow inboundEditor = new MappingEditorDialog(ID_MODAL_INBOUND, null, readOnlyModel) {

            @Override
            public void updateComponents(AjaxRequestTarget target){
                target.add(ResourceAssociationEditor.this.get(ID_INBOUND), parentStep.getAssociationList());
            }

        };
        add(inboundEditor);

        ModalWindow outboundEditor = new MappingEditorDialog(ID_MODAL_OUTBOUND, null, readOnlyModel) {

            @Override
            public void updateComponents(AjaxRequestTarget target) {
                target.add(ResourceAssociationEditor.this.get(ID_OUTBOUND_LABEL), ResourceAssociationEditor.this.get(ID_BUTTON_OUTBOUND), parentStep.getAssociationList());
            }
        };
        add(outboundEditor);
    }

    private List<QName> loadObjectReferences(boolean restrictObjectClass){
        List<QName> references = new ArrayList<>();

        ResourceSchema schema = loadResourceSchema();
        if (schema == null) {
            return references;
        }

        for(ObjectClassComplexTypeDefinition def: schema.getObjectClassDefinitions()){
            if(restrictObjectClass){
                if(objectType != null && def.getTypeName().equals(objectType.getObjectClass())){

                    for(ResourceAttributeDefinition attributeDefinition : def.getAttributeDefinitions()) {
                        references.add(attributeDefinition.getName());
                    }
                }
            } else {

                for(ResourceAttributeDefinition attributeDefinition : def.getAttributeDefinitions()) {
                    references.add(attributeDefinition.getName());
                }
            }
        }

        return references;
    }

    private ResourceSchema loadResourceSchema() {
        if(resource != null){
            Element xsdSchema = ResourceTypeUtil.getResourceXsdSchema(resource);
            if (xsdSchema == null) {
                return null;
            }

            try {
                return ResourceSchemaImpl.parse(xsdSchema, resource.toString(), getPageBase().getPrismContext());
            } catch (SchemaException|RuntimeException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse resource schema.", e);
                getSession().error(getString("ResourceAssociationEditor.message.cantParseSchema") + " " + e.getMessage());

                throw new RestartResponseException(PageResources.class);
            }
        }

        return null;
    }

    private String prepareReferenceDisplayValue(QName object){
        StringBuilder sb = new StringBuilder();

        if(object != null){
            sb.append(object.getLocalPart());

            if(object.getNamespaceURI() != null){
                sb.append(" (");
                String[] ns = object.getNamespaceURI().split("/");
                sb.append(ns[ns.length-1]);
                sb.append(")");
            }
        }

        return sb.toString();
    }

    private void limitationsEditPerformed(AjaxRequestTarget target){
        LimitationsEditorDialog window = (LimitationsEditorDialog)get(ID_MODAL_LIMITATIONS);
        window.show(target);
    }

	private void deleteOutboundPerformed(AjaxRequestTarget target) {
		ResourceObjectAssociationType def = getModelObject();
		def.setOutbound(null);
		target.add(this, parentStep.getAssociationList());
	}

	private void outboundEditPerformed(AjaxRequestTarget target){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_OUTBOUND);
        window.updateModel(target, new PropertyModel<>(getModel(), "outbound"), false);
        window.show(target);
    }

    private void inboundEditPerformed(AjaxRequestTarget target, MappingType mapping){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_INBOUND);
        window.updateModel(target, mapping, true);
        window.show(target);
    }
}
