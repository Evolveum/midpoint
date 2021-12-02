/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.page;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.component.FooterPanel;
import com.evolveum.midpoint.gui.api.page.component.NavigationPanel;
import com.evolveum.midpoint.gui.api.page.component.SidebarPanel;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.login.PageLogin;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageClass;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageInstance;
import com.evolveum.midpoint.web.component.dialog.MainPopupDialog;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.menu.BaseMenuItem;
import com.evolveum.midpoint.web.component.menu.SideBarMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public abstract class PageBase extends PageCommon implements ModelServiceLocator {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageBase.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    protected static final String OPERATION_LOAD_VIEW_COLLECTION_REF = DOT_CLASS + "loadViewCollectionRef";

    private static final String ID_DEBUG_PANEL = "debugPanel";
    private static final String ID_DEBUG_BAR = "debugBar";
    private static final String ID_CLEAR_CACHE = "clearCssCache";


    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_SIDEBAR = "sidebar";
    public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FOOTER_CONTAINER = "footerContainer";
    private static final String ID_MAIN_POPUP = "mainPopup";

    private static final int DEFAULT_BREADCRUMB_STEP = 2;

    private static final String CLASS_DEFAULT_SKIN = "skin-blue-light";

    private static final Trace LOGGER = TraceManager.getTrace(PageBase.class);

    private boolean initialized = false;

    // No need for this to store in session. It is used only during single init and render.
    private transient Task pageTask;

    private NavigationPanel navigationPanel;

    public PageBase(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        if (initialized) {
            return;
        }
        initialized = true;

        createBreadcrumb();
    }

    protected void createBreadcrumb() {
        BreadcrumbPageClass bc = new BreadcrumbPageClass(new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getPageTitleModel().getObject();
            }
        }, this.getClass(), getPageParameters());

        addBreadcrumb(bc);
    }

    protected void createInstanceBreadcrumb() {
        BreadcrumbPageInstance bc = new BreadcrumbPageInstance(new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getPageTitleModel().getObject();
            }
        }, this);

        addBreadcrumb(bc);
    }

    public void updateBreadcrumbParameters(String key, Object value) {
        List<Breadcrumb> list = getBreadcrumbs();
        if (list.isEmpty()) {
            return;
        }

        Breadcrumb bc = list.get(list.size() - 1);
        PageParameters params = bc.getParameters();
        if (params == null) {
            return;
        }

        params.set(key, value);
    }

    public PageBase() {
        this(null);
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        MidPointApplication application = getMidpointApplication();
        return application.getWebApplicationConfiguration();
    }

    @Override
    public Task getPageTask() {
        if (pageTask == null) {
            pageTask = createSimpleTask(this.getClass().getName());
        }
        return pageTask;
    }

    public GuiProfiledPrincipal getPrincipal() {
        return SecurityUtils.getPrincipalUser();
    }

    public FocusType getPrincipalFocus() {
        MidPointPrincipal principal = getPrincipal();
        if (principal == null) {
            return null;
        }
        return principal.getFocus();
    }

    public boolean hasSubjectRoleRelation(String oid, List<QName> subjectRelations) {
        FocusType focusType = getPrincipalFocus();
        if (focusType == null) {
            return false;
        }

        if (oid == null) {
            return false;
        }

        for (ObjectReferenceType roleMembershipRef : focusType.getRoleMembershipRef()) {
            if (oid.equals(roleMembershipRef.getOid()) &&
                    getPrismContext().relationMatches(subjectRelations, roleMembershipRef.getRelation())) {
                return true;
            }
        }
        return false;
    }

    public static StringResourceModel createStringResourceStatic(Component component, Enum<?> e) {
        String resourceKey = createEnumResourceKey(e);
        return createStringResourceStatic(component, resourceKey);
    }

    public static String createEnumResourceKey(Enum<?> e) {
        return e.getDeclaringClass().getSimpleName() + "." + e.name();
    }

    public Task createAnonymousTask(String operation) {
        TaskManager manager = getTaskManager();
        Task task = manager.createTaskInstance(operation);

        task.setChannel(SchemaConstants.CHANNEL_USER_URI);

        return task;
    }

    public Task createSimpleTask(String operation) {
        return createSimpleTask(operation, null);
    }

    public Task createSimpleTask(String operation, String channel) {
        MidPointPrincipal user = SecurityUtils.getPrincipalUser();
        if (user == null) {
            throw new RestartResponseException(PageLogin.class);
        }
        return WebModelServiceUtils.createSimpleTask(operation, channel, user.getFocus().asPrismObject(), getTaskManager());
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        FeedbackMessages messages = getSession().getFeedbackMessages();
        for (FeedbackMessage message : messages) {
            getFeedbackMessages().add(message);
        }

        getSession().getFeedbackMessages().clear();
    }

    //TODO change according to new tempalte
    protected IModel<String> getBodyCssClass() {
        return new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                if (info == null || StringUtils.isEmpty(info.getSkin())) {
                    return CLASS_DEFAULT_SKIN;
                }

                return info.getSkin();
            }
        };
    }

    private void initLayout() {
        navigationPanel = new NavigationPanel(ID_NAVIGATION, createPageTitleModel());
        navigationPanel.setOutputMarkupId(true);
        add(navigationPanel);

        SidebarPanel sidebar = new SidebarPanel(ID_SIDEBAR);
        add(sidebar);

        FooterPanel footerPanel = new FooterPanel(ID_FOOTER_CONTAINER);
        add(footerPanel);

        initDebugBarLayout();
        MainPopupDialog mainPopup = new MainPopupDialog(ID_MAIN_POPUP);
        add(mainPopup);
    }

    public MainPopupDialog getMainPopup() {
        return (MainPopupDialog) get(ID_MAIN_POPUP);
    }

    public String getMainPopupBodyId() {
        return ModalDialog.CONTENT_ID;
    }

    public void showMainPopup(Popupable popupable, AjaxRequestTarget target) {
        MainPopupDialog dialog = getMainPopup();
        dialog.getDialogComponent().add(AttributeModifier.replace("style",
                dialog.generateWidthHeightParameter("" + (popupable.getWidth() > 0 ? popupable.getWidth() : ""),
                        popupable.getWidthUnit(),
                        "" + (popupable.getHeight() > 0 ? popupable.getHeight() : ""), popupable.getHeightUnit())));
        dialog.setContent(popupable.getComponent());
        dialog.setTitle(popupable.getTitle());
        dialog.open(target);
    }

    public void hideMainPopup(AjaxRequestTarget target) {
        getMainPopup().close(target);
    }

    protected boolean isSideMenuVisible() {
        return SecurityUtils.getPrincipalUser() != null;
    }

    private void initDebugBarLayout() {
        DebugBar debugPanel = new DebugBar(ID_DEBUG_PANEL);
        add(debugPanel);

        WebMarkupContainer debugBar = new WebMarkupContainer(ID_DEBUG_BAR);
        debugBar.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                RuntimeConfigurationType runtime = getApplication().getConfigurationType();
                return RuntimeConfigurationType.DEVELOPMENT.equals(runtime);
            }
        });
        add(debugBar);

        AjaxButton clearCache = new AjaxButton(ID_CLEAR_CACHE, createStringResource("PageBase.clearCssCache")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearLessJsCache(target);
            }
        };
        debugBar.add(clearCache);
    }

    protected void clearLessJsCache(AjaxRequestTarget target) {
        try {
            ArrayList<MBeanServer> servers = MBeanServerFactory.findMBeanServer(null);
            if (servers.size() > 1) {
                LOGGER.info("Too many mbean servers, cache won't be cleared.");
                for (MBeanServer server : servers) {
                    LOGGER.info(server.getDefaultDomain());
                }
                return;
            }
            MBeanServer server = servers.get(0);
//            ObjectName objectName = ObjectName.getInstance(Wro4jConfig.WRO_MBEAN_NAME + ":type=WroConfiguration");
//            server.invoke(objectName, "reloadCache", new Object[] {}, new String[] {});
            if (target != null) {
                target.add(PageBase.this);
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't clear less/js cache", ex);
            error("Error occurred, reason: " + ex.getMessage());
            if (target != null) {
                target.add(getFeedbackPanel());
            }
        }
    }

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }

    public SessionStorage getSessionStorage() {
        MidPointAuthWebSession session = (MidPointAuthWebSession) getSession();
        return session.getSessionStorage();
    }

    protected IModel<String> createPageTitleModel() {
        return () -> {
            BaseMenuItem activeMenu = getActiveMenu();
            String pageTitleKey = null;
            if (activeMenu != null) {
                pageTitleKey = activeMenu.getNameModel();
            }

            if (StringUtils.isEmpty(pageTitleKey)) {
                pageTitleKey = PageBase.this.getClass().getSimpleName() + ".title";
            }
            return createStringResource(pageTitleKey).getString();
        };
    }

    private <MI extends BaseMenuItem> MI getActiveMenu() {
        SidebarPanel sideBarMenu = getSideBarMenuPanel();
        if (sideBarMenu == null || !sideBarMenu.isVisible()) {
            return null;
        }

        List<SideBarMenuItem> sideMenuItems = sideBarMenu.getItems();
        if (CollectionUtils.isEmpty(sideMenuItems)) {
            return null;
        }

        for (SideBarMenuItem sideBarMenuItem : sideMenuItems) {
            MI activeMenu = sideBarMenuItem.getActiveMenu(PageBase.this);
            if (activeMenu != null) {
                return activeMenu;
            }
        }

        return null;
    }

    public void refreshTitle(AjaxRequestTarget target) {
        target.add(navigationPanel);

        //TODO what about breadcrumbs and page title (header)?
        //target.add(getHeaderTitle()); cannot update component with rendetBodyOnly
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this).setModel(new Model<String>()).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(String resourceKey, IModel model, Object... objects) {
        return new StringResourceModel(resourceKey, model).setDefaultValue(resourceKey)
                .setParameters(objects);
    }

    public StringResourceModel createStringResource(Enum<?> e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    @NotNull
    public static StringResourceModel createStringResourceStatic(Component component, String resourceKey,
            Object... objects) {
        return new StringResourceModel(resourceKey, component).setModel(new Model<String>())
                .setDefaultValue(resourceKey).setParameters(objects);
    }

    public StringResourceModel createStringResourceDefault(String defaultKey, PolyStringType polystringKey, Object... objects) {
        if (polystringKey == null) {
            return createStringResource(defaultKey);
        } else {
            return createStringResource(polystringKey, objects);
        }
    }

    public OpResult showResult(OperationResult result, String errorMessageKey) {
        return showResult(result, errorMessageKey, true);
    }

    public OpResult showResult(OperationResult result, boolean showSuccess) {
        return showResult(result, null, showSuccess);
    }

    public OpResult showResult(OperationResult result) {
        return showResult(result, null, true);
    }

    // common result processing
    public void processResult(AjaxRequestTarget target, OperationResult result, boolean showSuccess) {
        result.computeStatusIfUnknown();
        if (!result.isSuccess()) {
            showResult(result, showSuccess);
            target.add(getFeedbackPanel());
        } else {
            showResult(result);
            redirectBack();
        }
    }

    public String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    public String createPropertyModelExpression(String... components) {
        return StringUtils.join(components, ".");
    }


    // returns to previous page via restart response exception
    public RestartResponseException getRestartResponseException(Class<? extends Page> defaultBackPageClass) {
        return new RestartResponseException(defaultBackPageClass);
    }

    // TODO untangle this brutal code (list vs objectable vs other cases)
    @SuppressWarnings("unchecked")
    public <T> void parseObject(String lexicalRepresentation, final Holder<T> objectHolder,
            String language, boolean validateSchema, boolean skipChecks, Class<T> clazz, OperationResult result) {

        boolean isListOfObjects = List.class.isAssignableFrom(clazz);
        boolean isObjectable = Objectable.class.isAssignableFrom(clazz);
        if (skipChecks || language == null || PrismContext.LANG_JSON.equals(language) || PrismContext.LANG_YAML.equals(language)
                || (!isObjectable && !isListOfObjects)) {
            T object;
            try {
                if (isListOfObjects) {
                    List<PrismObject<? extends Objectable>> prismObjects = getPrismContext().parserFor(lexicalRepresentation)
                            .language(language).parseObjects();
                    if (!skipChecks) {
                        for (PrismObject<? extends Objectable> prismObject : prismObjects) {
                            prismObject.checkConsistence();
                        }
                    }
                    object = (T) prismObjects;
                } else if (isObjectable) {
                    PrismObject<ObjectType> prismObject = getPrismContext().parserFor(lexicalRepresentation).language(language).parse();
                    if (!skipChecks) {
                        prismObject.checkConsistence();
                    }
                    object = (T) prismObject.asObjectable();
                } else {
                    object = getPrismContext().parserFor(lexicalRepresentation).language(language).type(clazz).parseRealValue();
                }
                objectHolder.setValue(object);
            } catch (RuntimeException | SchemaException e) {
                result.recordFatalError(createStringResource("PageBase.message.parseObject.fatalError", e.getMessage()).getString(), e);
            }
            return;
        }

        List<PrismObject<?>> list = new ArrayList<>();
        if (isListOfObjects) {
            objectHolder.setValue((T) list);
        }
        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                    OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <O extends Objectable> EventResult postMarshall(PrismObject<O> object, Element objectElement,
                    OperationResult objectResult) {
                if (isListOfObjects) {
                    list.add(object);
                } else {
                    @SuppressWarnings({ "unchecked", "raw" })
                    T value = (T) object.asObjectable();
                    objectHolder.setValue(value);
                }
                return EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
            }
        };
        LegacyValidator validator = new LegacyValidator(getPrismContext(), handler);
        validator.setVerbose(true);
        validator.setValidateSchema(validateSchema);
        validator.validate(lexicalRepresentation, result, OperationConstants.IMPORT_OBJECT);        // TODO the operation name

        result.computeStatus();
    }

    public long getItemsPerPage(UserProfileStorage.TableId tableId) {
        return getItemsPerPage(tableId.name());
    }

    public long getItemsPerPage(String tableIdName) {
        UserProfileStorage userProfile = getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableIdName);
    }

    public PrismObject<? extends FocusType> loadFocusSelf() {
        Task task = createSimpleTask(OPERATION_LOAD_USER);
        OperationResult result = task.getResult();
        PrismObject<? extends FocusType> focus = WebModelServiceUtils.loadObject(FocusType.class,
                WebModelServiceUtils.getLoggedInFocusOid(), PageBase.this, task, result);
        result.computeStatus();

        showResult(result, null, false);

        return focus;
    }

    public boolean canRedirectBack() {
        return canRedirectBack(DEFAULT_BREADCRUMB_STEP);
    }

    /**
     * Checks if it's possible to make backStep steps back.
     */
    public boolean canRedirectBack(int backStep) {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (breadcrumbs.size() > backStep) {
            return true;
        }
        if (breadcrumbs.size() == backStep && (breadcrumbs.get(breadcrumbs.size() - backStep)) != null) {
            Breadcrumb br = breadcrumbs.get(breadcrumbs.size() - backStep);
            if (br instanceof BreadcrumbPageInstance || br instanceof BreadcrumbPageClass) {
                return true;
            }
        }

        return false;
    }

    public Breadcrumb redirectBack() {
        return redirectBack(DEFAULT_BREADCRUMB_STEP);
    }

    /**
     * @param backStep redirects back to page with backStep step
     */
    public Breadcrumb redirectBack(int backStep) {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (canRedirectBack(backStep)) {
            Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - backStep);
            redirectBackToBreadcrumb(breadcrumb);
            return breadcrumb;
        } else if (canRedirectBack(DEFAULT_BREADCRUMB_STEP)) {
            Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - DEFAULT_BREADCRUMB_STEP);
            redirectBackToBreadcrumb(breadcrumb);
            return breadcrumb;
        } else {
            setResponsePage(getMidpointApplication().getHomePage());
            return null;
        }
    }

    public void navigateToNext(WebPage page) {
        if (!(page instanceof PageBase)) {
            setResponsePage(page);
            return;
        }

        PageBase next = (PageBase) page;
        next.setBreadcrumbs(getBreadcrumbs());

        setResponsePage(next);
    }

    // TODO deduplicate with redirectBack
    public RestartResponseException redirectBackViaRestartResponseException() {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (breadcrumbs.size() < 2) {
            return new RestartResponseException(getApplication().getHomePage());
        }

        Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - 2);
        redirectBackToBreadcrumb(breadcrumb);
        return breadcrumb.getRestartResponseException();
    }

    public void redirectBackToBreadcrumb(Breadcrumb breadcrumb) {
        navigationPanel.redirectBackToBreadcrumb(breadcrumb);
    }

    public void navigateToNext(Class<? extends WebPage> page) {
        navigateToNext(page, null);
    }

    public void navigateToNext(Class<? extends WebPage> pageType, PageParameters params) {
        WebPage page = createWebPage(pageType, params);
        navigateToNext(page);
    }

    public WebPage createWebPage(Class<? extends WebPage> pageType, PageParameters params) {
        IPageFactory pFactory = Session.get().getPageFactory();
        WebPage page;
        if (params == null) {
            page = pFactory.newPage(pageType);
        } else {
            page = pFactory.newPage(pageType, params);
        }
        return page;
    }

    public void setBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        getBreadcrumbs().clear();

        if (breadcrumbs != null) {
            getBreadcrumbs().addAll(breadcrumbs);
        }
    }

    public List<Breadcrumb> getBreadcrumbs() {
        if (navigationPanel == null) {
            return new ArrayList<>();
        }
        return navigationPanel.getBreadcrumbs();
    }

    public void addBreadcrumb(Breadcrumb breadcrumb) {
        Validate.notNull(breadcrumb, "Breadcrumb must not be null");

        Breadcrumb last = getBreadcrumbs().isEmpty() ?
                null : getBreadcrumbs().get(getBreadcrumbs().size() - 1);
        if (last != null && last.equals(breadcrumb)) {
            return;
        }

        getBreadcrumbs().add(breadcrumb);
    }

    public Breadcrumb getLastBreadcrumb() {
        if (getBreadcrumbs().isEmpty()) {
            return null;
        }

        return getBreadcrumbs().get(getBreadcrumbs().size() - 1);
    }

    public Breadcrumb getPreviousBreadcrumb() {
        if (getBreadcrumbs().isEmpty() || getBreadcrumbs().size() < 2) {
            return null;
        }

        return getBreadcrumbs().get(getBreadcrumbs().size() - 2);
    }

    protected String determineDataLanguage() {
        CompiledGuiProfile config = getCompiledGuiProfile();
        if (config.getPreferredDataLanguage() != null) {
            if (PrismContext.LANG_JSON.equals(config.getPreferredDataLanguage())) {
                return PrismContext.LANG_JSON;
            } else if (PrismContext.LANG_YAML.equals(config.getPreferredDataLanguage())) {
                return PrismContext.LANG_YAML;
            } else {
                return PrismContext.LANG_XML;
            }
        } else {
            return PrismContext.LANG_XML;
        }
    }

    public void reloadShoppingCartIcon(AjaxRequestTarget target) {
        target.add(navigationPanel);
//        target.add(get(createComponentPath(ID_MAIN_HEADER, ID_NAVIGATION, ID_CART_BUTTON)));
    }

    public AsyncWebProcessManager getAsyncWebProcessManager() {
        return MidPointApplication.get().getAsyncWebProcessManager();
    }

    @Override
    public Locale getLocale() {
        return getSession().getLocale();
    }


    private SidebarPanel getSideBarMenuPanel() {
        return (SidebarPanel) get(ID_SIDEBAR);
    }

    public ModelExecuteOptions executeOptions() {
        return ModelExecuteOptions.create(getPrismContext());
    }

    public boolean isNativeRepo() {
        return getRepositoryService().isNative();
    }
}
