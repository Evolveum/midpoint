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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.input.QNameEditorPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardUtil;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.LimitationsEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.MappingEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *  @author shood
 * */
public class ResourceAttributeEditor extends SimplePanel<ResourceAttributeDefinitionType>{

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAttributeEditor.class);

    private static final String ID_LABEL = "label";
    private static final String ID_SCHEMA_REF_PANEL = "schemaRefPanel";
    private static final String ID_NON_SCHEMA_REF_PANEL = "nonSchemaReferencePanel";
    private static final String ID_REFERENCE_SELECT = "referenceSelect";
    private static final String ID_REFERENCE_ALLOW = "allowRef";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXCLUSIVE_STRONG = "exclusiveStrong";
    private static final String ID_TOLERANT = "tolerant";
    private static final String ID_TOLERANT_VP = "tolerantValuePattern";
    private static final String ID_INTOLERANT_VP = "intolerantValuePattern";
    private static final String ID_FETCH_STRATEGY = "fetchStrategy";
    private static final String ID_MATCHING_RULE = "matchingRule";
    private static final String ID_INBOUND = "inbound";
    private static final String ID_OUTBOUND_LABEL = "outboundLabel";
    private static final String ID_BUTTON_OUTBOUND = "buttonOutbound";
    private static final String ID_BUTTON_LIMITATIONS = "buttonLimitations";
    private static final String ID_MODAL_LIMITATIONS = "limitationsEditor";
    private static final String ID_MODAL_INBOUND = "inboundEditor";
    private static final String ID_MODAL_OUTBOUND = "outboundEditor";
    private static final String ID_T_REF = "referenceTooltip";
    private static final String ID_T_ALLOW = "allowTooltip";
    private static final String ID_T_LIMITATIONS = "limitationsTooltip";
    private static final String ID_T_EXCLUSIVE_STRONG = "exclusiveStrongTooltip";
    private static final String ID_T_TOLERANT = "tolerantTooltip";
    private static final String ID_T_TOLERANT_VP = "tolerantVPTooltip";
    private static final String ID_T_INTOLERANT_VP = "intolerantVPTooltip";
    private static final String ID_T_FETCH = "fetchStrategyTooltip";
    private static final String ID_T_MATCHING_RULE = "matchingRuleTooltip";
    private static final String ID_T_OUTBOUND = "outboundTooltip";
    private static final String ID_T_INBOUND = "inboundTooltip";

    private PrismObject<ResourceType> resource;
    private ResourceObjectTypeDefinitionType objectType;
    private boolean nonSchemaRefValueAllowed = false;

    public ResourceAttributeEditor(String id, IModel<ResourceAttributeDefinitionType> model, ResourceObjectTypeDefinitionType objectType,
                                   PrismObject<ResourceType> resource){
        super(id, model);

        this.resource = resource;
        this.objectType = objectType;
    }

    @Override
    protected void initLayout(){
        Label label = new Label(ID_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceAttributeDefinitionType attribute = getModelObject();

                if(attribute.getRef() == null || attribute.getRef().equals(new ItemPathType())){
                    return getString("ResourceAttributeEditor.label.new");
                } else {
                    return getString("ResourceAttributeEditor.label.edit", ItemPathUtil.getOnlySegmentQName(attribute.getRef()).getLocalPart());
                }
            }
        });
        add(label);

        QNameEditorPanel nonSchemaRefPanel = new QNameEditorPanel(ID_NON_SCHEMA_REF_PANEL, new PropertyModel<ItemPathType>(getModel(), "ref"),
                "SchemaHandlingStep.attribute.label.attributeName", "SchemaHandlingStep.attribute.tooltip.attributeLocalPart",
                "SchemaHandlingStep.attribute.label.attributeNamespace", "SchemaHandlingStep.attribute.tooltip.attributeNamespace");

        nonSchemaRefPanel.setOutputMarkupId(true);
        nonSchemaRefPanel.setOutputMarkupPlaceholderTag(true);
        nonSchemaRefPanel.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return nonSchemaRefValueAllowed;
            }
        });
        add(nonSchemaRefPanel);

        WebMarkupContainer schemaRefPanel = new WebMarkupContainer(ID_SCHEMA_REF_PANEL);
        schemaRefPanel.setOutputMarkupId(true);
        schemaRefPanel.setOutputMarkupPlaceholderTag(true);
        schemaRefPanel.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return !nonSchemaRefValueAllowed;
            }

        });
        add(schemaRefPanel);

        Label refTooltip = new Label(ID_T_REF);
        refTooltip.add(new InfoTooltipBehavior());
        refTooltip.setOutputMarkupId(true);
        refTooltip.setOutputMarkupId(true);
        schemaRefPanel.add(refTooltip);

        DropDownChoice refSelect = new DropDownChoice<ItemPathType>(ID_REFERENCE_SELECT, new PropertyModel<ItemPathType>(getModel(), "ref"),
                new AbstractReadOnlyModel<List<ItemPathType>>() {

                    @Override
                    public List<ItemPathType> getObject() {
                        return loadObjectReferences();
                    }
                }, new IChoiceRenderer<ItemPathType>() {

            @Override
            public Object getDisplayValue(ItemPathType object) {
                return prepareReferenceDisplayValue(object);
            }

            @Override
            public String getIdValue(ItemPathType object, int index) {
                return Integer.toString(index);
            }
        }){

            @Override
            protected boolean isSelected(ItemPathType object, int index, String selected) {
                if(getModelObject() == null || getModelObject().equals(new ItemPathType())){
                    return false;
                }

                QName referenceQName = ItemPathUtil.getOnlySegmentQName(getModelObject());
                QName optionQName = ItemPathUtil.getOnlySegmentQName(object);

                return referenceQName.equals(optionQName);
            }
        };
        refSelect.setNullValid(false);

        refSelect.setOutputMarkupId(true);
        refSelect.setOutputMarkupPlaceholderTag(true);
        schemaRefPanel.add(refSelect);

        CheckBox allowNonSchema = new CheckBox(ID_REFERENCE_ALLOW, new PropertyModel<Boolean>(this, "nonSchemaRefValueAllowed"));
        allowNonSchema.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_NON_SCHEMA_REF_PANEL), get(ID_SCHEMA_REF_PANEL));
            }
        });
        add(allowNonSchema);

        TextField displayName = new TextField<>(ID_DISPLAY_NAME, new PropertyModel<String>(getModel(), "displayName"));
        add(displayName);

        TextArea description = new TextArea<>(ID_DESCRIPTION, new PropertyModel<String>(getModel(), "description"));
        add(description);

        AjaxLink limitations = new AjaxLink(ID_BUTTON_LIMITATIONS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                limitationsEditPerformed(target);
            }
        };
        add(limitations);

        CheckBox exclusiveStrong = new CheckBox(ID_EXCLUSIVE_STRONG, new PropertyModel<Boolean>(getModel(), "exclusiveStrong"));
        add(exclusiveStrong);

        CheckBox tolerant = new CheckBox(ID_TOLERANT, new PropertyModel<Boolean>(getModel(), "tolerant"));
        add(tolerant);

        MultiValueTextPanel tolerantVP = new MultiValueTextPanel<>(ID_TOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "tolerantValuePattern"));
        add(tolerantVP);

        MultiValueTextPanel intolerantVP = new MultiValueTextPanel<>(ID_INTOLERANT_VP,
                new PropertyModel<List<String>>(getModel(), "intolerantValuePattern"));
        add(intolerantVP);

        DropDownChoice fetchStrategy = new DropDownChoice<>(ID_FETCH_STRATEGY,
                new PropertyModel<AttributeFetchStrategyType>(getModel(), "fetchStrategy"),
                WebMiscUtil.createReadonlyModelFromEnum(AttributeFetchStrategyType.class),
                new EnumChoiceRenderer<AttributeFetchStrategyType>(this));
        fetchStrategy.setNullValid(true);
        add(fetchStrategy);

        DropDownChoice matchingRule = new DropDownChoice<>(ID_MATCHING_RULE,
                new PropertyModel<QName>(getModel(), "matchingRule"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return WebMiscUtil.getMatchingRuleList();
                    }
                }, new IChoiceRenderer<QName>() {

            @Override
            public Object getDisplayValue(QName object) {
                return object.getLocalPart();
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        });
        matchingRule.setNullValid(true);
        add(matchingRule);

        TextField outboundLabel = new TextField<>(ID_OUTBOUND_LABEL,
                new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        ResourceAttributeDefinitionType attributeDefinition = getModel().getObject();

                        if(attributeDefinition == null){
                            return null;
                        }

                        return MappingTypeDto.createMappingLabel(attributeDefinition.getOutbound(), LOGGER, getPageBase().getPrismContext(),
                                getString("MappingType.label.placeholder"), getString("MultiValueField.nameNotSpecified"));
                    }
                });
        outboundLabel.setEnabled(false);
        outboundLabel.setOutputMarkupId(true);
        add(outboundLabel);

        AjaxSubmitLink outbound = new AjaxSubmitLink(ID_BUTTON_OUTBOUND) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                outboundEditPerformed(target);
            }
        };
        outbound.setOutputMarkupId(true);
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<List<MappingType>>(getModel(), "inbound"), false){

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

        Label allowTooltip = new Label(ID_T_ALLOW);
        allowTooltip.add(new InfoTooltipBehavior());
        add(allowTooltip);

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

        initModals();
    }

    private void initModals(){
        ModalWindow limitationsEditor = new LimitationsEditorDialog(ID_MODAL_LIMITATIONS,
                new PropertyModel<List<PropertyLimitationsType>>(getModel(), "limitations"));
        add(limitationsEditor);

        ModalWindow inboundEditor = new MappingEditorDialog(ID_MODAL_INBOUND, null){

            @Override
            public void updateComponents(AjaxRequestTarget target) {
                target.add(ResourceAttributeEditor.this.get(ID_INBOUND));
            }
        };
        add(inboundEditor);

        ModalWindow outboundEditor = new MappingEditorDialog(ID_MODAL_OUTBOUND, null){

            @Override
            public void updateComponents(AjaxRequestTarget target) {
                target.add(ResourceAttributeEditor.this.get(ID_OUTBOUND_LABEL), ResourceAttributeEditor.this.get(ID_BUTTON_OUTBOUND));
            }
        };
        add(outboundEditor);
    }

    private List<ItemPathType> loadObjectReferences(){
        List<ItemPathType> references = new ArrayList<>();

        ResourceSchema schema = loadResourceSchema();
        if (schema == null) {
            return references;
        }

        for(ObjectClassComplexTypeDefinition def: schema.getObjectClassDefinitions()){
            if(objectType != null && def.getTypeName().equals(objectType.getObjectClass())){

                for (ResourceAttributeDefinition attributeDefinition : def.getAttributeDefinitions()) {
                    ItemPath itemPath = new ItemPath(attributeDefinition.getName());
                    references.add(new ItemPathType(itemPath));
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
                return ResourceSchema.parse(xsdSchema, resource.toString(), getPageBase().getPrismContext());
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't parse resource schema.", e);
                getSession().error(getString("ResourceAttributeEditor.message.cantParseSchema") + " " + e.getMessage());

                throw new RestartResponseException(PageResources.class);
            }
        }

        return null;
    }

    private String prepareReferenceDisplayValue(ItemPathType object){
        if(object != null && object.getItemPath() != null){
            return object.getItemPath().toString();
        }

        return "";
    }

    private void limitationsEditPerformed(AjaxRequestTarget target){
        LimitationsEditorDialog window = (LimitationsEditorDialog)get(ID_MODAL_LIMITATIONS);
        window.show(target);
    }

    private void outboundEditPerformed(AjaxRequestTarget target){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_OUTBOUND);
        window.updateModel(target, new PropertyModel<MappingType>(getModel(), "outbound"), false);
        window.show(target);
    }

    private void inboundEditPerformed(AjaxRequestTarget target, MappingType mapping){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_INBOUND);
        window.updateModel(target, mapping, true);
        window.show(target);
    }
}
