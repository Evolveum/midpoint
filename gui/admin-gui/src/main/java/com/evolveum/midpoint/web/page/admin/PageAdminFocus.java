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
import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.context.EvaluatedAbstractRole;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
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
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
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
	    
	    private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";
	    
	    private static final String ID_MAIN_FORM = "mainForm";
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
	    
	    private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel
        = new LoadableModel<ExecuteChangeOptionsDto>(false) {

    @Override
    protected ExecuteChangeOptionsDto load() {
        return new ExecuteChangeOptionsDto();
    }
};
	
	
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

	        initLayout();
	    }
	 
	 protected abstract T createNewFocus();
	 
	 protected abstract void initCustomLayout(Form mainForm);
	 
	 private void initLayout(){
		 final Form mainForm = new Form(ID_MAIN_FORM, true);
	        mainForm.setMaxSize(MidPointApplication.USER_PHOTO_MAX_FILE_SIZE);
	        mainForm.setMultiPart(true);
	        add(mainForm);

	        progressReporter = ProgressReporter.create(this, mainForm, "progressPanel");

	        PrismObjectPanel userForm = new PrismObjectPanel(ID_FOCUS_FORM, focusModel, new PackageResourceReference(
	                ImgResources.class, ImgResources.USER_PRISM), mainForm, this) {

	            @Override
	            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
	                return createStringResource("pageUser.description");
	            }
	        };
	        mainForm.add(userForm);

	        WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
	        shadows.setOutputMarkupId(true);
	        mainForm.add(shadows);
	        initShadows(shadows);

	        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
	        assignments.setOutputMarkupId(true);
	        mainForm.add(assignments);
	        initAssignments(assignments);

	        WebMarkupContainer tasks = new WebMarkupContainer(ID_TASKS);
	        tasks.setOutputMarkupId(true);
	        mainForm.add(tasks);
	        initTasks(tasks);

	        initButtons(mainForm);

	        initResourceModal();
	        initAssignableModal();
	        initConfirmationDialogs();
	        
	        initCustomLayout(mainForm);
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
	                getSession().error(getString("pageUser.message.cantEditUser"));
	            } else {
	                getSession().error(getString("pageUser.message.cantNewUser"));
	            }
	            throw new RestartResponseException(getRestartResponsePage());
	        }

	        ContainerStatus status = isEditingFocus() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
	        ObjectWrapper wrapper = null;
	        try{
	        	wrapper = ObjectWrapperUtil.createObjectWrapper("pageUser.userDetails", null, focus, status, this);
	        } catch (Exception ex){
	        	result.recordFatalError("Couldn't get user.", ex);
	            LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
	            wrapper = new ObjectWrapper("pageUser.userDetails", null, focus, null, status, this);
	        }
//	        ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails", null, user, status);
	        if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
	            showResultInSession(wrapper.getResult());
	        }

	        wrapper.setShowEmpty(!isEditingFocus());
	        return wrapper;
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
	            ObjectType targetObject = null;
	            AssignmentEditorDtoType type = AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION;
	            if (assignment.getTarget() != null) {
	                // object assignment
	                targetObject = assignment.getTarget();
	                type = AssignmentEditorDtoType.getType(targetObject.getClass());
	            } else if (assignment.getTargetRef() != null) {
	                // object assignment through reference
	                ObjectReferenceType ref = assignment.getTargetRef();
	                PrismObject target = getReference(ref, result);

	                if (target != null) {
	                    targetObject = (ObjectType) target.asObjectable();
	                    type = AssignmentEditorDtoType.getType(target.getCompileTimeClass());
	                }
	            } else if (assignment.getConstruction() != null) {
	                // account assignment through account construction
	                ConstructionType construction = assignment.getConstruction();
	                if (construction.getResource() != null) {
	                    targetObject = construction.getResource();
	                } else if (construction.getResourceRef() != null) {
	                    ObjectReferenceType ref = construction.getResourceRef();
	                    PrismObject target = getReference(ref, result);
	                    if (target != null) {
	                        targetObject = (ObjectType) target.asObjectable();
	                    }
	                }
	            }

	            list.add(new AssignmentEditorDto(targetObject, type, UserDtoStatus.MODIFY, assignment, this));
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
	        InlineMenuItem item = new InlineMenuItem(createStringResource("pageUser.menu.assignShadow"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showAssignablePopup(target, ResourceType.class);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageUser.menu.assignRole"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showAssignablePopup(target, RoleType.class);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageUser.menu.assignOrg"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showAssignableOrgPopup(target);
	            }
	        });
	        items.add(item);
	        items.add(new InlineMenuItem());
	        item = new InlineMenuItem(createStringResource("pageUser.menu.unassign"), new InlineMenuItemAction() {

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
	        InlineMenuItem item = new InlineMenuItem(createStringResource("pageUser.button.addShadow"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                showModalWindow(MODAL_ID_RESOURCE, target);
	            }
	        });
	        items.add(item);
	        items.add(new InlineMenuItem());
	        item = new InlineMenuItem(createStringResource("pageUser.button.enable"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                updateShadowActivation(target, getSelectedAccounts(), true);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageUser.button.disable"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                updateShadowActivation(target, getSelectedAccounts(), false);
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageUser.button.unlink"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                unlinkShadowPerformed(target, getSelectedAccounts());
	            }
	        });
	        items.add(item);
	        item = new InlineMenuItem(createStringResource("pageUser.button.unlock"), new InlineMenuItemAction() {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                unlockShadowPerformed(target, getSelectedAccounts());
	            }
	        });
	        items.add(item);
	        items.add(new InlineMenuItem());
	        item = new InlineMenuItem(createStringResource("pageUser.button.delete"), new InlineMenuItemAction() {

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
	                    error(getString("pageUser.message.couldntCreateAccountNoSchema", resource.getName()));
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
	                error(getString("pageUser.message.couldntCreateAccount", resource.getName(), ex.getMessage()));
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
	        AssignmentEditorDto dto = new AssignmentEditorDto(resource, AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION,
	                UserDtoStatus.ADD, assignment, this);
	        assignments.add(dto);

	        dto.setMinimized(false);
	        dto.setShowEmpty(true);
	    }

	    private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignables, String popupId) {
	        ModalWindow window = (ModalWindow) get(popupId);
	        window.close(target);

	        if (newAssignables.isEmpty()) {
	            warn(getString("pageUser.message.noAssignableSelected"));
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

	                AssignmentType assignment = new AssignmentType();
	                assignment.setTargetRef(targetRef);

	                AssignmentEditorDto dto = new AssignmentEditorDto(object, aType, UserDtoStatus.ADD, assignment, this);
	                dto.setMinimized(false);
	                dto.setShowEmpty(true);

	                assignments.add(dto);
	            } catch (Exception ex) {
	                error(getString("pageUser.message.couldntAssignObject", object.getName(), ex.getMessage()));
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
	                warn(getString("pageUser.message.noActivationFound", wrapper.getDisplayName()));
	                continue;
	            }

	            PropertyWrapper enabledProperty = (PropertyWrapper)activation.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
	            if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
	                warn(getString("pageUser.message.noEnabledPropertyFound", wrapper.getDisplayName()));
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
	            warn(getString("pageUser.message.noAccountSelected"));
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
	            warn(getString("pageUser.message.noAssignmentSelected"));
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
	                createStringResource("pageUser.title.selectResource"), 1100, 560);

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
	                createStringResource("pageUser.title.selectAssignable"), 1100, 560);
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
	                createStringResource("pageUser.title.selectAssignable"), 1150, 600);
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

	        ModalWindow assignmentPreviewPopup = new AssignmentPreviewDialog(MODAL_ID_ASSIGNMENTS_PREVIEW, null, null);
	        add(assignmentPreviewPopup);
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

	        columns.add(PageTasks.createTaskNameColumn(this, "pageUser.task.name"));
	        columns.add(PageTasks.createTaskCategoryColumn(this, "pageUser.task.category"));
	        columns.add(PageTasks.createTaskExecutionStatusColumn(this, "pageUser.task.execution"));
	        columns.add(PageTasks.createTaskResultStatusColumn(this, "pageUser.task.status"));
	        return columns;
	    }


	    private void initButtons(final Form mainForm) {
	        AjaxSubmitButton saveButton = new AjaxSubmitButton("save",
	                createStringResource("pageUser.button.save")) {

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
	                createStringResource("pageUser.button.abort")) {

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
//	                createStringResource("pageUser.button.recompute.assignments")) {
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

	        AjaxButton back = new AjaxButton("back", createStringResource("pageUser.button.back")) {

	            @Override
	            public void onClick(AjaxRequestTarget target) {
	                cancelPerformed(target);
	            }
	        };
	        mainForm.add(back);
	        
	        initCustomButtons(mainForm);

	        mainForm.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, true, false));
	    }
	    
	    private void initConfirmationDialogs() {
	        ConfirmationDialog dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_SHADOW,
	                createStringResource("pageUser.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

	            @Override
	            public String getObject() {
	                return createStringResource("pageUser.message.deleteAccountConfirm",
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
	                createStringResource("pageUser.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

	            @Override
	            public String getObject() {
	                return createStringResource("pageUser.message.deleteAssignmentConfirm",
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
	    
	    protected abstract void savePerformed(AjaxRequestTarget target);
	    protected abstract void cancelPerformed(AjaxRequestTarget target);
	    protected abstract void initCustomButtons(Form mainForm);
	    
	 

}
