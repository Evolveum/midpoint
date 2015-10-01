package com.evolveum.midpoint.web.page.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAbstractRole;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.CheckTableHeader;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.SimpleErrorPanel;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableOrgPopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignablePopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableRolePopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.page.admin.users.component.ResourcesPopup;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusShadowDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class PageAdminFocus <T extends FocusType> extends PageAdmin implements ProgressReportingAwarePage{
	
	 public static final String AUTH_USERS_ALL = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL;
	    public static final String AUTH_USERS_ALL_LABEL = "PageAdminUsers.auth.usersAll.label";
	    public static final String AUTH_USERS_ALL_DESCRIPTION = "PageAdminUsers.auth.usersAll.description";

	    public static final String AUTH_ORG_ALL = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL;
	    public static final String AUTH_ORG_ALL_LABEL = "PageAdminUsers.auth.orgAll.label";
	    public static final String AUTH_ORG_ALL_DESCRIPTION = "PageAdminUsers.auth.orgAll.description";
	
	
	 private LoadableModel<ObjectWrapper> focusModel;
	    private LoadableModel<List<FocusShadowDto>> shadowModel;
	    private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;
	    
	    private ProgressReporter progressReporter;
	    
	    private static final String DOT_CLASS = PageAdminFocus.class.getName() + ".";
	    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadFocus";
	    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
	    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
	    private static final String OPERATION_SAVE = DOT_CLASS + "save";
	    private static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";
//	    private static final String OPERATION_MODIFY_ACCOUNT = DOT_CLASS + "modifyAccount";
//	    private static final String OPERATION_PREPARE_ACCOUNTS = DOT_CLASS + "getAccountsForSubmit";
	    private static final String OPERATION_RECOMPUTE_ASSIGNMENTS = DOT_CLASS + "recomputeAssignments";
	    
	    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";
	    
	    protected static final String ID_MAIN_FORM = "mainForm";
	    private static final String ID_TAB_PANEL = "tabPanel";
	    private static final String ID_ASSIGNMENT_EDITOR = "assignmentEditor";
	    private static final String ID_ASSIGNMENT_LIST = "assignmentList";
	    private static final String ID_TASK_TABLE = "taskTable";
	    private static final String ID_FOCUS_FORM = "focusForm";
//	    private static final String ID_ACCOUNTS_DELTAS = "accountsDeltas";
	    private static final String ID_EXECUTE_OPTIONS = "executeOptions";
	    private static final String ID_SHADOW_LIST = "shadowList";
	    private static final String ID_SHADOWS = "shadows";
	    private static final String ID_ASSIGNMENTS = "assignments";
	    private static final String ID_TASKS = "tasks";
	    private static final String ID_SHADOW_MENU = "shadowMenu";
	    private static final String ID_ASSIGNMENT_MENU = "assignmentMenu";
	    private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";
	    private static final String ID_ASSIGNMENT_CHECK_ALL = "assignmentCheckAll";
	    
	    
	    private static final String MODAL_ID_RESOURCE = "resourcePopup";
	    private static final String MODAL_ID_ASSIGNABLE = "assignablePopup";
	    private static final String MODAL_ID_ASSIGNABLE_ORG = "assignableOrgPopup";
	    private static final String MODAL_ID_CONFIRM_DELETE_SHADOW = "confirmDeleteShadowPopup";
	    private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";
	    private static final String MODAL_ID_ASSIGNMENTS_PREVIEW = "assignmentsPreviewPopup";
	    
	    private static final Trace LOGGER = TraceManager.getTrace(PageAdminFocus.class);
	    
	 // used to determine whether to leave this page or stay on it (after operation finishing)
	    private ObjectDelta delta;    
	    
	    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
        = new LoadableModel<ExecuteChangeOptionsDto>(false) {
	    	
    @Override
    protected ExecuteChangeOptionsDto load() {
        return new ExecuteChangeOptionsDto();
    }
};


@Override
protected IModel<String> createPageTitleModel(){
    return new LoadableModel<String>() {

        @Override
        protected String load() {
            if(!isEditingFocus()){
                return createStringResource("pageAdminFocus.title.newFocusType").getObject();
            }

            return createStringResource("pageAdminFocus.title.editFocusType").getObject();
        }
    };
}

@Override
protected IModel<String> createPageSubTitleModel(){
    return new LoadableModel<String>() {

        @Override
        protected String load() {
            if(!isEditingFocus()){
                return createStringResource("pageAdminFocus.subTitle.new"+getCompileTimeClass().getSimpleName()).getObject();
            }

            String name = null;
            if(getFocusWrapper() != null && getFocusWrapper().getObject() != null){
                name = WebMiscUtil.getName(getFocusWrapper().getObject());
            }

            return createStringResource("pageAdminFocus.subTitle.edit"+getCompileTimeClass().getSimpleName(), name).getObject();
        }
    };
}

public LoadableModel<ObjectWrapper> getFocusModel() {
	return focusModel;
}
	
	
	 public void initialize(final PrismObject<T> userToEdit) {
		 focusModel = new LoadableModel<ObjectWrapper>(false) {

	            @Override
	            protected ObjectWrapper load() {
	                return loadFocusWrapper(userToEdit);
	            }
	        };
	        shadowModel = new LoadableModel<List<FocusShadowDto>>(false) {

	            @Override
	            protected List<FocusShadowDto> load() {
	                return loadShadowWrappers();
	            }
	        };
	        assignmentsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

	            @Override
	            protected List<AssignmentEditorDto> load() {
	                return loadAssignments();
	            }
	        };
	        
	        performCustomInitialization();

	        initLayout();
	    }
	 
	 protected void performCustomInitialization(){
		 
	 }
	 
	 protected abstract T createNewFocus();
	 
	 protected abstract void initCustomLayout(Form mainForm);
	 
	 protected abstract void initTabs(List<ITab> tabs);
	 
	 private void initLayout(){
		 final Form mainForm = new Form(ID_MAIN_FORM, true);
	        mainForm.setMaxSize(MidPointApplication.FOCUS_PHOTO_MAX_FILE_SIZE);
	        mainForm.setMultiPart(true);
	        add(mainForm);

	        progressReporter = ProgressReporter.create(this, mainForm, "progressPanel");

//	        PrismObjectPanel userForm = new PrismObjectPanel(ID_FOCUS_FORM, focusModel, new PackageResourceReference(
//	                ImgResources.class, ImgResources.USER_PRISM), mainForm, this) {
//
//	            @Override
//	            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
//	                return createStringResource("pageAdminFocus.description");
//	            }
//	        };
//	        mainForm.add(userForm);
	        
	        List<ITab> tabs = new ArrayList<>();
	        tabs.add(new AbstractTab(createStringResource("pageAdminFocus.basic")) {

	            @Override
	            public WebMarkupContainer getPanel(String panelId) {
	                return new BaseFocusPanel<T>(panelId, mainForm, focusModel, shadowModel, assignmentsModel, PageAdminFocus.this) {
	                	
	                	@Override
	                	protected T createNewFocus() {
	                		return PageAdminFocus.this.createNewFocus();
	                	}

						
						@Override
						protected void reviveCustomModels() throws SchemaException {
							PageAdminFocus.this.reviveCustomModels();
							
						}

						@Override
						protected Class<T> getCompileTimeClass() {
							return PageAdminFocus.this.getCompileTimeClass();
						}

						@Override
						protected Class getRestartResponsePage() {
							return PageAdminFocus.this.getRestartResponsePage();
						}
					};
	            }
	        });
	        
	        initTabs(tabs);
	        
	        TabbedPanel focusTabPanel = new TabbedPanel(ID_TAB_PANEL, tabs){
	            @Override
	            protected WebMarkupContainer newLink(String linkId, final int index) {
	                return new AjaxSubmitLink(linkId) {

	                	@Override
	                	protected void onError(AjaxRequestTarget target,
	                			org.apache.wicket.markup.html.form.Form<?> form) {
	                		super.onError(target, form);
	                		 target.add(getFeedbackPanel());
	                	}
	                  
	                	@Override
	                	protected void onSubmit(AjaxRequestTarget target,
	                			org.apache.wicket.markup.html.form.Form<?> form) {
	                		 super.onSubmit(target, form);

		                        setSelectedTab(index);
		                        if (target != null) {
		                            target.add(findParent(TabbedPanel.class));
		                        }
	                	}

	                };
	            }
	        };
	        focusTabPanel.setOutputMarkupId(true);

	        mainForm.add(focusTabPanel);

//	        WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
//	        shadows.setOutputMarkupId(true);
//	        mainForm.add(shadows);
//	        initShadows(shadows);
//
//	        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
//	        assignments.setOutputMarkupId(true);
//	        mainForm.add(assignments);
//	        initAssignments(assignments);
//
//	        WebMarkupContainer tasks = new WebMarkupContainer(ID_TASKS);
//	        tasks.setOutputMarkupId(true);
//	        mainForm.add(tasks);
//	        initTasks(tasks);
//
	        initButtons(mainForm);
	        initCustomLayout(mainForm);
//
//	        initResourceModal();
//	        initAssignableModal();
//	        initConfirmationDialogs();
//	        
//	        initCustomLayout(mainForm);
	 }
	 public ObjectWrapper getFocusWrapper() {
		return focusModel.getObject();
	}
	 
	 public List<FocusShadowDto> getFocusShadows(){
		 return shadowModel.getObject();
	 }
	 
	 public List<AssignmentEditorDto> getFocusAssignments(){
		 return assignmentsModel.getObject();
	 }
	 
	 protected abstract void reviveCustomModels()  throws SchemaException;
	 
	 protected void reviveModels() throws SchemaException {
	        WebMiscUtil.revive(focusModel, getPrismContext());
	        WebMiscUtil.revive(shadowModel, getPrismContext());
	        WebMiscUtil.revive(assignmentsModel, getPrismContext());
	        reviveCustomModels();
//	        WebMiscUtil.revive(summaryUser, getPrismContext());
	    }
	 protected abstract Class<T> getCompileTimeClass();
	 protected abstract Class getRestartResponsePage();
	 
	 private ObjectWrapper loadFocusWrapper(PrismObject<T> userToEdit) {
	        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
	        PrismObject<T> focus = null;
	        try {
	            if (!isEditingFocus()) {
	                if (userToEdit == null) {
//	                    UserType userType = new UserType();'
	                	T focusType = createNewFocus();
	                    getMidpointApplication().getPrismContext().adopt(focusType);
	                    focus = focusType.asPrismObject();
	                } else {
	                	focus= userToEdit;
	                }
	            } else {
	                Task task = createSimpleTask(OPERATION_LOAD_USER);

	                StringValue userOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
	                Collection options = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
	                        GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));

	                focus = WebModelUtils.loadObject(getCompileTimeClass(), userOid.toString(), options, result, this);
	                
	            }

	            result.recordSuccess();
	        } catch (Exception ex) {
	            result.recordFatalError("Couldn't get focus.", ex);
	            LoggingUtils.logException(LOGGER, "Couldn't load focus", ex);
	        }

	        if (!result.isSuccess()) {
	            showResultInSession(result);
	        }

	        if (focus == null) {
	            if (isEditingFocus()) {
	                getSession().error(getString("pageAdminFocus.message.cantEditFocus"));
	            } else {
	                getSession().error(getString("pageAdminFocus.message.cantNewFocus"));
	            }
	            throw new RestartResponseException(getRestartResponsePage());
	        }

	        ContainerStatus status = isEditingFocus() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
	        ObjectWrapper wrapper = null;
	        try{
	        	wrapper = ObjectWrapperUtil.createObjectWrapper("pageAdminFocus.focusDetails", null, focus, status, this);
	        } catch (Exception ex){
	        	result.recordFatalError("Couldn't get user.", ex);
	            LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
	            wrapper = new ObjectWrapper("pageAdminFocus.focusDetails", null, focus, null, status, this);
	        }
//	        ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails", null, user, status);
	        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
	            showResultInSession(wrapper.getResult());
	        }

	        wrapper.setShowEmpty(!isEditingFocus());
	        return wrapper;
	    }
	 
//	 protected void finishProcessing(ObjectDelta objectDelta, AjaxRequestTarget target, OperationResult result){
//		 delta = objectDelta;
//		 finishProcessing(target, result);
//	 }
	 
	 public Object findParam(String param, String oid, OperationResult result) {

	        Object object = null;

	        for (OperationResult subResult : result.getSubresults()) {
	            if (subResult != null && subResult.getParams() != null) {
	                if (subResult.getParams().get(param) != null
	                        && subResult.getParams().get(OperationResult.PARAM_OID) != null
	                        && subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
	                    return subResult.getParams().get(param);
	                }
	                object = findParam(param, oid, subResult);

	            }
	        }
	        return object;
	    }
	 
	 @Override
	    public void finishProcessing(AjaxRequestTarget target, OperationResult result) {
		    boolean userAdded = delta != null && delta.isAdd() && StringUtils.isNotEmpty(delta.getOid());
	        if (!executeOptionsModel.getObject().isKeepDisplayingResults() && progressReporter.isAllSuccess() && (userAdded || !result.isFatalError())) {           // TODO
	            showResultInSession(result);
	            // todo refactor this...what is this for? why it's using some
	            // "shadow" param from result???
	            PrismObject<T> focus = getFocusWrapper().getObject();
	            T focusType = focus.asObjectable();
	            for (ObjectReferenceType ref : focusType.getLinkRef()) {
	                Object o = findParam("shadow", ref.getOid(), result);
	                if (o != null && o instanceof ShadowType) {
	                    ShadowType accountType = (ShadowType) o;
	                    OperationResultType fetchResult = accountType.getFetchResult();
	                    if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
	                        showResultInSession(OperationResult.createOperationResult(fetchResult));
	                    }
	                }
	            }
	            setSpecificResponsePage();
	        } else {
	            showResult(result);
	            target.add(getFeedbackPanel());

	            // if we only stayed on the page because of displaying results, hide the Save button
	            // (the content of the page might not be consistent with reality, e.g. concerning the accounts part...
	            // this page was not created with the repeated save possibility in mind)
	            if (userAdded || !result.isFatalError()) {
	                progressReporter.hideSaveButton(target);
	            }
	        }
	    }
	 
	 
	 private List<FocusShadowDto> loadShadowWrappers() {
	        List<FocusShadowDto> list = new ArrayList<FocusShadowDto>();

	        ObjectWrapper focus = focusModel.getObject();
	        PrismObject<T> prismUser = focus.getObject();
	        List<ObjectReferenceType> references = prismUser.asObjectable().getLinkRef();

	        Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
	        for (ObjectReferenceType reference : references) {
	            OperationResult subResult = new OperationResult(OPERATION_LOAD_SHADOW);
	            String resourceName = null;
	            try {
	                Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
	                        ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

	                if (reference.getOid() == null) {
	                    continue;
	                }

	                PrismObject<ShadowType> shadow = WebModelUtils.loadObject(ShadowType.class, reference.getOid(), options, subResult, this);
	                
//	                PrismObject<ShadowType> account = getModelService().getObject(ShadowType.class,
//	                        reference.getOid(), options, task, subResult);
	                ShadowType shadowType = shadow.asObjectable();

	                OperationResultType fetchResult = shadowType.getFetchResult();

	                ResourceType resource = shadowType.getResource();
	                resourceName = WebMiscUtil.getName(resource);
	                
//	                resourceName = WebMiscUtil.getOrigStringFromPoly(shadowType.getResourceRef().getTargetName());

	                StringBuilder description = new StringBuilder();
	                if (shadowType.getIntent() != null) {
	                    description.append(shadowType.getIntent()).append(", ");
	                }
	                description.append(WebMiscUtil.getOrigStringFromPoly(shadowType.getName()));

	                ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(resourceName, description.toString(),
	                		shadow, ContainerStatus.MODIFYING, true, this);
//	                ObjectWrapper wrapper = new ObjectWrapper(resourceName, WebMiscUtil.getOrigStringFromPoly(accountType
//	                        .getName()), account, ContainerStatus.MODIFYING);
	                wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
	                wrapper.setSelectable(true);
	                wrapper.setMinimalized(true);
	                
	                PrismContainer<ShadowAssociationType> associationContainer = shadow.findContainer(ShadowType.F_ASSOCIATION);
	                if (associationContainer != null && associationContainer.getValues() != null){
	                	List<PrismProperty> associations = new ArrayList<>(associationContainer.getValues().size());
	                	for (PrismContainerValue associationVal : associationContainer.getValues()){
	                		ShadowAssociationType associationType = (ShadowAssociationType) associationVal.asContainerable();
	                		ObjectReferenceType shadowRef = associationType.getShadowRef();
	                		if (shadowRef != null) { // shadowRef can be null in case of "broken" associations
		                        // we can safely eliminate fetching from resource, because we need only the name
	                			PrismObject<ShadowType> association = getModelService().getObject(ShadowType.class, shadowRef.getOid(),
		                                SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, subResult);
		                		associations.add(association.findProperty(ShadowType.F_NAME));
	                		}
	                	}
	                	
	                	wrapper.setAssociations(associations);
	                	
	                }

	                wrapper.initializeContainers(this);

	                list.add(new FocusShadowDto(wrapper, UserDtoStatus.MODIFY));

	                subResult.recomputeStatus();
	            } catch (ObjectNotFoundException ex) {
	                // this is fix for MID-854, full user/accounts/assignments reload if accountRef reference is broken
	                // because consistency already fixed it.

	            	focusModel.reset();
	                shadowModel.reset();
	                assignmentsModel.reset();
	            } catch (Exception ex) {
	                subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
	                LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
	                list.add(new FocusShadowDto(false, resourceName, subResult));
	            } finally {
	                subResult.computeStatus();
	            }
	        }

	        return list;
	    }
	 
	 
	 private List<AssignmentEditorDto> loadAssignments() {
	        List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

	        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);

	        ObjectWrapper focusWrapper = focusModel.getObject();
	        PrismObject<T> focus = focusWrapper.getObject();
	        List<AssignmentType> assignments = focus.asObjectable().getAssignment();
	        for (AssignmentType assignment : assignments) {
	           
	            list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
	        }

	        Collections.sort(list);

	        return list;
	    }
	 
	 private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
	        OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
	        subResult.addParam("targetRef", ref.getOid());
	        PrismObject target = null;
	        try {
	            Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
	            Class type = ObjectType.class;
	            if (ref.getType() != null) {
	                type = getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
	            }
	            target = getModelService().getObject(type, ref.getOid(), null, task, subResult);
	            subResult.recordSuccess();
	        } catch (Exception ex) {
	            LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
	            subResult.recordFatalError("Couldn't get assignment target ref.", ex);
	        }

	        if(!subResult.isHandledError() && !subResult.isSuccess()){
	            showResult(subResult);
	        }

	        return target;
	    }
	 
	 private boolean isEditingFocus() {
	        StringValue focusOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
	        return focusOid != null && StringUtils.isNotEmpty(focusOid.toString());
	    }
	 
	 protected void savePerformed(AjaxRequestTarget target) {
	        LOGGER.debug("Save user.");
	        OperationResult result = new OperationResult(OPERATION_SAVE);
	        ObjectWrapper userWrapper = getFocusWrapper();
	        // todo: improve, delta variable is quickfix for MID-1006
	        // redirecting to user list page everytime user is created in repository
	        // during user add in gui,
	        // and we're not taking care about account/assignment create errors
	        // (error message is still displayed)
	        delta = null;

	        Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
	        ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
	        ModelExecuteOptions options = executeOptions.createOptions();
	        LOGGER.debug("Using options {}.", new Object[]{executeOptions});

	        try {
	            reviveModels();

	            delta = userWrapper.getObjectDelta();
	            if (userWrapper.getOldDelta() != null) {
	                delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
	            }
	            if (LOGGER.isTraceEnabled()) {
	                LOGGER.trace("User delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
	            }
	        } catch (Exception ex) {
	            result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
	            LoggingUtils.logException(LOGGER, "Create user failed", ex);
	            showResult(result);
	            return;
	        }

	        switch (userWrapper.getStatus()) {
	            case ADDING:
	                try {
	                    PrismObject<T> focus = delta.getObjectToAdd();
	                    WebMiscUtil.encryptCredentials(focus, true, getMidpointApplication());
	                    prepareFocusForAdd(focus);
	                    getPrismContext().adopt(focus, getCompileTimeClass());
	                    if (LOGGER.isTraceEnabled()) {
	                        LOGGER.trace("Delta before add user:\n{}", new Object[]{delta.debugDump(3)});
	                    }

	                    if (!delta.isEmpty()) {
	                        delta.revive(getPrismContext());

	                        Collection<SimpleValidationError> validationErrors = performCustomValidation(focus, WebMiscUtil.createDeltaCollection(delta));
	                        if(validationErrors != null && !validationErrors.isEmpty()){
	                            for(SimpleValidationError error: validationErrors){
	                                LOGGER.error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
	                                error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
	                            }

	                            target.add(getFeedbackPanel());
	                            return;
	                        }

	                        progressReporter.executeChanges(WebMiscUtil.createDeltaCollection(delta), options, task, result, target);
	                    } else {
	                        result.recordSuccess();
	                    }
	                } catch (Exception ex) {
	                    result.recordFatalError(getString("pageFocus.message.cantCreateFocus"), ex);
	                    LoggingUtils.logException(LOGGER, "Create user failed", ex);
	                }
	                break;

	            case MODIFYING:
	                try {
	                    WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
	                    prepareFocusDeltaForModify(delta);

	                    if (LOGGER.isTraceEnabled()) {
	                        LOGGER.trace("Delta before modify user:\n{}", new Object[]{delta.debugDump(3)});
	                    }

	                    List<ObjectDelta<? extends ObjectType>> accountDeltas = modifyShadows(result);
	                    Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

	                    if (!delta.isEmpty()) {
	                        delta.revive(getPrismContext());
	                        deltas.add(delta);
	                    }
	                    for (ObjectDelta accDelta : accountDeltas) {
	                        if (!accDelta.isEmpty()) {
	                            //accDelta.setPrismContext(getPrismContext());
	                            accDelta.revive(getPrismContext());
	                            deltas.add(accDelta);
	                        }
	                    }

	                    if (delta.isEmpty() && ModelExecuteOptions.isReconcile(options)) {
	                        ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(getCompileTimeClass(),
	                                userWrapper.getObject().getOid(), getPrismContext());
	                        deltas.add(emptyDelta);

	                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
	                        if(validationErrors != null && !validationErrors.isEmpty()){
	                            for(SimpleValidationError error: validationErrors){
	                                LOGGER.error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
	                                error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
	                            }

	                            target.add(getFeedbackPanel());
	                            return;
	                        }

	                        progressReporter.executeChanges(deltas, options, task, result, target);
	                    } else if (!deltas.isEmpty()) {
	                        Collection<SimpleValidationError> validationErrors = performCustomValidation(null, deltas);
	                        if(validationErrors != null && !validationErrors.isEmpty()){
	                            for(SimpleValidationError error: validationErrors){
	                                LOGGER.error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
	                                error("Validation error, attribute: '" + error.printAttribute() + "', message: '" + error.getMessage() + "'.");
	                            }

	                            target.add(getFeedbackPanel());
	                            return;
	                        }

	                        progressReporter.executeChanges(deltas, options, task, result, target);
	                    } else {
	                        result.recordSuccess();
	                    }

	                } catch (Exception ex) {
	                    if (!executeForceDelete(userWrapper, task, options, result)) {
	                        result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
	                        LoggingUtils.logException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
	                    } else {
	                        result.recomputeStatus();
	                    }
	                }
	                break;
	            // support for add/delete containers (e.g. delete credentials)
	            default:
	                error(getString("pageAdminFocus.message.unsupportedState", userWrapper.getStatus()));
	        }

	        result.recomputeStatus();

	        if (!result.isInProgress()) {
	            finishProcessing(target, result);
	        }
	    }
	 
	  private boolean executeForceDelete(ObjectWrapper userWrapper, Task task, ModelExecuteOptions options,
              OperationResult parentResult) {
if (executeOptionsModel.getObject().isForce()) {
OperationResult result = parentResult.createSubresult("Force delete operation");
// List<UserAccountDto> accountDtos = accountsModel.getObject();
// List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
// ObjectDelta<UserType> forceDeleteDelta = null;
// for (UserAccountDto accDto : accountDtos) {
// if (accDto.getStatus() == UserDtoStatus.DELETE) {
// ObjectWrapper accWrapper = accDto.getObject();
// ReferenceDelta refDelta =
// ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF,
// userWrapper.getObject().getDefinition(), accWrapper.getObject());
// refDeltas.add(refDelta);
// }
// }
// if (!refDeltas.isEmpty()) {
// forceDeleteDelta =
// ObjectDelta.createModifyDelta(userWrapper.getObject().getOid(),
// refDeltas,
// UserType.class, getPrismContext());
// }
// PrismContainerDefinition def =
// userWrapper.getObject().findContainer(UserType.F_ASSIGNMENT).getDefinition();
// if (forceDeleteDelta == null) {
// forceDeleteDelta =
// ObjectDelta.createEmptyModifyDelta(UserType.class,
// userWrapper.getObject().getOid(),
// getPrismContext());
// }
try {
ObjectDelta<UserType> forceDeleteDelta = getChange(userWrapper);
forceDeleteDelta.revive(getPrismContext());

if (forceDeleteDelta != null && !forceDeleteDelta.isEmpty()) {
getModelService().executeChanges(WebMiscUtil.createDeltaCollection(forceDeleteDelta), options,
   task, result);
}
} catch (Exception ex) {
result.recordFatalError("Failed to execute delete operation with force.");
LoggingUtils.logException(LOGGER, "Failed to execute delete operation with force", ex);
return false;
}

result.recomputeStatus();
result.recordSuccessIfUnknown();
return true;
}
return false;
	}

	private ObjectDelta getChange(ObjectWrapper focusWrapper) throws SchemaException {

		List<FocusShadowDto> accountDtos = getFocusShadows();
		List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
		ObjectDelta<T> forceDeleteDelta = null;
		for (FocusShadowDto accDto : accountDtos) {
			if (!accDto.isLoadedOK()) {
				continue;
			}

			if (accDto.getStatus() == UserDtoStatus.DELETE) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						focusWrapper.getObject().getDefinition(), accWrapper.getObject());
				refDeltas.add(refDelta);
			} else if (accDto.getStatus() == UserDtoStatus.UNLINK) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						focusWrapper.getObject().getDefinition(), accWrapper.getObject().getOid());
				refDeltas.add(refDelta);
			}
		}
		if (!refDeltas.isEmpty()) {
			forceDeleteDelta = ObjectDelta.createModifyDelta(focusWrapper.getObject().getOid(), refDeltas,
					getCompileTimeClass(), getPrismContext());
		}
		PrismContainerDefinition def = focusWrapper.getObject().findContainer(UserType.F_ASSIGNMENT)
				.getDefinition();
		if (forceDeleteDelta == null) {
			forceDeleteDelta = ObjectDelta.createEmptyModifyDelta(getCompileTimeClass(), focusWrapper.getObject()
					.getOid(), getPrismContext());
		}

		handleAssignmentDeltas(forceDeleteDelta, getFocusAssignments(), def);
		return forceDeleteDelta;
	}


	    private Collection<SimpleValidationError> performCustomValidation(PrismObject<T> user, Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
	        Collection<SimpleValidationError> errors = null;

	        if(user == null){
	            if(getFocusWrapper() != null && getFocusWrapper().getObject() != null){
	                user = getFocusWrapper().getObject();

	                for (ObjectDelta delta: deltas) {
	                    if (UserType.class.isAssignableFrom(delta.getObjectTypeClass())) {  // because among deltas there can be also ShadowType deltas
	                        delta.applyTo(user);
	                    }
	                }
	            }
	        }

	        if(user != null && user.asObjectable() != null){
	            for(AssignmentType assignment: user.asObjectable().getAssignment()){
	                for(MidpointFormValidator validator: getFormValidatorRegistry().getValidators()){
	                    if(errors == null){
	                        errors = validator.validateAssignment(assignment);
	                    } else {
	                        errors.addAll(validator.validateAssignment(assignment));
	                    }
	                }
	            }
	        }

	        for(MidpointFormValidator validator: getFormValidatorRegistry().getValidators()){
	            if(errors == null){
	                errors = validator.validateObject(user, deltas);
	            } else {
	                errors.addAll(validator.validateObject(user, deltas));
	            }
	        }

	        return errors;
	    }

	    protected void prepareFocusForAdd(PrismObject<T> focus) throws SchemaException {
	        T focusType = focus.asObjectable();
	        // handle added accounts
	        List<FocusShadowDto> accounts = getFocusShadows();
	        for (FocusShadowDto accDto : accounts) {
	            if(!accDto.isLoadedOK()){
	                continue;
	            }

	            if (!UserDtoStatus.ADD.equals(accDto.getStatus())) {
	                warn(getString("pageAdminFocus.message.illegalAccountState", accDto.getStatus()));
	                continue;
	            }

	            ObjectWrapper accountWrapper = accDto.getObject();
	            ObjectDelta delta = accountWrapper.getObjectDelta();
	            PrismObject<ShadowType> account = delta.getObjectToAdd();
	            WebMiscUtil.encryptCredentials(account, true, getMidpointApplication());

	            focusType.getLink().add(account.asObjectable());
	        }
	        
	        handleAssignmentForAdd(focus, UserType.F_ASSIGNMENT, focusType.getAssignment());

//	        PrismObjectDefinition userDef = focus.getDefinition();
//	        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
//
//	        // handle added assignments
//	        // existing user assignments are not relevant -> delete them
//	        focusType.getAssignment().clear();
//	        List<AssignmentEditorDto> assignments = getFocusAssignments();
//	        for (AssignmentEditorDto assDto : assignments) {
//	            if (UserDtoStatus.DELETE.equals(assDto.getStatus())) {
//	                continue;
//	            }
//
//	            AssignmentType assignment = new AssignmentType();
//	            PrismContainerValue value = assDto.getNewValue();
//	            assignment.setupContainerValue(value);
//	            value.applyDefinition(assignmentDef, false);
//	            focusType.getAssignment().add(assignment.clone());
//
//	            // todo remove this block [lazyman] after model is updated - it has
//	            // to remove resource from accountConstruction
//	            removeResourceFromAccConstruction(assignment);
//	        }
	    }

	    protected void handleAssignmentForAdd(PrismObject<T> focus, QName containerName, List<AssignmentType> assignmentTypes) throws SchemaException{
	    	 PrismObjectDefinition userDef = focus.getDefinition();
		        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(containerName);

		        // handle added assignments
		        // existing user assignments are not relevant -> delete them
		        assignmentTypes.clear();
		        List<AssignmentEditorDto> assignments = getFocusAssignments();
		        for (AssignmentEditorDto assDto : assignments) {
		            if (UserDtoStatus.DELETE.equals(assDto.getStatus())) {
		                continue;
		            }

		            AssignmentType assignment = new AssignmentType();
		            PrismContainerValue value = assDto.getNewValue();
		            assignment.setupContainerValue(value);
		            value.applyDefinition(assignmentDef, false);
		            assignmentTypes.add(assignment.clone());

		            // todo remove this block [lazyman] after model is updated - it has
		            // to remove resource from accountConstruction
		            removeResourceFromAccConstruction(assignment);
		        }
	    }
	    
	    private List<ObjectDelta<? extends ObjectType>> modifyShadows(OperationResult result) {
	        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

	        List<FocusShadowDto> accounts = getFocusShadows();
	        OperationResult subResult = null;
	        for (FocusShadowDto account : accounts) {
	            if(!account.isLoadedOK())
	                continue;

	            try {
	                ObjectWrapper accountWrapper = account.getObject();
	                ObjectDelta delta = accountWrapper.getObjectDelta();
	                if (LOGGER.isTraceEnabled()) {
	                    LOGGER.trace("Account delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
	                }

	                if (!UserDtoStatus.MODIFY.equals(account.getStatus())) {
	                    continue;
	                }

	                if (delta.isEmpty() && (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty())) {
	                    continue;
	                }

	                if (accountWrapper.getOldDelta() != null) {
	                    delta = ObjectDelta.summarize(delta, accountWrapper.getOldDelta());
	                }

	                //what is this???
//					subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);

	                WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
	                if (LOGGER.isTraceEnabled()) {
	                    LOGGER.trace("Modifying account:\n{}", new Object[]{delta.debugDump(3)});
	                }

	                deltas.add(delta);
//					subResult.recordSuccess();
	            } catch (Exception ex) {
//					if (subResult != null) {
	                result.recordFatalError("Couldn't compute account delta.", ex);
//					}
	                LoggingUtils.logException(LOGGER, "Couldn't compute account delta", ex);
	            }
	        }

	        return deltas;
	    }

	    /**
	     * remove this method after model is updated - it has to remove resource
	     * from accountConstruction
	     */
	    @Deprecated
	    private void removeResourceFromAccConstruction(AssignmentType assignment) {
	        ConstructionType accConstruction = assignment.getConstruction();
	        if (accConstruction == null || accConstruction.getResource() == null) {
	            return;
	        }

	        ObjectReferenceType ref = new ObjectReferenceType();
	        ref.setOid(assignment.getConstruction().getResource().getOid());
	        ref.setType(ResourceType.COMPLEX_TYPE);
	        assignment.getConstruction().setResourceRef(ref);
	        assignment.getConstruction().setResource(null);
	    }

	    private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef) throws SchemaException {
	        ReferenceDelta refDelta = new ReferenceDelta(refDef, getPrismContext());

	        List<FocusShadowDto> accounts = getFocusShadows();
	        for (FocusShadowDto accDto : accounts) {
	            if(accDto.isLoadedOK()){
	                ObjectWrapper accountWrapper = accDto.getObject();
	                ObjectDelta delta = accountWrapper.getObjectDelta();
	                PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

	                PrismObject<ShadowType> account;
	                switch (accDto.getStatus()) {
	                    case ADD:
	                        account = delta.getObjectToAdd();
	                        WebMiscUtil.encryptCredentials(account, true, getMidpointApplication());
	                        refValue.setObject(account);
	                        refDelta.addValueToAdd(refValue);
	                        break;
	                    case DELETE:
	                        account = accountWrapper.getObject();
	                        refValue.setObject(account);
	                        refDelta.addValueToDelete(refValue);
	                        break;
	                    case MODIFY:
	                        // nothing to do, account modifications were applied before
	                        continue;
	                    case UNLINK:
	                        refValue.setOid(delta.getOid());
	                        refValue.setTargetType(ShadowType.COMPLEX_TYPE);
	                        refDelta.addValueToDelete(refValue);
	                        break;
	                    default:
	                        warn(getString("pageAdminFocus.message.illegalAccountState", accDto.getStatus()));
	                }
	            }
	        }

	        return refDelta;
	    }

	    protected ContainerDelta handleAssignmentDeltas(ObjectDelta<T> focusDelta, List<AssignmentEditorDto> assignments, PrismContainerDefinition def)
	            throws SchemaException {
	        ContainerDelta assDelta = new ContainerDelta(new ItemPath(), def.getName(), def, getPrismContext());

//	        PrismObject<UserType> user = getFocusWrapper().getObject();
//	        PrismObjectDefinition userDef = user.getDefinition();
//	        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

//	        List<AssignmentEditorDto> assignments = getFocusAssignments();
	        for (AssignmentEditorDto assDto : assignments) {
	            PrismContainerValue newValue = assDto.getNewValue();

	            switch (assDto.getStatus()) {
	                case ADD:
	                    newValue.applyDefinition(def, false);
	                    assDelta.addValueToAdd(newValue.clone());
	                    break;
	                case DELETE:
	                    PrismContainerValue oldValue = assDto.getOldValue();
	                    oldValue.applyDefinition(def);
	                    assDelta.addValueToDelete(oldValue.clone());
	                    break;
	                case MODIFY:
	                    if (!assDto.isModified()) {
	                        LOGGER.trace("Assignment '{}' not modified.", new Object[]{assDto.getName()});
	                        continue;
	                    }

	                    handleModifyAssignmentDelta(assDto, def, newValue, focusDelta);
	                    break;
	                default:
	                    warn(getString("pageAdminUser.message.illegalAssignmentState", assDto.getStatus()));
	            }
	        }

	        if (!assDelta.isEmpty()) {
	        	assDelta = focusDelta.addModification(assDelta);
	        }

	        // todo remove this block [lazyman] after model is updated - it has to
	        // remove resource from accountConstruction
	        Collection<PrismContainerValue> values = assDelta.getValues(PrismContainerValue.class);
	        for (PrismContainerValue value : values) {
	            AssignmentType ass = new AssignmentType();
	            ass.setupContainerValue(value);
	            removeResourceFromAccConstruction(ass);
	        }

	        return assDelta;
	    }

	    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto, PrismContainerDefinition assignmentDef,
	                                             PrismContainerValue newValue, ObjectDelta<T> focusDelta) throws SchemaException {
	        LOGGER.debug("Handling modified assignment '{}', computing delta.", new Object[]{assDto.getName()});

	        PrismValue oldValue = assDto.getOldValue();
	        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

	        for (ItemDelta delta : deltas) {
	            ItemPath deltaPath = delta.getPath().rest();
	            ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

	            delta.setParentPath(joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
	            delta.applyDefinition(deltaDef);

	            focusDelta.addModification(delta);
	        }
	    }

	    private ItemPath joinPath(ItemPath path, ItemPath deltaPath) {
	        List<ItemPathSegment> newPath = new ArrayList<ItemPathSegment>();

	        ItemPathSegment firstDeltaSegment = deltaPath != null ? deltaPath.first() : null;
	        if (path != null) {
	            for (ItemPathSegment seg : path.getSegments()) {
	                if (seg.equivalent(firstDeltaSegment)) {
	                    break;
	                }
	                newPath.add(seg);
	            }
	        }
	        if (deltaPath != null) {
	            newPath.addAll(deltaPath.getSegments());
	        }

	        return new ItemPath(newPath);
	    }

	    protected void prepareFocusDeltaForModify(ObjectDelta<T> focusDelta) throws SchemaException {
	        // handle accounts
	        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
	        PrismObjectDefinition<T> objectDefinition = registry.findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
	        PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(FocusType.F_LINK_REF);
	        ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
	        if (!refDelta.isEmpty()) {
	        	focusDelta.addModification(refDelta);
	        }

	        // handle assignments
	        PrismContainerDefinition def = objectDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
	        handleAssignmentDeltas(focusDelta, getFocusAssignments(), def);
	    }

	    protected void recomputeAssignmentsPerformed(AssignmentPreviewDialog dialog, AjaxRequestTarget target){
	        LOGGER.debug("Recompute user assignments");
	        Task task = createSimpleTask(OPERATION_RECOMPUTE_ASSIGNMENTS);
	        OperationResult result = new OperationResult(OPERATION_RECOMPUTE_ASSIGNMENTS);
	        ObjectDelta<T> delta;
	        Set<AssignmentsPreviewDto> assignmentDtoSet = new TreeSet<>();

	        try {
	            reviveModels();

	            ObjectWrapper userWrapper = getFocusWrapper();
	            delta = userWrapper.getObjectDelta();
	            if (userWrapper.getOldDelta() != null) {
	                delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
	            }

	            switch (userWrapper.getStatus()) {
	                case ADDING:
	                    PrismObject<T> focus = delta.getObjectToAdd();
	                    prepareFocusForAdd(focus);
	                    getPrismContext().adopt(focus, getCompileTimeClass());

	                    if (LOGGER.isTraceEnabled()) {
	                        LOGGER.trace("Delta before add user:\n{}", new Object[]{delta.debugDump(3)});
	                    }

	                    if (!delta.isEmpty()) {
	                        delta.revive(getPrismContext());
	                    } else {
	                        result.recordSuccess();
	                    }

	                    break;
	                case MODIFYING:
	                    prepareFocusDeltaForModify(delta);

	                    if (LOGGER.isTraceEnabled()) {
	                        LOGGER.trace("Delta before modify user:\n{}", new Object[]{delta.debugDump(3)});
	                    }

	                    List<ObjectDelta<? extends ObjectType>> accountDeltas = modifyShadows(result);
	                    Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

	                    if (!delta.isEmpty()) {
	                        delta.revive(getPrismContext());
	                        deltas.add(delta);
	                    }

	                    for (ObjectDelta accDelta : accountDeltas) {
	                        if (!accDelta.isEmpty()) {
	                             accDelta.revive(getPrismContext());
	                            deltas.add(accDelta);
	                        }
	                    }

	                    break;
	                default:
	                    error(getString("pageAdminFocus.message.unsupportedState", userWrapper.getStatus()));
	            }

	            ModelContext<UserType> modelContext = null;
	            try {
	                modelContext = getModelInteractionService().previewChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
	            } catch (NoFocusNameSchemaException e) {
	                info(getString("pageAdminFocus.message.noUserName"));
	                target.add(getFeedbackPanel());
	                return;
	            }

	            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
	            Collection<? extends EvaluatedAssignment> evaluatedAssignments = evaluatedAssignmentTriple.getNonNegativeValues();

	            if (evaluatedAssignments.isEmpty()){
	                info(getString("pageAdminFocus.message.noAssignmentsAvailable"));
	                target.add(getFeedbackPanel());
	                return;
	            }

	            List<String> directAssignmentsOids = new ArrayList<>();
	            for (EvaluatedAssignment<UserType> evaluatedAssignment : evaluatedAssignments) {
	                if (!evaluatedAssignment.isValid()) {
	                    continue;
	                }
	                // roles and orgs
	                DeltaSetTriple<? extends EvaluatedAbstractRole> evaluatedRolesTriple = evaluatedAssignment.getRoles();
	                Collection<? extends EvaluatedAbstractRole> evaluatedRoles = evaluatedRolesTriple.getNonNegativeValues();
	                for (EvaluatedAbstractRole role: evaluatedRoles) {
	                    if (role.isEvaluateConstructions()) {
	                        assignmentDtoSet.add(createAssignmentsPreviewDto(role, task, result));
	                    }
	                }

	                // all resources
	                DeltaSetTriple<EvaluatedConstruction> evaluatedConstructionsTriple = evaluatedAssignment.getEvaluatedConstructions(task, result);
	                Collection<EvaluatedConstruction> evaluatedConstructions = evaluatedConstructionsTriple.getNonNegativeValues();
	                for (EvaluatedConstruction construction : evaluatedConstructions) {
	                    assignmentDtoSet.add(createAssignmentsPreviewDto(construction));
	                }
	            }

	            
	            dialog.updateData(target, new ArrayList<>(assignmentDtoSet), directAssignmentsOids);
	            dialog.show(target);

	        } catch (Exception e) {
	            LoggingUtils.logUnexpectedException(LOGGER, "Could not create assignments preview.", e);
	            error("Could not create assignments preview. Reason: " + e);
	            target.add(getFeedbackPanel());
	        }
	    }

	    private AssignmentsPreviewDto createAssignmentsPreviewDto(EvaluatedAbstractRole evaluatedAbstractRole, Task task, OperationResult result) {
	        AssignmentsPreviewDto dto = new AssignmentsPreviewDto();
	        PrismObject<? extends AbstractRoleType> role = evaluatedAbstractRole.getRole();
	        dto.setTargetOid(role.getOid());
	        dto.setTargetName(getNameToDisplay(role));
	        dto.setTargetDescription(role.asObjectable().getDescription());
	        dto.setTargetClass(role.getCompileTimeClass());
	        dto.setDirect(evaluatedAbstractRole.isDirectlyAssigned());
	        if (evaluatedAbstractRole.getAssignment() != null) {
	            if (evaluatedAbstractRole.getAssignment().getTenantRef() != null) {
	                dto.setTenantName(nameFromReference(evaluatedAbstractRole.getAssignment().getTenantRef(), task, result));
	            }
	            if (evaluatedAbstractRole.getAssignment().getOrgRef() != null) {
	                dto.setOrgRefName(nameFromReference(evaluatedAbstractRole.getAssignment().getOrgRef(), task, result));
	            }
	        }
	        return dto;
	    }

	    private String getNameToDisplay(PrismObject<? extends AbstractRoleType> role) {
	        String n = PolyString.getOrig(role.asObjectable().getDisplayName());
	        if (StringUtils.isNotBlank(n)) {
	            return n;
	        }
	        return PolyString.getOrig(role.asObjectable().getName());
	    }

	    private String nameFromReference(ObjectReferenceType reference, Task task, OperationResult result) {
	        String oid = reference.getOid();
	        QName type = reference.getType();
	        Class<? extends ObjectType> clazz = getPrismContext().getSchemaRegistry().getCompileTimeClass(type);
	        PrismObject<? extends ObjectType> prismObject;
	        try {
	            prismObject = getModelService().getObject(clazz, oid, SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);
	        } catch (ObjectNotFoundException|SchemaException|SecurityViolationException|CommunicationException|ConfigurationException|RuntimeException e) {
	            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve name for {}: {}", e, clazz.getSimpleName(), oid);
	            return "Couldn't retrieve name for " + oid;
	        }
	        ObjectType object = prismObject.asObjectable();
	        if (object instanceof AbstractRoleType) {
	            return getNameToDisplay(object.asPrismObject());
	        } else {
	            return PolyString.getOrig(object.getName());
	        }
	    }

	    private AssignmentsPreviewDto createAssignmentsPreviewDto(EvaluatedConstruction evaluatedConstruction) {
	        AssignmentsPreviewDto dto = new AssignmentsPreviewDto();
	        PrismObject<ResourceType> resource = evaluatedConstruction.getResource();
	        dto.setTargetOid(resource.getOid());
	        dto.setTargetName(PolyString.getOrig(resource.asObjectable().getName()));
	        dto.setTargetDescription(resource.asObjectable().getDescription());
	        dto.setTargetClass(resource.getCompileTimeClass());
	        dto.setDirect(evaluatedConstruction.isDirectlyAssigned());
	        dto.setKind(evaluatedConstruction.getKind());
	        dto.setIntent(evaluatedConstruction.getIntent());
	        return dto;
	    }

	 
	

	 private void initShadows(final WebMarkupContainer accounts) {
	        InlineMenu accountMenu = new InlineMenu(ID_SHADOW_MENU, new Model((Serializable) createShadowMenu()));
	        accounts.add(accountMenu);

	        final ListView<FocusShadowDto> accountList = new ListView<FocusShadowDto>(ID_SHADOW_LIST, shadowModel) {

	            @Override
	            protected void populateItem(final ListItem<FocusShadowDto> item) {
	                PackageResourceReference packageRef;
	                final FocusShadowDto dto = item.getModelObject();

	                Panel panel;

	                if(dto.isLoadedOK()){
	                    packageRef = new PackageResourceReference(ImgResources.class,
	                            ImgResources.HDD_PRISM);

	                    panel = new PrismObjectPanel("shadow", new PropertyModel<ObjectWrapper>(
	                            item.getModel(), "object"), packageRef, (Form) PageAdminFocus.this.get(ID_MAIN_FORM), PageAdminFocus.this) {

	                        @Override
	                        protected Component createHeader(String id, IModel<ObjectWrapper> model) {
	                            return new CheckTableHeader(id, model) {

	                                @Override
	                                protected List<InlineMenuItem> createMenuItems() {
	                                    return createDefaultMenuItems(getModel());
	                                }
	                            };
	                        }
	                    };
	                } else{
	                    panel = new SimpleErrorPanel("shadow", item.getModel()){

	                        @Override
	                        public void onShowMorePerformed(AjaxRequestTarget target){
	                            OperationResult fetchResult = dto.getResult();
	                            if (fetchResult != null) {
	                                showResult(fetchResult);
	                                target.add(getPageBase().getFeedbackPanel());
	                            }
	                        }
	                    };
	                }

	                panel.setOutputMarkupId(true);
	                item.add(panel);
	            }
	        };

	        AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_SHADOW_CHECK_ALL, new Model()) {

	            @Override
	            protected void onUpdate(AjaxRequestTarget target) {
	                for(FocusShadowDto dto: accountList.getModelObject()){
	                    if(dto.isLoadedOK()){
	                        ObjectWrapper accModel = dto.getObject();
	                        accModel.setSelected(getModelObject());
	                    }
	                }

	                target.add(accounts);
	            }
	        };
	        accounts.add(accountCheckAll);

	        accounts.add(accountList);
	    }
	 
	 private void initAssignments(final WebMarkupContainer assignments) {
	        InlineMenu accountMenu = new InlineMenu(ID_ASSIGNMENT_MENU, new Model((Serializable) createAssignmentsMenu()));
	        assignments.add(accountMenu);

	        final ListView<AssignmentEditorDto> assignmentList = new ListView<AssignmentEditorDto>(ID_ASSIGNMENT_LIST,
	                assignmentsModel) {

	            @Override
	            protected void populateItem(final ListItem<AssignmentEditorDto> item) {
	                AssignmentEditorPanel assignmentEditor = new AssignmentEditorPanel(ID_ASSIGNMENT_EDITOR,
	                        item.getModel());
	                item.add(assignmentEditor);
	            }
	        };
	        assignmentList.setOutputMarkupId(true);
	        assignments.add(assignmentList);

	        AjaxCheckBox assignmentCheckAll = new AjaxCheckBox(ID_ASSIGNMENT_CHECK_ALL, new Model()) {

	            @Override
	            protected void onUpdate(AjaxRequestTarget target) {
	                for(AssignmentEditorDto item: assignmentList.getModelObject()){
	                    item.setSelected(this.getModelObject());
	                }

	                target.add(assignments);
	            }
	        };
	        assignmentCheckAll.setOutputMarkupId(true);
	        assignments.add(assignmentCheckAll);
	    }
	 
	 private List<InlineMenuItem> createAssignmentsMenu() {
	        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
	        InlineMenuItem item = new InlineMenuItem(createStringResource("pageAdminFocus.menu.assignShadow"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showAssignablePopup(target, ResourceType.class);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.menu.assignRole"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showAssignablePopup(target, RoleType.class);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.menu.assignOrg"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showAssignableOrgPopup(target);
	            }
	        });
	        items.add(item);
	        items.add(new InlineMenuItem());
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.menu.unassign"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                deleteAssignmentPerformed(target);
	            }
	        });
	        items.add(item);

	        return items;
	    }

	    private List<InlineMenuItem> createShadowMenu() {
	        List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
	        InlineMenuItem item = new InlineMenuItem(createStringResource("pageAdminFocus.button.addShadow"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showModalWindow(MODAL_ID_RESOURCE, target);
	            }
	        });
	        items.add(item);
	        items.add(new InlineMenuItem());
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.button.enable"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                updateShadowActivation(target, getSelectedAccounts(), true);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.button.disable"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                updateShadowActivation(target, getSelectedAccounts(), false);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlink"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                unlinkShadowPerformed(target, getSelectedAccounts());
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlock"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                unlockShadowPerformed(target, getSelectedAccounts());
	            }
	        });
	        items.add(item);
	        items.add(new InlineMenuItem());
	        item = new InlineMenuItem(createStringResource("pageAdminFocus.button.delete"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                deleteShadowPerformed(target);
	            }
	        });
	        items.add(item);

	        return items;
	    }
	    
	    
	    private List<FocusShadowDto> getSelectedAccounts() {
	        List<FocusShadowDto> selected = new ArrayList<FocusShadowDto>();

	        List<FocusShadowDto> all = shadowModel.getObject();
	        for (FocusShadowDto shadow : all) {
	            if (shadow.isLoadedOK() && shadow.getObject().isSelected()) {
	                selected.add(shadow);
	            }
	        }

	        return selected;
	    }

	    private List<AssignmentEditorDto> getSelectedAssignments() {
	        List<AssignmentEditorDto> selected = new ArrayList<AssignmentEditorDto>();

	        List<AssignmentEditorDto> all = assignmentsModel.getObject();
	        for (AssignmentEditorDto wrapper : all) {
	            if (wrapper.isSelected()) {
	                selected.add(wrapper);
	            }
	        }

	        return selected;
	    }

	    private void addSelectedAccountPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
	        ModalWindow window = (ModalWindow) get(MODAL_ID_RESOURCE);
	        window.close(target);

	        if (newResources.isEmpty()) {
	            warn(getString("pageUser.message.noResourceSelected"));
	            target.add(getFeedbackPanel());
	            return;
	        }

	        for (ResourceType resource : newResources) {
	            try {
	                ShadowType shadow = new ShadowType();
	                shadow.setResource(resource);

	                RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource.asPrismObject(),
	                        LayerType.PRESENTATION, getPrismContext());
	                if (refinedSchema == null) {
	                    error(getString("pageAdminFocus.message.couldntCreateAccountNoSchema", resource.getName()));
	                    continue;
	                }
	                if (LOGGER.isTraceEnabled()) {
	                    LOGGER.trace("Refined schema for {}\n{}", resource, refinedSchema.debugDump());
	                }

	                QName objectClass = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT).getObjectClassDefinition()
	                        .getTypeName();
	                shadow.setObjectClass(objectClass);

	                getPrismContext().adopt(shadow);

	                ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(WebMiscUtil.getOrigStringFromPoly(resource.getName()), null,
	                        shadow.asPrismObject(), ContainerStatus.ADDING, this);
//	                ObjectWrapper wrapper = new ObjectWrapper(WebMiscUtil.getOrigStringFromPoly(resource.getName()), null,
//	                        shadow.asPrismObject(), ContainerStatus.ADDING);
	                if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
	                    showResultInSession(wrapper.getResult());
	                }

	                wrapper.setShowEmpty(true);
	                wrapper.setMinimalized(false);
	                shadowModel.getObject().add(new FocusShadowDto(wrapper, UserDtoStatus.ADD));
	                setResponsePage(getPage());
	            } catch (Exception ex) {
	                error(getString("pageAdminFocus.message.couldntCreateAccount", resource.getName(), ex.getMessage()));
	                LoggingUtils.logException(LOGGER, "Couldn't create account", ex);
	            }
	        }

	        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_SHADOWS)));
	    }

	    private void addSelectedResourceAssignPerformed(ResourceType resource) {
	        AssignmentType assignment = new AssignmentType();
	        ConstructionType construction = new ConstructionType();
	        assignment.setConstruction(construction);

	        try {
	            getPrismContext().adopt(assignment, getCompileTimeClass(), new ItemPath(FocusType.F_ASSIGNMENT));
	        } catch (SchemaException e) {
	            error(getString("Could not create assignment", resource.getName(), e.getMessage()));
	            LoggingUtils.logException(LOGGER, "Couldn't create assignment", e);
	            return;
	        }

	        construction.setResource(resource);

	        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
	        AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, this);
	        assignments.add(dto);

	        dto.setMinimized(false);
	        dto.setShowEmpty(true);
	    }

	    private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignables, String popupId) {
	        ModalWindow window = (ModalWindow) get(popupId);
	        window.close(target);

	        if (newAssignables.isEmpty()) {
	            warn(getString("pageAdminFocus.message.noAssignableSelected"));
	            target.add(getFeedbackPanel());
	            return;
	        }

	        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
	        for (ObjectType object : newAssignables) {
	            try {
	                if (object instanceof ResourceType) {
	                    addSelectedResourceAssignPerformed((ResourceType) object);
	                    continue;
	                }

	                AssignmentEditorDtoType aType = AssignmentEditorDtoType.getType(object.getClass());

	                ObjectReferenceType targetRef = new ObjectReferenceType();
	                targetRef.setOid(object.getOid());
	                targetRef.setType(aType.getQname());
	                targetRef.setTargetName(object.getName());

	                AssignmentType assignment = new AssignmentType();
	                assignment.setTargetRef(targetRef);

	                AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, this);
	                dto.setMinimized(false);
	                dto.setShowEmpty(true);

	                assignments.add(dto);
	            } catch (Exception ex) {
	                error(getString("pageAdminFocus.message.couldntAssignObject", object.getName(), ex.getMessage()));
	                LoggingUtils.logException(LOGGER, "Couldn't assign object", ex);
	            }
	        }

	        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS)));
	    }

	 
	    
	    
	    private void updateShadowActivation(AjaxRequestTarget target, List<FocusShadowDto> accounts, boolean enabled) {
	        if (!isAnyAccountSelected(target)) {
	            return;
	        }

	        for (FocusShadowDto account : accounts) {
	            if(!account.isLoadedOK()){
	                continue;
	            }

	            ObjectWrapper wrapper = account.getObject();
	            ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(
	                    ShadowType.F_ACTIVATION));
	            if (activation == null) {
	                warn(getString("pageAdminFocus.message.noActivationFound", wrapper.getDisplayName()));
	                continue;
	            }

	            PropertyWrapper enabledProperty = (PropertyWrapper)activation.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
	            if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
	                warn(getString("pageAdminFocus.message.noEnabledPropertyFound", wrapper.getDisplayName()));
	                continue;
	            }
	            ValueWrapper value = enabledProperty.getValues().get(0);
	            ActivationStatusType status = enabled ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
	            ((PrismPropertyValue)value.getValue()).setValue(status);

	            wrapper.setSelected(false);
	        }

	        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_SHADOWS)));
	    }

	    private boolean isAnyAccountSelected(AjaxRequestTarget target) {
	        List<FocusShadowDto> selected = getSelectedAccounts();
	        if (selected.isEmpty()) {
	            warn(getString("pageAdminFocus.message.noAccountSelected"));
	            target.add(getFeedbackPanel());
	            return false;
	        }

	        return true;
	    }

	    private void deleteShadowPerformed(AjaxRequestTarget target) {
	        if (!isAnyAccountSelected(target)) {
	            return;
	        }

	        showModalWindow(MODAL_ID_CONFIRM_DELETE_SHADOW, target);
	    }

	    private void showModalWindow(String id, AjaxRequestTarget target) {
	        ModalWindow window = (ModalWindow) get(id);
	        window.show(target);
	        target.add(getFeedbackPanel());
	    }

	    private void deleteAccountConfirmedPerformed(AjaxRequestTarget target, List<FocusShadowDto> selected) {
	        List<FocusShadowDto> accounts = shadowModel.getObject();
	        for (FocusShadowDto account : selected) {
	            if (UserDtoStatus.ADD.equals(account.getStatus())) {
	                accounts.remove(account);
	            } else {
	                account.setStatus(UserDtoStatus.DELETE);
	            }
	        }
	        target.add(get(createComponentPath(ID_MAIN_FORM, ID_SHADOWS)));
	    }

	    private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target, List<AssignmentEditorDto> selected) {
	        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
	        for (AssignmentEditorDto assignment : selected) {
	            if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
	                assignments.remove(assignment);
	            } else {
	                assignment.setStatus(UserDtoStatus.DELETE);
	                assignment.setSelected(false);
	            }
	        }

	        target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS)));
	    }

	    private void unlinkShadowPerformed(AjaxRequestTarget target, List<FocusShadowDto> selected) {
	        if (!isAnyAccountSelected(target)) {
	            return;
	        }

	        for (FocusShadowDto account : selected) {
	            if (UserDtoStatus.ADD.equals(account.getStatus())) {
	                continue;
	            }
	            account.setStatus(UserDtoStatus.UNLINK);
	        }
	        target.add(get(createComponentPath(ID_MAIN_FORM, ID_SHADOWS)));
	    }

	    private void unlockShadowPerformed(AjaxRequestTarget target, List<FocusShadowDto> selected) {
	        if (!isAnyAccountSelected(target)) {
	            return;
	        }

	        for (FocusShadowDto account : selected) {
	            // TODO: implement unlock
	        }
	    }

	    private void deleteAssignmentPerformed(AjaxRequestTarget target) {
	        List<AssignmentEditorDto> selected = getSelectedAssignments();
	        if (selected.isEmpty()) {
	            warn(getString("pageAdminFocus.message.noAssignmentSelected"));
	            target.add(getFeedbackPanel());
	            return;
	        }

	        showModalWindow(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT, target);
	    }
	    
	    private void showAssignablePopup(AjaxRequestTarget target, Class<? extends ObjectType> type) {
	        ModalWindow modal = (ModalWindow) get(MODAL_ID_ASSIGNABLE);
	        AssignablePopupContent content =  (AssignableRolePopupContent) modal.get(modal.getContentId());
	        content.setType(type);
	        showModalWindow(MODAL_ID_ASSIGNABLE, target);
	        target.add(getFeedbackPanel());
	    }
	    
	    private void showAssignableOrgPopup(AjaxRequestTarget target) {
	        ModalWindow modal = (ModalWindow) get(MODAL_ID_ASSIGNABLE_ORG);
	        AssignablePopupContent content =  (AssignableOrgPopupContent) modal.get(modal.getContentId());
	        content.setType(OrgType.class);
	        showModalWindow(MODAL_ID_ASSIGNABLE_ORG, target);
	        target.add(getFeedbackPanel());
	    }

	    private void initResourceModal() {
	        ModalWindow window = createModalWindow(MODAL_ID_RESOURCE,
	                createStringResource("pageAdminFocus.title.selectResource"), 1100, 560);

	        final SimpleUserResourceProvider provider = new SimpleUserResourceProvider(this, shadowModel){

	            @Override
	            protected void handlePartialError(OperationResult result) {
	                showResult(result);
	            }
	        };
	        window.setContent(new ResourcesPopup(window.getContentId()) {

	            @Override
	            public SimpleUserResourceProvider getProvider() {
	                return provider;
	            }

	            @Override
	            protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
	                addSelectedAccountPerformed(target, newResources);
	            }
	        });
	        add(window);
	    }

	    private void initAssignableModal() {
	        ModalWindow window = createModalWindow(MODAL_ID_ASSIGNABLE,
	                createStringResource("pageAdminFocus.title.selectAssignable"), 1100, 560);
	        window.setContent(new AssignableRolePopupContent(window.getContentId()) {

	            @Override
	            protected void handlePartialError(OperationResult result) {
	                showResult(result);
	            }

	            @Override
	            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
	                addSelectedAssignablePerformed(target, selected, MODAL_ID_ASSIGNABLE);
	            }

	            @Override
	            protected PrismObject<UserType> getUserDefinition() {
	                return focusModel.getObject().getObject();
	            }
	        });
	        add(window);
	        
	        window = createModalWindow(MODAL_ID_ASSIGNABLE_ORG,
	                createStringResource("pageAdminFocus.title.selectAssignable"), 1150, 600);
	        window.setContent(new AssignableOrgPopupContent(window.getContentId()) {

	            @Override
	            protected void handlePartialError(OperationResult result) {
	                showResult(result);
	            }

	            @Override
	            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
	                addSelectedAssignablePerformed(target, selected, MODAL_ID_ASSIGNABLE_ORG);
	            }
	        });
	        add(window);

//	        ModalWindow assignmentPreviewPopup = new AssignmentPreviewDialog(MODAL_ID_ASSIGNMENTS_PREVIEW, null, null);
//	        add(assignmentPreviewPopup);
	    }
	    
	    private void initTasks(WebMarkupContainer tasks) {
	        List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();
	        final TaskDtoProvider taskDtoProvider = new TaskDtoProvider(PageAdminFocus.this,
	                TaskDtoProviderOptions.minimalOptions());
	        taskDtoProvider.setQuery(createTaskQuery(null));
	        TablePanel taskTable = new TablePanel<TaskDto>(ID_TASK_TABLE, taskDtoProvider, taskColumns) {

	            @Override
	            protected void onInitialize() {
	                super.onInitialize();
	                StringValue oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

	                taskDtoProvider.setQuery(createTaskQuery(oidValue != null ? oidValue.toString() : null));
	            }
	        };
	        tasks.add(taskTable);

	        tasks.add(new VisibleEnableBehaviour() {
	            @Override
	            public boolean isVisible() {
	                return taskDtoProvider.size() > 0;
	            }
	        });
	    }

	    private ObjectQuery createTaskQuery(String oid) {
	        List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

	        if (oid == null) {
	            oid = "non-existent";      // TODO !!!!!!!!!!!!!!!!!!!!
	        }
	        try {
	            filters.add(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class, getPrismContext(), oid));
	            filters.add(NotFilter.createNot(EqualFilter.createEqual(TaskType.F_EXECUTION_STATUS, TaskType.class, getPrismContext(), null, TaskExecutionStatusType.CLOSED)));
	            filters.add(EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, getPrismContext(), null));
	        } catch (SchemaException e) {
	            throw new SystemException("Unexpected SchemaException when creating task filter", e);
	        }

	        return new ObjectQuery().createObjectQuery(AndFilter.createAnd(filters));
	    }

	    private List<IColumn<TaskDto, String>> initTaskColumns() {
	        List<IColumn<TaskDto, String>> columns = new ArrayList<IColumn<TaskDto, String>>();

	        columns.add(PageTasks.createTaskNameColumn(this, "pageAdminFocus.task.name"));
	        columns.add(PageTasks.createTaskCategoryColumn(this, "pageAdminFocus.task.category"));
	        columns.add(PageTasks.createTaskExecutionStatusColumn(this, "pageAdminFocus.task.execution"));
	        columns.add(PageTasks.createTaskResultStatusColumn(this, "pageAdminFocus.task.status"));
	        return columns;
	    }


	    private void initButtons(final Form mainForm) {
	        AjaxSubmitButton saveButton = new AjaxSubmitButton("save",
	                createStringResource("pageAdminFocus.button.save")) {

	            @Override
	            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
	                progressReporter.onSaveSubmit();
	                savePerformed(target);
	            }

	            @Override
	            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
	                target.add(getFeedbackPanel());
	            }
	        };
	        progressReporter.registerSaveButton(saveButton);
	        mainForm.add(saveButton);

	        AjaxSubmitButton abortButton = new AjaxSubmitButton("abort",
	                createStringResource("pageAdminFocus.button.abort")) {

	            @Override
	            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
	                progressReporter.onAbortSubmit(target);
	            }

	            @Override
	            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
	                target.add(getFeedbackPanel());
	            }
	        };
	        progressReporter.registerAbortButton(abortButton);
	        mainForm.add(abortButton);
	        
//	        AjaxSubmitButton recomputeAssignments = new AjaxSubmitButton(ID_BUTTON_RECOMPUTE_ASSIGNMENTS,
//	                createStringResource("pageAdminFocus.button.recompute.assignments")) {
//
//	            @Override
//	            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
//	                recomputeAssignmentsPerformed(target);
//	            }
//
//	            @Override
//	            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
//	                target.add(getFeedbackPanel());
//	            }
//	        };
//	        mainForm.add(recomputeAssignments);
	        
	        AjaxButton back = new AjaxButton("back", createStringResource("pageAdminFocus.button.back")) {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	            	setSpecificResponsePage();
	            }
	        };
	        mainForm.add(back);
	    
	        mainForm.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, true, false));
	    }
	    
	    private void initConfirmationDialogs() {
	        ConfirmationDialog dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_SHADOW,
	                createStringResource("pageAdminFocus.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

	            @Override
	            public String getObject() {
	                return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
	                        getSelectedAccounts().size()).getString();
	            }
	        }) {

	            @Override
	            public void yesPerformed(AjaxRequestTarget target) {
	                close(target);
	                deleteAccountConfirmedPerformed(target, getSelectedAccounts());
	            }
	        };
	        add(dialog);

	        dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT,
	                createStringResource("pageAdminFocus.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

	            @Override
	            public String getObject() {
	                return createStringResource("pageAdminFocus.message.deleteAssignmentConfirm",
	                        getSelectedAssignments().size()).getString();
	            }
	        }) {

	            @Override
	            public void yesPerformed(AjaxRequestTarget target) {
	                close(target);
	                deleteAssignmentConfirmedPerformed(target, getSelectedAssignments());
	            }
	        };
	        add(dialog);

	        // TODO: uncoment later -> check for unsaved changes
	        // dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_CANCEL,
	        // createStringResource("pageUser.title.confirmCancel"), new
	        // AbstractReadOnlyModel<String>() {
	        //
	        // @Override
	        // public String getObject() {
	        // return createStringResource("pageUser.message.cancelConfirm",
	        // getSelectedAssignments().size()).getString();
	        // }
	        // }) {
	        //
	        // @Override
	        // public void yesPerformed(AjaxRequestTarget target) {
	        // close(target);
	        // setResponsePage(PageUsers.class);
	        // // deleteAssignmentConfirmedPerformed(target,
	        // getSelectedAssignments());
	        // }
	        // };
	        // add(dialog);
	    }
	    
	    
	protected abstract void setSpecificResponsePage();

}
