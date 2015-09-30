package com.evolveum.midpoint.web.page.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
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
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableOrgPopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignablePopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignableRolePopupContent;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.ResourcesPopup;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusShadowDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class BaseFocusPanel<T extends FocusType> extends Panel{

	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
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
	    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
	    
	    protected static final String ID_MAIN_FORM = "mainForm";
	    private static final String ID_ASSIGNMENT_EDITOR = "assignmentEditor";
	    private static final String ID_ASSIGNMENT_LIST = "assignmentList";
	    private static final String ID_TASK_TABLE = "taskTable";
	    private static final String ID_FOCUS_FORM = "focusForm";
//	    private static final String ID_ACCOUNTS_DELTAS = "accountsDeltas";
	    private static final String ID_SHADOW_LIST = "shadowList";
	    private static final String ID_SHADOWS = "shadows";
	    private static final String ID_ASSIGNMENTS = "assignmentsContainer";
	    private static final String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";
	    private static final String ID_TASKS = "tasks";
	    private static final String ID_SHADOW_MENU = "shadowMenu";
	    private static final String ID_ASSIGNMENT_MENU = "assignmentMenu";
	    private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";
	    private static final String ID_ASSIGNMENT_CHECK_ALL = "assignmentCheckAll";
	    private static final String ID_BUTTON_RECOMPUTE_ASSIGNMENTS = "recomputeAssignments";
	    
	    
	    private static final String MODAL_ID_RESOURCE = "resourcePopup";
	    private static final String MODAL_ID_ASSIGNABLE = "assignablePopup";
	    private static final String MODAL_ID_ASSIGNABLE_ORG = "assignableOrgPopup";
	    private static final String MODAL_ID_CONFIRM_DELETE_SHADOW = "confirmDeleteShadowPopup";
	    private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";
	    private static final String MODAL_ID_ASSIGNMENTS_PREVIEW = "assignmentsPreviewPopup";
	    
	    private static final Trace LOGGER = TraceManager.getTrace(PageAdminFocus.class);
	    
	 // used to determine whether to leave this page or stay on it (after operation finishing)
	    private ObjectDelta delta;    
	    
	    private PageBase page;
	    
	    private Form mainForm;
	    
	    public BaseFocusPanel(String id, Form mainForm, LoadableModel<ObjectWrapper> focusModel, LoadableModel<List<FocusShadowDto>> shadowModel, LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase page) {
			super(id);
			this.page = page;
			this.focusModel = focusModel;
			this.shadowModel = shadowModel;
			this.assignmentsModel = assignmentsModel;
			this.mainForm = mainForm;
			initLayout();
		}
	 

 
	    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
	        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
	    }
	    
	    public String getString(String resourceKey, Object... objects) {
	        return createStringResource(resourceKey, objects).getString();
	    }
	    
	    protected String createComponentPath(String... components) {
	        return StringUtils.join(components, ":");
	    }

public LoadableModel<ObjectWrapper> getFocusModel() {
	return focusModel;
}
	

//	 protected abstract void initCustomLayout(Form mainForm);
	 
	 protected abstract T createNewFocus();
	 
	 protected void initLayout(){
//		 final Form mainForm = new Form(ID_MAIN_FORM, true);
//	        mainForm.setMaxSize(MidPointApplication.USER_PHOTO_MAX_FILE_SIZE);
//	        mainForm.setMultiPart(true);
//	        add(mainForm);

//	        progressReporter = ProgressReporter.create(page, mainForm, "progressPanel");

	        PrismObjectPanel userForm = new PrismObjectPanel(ID_FOCUS_FORM, focusModel, new PackageResourceReference(
	                ImgResources.class, ImgResources.USER_PRISM), mainForm, page) {

	            @Override
	            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
	                return createStringResource("pageAdminFocus.description");
	            }
	        };
	        add(userForm);

	        WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
	        shadows.setOutputMarkupId(true);
	        add(shadows);
	        initShadows(shadows);

	        WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
	        assignments.setOutputMarkupId(true);
	        add(assignments);
	        initAssignments(assignments);
//	        add(assi);

	        WebMarkupContainer tasks = new WebMarkupContainer(ID_TASKS);
	        tasks.setOutputMarkupId(true);
	        add(tasks);
	        initTasks(tasks);


	        initResourceModal();
	        initAssignableModal();
	        
//	        ModalWindow assignmentPreviewPopup = new AssignmentPreviewDialog(MODAL_ID_ASSIGNMENTS_PREVIEW, null, null);
//	        add(assignmentPreviewPopup);
	        
	        initConfirmationDialogs();
//	        initButtons();
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
	 
	 private Task createSimpleTask(String operation){
		 return page.createSimpleTask(operation);
	 }
	 
	 private void showResultInSession(OperationResult result){
		 page.showResultInSession(result);
	 }
	 
	 private PrismContext getPrismContext(){
		 return page.getPrismContext();
	 }
	 
	 private PageParameters getPageParameters(){
		 return page.getPageParameters();
	 }
	 
	 private ModelService getModelService(){
		 return page.getModelService();
	 }
	 
	 private void showResult(OperationResult result){
		 page.showResult(result);
	 }
	 
	 private WebMarkupContainer getFeedbackPanel(){
		 return page.getFeedbackPanel();
	 }
	 
	
	 protected ModalWindow createModalWindow(final String id, IModel<String> title, int width, int height) {
	        final ModalWindow modal = new ModalWindow(id);
	        add(modal);

	        modal.setResizable(false);
	        modal.setTitle(title);
	        modal.setCookieName(PageBase.class.getSimpleName() + ((int) (Math.random() * 100)));

	        modal.setInitialWidth(width);
	        modal.setWidthUnit("px");
	        modal.setInitialHeight(height);
	        modal.setHeightUnit("px");

	        modal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

	            @Override
	            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
	                return true;
	            }
	        });

	        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

	            @Override
	            public void onClose(AjaxRequestTarget target) {
	                modal.close(target);
	            }
	        });

	        modal.add(new AbstractDefaultAjaxBehavior() {

	            @Override
	            public void renderHead(Component component, IHeaderResponse response) {
	                response.render(OnDomReadyHeaderItem.forScript("Wicket.Window.unloadConfirmation = false;"));
	                response.render(JavaScriptHeaderItem.forScript("$(document).ready(function() {\n" +
	                        "  $(document).bind('keyup', function(evt) {\n" +
	                        "    if (evt.keyCode == 27) {\n" +
	                        getCallbackScript() + "\n" +
	                        "        evt.preventDefault();\n" +
	                        "    }\n" +
	                        "  });\n" +
	                        "});", id));
	            }

	            @Override
	            protected void respond(AjaxRequestTarget target) {
	                modal.close(target);

	            }
	        });

	        return modal;
	    }
	 
	 protected void reviveModels() throws SchemaException {
	        WebMiscUtil.revive(focusModel, getPrismContext());
	        WebMiscUtil.revive(shadowModel, getPrismContext());
	        WebMiscUtil.revive(assignmentsModel, getPrismContext());
	        reviveCustomModels();
//	        WebMiscUtil.revive(summaryUser, getPrismContext());
	    }
	 protected abstract Class<T> getCompileTimeClass();
	 protected abstract Class getRestartResponsePage();
	 
 
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
	                            item.getModel(), "object"), packageRef, (Form) page.get(ID_MAIN_FORM), page) {

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
	 
	 private void initAssignments(WebMarkupContainer assignments) {
		 
		 
		 AssignmentTablePanel panel = new AssignmentTablePanel(ID_ASSIGNMENTS_PANEL, createStringResource("FocusType.assignment"), assignmentsModel){
			 
			 @Override
			protected void showAllAssignments(AjaxRequestTarget target) {
				 AssignmentPreviewDialog dialog = (AssignmentPreviewDialog) getParent().getParent().get(createComponentPath(MODAL_ID_ASSIGNMENTS_PREVIEW));
	             ((PageAdminFocus)page).recomputeAssignmentsPerformed(dialog, target);
			}
			 
		 };
		 assignments.add(panel);
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
	                        shadow.asPrismObject(), ContainerStatus.ADDING, page);
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
	        AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, page);
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

	                AssignmentType assignment = new AssignmentType();
	                assignment.setTargetRef(targetRef);

	                AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, page);
	                dto.setMinimized(false);
	                dto.setShowEmpty(true);

	                assignments.add(dto);
	            } catch (Exception ex) {
	                error(getString("pageAdminFocus.message.couldntAssignObject", object.getName(), ex.getMessage()));
	                LoggingUtils.logException(LOGGER, "Couldn't assign object", ex);
	            }
	        }

	        target.add(getFeedbackPanel(), get(createComponentPath(ID_ASSIGNMENTS)));
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

	        ModalWindow assignmentPreviewPopup = new AssignmentPreviewDialog(MODAL_ID_ASSIGNMENTS_PREVIEW, null, null);
	        add(assignmentPreviewPopup);
	    }
	    
	    private void initTasks(WebMarkupContainer tasks) {
	        List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();
	        final TaskDtoProvider taskDtoProvider = new TaskDtoProvider(page,
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
	    
//	    private void initButtons(){
//	    	 AjaxSubmitButton recomputeAssignments = new AjaxSubmitButton(ID_BUTTON_RECOMPUTE_ASSIGNMENTS,
//		                createStringResource("pageAdminFocus.button.recompute.assignments")) {
//
//		            @Override
//		            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
//		            	AssignmentPreviewDialog dialog = (AssignmentPreviewDialog) getParent().get(createComponentPath(MODAL_ID_ASSIGNMENTS_PREVIEW));
//		                ((PageAdminFocus)page).recomputeAssignmentsPerformed(dialog, target);
//		            }
//
//		            @Override
//		            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
//		                target.add(getFeedbackPanel());
//		            }
//		        };
//		        add(recomputeAssignments);
//	    }



}
