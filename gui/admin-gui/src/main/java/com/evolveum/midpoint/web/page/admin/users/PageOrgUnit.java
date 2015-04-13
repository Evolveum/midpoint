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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentTableDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.form.*;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextFormGroup;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;
import org.apache.wicket.util.string.StringValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/unit", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminUsers.AUTH_ORG_ALL,
                label = PageAdminUsers.AUTH_ORG_ALL_LABEL,
                description = PageAdminUsers.AUTH_ORG_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#orgUnit",
                label = "PageOrgUnit.auth.orgUnit.label",
                description = "PageOrgUnit.auth.orgUnit.description")})
public class PageOrgUnit extends PageAdminUsers implements ProgressReportingAwarePage {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);
    private static final String DOT_CLASS = PageOrgUnit.class.getName() + ".";
    private static final String LOAD_UNIT = DOT_CLASS + "loadOrgUnit";
    private static final String SAVE_UNIT = DOT_CLASS + "saveOrgUnit";
    private static final String LOAD_PARENT_UNITS = DOT_CLASS + "loadParentOrgUnits";
    private static final String OPERATION_LOAD_EXTENSION_WRAPPER = "loadExtensionWrapper";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    private static final String ID_FORM = "form";
    private static final String ID_NAME = "name";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_REQUESTABLE = "requestable";
    private static final String ID_TENANT = "tenant";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_COST_CENTER = "costCenter";
    private static final String ID_LOCALITY = "locality";
    private static final String ID_MAIL_DOMAIN = "mailDomain";
    private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_PARENT_ORG_UNITS = "parentOrgUnits";
    private static final String ID_ORG_TYPE = "orgType";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";
    private static final String ID_EXECUTE_OPTIONS = "executeOptions";

    private static final String ID_ASSIGNMENTS_TABLE = "assignmentsPanel";
    private static final String ID_INDUCEMENTS_TABLE = "inducementsPanel";
    private static final String ID_EXTENSION_LABEL = "extensionLabel";
    private static final String ID_EXTENSION = "extension";
    private static final String ID_EXTENSION_PROPERTY = "property";

    //private ContainerStatus status;
    private IModel<PrismObject<OrgType>> orgModel;
    private IModel<List<OrgType>> parentOrgUnitsModel;
    private IModel<List<PrismPropertyValue>> orgTypeModel;
    private IModel<List<PrismPropertyValue>> orgMailDomainModel;
    private IModel<ContainerWrapper> extensionModel;
    private ObjectWrapper orgWrapper;

    private ProgressReporter progressReporter;
    private ObjectDelta delta;

    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
            = new LoadableModel<ExecuteChangeOptionsDto>(false) {

        @Override
        protected ExecuteChangeOptionsDto load() {
            return new ExecuteChangeOptionsDto();
        }
    };

    public PageOrgUnit() {
        this(null);
    }

    //todo improve [erik]
    public PageOrgUnit(final PrismObject<OrgType> unitToEdit) {
        orgModel = new LoadableModel<PrismObject<OrgType>>(false) {

            @Override
            protected PrismObject<OrgType> load() {
                return loadOrgUnit(unitToEdit);
            }
        };

        orgTypeModel = new LoadableModel<List<PrismPropertyValue>>(false) {

            @Override
            protected List<PrismPropertyValue> load() {
                List<PrismPropertyValue> values = new ArrayList<>();

                PrismObject<OrgType> org = orgModel.getObject();
                PrismProperty orgType = org.findProperty(OrgType.F_ORG_TYPE);
                if (orgType == null || orgType.isEmpty()) {
                    values.add(new PrismPropertyValue(null, OriginType.USER_ACTION, null));
                } else {
                    values.addAll(orgType.getValues());
                }

                return values;
            }
        };

        orgMailDomainModel = new LoadableModel<List<PrismPropertyValue>>(false) {

            @Override
            protected List<PrismPropertyValue> load() {
                List<PrismPropertyValue> values = new ArrayList<>();

                PrismObject<OrgType> org = orgModel.getObject();
                PrismProperty orgMailDomain = org.findProperty(OrgType.F_MAIL_DOMAIN);
                if(orgMailDomain == null || orgMailDomain.isEmpty()){
                    values.add(new PrismPropertyValue(null, OriginType.USER_ACTION, null));
                } else {
                    values.addAll(orgMailDomain.getValues());
                }

                return values;
            }
        };

        parentOrgUnitsModel = new LoadableModel<List<OrgType>>(false) {

            @Override
            protected List<OrgType> load() {
                return loadParentOrgUnits();
            }
        };

        extensionModel = new LoadableModel<ContainerWrapper>() {

            @Override
            protected ContainerWrapper load() {
                return loadExtensionWrapper();
            }
        };

        //status = isEditingOrgUnit() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;

        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isEditingOrgUnit()) {
                    return PageOrgUnit.super.createPageTitleModel().getObject();
                }

                String name = WebMiscUtil.getName(orgModel.getObject());
                return new StringResourceModel("page.title.edit", PageOrgUnit.this, null, null, name).getString();
            }
        };
    }

    private ContainerWrapper loadExtensionWrapper(){
        OperationResult result = new OperationResult(OPERATION_LOAD_EXTENSION_WRAPPER);
        ContainerStatus status = isEditingOrgUnit() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        ObjectWrapper wrapper;
        ContainerWrapper extensionWrapper = null;
        PrismObject<OrgType> org = orgModel.getObject();

        try{
            wrapper = ObjectWrapperUtil.createObjectWrapper("PageOrgUnit.extension", null, org, status, this);
        } catch (Exception e){
            result.recordFatalError("Couldn't create wrapper for Org. unit.", e);
            LoggingUtils.logException(LOGGER, "Couldn't create wrapper for Org. unit", e);
            wrapper = new ObjectWrapper("PageOrgUnit.extension", null, org, null, status, this);
        }

        if(wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())){
            showResultInSession(wrapper.getResult());
        }

        wrapper.setShowEmpty(true);
        orgWrapper = wrapper;

        List<ContainerWrapper> list = wrapper.getContainers();
        for(ContainerWrapper cont: list){
            if("extension".equals(cont.getItem().getDefinition().getName().getLocalPart())){
                extensionWrapper = cont;
            }
        }

        return extensionWrapper;
    }

    private void initLayout() {
        final Form form = new Form(ID_FORM);
        add(form);

        progressReporter = ProgressReporter.create(this, form, "progressPanel");

        TextFormGroup name = new TextFormGroup(ID_NAME, new PrismPropertyModel(orgModel, OrgType.F_NAME),
                createStringResource("ObjectType.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        form.add(name);
        TextFormGroup displayName = new TextFormGroup(ID_DISPLAY_NAME, new PrismPropertyModel(orgModel,
                OrgType.F_DISPLAY_NAME), createStringResource("OrgType.displayName"), ID_LABEL_SIZE, ID_INPUT_SIZE, true);
        form.add(displayName);

        TextAreaFormGroup description = new TextAreaFormGroup(ID_DESCRIPTION, new PrismPropertyModel(orgModel,
                OrgType.F_DESCRIPTION), createStringResource("ObjectType.description"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(description);

        CheckFormGroup requestable = new CheckFormGroup(ID_REQUESTABLE, new PrismPropertyModel(orgModel,
                OrgType.F_REQUESTABLE), createStringResource("OrgType.requestable"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(requestable);

        CheckFormGroup tenant = new CheckFormGroup(ID_TENANT, new PrismPropertyModel(orgModel,
                OrgType.F_TENANT), createStringResource("OrgType.tenant"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        form.add(tenant);

        TextFormGroup identifier = new TextFormGroup(ID_IDENTIFIER, new PrismPropertyModel(orgModel, OrgType.F_IDENTIFIER),
                createStringResource("OrgType.identifier"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(identifier);
        TextFormGroup costCenter = new TextFormGroup(ID_COST_CENTER, new PrismPropertyModel(orgModel, OrgType.F_COST_CENTER),
                createStringResource("OrgType.costCenter"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(costCenter);
        TextFormGroup locality = new TextFormGroup(ID_LOCALITY, new PrismPropertyModel(orgModel, OrgType.F_LOCALITY),
                createStringResource("OrgType.locality"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(locality);

        IModel choices = WebMiscUtil.createReadonlyModelFromEnum(ActivationStatusType.class);
        IChoiceRenderer renderer = new EnumChoiceRenderer();
        DropDownFormGroup administrativeStatus = new DropDownFormGroup(ID_ADMINISTRATIVE_STATUS, new PrismPropertyModel(
                orgModel, new ItemPath(OrgType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)), choices,
                renderer, createStringResource("ActivationType.administrativeStatus"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(administrativeStatus);

        DateFormGroup validFrom = new DateFormGroup(ID_VALID_FROM, new PrismPropertyModel(orgModel, new ItemPath(
                OrgType.F_ACTIVATION, ActivationType.F_VALID_FROM)), createStringResource("ActivationType.validFrom"),
                ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(validFrom);

        DateFormGroup validTo = new DateFormGroup(ID_VALID_TO, new PrismPropertyModel(orgModel, new ItemPath(
                OrgType.F_ACTIVATION, ActivationType.F_VALID_TO)), createStringResource("ActivationType.validTo"),
                ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(validTo);

        //todo not finished [lazyman]
        MultiValueTextFormGroup orgType = new MultiValueTextFormGroup<PrismPropertyValue>(ID_ORG_TYPE, orgTypeModel,
                createStringResource("OrgType.orgType"), ID_LABEL_SIZE, ID_INPUT_SIZE, false) {

            @Override
            protected IModel<String> createTextModel(IModel model) {
                return new PropertyModel<>(model, "value");
            }

            @Override
            protected PrismPropertyValue createNewEmptyItem() {
                return new PrismPropertyValue<OriginType>(null, OriginType.USER_ACTION, null);
            }
        };
        form.add(orgType);

        MultiValueTextFormGroup mailDomain = new MultiValueTextFormGroup<PrismPropertyValue>(ID_MAIL_DOMAIN, orgMailDomainModel,
                createStringResource("OrgType.mailDomain"), ID_LABEL_SIZE, ID_INPUT_SIZE, false) {

            @Override
            protected IModel<String> createTextModel(IModel model) {
                return new PropertyModel<>(model, "value");
            }

            @Override
            protected PrismPropertyValue createNewEmptyItem() {
                return new PrismPropertyValue<OriginType>(null, OriginType.USER_ACTION, null);
            }
        };
        form.add(mailDomain);

        MultiValueChoosePanel parentOrgType = new MultiValueChoosePanel<OrgType>(ID_PARENT_ORG_UNITS, parentOrgUnitsModel,
                createStringResource("ObjectType.parentOrgRef"), ID_LABEL_SIZE, ID_INPUT_SIZE, false, OrgType.class) {

            @Override
            protected IModel<String> createTextModel(final IModel model) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        OrgType org = (OrgType) model.getObject();

                        return org == null ? null : WebMiscUtil.getOrigStringFromPoly(org.getName());
                    }
                };
            }

            @Override
            protected OrgType createNewEmptyItem() {
                return new OrgType();
            }

            @Override
            protected ObjectQuery createChooseQuery(){
                ArrayList<String> oidList = new ArrayList<>();
                ObjectQuery query = new ObjectQuery();

                for(OrgType org: parentOrgUnitsModel.getObject()){
                    if(org != null){
                        if(org.getOid() != null && !org.getOid().isEmpty()){
                            oidList.add(org.getOid());
                        }
                    }
                }

                if(isEditingOrgUnit()){
                    oidList.add(orgModel.getObject().asObjectable().getOid());
                }

                if(oidList.isEmpty()){
                    return null;
                }

                ObjectFilter oidFilter = InOidFilter.createInOid(oidList);
                query.setFilter(NotFilter.createNot(oidFilter));
                //query.setFilter(oidFilter);

                return query;
            }

            @Override
            protected void replaceIfEmpty(Object object) {

                boolean added = false;

                List<OrgType> parents = parentOrgUnitsModel.getObject();
                for (OrgType org : parents) {
                    if (WebMiscUtil.getName(org) == null || WebMiscUtil.getName(org).isEmpty()) {
                        parents.remove(org);
                        parents.add((OrgType) object);
                        added = true;
                        break;
                    }
                }

                if (!added) {
                    parents.add((OrgType) object);
                }
            }
        };
        form.add(parentOrgType);

        AssignmentTablePanel assignments = new AssignmentTablePanel(ID_ASSIGNMENTS_TABLE, new Model<AssignmentTableDto>(),
                createStringResource("PageOrgUnit.title.assignments")){

            @Override
            public List<AssignmentType> getAssignmentTypeList(){
                return orgModel.getObject().asObjectable().getAssignment();
            }

            @Override
            public String getExcludeOid(){
                return orgModel.getObject().asObjectable().getOid();
            }
        };
        form.add(assignments);

        AssignmentTablePanel inducements = new AssignmentTablePanel(ID_INDUCEMENTS_TABLE, new Model<AssignmentTableDto>(),
                createStringResource("PageOrgUnit.title.inducements")){

            @Override
            public List<AssignmentType> getAssignmentTypeList(){
                return orgModel.getObject().asObjectable().getInducement();
            }

            @Override
            public String getExcludeOid(){
                return orgModel.getObject().asObjectable().getOid();
            }
        };
        form.add(inducements);

        Label extensionLabel = new Label(ID_EXTENSION_LABEL, createStringResource("PageOrgUnit.extension"));
        extensionLabel.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                if(extensionModel == null || extensionModel.getObject() == null
                        || extensionModel.getObject().getProperties().isEmpty()){
                    return false;
                } else {
                    return true;
                }
            }
        });
        form.add(extensionLabel);

        ListView<PropertyWrapper> extensionProperties = new ListView<PropertyWrapper>(ID_EXTENSION,
                new PropertyModel(extensionModel, "properties")) {

            @Override
            protected void populateItem(ListItem<PropertyWrapper> item) {
                PrismPropertyPanel propertyPanel = new PrismPropertyPanel(ID_EXTENSION_PROPERTY, item.getModel(), form, PageOrgUnit.this);
                propertyPanel.get("labelContainer:label").add(new AttributeAppender("style", "font-weight:bold;"));
                propertyPanel.get("labelContainer").add(new AttributeModifier("class", ID_LABEL_SIZE + " control-label"));
                item.add(propertyPanel);
                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
            }
        };
        extensionProperties.setReuseItems(true);
        form.add(extensionProperties);

        initButtons(form);
    }

    private IModel<String> createStyleClassModel(final IModel<PropertyWrapper> wrapper) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PropertyWrapper property = wrapper.getObject();
                return property.isVisible() ? "visible" : null;
            }
        };
    }

    private void initButtons(Form form) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                progressReporter.onSaveSubmit();
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(form);
                target.add(getFeedbackPanel());
            }
        };
        progressReporter.registerSaveButton(save);
        form.add(save);

        AjaxSubmitButton abortButton = new AjaxSubmitButton("abort",
                createStringResource("PageBase.button.abort")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                progressReporter.onAbortSubmit(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        progressReporter.registerAbortButton(abortButton);
        form.add(abortButton);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                backPerformed(target);
            }
        };
        form.add(back);

        form.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, true, false));          // TODO add "show reconcile affected" when implemented for Orgs
    }

    private boolean isEditingOrgUnit() {
        StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return oid != null && StringUtils.isNotEmpty(oid.toString());
    }

    private void backPerformed(AjaxRequestTarget target) {
        setResponsePage(PageOrgTree.class);
    }

    //todo improve later [erik]
    private PrismObject<OrgType> buildUnitFromModel(List<ObjectReferenceType> parentOrgList) throws SchemaException {
        PrismObject<OrgType> org = orgModel.getObject();

        //update orgType values
        List<PrismPropertyValue> orgTypes = orgTypeModel.getObject();
        PrismProperty orgType = org.findOrCreateProperty(OrgType.F_ORG_TYPE);
        orgType.clear();

        for (PrismPropertyValue type : orgTypes) {
            if (StringUtils.isNotEmpty((String) type.getValue())) {
                orgType.addValue(type);
            }
        }

        //update mailDomain values
        List<PrismPropertyValue> orgMailDomains = orgMailDomainModel.getObject();
        PrismProperty mailDomain = org.findOrCreateProperty(OrgType.F_MAIL_DOMAIN);
        mailDomain.clear();

        for(PrismPropertyValue mail: orgMailDomains){
            if(StringUtils.isNotEmpty((String)mail.getValue())){
                mailDomain.addValue(mail);
            }
        }

        //We are creating new OrgUnit
        if(parentOrgList == null){
            if(parentOrgUnitsModel != null && parentOrgUnitsModel.getObject() != null){
                for (OrgType parent : parentOrgUnitsModel.getObject()) {
                    if (parent != null && WebMiscUtil.getName(parent) != null && !WebMiscUtil.getName(parent).isEmpty()) {
                        ObjectReferenceType ref = new ObjectReferenceType();
                        ref.setOid(parent.getOid());
                        ref.setType(OrgType.COMPLEX_TYPE);
                        org.asObjectable().getParentOrgRef().add(ref);
                    }
                }
            }

        //We are editing OrgUnit
        }else if (parentOrgUnitsModel != null && parentOrgUnitsModel.getObject() != null) {
            for (OrgType parent : parentOrgUnitsModel.getObject()) {
                if (parent != null && WebMiscUtil.getName(parent) != null && !WebMiscUtil.getName(parent).isEmpty()) {
                    if(!isOrgParent(parent, parentOrgList)){
                        ObjectReferenceType ref = new ObjectReferenceType();
                        ref.setOid(parent.getOid());
                        ref.setType(OrgType.COMPLEX_TYPE);
                        org.asObjectable().getParentOrgRef().add(ref);
                    }
                }
            }
        }

        //Delete parentOrgUnits from edited OrgUnit
        if(isEditingOrgUnit()){
            if(parentOrgUnitsModel != null && parentOrgUnitsModel.getObject() != null){
                for(ObjectReferenceType parent: parentOrgList){
                    if(!isRefInParentOrgModel(parent)){
                        org.asObjectable().getParentOrgRef().remove(parent);
                    }
                }
            }
        }

        return org;
    }

    private boolean isRefInParentOrgModel(ObjectReferenceType reference){
        for(OrgType parent: parentOrgUnitsModel.getObject()){
            if(reference.getOid().equals(parent.getOid())){
                return true;
            }
        }
        return false;
    }

    private boolean isOrgParent(OrgType unit, List<ObjectReferenceType> parentList){
        for(ObjectReferenceType parentRef: parentList){
            if(unit.getOid().equals(parentRef.getOid())){
                return true;
            }
        }

        return false;
    }

    //todo improve later [erik]
    private void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(SAVE_UNIT);
        PrismObject<OrgType> newOrgUnit;

        try {
            reviveModels();
            delta = null;

            if (!isEditingOrgUnit()) {
                newOrgUnit = buildUnitFromModel(null);

                //handle assignments
                PrismObjectDefinition orgDef = newOrgUnit.getDefinition();
                PrismContainerDefinition assignmentDef = orgDef.findContainerDefinition(OrgType.F_ASSIGNMENT);
                AssignmentTablePanel assignmentPanel = (AssignmentTablePanel)get(createComponentPath(ID_FORM, ID_ASSIGNMENTS_TABLE));
                assignmentPanel.handleAssignmentsWhenAdd(newOrgUnit, assignmentDef, newOrgUnit.asObjectable().getAssignment());

                //handle inducements
                PrismContainerDefinition inducementDef = orgDef.findContainerDefinition(OrgType.F_INDUCEMENT);
                AssignmentTablePanel inducementPanel = (AssignmentTablePanel)get(createComponentPath(ID_FORM, ID_INDUCEMENTS_TABLE));
                inducementPanel.handleAssignmentsWhenAdd(newOrgUnit, inducementDef, newOrgUnit.asObjectable().getInducement());

                delta = ObjectDelta.createAddDelta(newOrgUnit);
            } else {
                PrismObject<OrgType> oldOrgUnit = WebModelUtils.loadObject(OrgType.class, orgModel.getObject().asObjectable().getOid(), result, this);
                newOrgUnit = buildUnitFromModel(oldOrgUnit.asObjectable().getParentOrgRef());

                delta = oldOrgUnit.diff(newOrgUnit);

                //handle assignments
                SchemaRegistry registry = getPrismContext().getSchemaRegistry();
                PrismObjectDefinition objectDefinition = registry.findObjectDefinitionByCompileTimeClass(OrgType.class);
                PrismContainerDefinition assignmentDef = objectDefinition.findContainerDefinition(OrgType.F_ASSIGNMENT);
                AssignmentTablePanel assignmentPanel = (AssignmentTablePanel)get(createComponentPath(ID_FORM, ID_ASSIGNMENTS_TABLE));
                assignmentPanel.handleAssignmentDeltas(delta, assignmentDef, OrgType.F_ASSIGNMENT);

                //handle inducements
                PrismContainerDefinition inducementDef = objectDefinition.findContainerDefinition(OrgType.F_INDUCEMENT);
                AssignmentTablePanel inducementPanel = (AssignmentTablePanel)get(createComponentPath(ID_FORM, ID_INDUCEMENTS_TABLE));
                inducementPanel.handleAssignmentDeltas(delta, inducementDef, OrgType.F_INDUCEMENT);
            }

            ObjectDelta extensionDelta = saveExtension(result);
            ObjectDelta extDelta = null;

            if(extensionDelta != null){
                if(isEditingOrgUnit()){
                    extDelta = extensionDelta;
                } else {
                    extDelta = delta.getObjectToAdd().diff(extensionDelta.getObjectToAdd());
                }
            }

            if(delta != null){
                if(extDelta != null){
                    delta = ObjectDelta.summarize(delta, extDelta);
                }

                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Saving changes for org. unit: {}", delta.debugDump());
                }

                ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
                ModelExecuteOptions options = executeOptions.createOptions();

                progressReporter.executeChanges(deltas, options, createSimpleTask(SAVE_UNIT), result, target);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save org. unit", ex);
            result.recordFatalError("Couldn't save org. unit.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!result.isInProgress()) {
            finishProcessing(target, result);
        }
    }

    public void finishProcessing(AjaxRequestTarget target, OperationResult result) {
        if (!executeOptionsModel.getObject().isKeepDisplayingResults() && progressReporter.isAllSuccess() && WebMiscUtil.isSuccessOrHandledErrorOrInProgress(result)) {
            showResultInSession(result);
            setResponsePage(PageOrgTree.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private ObjectDelta saveExtension(OperationResult result){
        ObjectDelta delta = null;

        try{
            WebMiscUtil.revive(orgModel, getPrismContext());
            WebMiscUtil.revive(extensionModel, getPrismContext());

            delta = orgWrapper.getObjectDelta();
            if(orgWrapper.getOldDelta() != null){
                delta = ObjectDelta.summarize(orgWrapper.getOldDelta(), delta);
            }

            if(LOGGER.isTraceEnabled()){
                LOGGER.trace("Org delta computed from extension:\n{}", new Object[]{delta.debugDump(3)});
            }
        } catch (Exception e){
            result.recordFatalError(getString("PageOrgUnit.message.cantCreateExtensionDelta"), e);
            LoggingUtils.logException(LOGGER, "Can't create delta for org. unit extension.", e);
            showResult(result);

        }

        return delta;
    }

    private PrismObject<OrgType> loadOrgUnit(PrismObject<OrgType> unitToEdit) {
        OperationResult result = new OperationResult(LOAD_UNIT);

        PrismObject<OrgType> org = null;
        try {
            if (!isEditingOrgUnit()) {
                if (unitToEdit == null) {
                    OrgType o = new OrgType();
                    ActivationType defaultActivation = new ActivationType();
                    defaultActivation.setAdministrativeStatus(ActivationStatusType.ENABLED);
                    o.setActivation(defaultActivation);
                    getPrismContext().adopt(o);
                    org = o.asPrismObject();
                } else {
                    org = unitToEdit;
                }
            } else {
                StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                org = WebModelUtils.loadObject(OrgType.class, oid.toString(), result, this);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load org. unit", ex);
            result.recordFatalError("Couldn't load org. unit.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
        }

        if (org == null) {
            showResultInSession(result);
            throw new RestartResponseException(PageOrgTree.class);
        }

        return org;
    }

    private List<OrgType> loadParentOrgUnits() {
        List<OrgType> parentList = new ArrayList<>();
        List<ObjectReferenceType> refList = new ArrayList<>();
        OrgType orgHelper;
        Task loadTask = createSimpleTask(LOAD_PARENT_UNITS);
        OperationResult result = new OperationResult(LOAD_PARENT_UNITS);

        OrgType actOrg = orgModel.getObject().asObjectable();

        if (actOrg != null) {
            refList.addAll(actOrg.getParentOrgRef());
        }

        try {
            if (!refList.isEmpty()) {
                //todo improve, use IN OID search, use WebModelUtils
                for (ObjectReferenceType ref : refList) {
                    String oid = ref.getOid();
                    orgHelper = getModelService().getObject(OrgType.class, oid, null, loadTask, result).asObjectable();
                    parentList.add(orgHelper);
                }
            }
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't load parent org. unit refs.", e);
            result.recordFatalError("Couldn't load parent org. unit refs.", e);
        } finally {
            result.computeStatus();
        }

        if (parentList.isEmpty())
            parentList.add(new OrgType());

        return parentList;
    }

    private void reviveModels() throws SchemaException {
        WebMiscUtil.revive(orgModel, getPrismContext());
        WebMiscUtil.revive(parentOrgUnitsModel, getPrismContext());
    }


}
