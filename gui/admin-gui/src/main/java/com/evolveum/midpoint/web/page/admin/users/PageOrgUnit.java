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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.*;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;

import java.util.Collection;

/**
 * @author lazyman
 */
public class PageOrgUnit extends PageAdminUsers {

    public static final String PARAM_ORG_ID = "orgId";

    private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);
    private static final String DOT_CLASS = PageOrgUnit.class.getName() + ".";
    private static final String LOAD_UNIT = DOT_CLASS + "loadOrgUnit";
    private static final String SAVE_UNIT = DOT_CLASS + "saveOrgUnit";

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

    private IModel<PrismObject<OrgType>> orgModel = new LoadableModel<PrismObject<OrgType>>(false) {

        @Override
        protected PrismObject<OrgType> load() {
            return loadOrgUnit();
        }
    };

    public PageOrgUnit() {
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
        MultiValueTextFormGroup orgType = new MultiValueTextFormGroup(ID_ORG_TYPE,
                new PrismPropertyModel(orgModel, OrgType.F_ORG_TYPE),
                createStringResource("OrgType.orgType"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        form.add(orgType);

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
        StringValue oid = getPageParameters().get(PageOrgUnit.PARAM_ORG_ID);
        return oid != null && StringUtils.isNotEmpty(oid.toString());
    }

    private void backPerformed(AjaxRequestTarget target) {

    }

    private void savePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(SAVE_UNIT);
        try {
            ModelService model = getModelService();
            ObjectDelta delta;
            if (!isEditing()) {
                delta = ObjectDelta.createAddDelta(orgModel.getObject());
            } else {
                PrismObject<OrgType> newOrgUnit = orgModel.getObject();
                PrismObject<OrgType> oldOrgUnit;

                OperationResult subResult = result.createSubresult(LOAD_UNIT);
                try {
                    oldOrgUnit = getModelService().getObject(OrgType.class, newOrgUnit.getOid(), null,
                            createSimpleTask(LOAD_UNIT), subResult);
                } finally {
                    subResult.computeStatus();
                }

                delta = oldOrgUnit.diff(newOrgUnit);
            }

            Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
            model.executeChanges(deltas, null, createSimpleTask(SAVE_UNIT), result);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't save org. unit", ex);
            result.recordFatalError("Couldn't save org. unit.", ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            setResponsePage(PageOrgStruct.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private PrismObject<OrgType> loadOrgUnit() {
        OperationResult result = new OperationResult(LOAD_UNIT);

        PrismObject<OrgType> org = null;
        try {
            if (!isEditing()) {
                OrgType o = new OrgType();
                getMidpointApplication().getPrismContext().adopt(o);
                org = o.asPrismObject();
            } else {
                StringValue oid = getPageParameters().get(PageOrgUnit.PARAM_ORG_ID);
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
}
