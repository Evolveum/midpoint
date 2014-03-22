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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentTableDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.form.*;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.*;
import org.apache.wicket.util.string.StringValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/unit", encoder = OnePageParameterEncoder.class, action = {
        PageAdminUsers.AUTHORIZATION_ORG_ALL,
        AuthorizationConstants.NS_AUTHORIZATION + "#orgUnit"})
public class PageOrgUnit extends PageAdminUsers {

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);
    private static final String DOT_CLASS = PageOrgUnit.class.getName() + ".";
    private static final String LOAD_UNIT = DOT_CLASS + "loadOrgUnit";
    private static final String SAVE_UNIT = DOT_CLASS + "saveOrgUnit";
    private static final String LOAD_PARENT_UNITS = DOT_CLASS + "loadParentOrgUnits";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-6";

    private static final String ID_FORM = "form";
    private static final String ID_NAME = "name";
    private static final String ID_DISPLAY_NAME = "displayName";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_REQUESTABLE = "requestable";
    private static final String ID_IDENTIFIER = "identifier";
    private static final String ID_COST_CENTER = "costCenter";
    private static final String ID_LOCALITY = "locality";
    private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_PARENT_ORG_UNITS = "parentOrgUnits";
    private static final String ID_ORG_TYPE = "orgType";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";

    private static final String ID_ASSIGNMENTS_TABLE = "assignmentsPanel";
    private static final String ID_INDUCEMENTS_TABLE = "inducementsPanel";

    //private ContainerStatus status;
    private IModel<PrismObject<OrgType>> orgModel;
    private IModel<List<OrgType>> parentOrgUnitsModel;
    private IModel<List<PrismPropertyValue>> orgTypeModel;

    public PageOrgUnit() {
        this(null);
    }

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
                List<PrismPropertyValue> values = new ArrayList<PrismPropertyValue>();

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

        parentOrgUnitsModel = new LoadableModel<List<OrgType>>(false) {

            @Override
            protected List<OrgType> load() {
                return loadParentOrgUnits();
            }
        };

        //status = isEditing() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;

        initLayout();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                if (!isEditing()) {
                    return PageOrgUnit.super.createPageTitleModel().getObject();
                }

                String name = WebMiscUtil.getName(orgModel.getObject());
                return new StringResourceModel("page.title.edit", PageOrgUnit.this, null, null, name).getString();
            }
        };
    }

    private void initLayout() {
        Form form = new Form(ID_FORM);
        add(form);

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
        MultiValueTextFormGroup orgType = new MultiValueTextFormGroup(ID_ORG_TYPE, orgTypeModel,
                createStringResource("OrgType.orgType"), ID_LABEL_SIZE, ID_INPUT_SIZE, false) {

            @Override
            protected IModel<String> createTextModel(IModel model) {
                return new PropertyModel<String>(model, "value");
            }

            @Override
            protected Serializable createNewEmptyItem() {
                return new PrismPropertyValue(null, OriginType.USER_ACTION, null);
            }
        };
        form.add(orgType);

        MultiValueChoosePanel parentOrgType = new MultiValueChoosePanel(ID_PARENT_ORG_UNITS, parentOrgUnitsModel,
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
            protected Serializable createNewEmptyItem() {
                return new OrgType();
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
        };
        form.add(assignments);

        AssignmentTablePanel inducements = new AssignmentTablePanel(ID_INDUCEMENTS_TABLE, new Model<AssignmentTableDto>(),
                createStringResource("PageOrgUnit.title.inducements")){

            @Override
            public List<AssignmentType> getAssignmentTypeList(){
                return orgModel.getObject().asObjectable().getInducement();
            }
        };
        form.add(inducements);

        initButtons(form);
    }

    private void initButtons(Form form) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(form);
                target.add(getFeedbackPanel());
            }
        };
        form.add(save);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                backPerformed(target);
            }
        };
        form.add(back);
    }

    private boolean isEditing() {
        StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return oid != null && StringUtils.isNotEmpty(oid.toString());
    }

    private void backPerformed(AjaxRequestTarget target) {
        setResponsePage(PageOrgTree.class);
    }

    private PrismObject<OrgType> buildUnitFromModel() throws SchemaException {
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

        if (parentOrgUnitsModel != null && parentOrgUnitsModel.getObject() != null) {
            for (OrgType parent : parentOrgUnitsModel.getObject()) {
                if (parent != null && WebMiscUtil.getName(parent) != null && !WebMiscUtil.getName(parent).isEmpty()) {
                    ObjectReferenceType ref = new ObjectReferenceType();
                    ref.setOid(parent.getOid());
                    org.asObjectable().getParentOrgRef().add(ref);
                }
            }
        }

        return org;
    }

    private void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(SAVE_UNIT);
        try {
            ModelService model = getModelService();

            PrismObject<OrgType> newOrgUnit = buildUnitFromModel();

            ObjectDelta delta = null;
            if (!isEditing()) {
                delta = ObjectDelta.createAddDelta(newOrgUnit);

                //handle assignments
                PrismObjectDefinition orgDef = newOrgUnit.getDefinition();
                PrismContainerDefinition assignmentDef = orgDef.findContainerDefinition(OrgType.F_ASSIGNMENT);
                AssignmentTablePanel assignmentPanel = (AssignmentTablePanel)get(createComponentPath(ID_FORM, ID_ASSIGNMENTS_TABLE));
                assignmentPanel.handleAssignmentsWhenAdd(newOrgUnit, assignmentDef, newOrgUnit.asObjectable().getAssignment());

                //handle inducements
                PrismContainerDefinition inducementDef = orgDef.findContainerDefinition(OrgType.F_INDUCEMENT);
                AssignmentTablePanel inducementPanel = (AssignmentTablePanel)get(createComponentPath(ID_FORM, ID_INDUCEMENTS_TABLE));
                inducementPanel.handleAssignmentsWhenAdd(newOrgUnit, inducementDef, newOrgUnit.asObjectable().getInducement());

            } else {
                PrismObject<OrgType> oldOrgUnit = WebModelUtils.loadObject(OrgType.class, newOrgUnit.getOid(), result, this);
                if (oldOrgUnit != null) {
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
            }

            if (delta != null) {
                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Saving changes for org. unit: {}", delta.debugDump());
                }
                model.executeChanges(deltas, null, createSimpleTask(SAVE_UNIT), result);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save org. unit", ex);
            result.recordFatalError("Couldn't save org. unit.", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            setResponsePage(PageOrgTree.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private PrismObject<OrgType> loadOrgUnit(PrismObject<OrgType> unitToEdit) {
        OperationResult result = new OperationResult(LOAD_UNIT);

        PrismObject<OrgType> org = null;
        try {
            if (!isEditing()) {
                if (unitToEdit == null) {
                    OrgType o = new OrgType();
                    getMidpointApplication().getPrismContext().adopt(o);
                    org = o.asPrismObject();
                } else {
                    org = unitToEdit;
                }
            } else {
                StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                org = getModelService().getObject(OrgType.class, oid.toString(), null,
                        createSimpleTask(LOAD_UNIT), result);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't load org. unit", ex);
            result.recordFatalError("Couldn't load org. unit.", ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
        }

        if (org == null) {
            throw new RestartResponseException(PageOrgUnit.class);
        }

        return org;
    }

    private List<OrgType> loadParentOrgUnits() {
        List<OrgType> parentList = new ArrayList<OrgType>();
        List<ObjectReferenceType> refList = new ArrayList<ObjectReferenceType>();
        OrgType orgHelper;
        Task loadTask = createSimpleTask(LOAD_PARENT_UNITS);
        OperationResult result = new OperationResult(LOAD_PARENT_UNITS);

        OrgType actOrg = orgModel.getObject().asObjectable();

        if (actOrg != null) {
            refList.addAll(actOrg.getParentOrgRef());
        }

        try {
            if (!refList.isEmpty()) {
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

}
