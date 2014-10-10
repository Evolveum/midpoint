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
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.LimitationsEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.modal.MappingEditorDialog;
import com.evolveum.midpoint.web.component.wizard.resource.dto.MappingTypeDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  @author shood
 * */
public class ResourceAssociationEditor extends SimplePanel<ResourceObjectAssociationType>{

    private static final Trace LOGGER = TraceManager.getTrace(ResourceAssociationEditor.class);

    private static final String ID_LABEL = "label";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_DIRECTION = "direction";
    private static final String ID_ASSOCIATION_ATTRIBUTE = "associationAttribute";
    private static final String ID_VALUE_ATTRIBUTE = "valueAttribute";
    private static final String ID_EXPLICIT_REF_INTEGRITY = "explicitRefIntegrity";

    private static final String ID_REFERENCE_FIELD = "referenceField";
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
    private static final String ID_MODAL_MAPPING = "mappingEditor";

    private PrismObject<ResourceType> resource;
    private ResourceObjectTypeDefinitionType objectType;
    private boolean nonSchemaRefValueAllowed = false;

    public ResourceAssociationEditor(String id, IModel<ResourceObjectAssociationType> model,
                                     ResourceObjectTypeDefinitionType objectType, PrismObject<ResourceType> resource){
        super(id, model);

        this.resource = resource;
        this.objectType = objectType;
    }

    @Override
    protected void initLayout(){
        Label label = new Label(ID_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ResourceObjectAssociationType association = getModelObject();

                if(association.getDisplayName() == null && association.getRef() == null){
                    return getString("ResourceAssociationEditor.label.new");
                } else {
                    return getString("ResourceAssociationEditor.label.edit", association.getRef().getLocalPart());
                }
            }
        });
        add(label);

        DropDownChoice kind = new DropDownChoice<>(ID_KIND,
                new PropertyModel<ShadowKindType>(getModel(), "kind"),
                WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                new EnumChoiceRenderer<ShadowKindType>(this));
        kind.setNullValid(true);
        add(kind);

        MultiValueTextPanel intent = new MultiValueTextPanel<>(ID_INTENT,
                new PropertyModel<List<String>>(getModel(), "intent"));
        add(intent);

        DropDownChoice direction = new DropDownChoice<>(ID_DIRECTION,
                new PropertyModel<ResourceObjectAssociationDirectionType>(getModel(), "direction"),
                WebMiscUtil.createReadonlyModelFromEnum(ResourceObjectAssociationDirectionType.class),
                new EnumChoiceRenderer<ResourceObjectAssociationDirectionType>(this));
        direction.setNullValid(true);
        add(direction);

        DropDownChoice associationAttribute = new DropDownChoice<>(ID_ASSOCIATION_ATTRIBUTE,
                new PropertyModel<QName>(getModel(), "associationAttribute"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return loadObjectReferences(false);
                    }
                }, new IChoiceRenderer<QName>() {

            @Override
            public Object getDisplayValue(QName object) {
                return prepareReferenceDisplayValue(object);
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        });
        associationAttribute.setNullValid(true);
        add(associationAttribute);

        DropDownChoice valueAttribute = new DropDownChoice<>(ID_VALUE_ATTRIBUTE,
                new PropertyModel<QName>(getModel(), "valueAttribute"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return loadObjectReferences(false);
                    }
                }, new IChoiceRenderer<QName>() {

            @Override
            public Object getDisplayValue(QName object) {
                return prepareReferenceDisplayValue(object);
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        });
        valueAttribute.setNullValid(true);
        add(valueAttribute);

        CheckBox explicitRefIntegrity = new CheckBox(ID_EXPLICIT_REF_INTEGRITY,
                new PropertyModel<Boolean>(getModel(), "explicitReferentialIntegrity"));
        add(explicitRefIntegrity);

        TextField refField = new TextField<>(ID_REFERENCE_FIELD, new PropertyModel<String>(getModel(), "ref.localPart"));
        refField.setOutputMarkupId(true);
        refField.setOutputMarkupPlaceholderTag(true);
        refField.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return nonSchemaRefValueAllowed;
            }

        });
        add(refField);

        DropDownChoice refSelect = new DropDownChoice<>(ID_REFERENCE_SELECT, new PropertyModel<QName>(getModel(), "ref"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return loadObjectReferences(true);
                    }
                }, new IChoiceRenderer<QName>() {

            @Override
            public Object getDisplayValue(QName object) {
                return prepareReferenceDisplayValue(object);
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        });
        refSelect.setOutputMarkupId(true);
        refSelect.setOutputMarkupPlaceholderTag(true);
        refSelect.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return !nonSchemaRefValueAllowed;
            }

        });
        add(refSelect);

        CheckBox allowNonSchema = new CheckBox(ID_REFERENCE_ALLOW, new PropertyModel<Boolean>(this, "nonSchemaRefValueAllowed"));
        allowNonSchema.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(get(ID_REFERENCE_FIELD), get(ID_REFERENCE_SELECT));
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
        add(outboundLabel);

        AjaxSubmitButton outbound = new AjaxSubmitButton(ID_BUTTON_OUTBOUND) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                outboundEditPerformed(target);
            }
        };
        outbound.setOutputMarkupId(true);
        add(outbound);

        MultiValueTextEditPanel inbound = new MultiValueTextEditPanel<MappingType>(ID_INBOUND,
                new PropertyModel<List<MappingType>>(getModel(), "inbound"), false, true){

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

        initModals();
    }

    private void initModals(){
        ModalWindow limitationsEditor = new LimitationsEditorDialog(ID_MODAL_LIMITATIONS,
                new PropertyModel<List<PropertyLimitationsType>>(getModel(), "limitations"));
        add(limitationsEditor);

        ModalWindow mappingEditor = new MappingEditorDialog(ID_MODAL_MAPPING, null){

            @Override
            public void updateComponents(AjaxRequestTarget target){
                target.add(ResourceAssociationEditor.this.get(ID_INBOUND), ResourceAssociationEditor.this.get(ID_OUTBOUND_LABEL),
                        ResourceAssociationEditor.this.get(ID_BUTTON_OUTBOUND));
            }

        };
        add(mappingEditor);
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
                    Iterator it = def.getAttributeDefinitions().iterator();

                    while(it.hasNext()){
                        ResourceAttributeDefinition attributeDefinition = (ResourceAttributeDefinition)it.next();
                        references.add(attributeDefinition.getName());
                    }
                }
            } else {
                Iterator it = def.getAttributeDefinitions().iterator();

                while(it.hasNext()){
                    ResourceAttributeDefinition attributeDefinition = (ResourceAttributeDefinition)it.next();
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
                return ResourceSchema.parse(xsdSchema, resource.toString(), getPageBase().getPrismContext());
            } catch (Exception e) {
                LoggingUtils.logException(LOGGER, "Couldn't parse resource schema.", e);
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

    private void outboundEditPerformed(AjaxRequestTarget target){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_MAPPING);
        window.updateModel(target, new PropertyModel<MappingType>(getModel(), "outbound"));
        window.show(target);
    }

    private void mappingEditPerformed(AjaxRequestTarget target, MappingType mapping){
        MappingEditorDialog window = (MappingEditorDialog) get(ID_MODAL_MAPPING);
        window.updateModel(target, mapping);
        window.show(target);
    }
}
