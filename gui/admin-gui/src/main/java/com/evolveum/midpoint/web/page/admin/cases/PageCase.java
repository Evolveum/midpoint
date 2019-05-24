/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.web.page.admin.cases;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.ComponentConstants;
import com.evolveum.midpoint.gui.api.component.tabs.CountablePanelTab;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.objectdetails.AssignmentHolderTypeMainPanel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@PageDescriptor(url = "/admin/case", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                label = "PageAdminCases.auth.casesAll.label",
                description = "PageAdminCases.auth.casesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_URL,
                label = "PageCase.auth.case.label",
                description = "PageCase.auth.case.description")})
public class PageCase  extends PageAdminObjectDetails<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageCase.class);
    private static final String DOT_CLASS = PageCase.class.getName() + ".";
    private static final String OPERATION_LOAD_CASE = DOT_CLASS + "loadCase";
    private static final String DEFAULT_OPERATOR_OID = "00000000-0000-0000-0000-000000000002";  // administrator

    private static final String ID_SUMMARY_PANEL = "summaryPanel";

    public PageCase() {
        initialize(null);
    }

    public PageCase(PageParameters parameters) {
        getPageParameters().overwriteWith(parameters);
        initialize(null);
    }


//    @Override
//    protected void initializeModel(final PrismObject<CaseType> caseObject, boolean isNewObject, boolean isReadonly) {
//        super.initializeModel(loadCase(), isNewObject, isReadonly);
//    }

    private PrismObject<CaseType> loadCase() {
        Task task = createSimpleTask(OPERATION_LOAD_CASE);
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = getOperationOptionsBuilder()
                .item(CaseType.F_OBJECT_REF).resolve()
                .build();
        boolean emptyCase = !isEditingFocus();
        PrismObject<CaseType> caseInstance = null;
        try {
            if (emptyCase) {
                LOGGER.trace("Loading case: New case (creating)");
                CaseType newCase = new CaseType();
                getMidpointApplication().getPrismContext().adopt(newCase);
                caseInstance = newCase.asPrismObject();
            } else {
                String oid = getObjectOidParameter();

                caseInstance = WebModelServiceUtils.loadObject(CaseType.class, oid, options,
                        PageCase.this, task, result);

                if (caseInstance == null) {
                    LOGGER.trace("caseInstance:[oid]={} was null", oid);
                    getSession().error(getString("pageCase.message.cantEditCase"));
                    showResult(result);
                    throw new RestartResponseException(PageCases.class);
                }
                LOGGER.debug("CASE WORK ITEMS: {}", caseInstance.asObjectable().getWorkItem());
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get case.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load case", ex);
        }

        if (caseInstance == null) {
            if (isEditingFocus()) {
                getSession().error(getString("pageAdminFocus.message.cantEditFocus"));
            } else {
                getSession().error(getString("pageAdminFocus.message.cantNewFocus"));
            }
            throw new RestartResponseException(PageCases.class);
        }
//
//        ObjectWrapper<CaseType> wrapper;
//        ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
//        ContainerStatus status = isEditingFocus() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
//        try {
//            wrapper = owf.createObjectWrapper("PageCase.details", null, caseInstance, status, task);
//        } catch (Exception ex) {
//            result.recordFatalError("Couldn't get case.", ex);
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load case", ex);
//            try {
//				wrapper = owf.createObjectWrapper("PageCase.details", null, caseInstance, null, null, status, task);
//			} catch (SchemaException e) {
//				throw new SystemException(e.getMessage(), e);
//			}
//        }
//
//        wrapper.setShowEmpty(emptyCase);
//
//        //for now decided to make targetRef readonly
//        wrapper.getContainers().forEach(containerWrapper -> {
//            if (containerWrapper.isMain()){
//                containerWrapper.getValues().forEach(containerValueWrapper -> {
//                    PropertyOrReferenceWrapper itemWrapper = containerValueWrapper.findPropertyWrapperByName(CaseType.F_TARGET_REF);
//                    if (itemWrapper != null){
//                        itemWrapper.setReadonly(true);
//                    }
//                });
//            }
//        });

        PrismObjectWrapperFactory<CaseType> owf = getRegistry().getObjectWrapperFactory(caseInstance.getDefinition());
        PrismObjectWrapper<CaseType> wrapper;
//        ObjectWrapperFactory owf = new ObjectWrapperFactory(this);
        ItemStatus status = isEditingFocus() ? ItemStatus.NOT_CHANGED : ItemStatus.ADDED;
        try {
        	WrapperContext context = new WrapperContext(task, result);
            wrapper = owf.createObjectWrapper(caseInstance, status, context);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get case.", ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load case", ex);
            try {
            	WrapperContext context = new WrapperContext(task, result);
				wrapper = owf.createObjectWrapper(caseInstance,status, context);
			} catch (SchemaException e) {
				throw new SystemException(e.getMessage(), e);
			}
        }

//        wrapper.setShowEmpty(emptyCase);

        //for now decided to make targetRef readonly

        //TODO maybe do it while creating wrappers
//        wrapper.getContainers().forEach(containerWrapper -> {
//            if (containerWrapper.isMain()){
//                containerWrapper.getValues().forEach(containerValueWrapper -> {
//                    PropertyOrReferenceWrapper itemWrapper = containerValueWrapper.findPropertyWrapperByName(CaseType.F_TARGET_REF);
//                    if (itemWrapper != null){
//                        itemWrapper.setReadonly(true);
//                    }
//                });
//            }
//        });

        return wrapper.getObject();
    }

    @Override
    protected AbstractObjectMainPanel<CaseType> createMainPanel(String id) {
        return new AssignmentHolderTypeMainPanel<CaseType>(id, getObjectModel(), this) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ITab> createTabs(final PageAdminObjectDetails<CaseType> parentPage) {
                List<ITab> tabs = super.createTabs(parentPage);

                tabs.add(
                        new CountablePanelTab(parentPage.createStringResource("PageCase.workitemsTab"),
                                getTabVisibility(ComponentConstants.UI_CASE_TAB_WORKITEMS_URL, false, parentPage)) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return new CaseWorkitemsTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                            }

                            @Override
                            public String getCount() {
                                return Integer.toString(countWorkItems());
                            }
                        });
                tabs.add(
                        new CountablePanelTab(parentPage.createStringResource("PageCase.events"),
                                getTabVisibility(ComponentConstants.UI_CASE_TAB_EVENTS_URL, false, parentPage)) {

                            private static final long serialVersionUID = 1L;

                            @Override
                            public WebMarkupContainer createPanel(String panelId) {
                                return new CaseEventsTabPanel(panelId, getMainForm(), getObjectModel(), parentPage);
                            }

                            @Override
                            public String getCount() {
                                return Integer.toString(countEvents());
                            }
                        });
                return tabs;
            }

            @Override
            protected boolean getOptionsPanelVisibility() {
                return false;
            }

//            @Override
//            protected boolean isPreviewButtonVisible() {
//                return false;
//            }

        };
    }

//    private void savePerformed(AjaxRequestTarget target) {
//        LOGGER.debug("Saving case changes.");
//
//        OperationResult result = new OperationResult(OPERATION_SAVE_CASE);
//        Task task = createSimpleTask(OPERATION_SAVE_CASE);
//        try {
//            WebComponentUtil.revive(caseModel, getPrismContext());
//            ObjectWrapper<CaseType> wrapper = caseModel.getObject();
//            ObjectDelta<CaseType> delta = wrapper.getObjectDelta();
//            if (delta == null) {
//                return;
//            }
//            if (delta.isAdd()) {
//                CaseType object = delta.getObjectToAdd().asObjectable();
//                if (object.getName() == null || object.getName().getOrig().isEmpty()) {
//                    object.setName(new PolyStringType(OidUtil.generateOid()));
//                }
//                if (object.getState() == null || object.getState().isEmpty()) {
//                    object.setState("open");
//                }
//                createCaseWorkItems(object, task, result);
//            }
//            if (delta.getPrismContext() == null) {
//                getPrismContext().adopt(delta);
//            }
//            if (LOGGER.isTraceEnabled()) {
//                LOGGER.trace("Case delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
//            }
//
//            if (delta.isEmpty()) {
//                return;
//            }
//            WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
//
//            Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
//            deltas.add(delta);
//
//            getModelService().executeChanges(deltas, null, task, result);
//            result.recomputeStatus();
//        } catch (Exception ex) {
//            result.recordFatalError("Couldn't save case.", ex);
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save case", ex);
//        }
//
//        if (!result.isSuccess()) {
//            showResult(result);
//            target.add(getFeedbackPanel());
//        } else {
//            showResult(result);
//
//            redirectBack();
//        }
//    }

    private void createCaseWorkItems(CaseType caseInstance, Task task, OperationResult result) {
        PrismObject<ResourceType> resource;
        ObjectReferenceType resourceRef = caseInstance.getObjectRef();
        if (resourceRef != null) {
            String resourceOid = resourceRef.getOid();
            resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceOid, PageCase.this, task, result);

            if (resource != null) {
                // If resource exists, create work items for each resource business operator
                ResourceBusinessConfigurationType businessConfiguration = resource.asObjectable().getBusiness();
                List<ObjectReferenceType> operators = new ArrayList<>();
                if (businessConfiguration != null) {
                    operators.addAll(businessConfiguration.getOperatorRef());
                }
                if (operators.isEmpty()) {
                    operators.add(new ObjectReferenceType().oid(DEFAULT_OPERATOR_OID).type(UserType.COMPLEX_TYPE));
                }
                for (ObjectReferenceType operator : operators) {
                    CaseWorkItemType workItem = new CaseWorkItemType(getPrismContext())
                            .originalAssigneeRef(operator.clone())
                            .assigneeRef(operator.clone())
                            .name(caseInstance.getName().getOrig());
                    caseInstance.getWorkItem().add(workItem);
                    // TODO deadline and maybe other fields
                }
            }
        }
    }


    @Override
    protected ObjectSummaryPanel<CaseType> createSummaryPanel() {
        return new CaseSummaryPanel(ID_SUMMARY_PANEL, CaseType.class, Model.of(getObjectModel().getObject().getObject().asObjectable()), this);
    }

    @Override
    public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

    }

    @Override
    public void continueEditing(AjaxRequestTarget target) {

    }

    @Override
    public Class<CaseType> getCompileTimeClass() {
        return CaseType.class;
    }

    @Override
    protected CaseType createNewObject(){
        return new CaseType();
    }

    @Override
    protected Class getRestartResponsePage() {
        return PageCases.class;
    }

    private int countWorkItems(){
        List<CaseWorkItemType> workItemsList = getObjectModel().getObject().getObject().asObjectable().getWorkItem();
        return workItemsList == null ? 0 : workItemsList.size();
    }

    private int countEvents(){
        List<CaseEventType> eventsList = getObjectModel().getObject().getObject().asObjectable().getEvent();
        return eventsList == null ? 0 : eventsList.size();
    }
}
