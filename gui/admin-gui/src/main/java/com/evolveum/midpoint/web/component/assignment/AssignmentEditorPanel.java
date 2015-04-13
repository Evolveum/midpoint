/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TwoStateBooleanPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.*;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends SimplePanel<AssignmentEditorDto> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentEditorPanel.class);

    private static final String DOT_CLASS = AssignmentEditorPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_LOAD_ATTRIBUTES = DOT_CLASS + "loadAttributes";

    private static final String ID_MAIN = "main";
    private static final String ID_HEADER_ROW = "headerRow";
    private static final String ID_SELECTED = "selected";
    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_ACTIVATION_BLOCK = "activationBlock";
    private static final String ID_BODY = "body";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";
    private static final String ID_RELATION_LABEL = "relationLabel";
    private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_SHOW_EMPTY = "showEmpty";
    private static final String ID_SHOW_EMPTY_LABEL = "showEmptyLabel";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";
    private static final String ID_TARGET = "target";
    private static final String ID_TARGET_CONTAINER = "targetContainer";
    private static final String ID_CONSTRUCTION_CONTAINER = "constructionContainer";
    private static final String ID_CONTAINER_TENANT_REF = "tenantRefContainer";
    private static final String ID_TENANT_CHOOSER = "tenantRefChooser";
    private static final String ID_CONTAINER_ORG_REF = "orgRefContainer";
    private static final String ID_ORG_CHOOSER = "orgRefChooser";
    private static final String ID_BUTTON_SHOW_MORE = "errorLink";
    private static final String ID_ERROR_ICON = "errorIcon";

    private IModel<List<ACAttributeDto>> attributesModel;

    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);

        attributesModel = new LoadableModel<List<ACAttributeDto>>(false) {
            @Override
            protected List<ACAttributeDto> load() {
                return loadAttributes();
            }
        };

        initPanelLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(AssignmentEditorPanel.class, "AssignmentEditorPanel.css")));
    }

    private void initPanelLayout() {
        WebMarkupContainer headerRow = new WebMarkupContainer(ID_HEADER_ROW);
        headerRow.add(AttributeModifier.append("class", createHeaderClassModel(getModel())));
        headerRow.setOutputMarkupId(true);
        add(headerRow);

        AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
                new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_SELECTED)) {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //do we want to update something?
            }
        };
        headerRow.add(selected);

        WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        typeImage.add(AttributeModifier.replace("class",
                createImageTypeModel(new PropertyModel<AssignmentEditorDtoType>(getModel(), AssignmentEditorDto.F_TYPE))));
        headerRow.add(typeImage);

        Label errorIcon = new Label(ID_ERROR_ICON);
        errorIcon.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return !isTargetValid();
            }
        });
        headerRow.add(errorIcon);

        AjaxLink name = new AjaxLink(ID_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        headerRow.add(name);

        AjaxLink errorLink = new AjaxLink(ID_BUTTON_SHOW_MORE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showErrorPerformed(target);
            }
        };
        errorLink.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return !isTargetValid();
            }
        });
        headerRow.add(errorLink);

        Label nameLabel = new Label(ID_NAME_LABEL, createAssignmentNameLabelModel());
        name.add(nameLabel);

        Label activation = new Label(ID_ACTIVATION, createActivationModel());
        headerRow.add(activation);

        WebMarkupContainer main = new WebMarkupContainer(ID_MAIN);
        main.setOutputMarkupId(true);
        add(main);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
                return !editorDto.isMinimized();
            }
        });
        main.add(body);

        initBodyLayout(body);
    }

    private IModel<String> createAssignmentNameLabelModel(){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(getModel() != null && getModel().getObject() != null){
                    AssignmentEditorDto dto = getModelObject();

                    if(dto.getName() != null){
                        return dto.getName();
                    }

                    if(dto.getAltName() != null){
                        return getString("AssignmentEditorPanel.name.focus");
                    }
                }

                return getString("AssignmentEditorPanel.name.noTarget");
            }
        };
    }

    private boolean isTargetValid(){

        if(getModel() != null && getModel().getObject() != null){
            AssignmentEditorDto dto = getModelObject();

            if(dto.getName() == null && dto.getAltName() == null){
                return false;
            }
        }

        return true;
    }

    private IModel<String> createHeaderClassModel(final IModel<AssignmentEditorDto> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = model.getObject();
                return dto.getStatus().name().toLowerCase();
            }
        };
    }

    private IModel<String> createActivationModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = getModel().getObject();
                ActivationType activation = dto.getActivation();
                if (activation == null) {
                    return "-";
                }

                ActivationStatusType status = activation.getAdministrativeStatus();
                String strEnabled = createStringResource(status, "lower", "ActivationStatusType.null").getString();

                if (activation.getValidFrom() != null && activation.getValidTo() != null) {
                    return getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()), MiscUtil.asDate(activation.getValidTo()));
                } else if (activation.getValidFrom() != null) {
                    return getString("AssignmentEditorPanel.enabledFrom", strEnabled,
                            MiscUtil.asDate(activation.getValidFrom()));
                } else if (activation.getValidTo() != null) {
                    return getString("AssignmentEditorPanel.enabledTo", strEnabled,
                            MiscUtil.asDate(activation.getValidTo()));
                }

                return "-";
            }
        };
    }

    private IModel<Date> createDateModel(final IModel<XMLGregorianCalendar> model) {
        return new Model<Date>() {

            @Override
            public Date getObject() {
                XMLGregorianCalendar calendar = model.getObject();
                if (calendar == null) {
                    return null;
                }
                return MiscUtil.asDate(calendar);
            }

            @Override
            public void setObject(Date object) {
                if (object == null) {
                    model.setObject(null);
                } else {
                    model.setObject(MiscUtil.asXMLGregorianCalendar(object));
                }
            }
        };
    }

    private void initBodyLayout(WebMarkupContainer body) {
        TextArea description = new TextArea<>(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        body.add(description);

        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        relationContainer.setOutputMarkupPlaceholderTag(true);
        relationContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                if(dto != null){
                    if(AssignmentEditorDtoType.ORG_UNIT.equals(dto.getType())){
                        return true;
                    }
                }

                return false;
            }
        });
        body.add(relationContainer);

        TwoStateBooleanPanel relation = new TwoStateBooleanPanel(ID_RELATION, new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_IS_ORG_UNIT_MANAGER),
                "AssignmentEditorPanel.member", "AssignmentEditorPanel.manager", null);
        relation.setOutputMarkupId(true);
        relation.setOutputMarkupPlaceholderTag(true);
        relation.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return isCreatingNewAssignment();
            }
        });
        relationContainer.add(relation);

        Label relationLabel = new Label(ID_RELATION_LABEL, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(getModel() == null || getModel().getObject() == null){
                    return getString("AssignmentEditorPanel.relation.notSpecified");
                }

                AssignmentEditorDto object = getModel().getObject();
                return object.isOrgUnitManager() ? getString("AssignmentEditorPanel.manager") : getString("AssignmentEditorPanel.member");
            }
        });
        relationLabel.setOutputMarkupId(true);
        relationLabel.setOutputMarkupPlaceholderTag(true);
        relationLabel.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                return !isCreatingNewAssignment();
            }
        });
        relationContainer.add(relationLabel);

        WebMarkupContainer tenantRefContainer = createTenantContainer();
        body.add(tenantRefContainer);
        
        WebMarkupContainer orgRefContainer = createOrgContainer();
        body.add(orgRefContainer);

        WebMarkupContainer activationBlock = new WebMarkupContainer(ID_ACTIVATION_BLOCK);
        activationBlock.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                //enabled activation in assignments for now.
                return true;
            }
        });
        body.add(activationBlock);

        DropDownChoicePanel administrativeStatus = WebMiscUtil.createEnumPanel(ActivationStatusType.class, ID_ADMINISTRATIVE_STATUS,
                new PropertyModel<ActivationStatusType>(getModel(), AssignmentEditorDto.F_ACTIVATION + "."
                        + ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart()), this);
        activationBlock.add(administrativeStatus);

        DateInput validFrom = new DateInput(ID_VALID_FROM, createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
        activationBlock.add(validFrom);

        DateInput validTo = new DateInput(ID_VALID_TO, createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                AssignmentEditorDto.F_ACTIVATION + ".validTo")));
        activationBlock.add(validTo);
        WebMarkupContainer targetContainer = new WebMarkupContainer(ID_TARGET_CONTAINER);
        targetContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return !AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        body.add(targetContainer);

        Label target = new Label(ID_TARGET, createTargetModel());
        targetContainer.add(target);

        WebMarkupContainer constructionContainer = new WebMarkupContainer(ID_CONSTRUCTION_CONTAINER);
        constructionContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        body.add(constructionContainer);

        AjaxLink showEmpty = new AjaxLink(ID_SHOW_EMPTY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showEmptyPerformed(target);
            }
        };
        constructionContainer.add(showEmpty);

        Label showEmptyLabel = new Label(ID_SHOW_EMPTY_LABEL, createShowEmptyLabel());
        showEmptyLabel.setOutputMarkupId(true);
        showEmpty.add(showEmptyLabel);

        initAttributesLayout(constructionContainer);

        addAjaxOnUpdateBehavior(body);
    }
    
    private WebMarkupContainer createTenantContainer(){
    	WebMarkupContainer tenantRefContainer = new WebMarkupContainer(ID_CONTAINER_TENANT_REF);
        ChooseTypePanel tenantRef = new ChooseTypePanel(ID_TENANT_CHOOSER,
                new PropertyModel<ObjectViewDto>(getModel(), AssignmentEditorDto.F_TENANT_REF)){

            @Override
            protected ObjectQuery getChooseQuery(){
                ObjectQuery query = new ObjectQuery();

                ObjectFilter filter = EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class,
                        getPageBase().getPrismContext(), null, true);
                query.setFilter(filter);

                return query;
            }

            @Override
            protected boolean isSearchEnabled() {
                return true;
            }

            @Override
            protected QName getSearchProperty() {
                return OrgType.F_NAME;
            }
        };
        tenantRefContainer.add(tenantRef);
        tenantRefContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                if(dto != null){
                    if(AssignmentEditorDtoType.ROLE.equals(dto.getType())){
                        return true;
                    }
                }

                return false;
            }
        });
        return tenantRefContainer;
    }
    
    private WebMarkupContainer createOrgContainer(){
    	WebMarkupContainer tenantRefContainer = new WebMarkupContainer(ID_CONTAINER_ORG_REF);
        ChooseTypePanel tenantRef = new ChooseTypePanel(ID_ORG_CHOOSER,
                new PropertyModel<ObjectViewDto>(getModel(), AssignmentEditorDto.F_ORG_REF)){

            @Override
            protected ObjectQuery getChooseQuery(){
                ObjectQuery query = new ObjectQuery();

                ObjectFilter filter = NotFilter.createNot(EqualFilter.createEqual(OrgType.F_TENANT, OrgType.class,
                        getPageBase().getPrismContext(), null, true));
                query.setFilter(filter);

                return query;
            }

            @Override
            protected boolean isSearchEnabled() {
                return true;
            }

            @Override
            protected QName getSearchProperty() {
                return OrgType.F_NAME;
            }
        };
        tenantRefContainer.add(tenantRef);
        tenantRefContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                if(dto != null){
                    if(AssignmentEditorDtoType.ROLE.equals(dto.getType())){
                        return true;
                    }
                }

                return false;
            }
        });
        return tenantRefContainer;
    }

    private void addAjaxOnBlurUpdateBehaviorToComponent(final Component component){
        component.setOutputMarkupId(true);
        component.add(new AjaxFormComponentUpdatingBehavior("onBlur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
    }

    private void addAjaxOnUpdateBehavior(WebMarkupContainer container){
        container.visitChildren(new IVisitor<Component, Object>() {
            @Override
            public void component(Component component, IVisit<Object> objectIVisit) {
                if(component instanceof InputPanel){
                    addAjaxOnBlurUpdateBehaviorToComponent(((InputPanel) component).getBaseFormComponent());
                } else if(component instanceof FormComponent){
                    addAjaxOnBlurUpdateBehaviorToComponent(component);
                }
            }
        });
    }

    private void initAttributesLayout(WebMarkupContainer constructionContainer) {
        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
        attributes.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        constructionContainer.add(attributes);

        ListView<ACAttributeDto> attribute = new ListView<ACAttributeDto>(ID_ATTRIBUTE, attributesModel){

            @Override
            protected void populateItem(ListItem<ACAttributeDto> listItem) {
                final IModel<ACAttributeDto> attrModel = listItem.getModel();
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, attrModel);
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);
                listItem.setOutputMarkupId(true);

                listItem.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
                        if (editorDto.isShowEmpty()) {
                            return true;
                        }

                        ACAttributeDto dto = attrModel.getObject();
                        return !dto.isEmpty();
                    }
                });
            }
        };
        attributes.add(attribute);
        //todo extension
    }

    private IModel<String> createShowEmptyLabel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();

                if (dto.isShowEmpty()) {
                    return getString("AssignmentEditorPanel.hideEmpty");
                } else {
                    return getString("AssignmentEditorPanel.showEmpty");
                }
            }
        };
    }

    private void showEmptyPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();
        dto.setShowEmpty(!dto.isShowEmpty());

        WebMarkupContainer parent = (WebMarkupContainer) get(createComponentPath(ID_MAIN, ID_BODY,
                ID_CONSTRUCTION_CONTAINER));

        target.add(parent.get(ID_ATTRIBUTES), parent.get(createComponentPath(ID_SHOW_EMPTY, ID_SHOW_EMPTY_LABEL)),
                getPageBase().getFeedbackPanel());
    }

    private List<ACAttributeDto> loadAttributes() {
        AssignmentEditorDto dto = getModel().getObject();

        if(dto.getAttributes() != null && !dto.getAttributes().isEmpty()){
            return dto.getAttributes();
        }

        OperationResult result = new OperationResult(OPERATION_LOAD_ATTRIBUTES);
        List<ACAttributeDto> attributes = new ArrayList<>();
        try {
            ConstructionType construction = WebMiscUtil.getContainerValue(dto.getOldValue(),
                    AssignmentType.F_CONSTRUCTION, ConstructionType.class);

            if(construction == null){
                return attributes;
            }

            PrismObject<ResourceType> resource = construction.getResource() != null
                    ? construction.getResource().asPrismObject() : null;
            if (resource == null) {
                resource = getReference(construction.getResourceRef(), result);
            }

            PrismContext prismContext = getPageBase().getPrismContext();
            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource,
                    LayerType.PRESENTATION, prismContext);
            RefinedObjectClassDefinition objectClassDefinition = refinedSchema.getRefinedDefinition(ShadowKindType.ACCOUNT, construction.getIntent());

            if(objectClassDefinition == null){
                return attributes;
            }

            PrismContainerDefinition definition = objectClassDefinition.toResourceAttributeContainerDefinition();

            if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Refined definition for {}\n{}", construction, definition.debugDump());
			}

            List<ResourceAttributeDefinitionType> attrConstructions = construction.getAttribute();

            Collection<ItemDefinition> definitions = definition.getDefinitions();
            for (ItemDefinition attrDef : definitions) {
                if (!(attrDef instanceof PrismPropertyDefinition)) {
                    //log skipping or something...
                    continue;
                }

                PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) attrDef;
                if (propertyDef.isOperational() || propertyDef.isIgnored()) {
                    continue;
                }
                attributes.add(ACAttributeDto.createACAttributeDto(propertyDef,
                        findOrCreateValueConstruction(propertyDef, attrConstructions), prismContext));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Exception occurred during assignment attribute loading", ex);
            result.recordFatalError("Exception occurred during assignment attribute loading.", ex);
        } finally {
            result.recomputeStatus();
        }

        Collections.sort(attributes, new Comparator<ACAttributeDto>() {

            @Override
            public int compare(ACAttributeDto a1, ACAttributeDto a2) {
                return String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName());
            }
        });

        dto.setAttributes(attributes);

        if (!result.isSuccess() && !result.isHandledError()) {
            getPageBase().showResultInSession(result);
        }

        return dto.getAttributes();
    }

    private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_RESOURCE);
        subResult.addParam("targetRef", ref.getOid());
        PrismObject target = null;
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
            Class type = ObjectType.class;
            if (ref.getType() != null){
            	type = getPageBase().getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
            }
            target = getPageBase().getModelService().getObject(type, ref.getOid(), null, task,
                    subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get account construction resource ref", ex);
            subResult.recordFatalError("Couldn't get account construction resource ref.", ex);
        }

        return target;
    }

    private ResourceAttributeDefinitionType findOrCreateValueConstruction(PrismPropertyDefinition attrDef,
                                                                          List<ResourceAttributeDefinitionType> attrConstructions) {
        for (ResourceAttributeDefinitionType construction : attrConstructions) {
            if (attrDef.getName().equals(construction.getRef())) {
                return construction;
            }
        }

        ResourceAttributeDefinitionType construction = new ResourceAttributeDefinitionType();
        construction.setRef(new ItemPathType(new ItemPath(attrDef.getName())));

        return construction;
    }

    private IModel<String> createImageTypeModel(final IModel<AssignmentEditorDtoType> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                AssignmentEditorDtoType type = model.getObject();
                switch (type) {
                    case ROLE:
                        return "silk-user_suit";
                    case ORG_UNIT:
                        return "silk-building";
                    case ACCOUNT_CONSTRUCTION:
                    default:
                        return "silk-drive";
                }
            }
        };
    }

    private void nameClickPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = getModel().getObject();
        boolean minimized = dto.isMinimized();
        if (minimized) {
//            dto.startEditing();//todo ???
        }

        dto.setMinimized(!minimized);

        target.add(get(ID_MAIN));
        target.add(get(ID_HEADER_ROW));
    }

    private IModel<String> createTargetModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                AssignmentEditorDto dto = getModel().getObject();
                PrismContainerValue assignment = dto.getOldValue();

                PrismReference targetRef = assignment.findReference(AssignmentType.F_TARGET_REF);
                if (targetRef == null) {
                    return getString("AssignmentEditorPanel.undefined");
                }

                PrismReferenceValue refValue = targetRef.getValue();
                if (refValue != null && refValue.getObject() != null) {
                    PrismObject object = refValue.getObject();
                    return WebMiscUtil.getName(object);
                }

                String oid = targetRef.getOid();
                OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT);
                try {
                    PageBase page = getPageBase();
                    ModelService model = page.getMidpointApplication().getModel();
                    Task task = page.createSimpleTask(OPERATION_LOAD_OBJECT);

                    Collection<SelectorOptions<GetOperationOptions>> options =
                            SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
                    Class type = ObjectType.class;
                    if (refValue.getTargetType() != null){
                    	type = getPageBase().getPrismContext().getSchemaRegistry().determineCompileTimeClass(refValue.getTargetType());
                    }
                    PrismObject object = model.getObject(type, oid, options, task, result);

                    return WebMiscUtil.getName(object);
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't load object", ex);
                }

                return oid;
            }
        };
    }

    private void showErrorPerformed(AjaxRequestTarget target){
        error(getString("AssignmentEditorPanel.targetError"));
        target.add(getPageBase().getFeedbackPanel());
    }

    /**
     *  Override to provide the information if object that contains this assignment
     *  is being edited or created.
     * */
    protected boolean isCreatingNewAssignment(){
        if(getModelObject() == null){
            return false;
        }

        return UserDtoStatus.ADD.equals(getModelObject().getStatus());
    }
}
