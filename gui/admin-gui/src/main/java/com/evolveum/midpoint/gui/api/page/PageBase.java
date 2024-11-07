/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.page;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.TaskAwareExecutor;
import com.evolveum.midpoint.web.component.menu.top.LocaleTopMenuPanel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebSession;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.gui.api.AdminLTESkin;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.menu.LeftMenuPanel;
import com.evolveum.midpoint.gui.impl.page.self.PageRequestAccess;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.ShoppingCartPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerValuePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.web.application.AsyncWebProcessManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.dialog.MainPopupDialog;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.BaseMenuItem;
import com.evolveum.midpoint.web.component.menu.SideBarMenuItem;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.error.PageError404;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 * @author semancik
 */
public abstract class PageBase extends PageAdminLTE {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageBase.class.getName() + ".";

    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";

    private static final String ID_MAIN_HEADER = "mainHeader";
    private static final String ID_PAGE_TITLE_CONTAINER = "pageTitleContainer";
    private static final String ID_PAGE_TITLE_REAL = "pageTitleReal";
    private static final String ID_PAGE_TITLE = "pageTitle";
    public static final String ID_CONTENT_VISIBLE = "contentVisible";
    public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_CART_ITEMS_COUNT = "itemsCount";
    private static final String ID_SIDEBAR_MENU = "sidebarMenu";
    private static final String ID_LOCALE = "locale";
    private static final String ID_MENU_TOGGLE = "menuToggle";
    private static final String ID_BREADCRUMB = "breadcrumb";
    private static final String ID_BC_LINK = "bcLink";
    private static final String ID_BC_ICON = "bcIcon";
    private static final String ID_BC_SR_CURRENT_MESSAGE = "bcSrCurrentMessage";
    private static final String ID_BC_NAME = "bcName";
    private static final String ID_MAIN_POPUP = "mainPopup";
    private static final String ID_DEPLOYMENT_NAME = "deploymentName";
    private static final String ID_LOGOUT_FORM = "logoutForm";
    private static final String ID_MODE = "mode";
    private static final String ID_CART_ITEM = "cartItem";
    private static final String ID_CART_LINK = "cartLink";
    private static final String ID_CART_COUNT = "cartCount";
    private static final int DEFAULT_BREADCRUMB_STEP = 2;
    public static final String PARAMETER_OBJECT_COLLECTION_NAME = "collectionName";
    public static final String PARAMETER_DASHBOARD_TYPE_OID = "dashboardOid";
    public static final String PARAMETER_DASHBOARD_WIDGET_NAME = "dashboardWidgetName";
    public static final String PARAMETER_SEARCH_BY_NAME = "name";

    private List<Breadcrumb> breadcrumbs;

    private boolean initialized = false;

    public PageBase(PageParameters parameters) {
        super(parameters);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        if (getSubscriptionState().isGenericRepoWithoutSubscription()) {
            new Toast()
                    .warning()
                    .autohide(false)
                    .title(getString("PageBase.nonActiveSubscription"))
                    .body(getString("PageBase.nonActiveSubscriptionAndGenericRepo"))
                    .show(response);
        }
        if (isUserStatusVisible()) {
            response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.initPushMenuButton();"));
        }
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
        PageParameters pageParameters = getPageParameters();
        removePageParametersIfNeeded(pageParameters);
        addBreadcrumb(new Breadcrumb(getPageTitleModel(), this.getClass(), pageParameters));
    }

    private void removePageParametersIfNeeded(PageParameters parameters) {
        pageParametersToBeRemoved().forEach(parameters::remove);
    }

    protected List<String> pageParametersToBeRemoved() {
        return new ArrayList<>();
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

    public <O extends ObjectType, T extends ObjectType> void authorize(
            String operationUrl, AuthorizationPhaseType phase,
            PrismObject<O> object, ObjectDelta<O> delta, PrismObject<T> target,
            OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        AuthorizationParameters<O, T> params = new AuthorizationParameters.Builder<O, T>()
                .oldObject(object)
                .delta(delta)
                .target(target)
                .build();
        getSecurityEnforcer().authorize(operationUrl, phase, params, getPageTask(), result);
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

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        FeedbackMessages messages = getSession().getFeedbackMessages();
        for (FeedbackMessage message : messages) {
            getFeedbackMessages().add(message);
        }

        getSession().getFeedbackMessages().clear();
    }

    private void initHeaderLayout(WebMarkupContainer container) {
        WebMarkupContainer menuToggle = new WebMarkupContainer(ID_MENU_TOGGLE);
        menuToggle.add(createUserStatusBehaviour());
        container.add(menuToggle);

        LocaleTopMenuPanel locale = new LocaleTopMenuPanel(ID_LOCALE);
        container.add(locale);

        AjaxIconButton mode = new AjaxIconButton(ID_MODE,
                () -> getSessionStorage().getMode() == SessionStorage.Mode.DARK ? "fas fa-sun" : "fas fa-moon",
                () -> getSessionStorage().getMode() == SessionStorage.Mode.DARK ? getString("PageBase.switchToLight") : getString("PageBase.switchToDark")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                SessionStorage.Mode mode = getSessionStorage().getMode();
                if (mode == SessionStorage.Mode.DARK) {
                    mode = SessionStorage.Mode.LIGHT;
                } else {
                    mode = SessionStorage.Mode.DARK;
                }

                getSessionStorage().setMode(mode);

                target.add(PageBase.this);
            }
        };
        mode.add(new VisibleBehaviour(() -> AuthUtil.getPrincipalUser() != null && WebModelServiceUtils.isEnableExperimentalFeature(this)));
        container.add(mode);

        MidpointForm<?> form = new MidpointForm<>(ID_LOGOUT_FORM);
        form.add(new VisibleBehaviour(() -> AuthUtil.getPrincipalUser() != null));
        form.add(AttributeModifier.replace("action", () -> getUrlForLogout()));

        container.add(form);
    }

    private String getUrlForLogout() {
        ModuleAuthentication module = AuthUtil.getAuthenticatedModule();

        String prefix = module != null ? module.getPrefix() : "";

        return SecurityUtils.getPathForLogoutWithContextPath(getRequest().getContextPath(), prefix);
    }

    private void initTitleLayout(WebMarkupContainer mainHeader) {
        WebMarkupContainer pageTitleContainer = new WebMarkupContainer(ID_PAGE_TITLE_CONTAINER);
        pageTitleContainer.add(createUserStatusBehaviour());
        pageTitleContainer.setOutputMarkupId(true);
        mainHeader.add(pageTitleContainer);

        WebMarkupContainer pageTitle = new WebMarkupContainer(ID_PAGE_TITLE);
        pageTitleContainer.add(pageTitle);

        IModel<String> deploymentNameModel = new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                if (info == null) {
                    return "";
                }

                return StringUtils.isEmpty(info.getName()) ? "" : info.getName() + ": ";
            }
        };

        Label deploymentName = new Label(ID_DEPLOYMENT_NAME, deploymentNameModel);
        deploymentName.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(deploymentNameModel.getObject())));
        deploymentName.setRenderBodyOnly(true);
        pageTitle.add(deploymentName);

        Label pageTitleReal = new Label(ID_PAGE_TITLE_REAL, createPageTitleModel());
        pageTitleReal.add(getPageTitleBehaviour());
        pageTitleReal.setRenderBodyOnly(true);
        pageTitle.add(pageTitleReal);

        IModel<List<Breadcrumb>> breadcrumbsModel = () -> getBreadcrumbs();

        ListView<Breadcrumb> breadcrumbs = new ListView<>(ID_BREADCRUMB, breadcrumbsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
                AjaxLink<String> bcLink = new AjaxLink<>(ID_BC_LINK) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        redirectBackToBreadcrumb(item.getModelObject());
                    }
                };
                item.add(bcLink);
                bcLink.add(new EnableBehaviour(() -> item.getModelObject().isUseLink()));

                WebMarkupContainer bcSrCurrentMessage = new WebMarkupContainer(ID_BC_SR_CURRENT_MESSAGE);
                bcLink.add(bcSrCurrentMessage);

                if (item.getIndex() == getModelObject().size() - 1) {
                    bcLink.add(AttributeAppender.append("aria-current", "page"));
                } else {
                    bcSrCurrentMessage.add(VisibleBehaviour.ALWAYS_INVISIBLE);
                }

                WebMarkupContainer bcIcon = new WebMarkupContainer(ID_BC_ICON);
                bcIcon.add(new VisibleBehaviour(() -> item.getModelObject().getIcon() != null && item.getModelObject().getIcon().getObject() != null));
                bcIcon.add(AttributeModifier.replace("class", item.getModelObject().getIcon()));
                bcLink.add(bcIcon);

                Label bcName = new Label(ID_BC_NAME, item.getModelObject().getLabel());
                bcLink.add(bcName);

                item.add(new VisibleBehaviour(() -> item.getModelObject().isVisible()));
            }
        };
        breadcrumbs.add(new VisibleBehaviour(() -> !isErrorPage()));
        mainHeader.add(breadcrumbs);

        initCartButton(mainHeader);
    }

    private void initCartButton(WebMarkupContainer mainHeader) {
        AjaxLink cartLink = new AjaxLink<>(ID_CART_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters params = new PageParameters();
                params.set(WizardModel.PARAM_STEP, ShoppingCartPanel.STEP_ID);

                setResponsePage(new PageRequestAccess(params));
            }
        };
        cartLink.add(new VisibleBehaviour(() -> getPage() instanceof PageRequestAccess || !getSessionStorage().getRequestAccess().getShoppingCartAssignments().isEmpty()));
        mainHeader.add(cartLink);

        Label cartCount = new Label(ID_CART_COUNT, () -> {
            List list = getSessionStorage().getRequestAccess().getShoppingCartAssignments();
            return list.isEmpty() ? null : list.size();
        });
        cartLink.add(cartCount);
    }

    private void initLayout() {
        WebMarkupContainer mainHeader = new WebMarkupContainer(ID_MAIN_HEADER);
        mainHeader.add(AttributeAppender.append("class", () -> {
            String skin = WebComponentUtil.getMidPointSkin().getNavbarCss();

            if (skin != null && Arrays.stream(skin.split(" ")).noneMatch(s -> "navbar-light".equals(s))) {
                return "navbar-dark " + skin;
            }

            return skin;
        }));
        mainHeader.setOutputMarkupId(true);
        add(mainHeader);

        IModel<IconType> logoModel = new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public IconType getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                return info != null ? info.getLogo() : null;
            }
        };

        mainHeader.add(new AttributeAppender("style", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return logoModel.getObject() != null ? "background-color: " + GuiStyleConstants.DEFAULT_BG_COLOR + " !important;" : null;
            }
        }));

        initHeaderLayout(mainHeader);
        initTitleLayout(mainHeader);

        mainHeader.add(createHeaderColorStyleModel(false));

        LeftMenuPanel sidebarMenu = new LeftMenuPanel(ID_SIDEBAR_MENU);
        sidebarMenu.add(AttributeAppender.append("class",
                () -> {
                    boolean dark = getSessionStorage().getMode() == SessionStorage.Mode.DARK;

                    AdminLTESkin skin = WebComponentUtil.getMidPointSkin();
                    return skin.getSidebarCss(dark);
                }));
        sidebarMenu.add(createUserStatusBehaviour());
        add(sidebarMenu);

        WebMarkupContainer content = new WebMarkupContainer(ID_CONTENT_VISIBLE);
        content.setOutputMarkupId(true);
        content.add(new VisibleBehaviour(this::isContentVisible));
        add(content);

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);

        MainPopupDialog mainPopup = new MainPopupDialog(ID_MAIN_POPUP);
//        mainPopup.showUnloadConfirmation(false);
//        mainPopup.setResizable(false);
        mainPopup.setOutputMarkupId(true);
        add(mainPopup);
    }

    protected boolean isContentVisible() {
        return !getSubscriptionState().isGenericRepoWithoutSubscription();
    }

    public static AttributeAppender createHeaderColorStyleModel(boolean checkSkinUsage) {
        return AttributeAppender.append("style", () -> {
            DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
            if (info == null || StringUtils.isEmpty(info.getHeaderColor())) {
                return null;
            }

            return "background-color: " + GuiDisplayTypeUtil.removeStringAfterSemicolon(info.getHeaderColor()) + " !important;";
        });
    }

    public MainPopupDialog getMainPopup() {
        return (MainPopupDialog) get(ID_MAIN_POPUP);
    }

    public String getMainPopupBodyId() {
        return ModalDialog.CONTENT_ID;
    }

    public void replaceMainPopup(Popupable popupable, AjaxRequestTarget target) {
        target.appendJavaScript("$('.modal-backdrop').remove();");
        showMainPopup(popupable, target);
    }

    public void showMainPopup(Popupable popupable, AjaxRequestTarget target) {
        MainPopupDialog dialog = getMainPopup();
        dialog.getDialogComponent().add(AttributeModifier.replace("style",
                dialog.generateWidthHeightParameter("" + (popupable.getWidth() > 0 ? popupable.getWidth() : ""),
                        popupable.getWidthUnit(),
                        "" + (popupable.getHeight() > 0 ? popupable.getHeight() : ""), popupable.getHeightUnit())));
        dialog.setContent(popupable.getContent());
        dialog.setFooter(popupable.getFooter());

        if (popupable.getTitleComponent() != null) {
            dialog.setTitleComponent(popupable.getTitleComponent());
        }

        dialog.setTitle(popupable.getTitle());
        dialog.setTitleIconClass(popupable.getTitleIconClass());
        dialog.open(target);
    }

    public void hideMainPopup(AjaxRequestTarget target) {
        getMainPopup().close(target);
        target.appendJavaScript("$('body').removeClass('modal-open');\n"
                + "$('.modal-backdrop').remove();");
    }

    private VisibleBehaviour createUserStatusBehaviour() {
        return new VisibleBehaviour(() -> isUserStatusVisible());
    }

    private boolean isUserStatusVisible() {
        return !isErrorPage() && isSideMenuVisible();
    }

    protected boolean isSideMenuVisible() {
        return AuthUtil.getPrincipalUser() != null;
    }

    protected IModel<String> createPageTitleModel() {
        return () -> {
            BaseMenuItem activeMenu = getActiveMenu();
            String pageTitleKey = null;
            if (activeMenu != null) {
                pageTitleKey = activeMenu.getNameModel();
            }

            if (StringUtils.isNotEmpty(pageTitleKey)) {
                return createStringResource(pageTitleKey).getString();
            }

            return super.createPageTitleModel().getObject();
        };
    }

    private <MI extends BaseMenuItem> MI getActiveMenu() {
        LeftMenuPanel sideBarMenu = getSideBarMenuPanel();
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
        target.add(getTitleContainer());

        //TODO what about breadcrumbs and page title (header)?
        //target.add(getHeaderTitle()); cannot update component with rendetBodyOnly
    }

    public WebMarkupContainer getTitleContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_MAIN_HEADER, ID_PAGE_TITLE_CONTAINER));
    }

//    public OpResult showResult(OperationResult result, String errorMessageKey) {
//        return showResult(result, errorMessageKey, true);
//    }
//
//    public OpResult showResult(OperationResult result, boolean showSuccess) {
//        return showResult(result, null, showSuccess);
//    }
//
//    public OpResult showResult(OperationResult result) {
//        return showResult(result, null, true);
//    }
//
//    public OpResult showResult(OperationResult result, String errorMessageKey, boolean showSuccess) {
//        Validate.notNull(result, "Operation result must not be null.");
//        Validate.notNull(result.getStatus(), "Operation result status must not be null.");
//
//        OperationResult scriptResult = executeResultScriptHook(result);
//        if (scriptResult == null) {
//            return null;
//        }
//
//        result = scriptResult;
//
//        OpResult opResult = OpResult.getOpResult((PageBase) getPage(), result);
//        opResult.determineObjectsVisibility(this);
//        switch (opResult.getStatus()) {
//            case FATAL_ERROR:
//            case PARTIAL_ERROR:
//                getSession().error(opResult);
//
//                break;
//            case IN_PROGRESS:
//            case NOT_APPLICABLE:
//                getSession().info(opResult);
//                break;
//            case SUCCESS:
//                if (!showSuccess) {
//                    break;
//                }
//                getSession().success(opResult);
//
//                break;
//            case UNKNOWN:
//            case WARNING:
//            default:
//                getSession().warn(opResult);
//
//        }
//        return opResult;
//    }

//    private OperationResult executeResultScriptHook(OperationResult result) {
//        CompiledGuiProfile adminGuiConfiguration = getCompiledGuiProfile();
//        if (adminGuiConfiguration.getFeedbackMessagesHook() == null) {
//            return result;
//        }
//
//        FeedbackMessagesHookType hook = adminGuiConfiguration.getFeedbackMessagesHook();
//        ExpressionType expressionType = hook.getOperationResultHook();
//        if (expressionType == null) {
//            return result;
//        }
//
//        String contextDesc = "operation result (" + result.getOperation() + ") script hook";
//
//        Task task = getPageTask();
//        OperationResult topResult = task.getResult();
//        try {
//            ExpressionFactory factory = getExpressionFactory();
//            PrismPropertyDefinition<OperationResultType> outputDefinition = getPrismContext().definitionFactory().createPropertyDefinition(
//                    ExpressionConstants.OUTPUT_ELEMENT_NAME, OperationResultType.COMPLEX_TYPE);
//            Expression<PrismPropertyValue<OperationResultType>, PrismPropertyDefinition<OperationResultType>> expression = factory.makeExpression(expressionType, outputDefinition, MiscSchemaUtil.getExpressionProfile(), contextDesc, task, topResult);
//
//            VariablesMap variables = new VariablesMap();
//
//            OperationResultType resultType = result.createOperationResultType();
//
//            variables.put(ExpressionConstants.VAR_INPUT, resultType, OperationResultType.class);
//
//            ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task);
//            PrismValueDeltaSetTriple<PrismPropertyValue<OperationResultType>> outputTriple = expression.evaluate(context, topResult);
//            if (outputTriple == null) {
//                return null;
//            }
//
//            Collection<PrismPropertyValue<OperationResultType>> values = outputTriple.getNonNegativeValues();
//            if (values == null || values.isEmpty()) {
//                return null;
//            }
//
//            if (values.size() > 1) {
//                throw new SchemaException("Expression " + contextDesc + " produced more than one value");
//            }
//
//            OperationResultType newResultType = values.iterator().next().getRealValue();
//            if (newResultType == null) {
//                return null;
//            }
//
//            return OperationResult.createOperationResult(newResultType);
//        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
//                ConfigurationException | SecurityViolationException e) {
//            topResult.recordFatalError(e);
//            if (StringUtils.isEmpty(result.getMessage())) {
//                topResult.setMessage("Couldn't process operation result script hook.");
//            }
//            topResult.addSubresult(result);
//            LoggingUtils.logUnexpectedException(LOGGER, contextDesc, e);
//            if (InternalsConfig.nonCriticalExceptionsAreFatal()) {
//                throw new SystemException(e.getMessage(), e);
//            } else {
//                return topResult;
//            }
//        }
//    }

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

    public String createPropertyModelExpression(String... components) {
        return StringUtils.join(components, ".");
    }

    // returns to previous page via restart response exception
    public RestartResponseException getRestartResponseException(Class<? extends Page> defaultBackPageClass) {
        return new RestartResponseException(defaultBackPageClass);
    }

    // TODO untangle this brutal code (list vs objectable vs other cases)
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
        EventHandler<Objectable> handler = new EventHandler<>() {
            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                    OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public EventResult postMarshall(
                    Objectable object, Element objectElement, OperationResult objectResult) {
                if (isListOfObjects) {
                    list.add(object.asPrismObject());
                } else {
                    //noinspection unchecked
                    objectHolder.setValue((T) object);
                }
                return EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
            }
        };
        LegacyValidator<?> validator = new LegacyValidator<>(getPrismContext(), handler);
        validator.setVerbose(true);
        validator.setValidateSchema(validateSchema);
        validator.validate(lexicalRepresentation, result, OperationConstants.IMPORT_OBJECT); // TODO the operation name

        result.computeStatus();
    }

    public long getItemsPerPage(UserProfileStorage.TableId tableId) {
        return getItemsPerPage(tableId.name());
    }

    public long getItemsPerPage(String tableIdName) {
        UserProfileStorage userProfile = getSessionStorage().getUserProfile();
        return userProfile.getPagingSize(tableIdName);
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
            if (br != null) {
                return true;
            }
        }

        return false;
    }

    public Breadcrumb redirectBack() {
        return redirectBack(DEFAULT_BREADCRUMB_STEP);
    }

    public void redirectToNotFoundPage() {
        PageError404 notFound = new PageError404();
        notFound.setBreadcrumbs(getBreadcrumbs());

        throw new RestartResponseException(notFound);
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

    public void navigateToNext(WebPage page) {
        if (!(page instanceof PageBase)) {
            setResponsePage(page);
            return;
        }

        PageBase next = (PageBase) page;
        next.setBreadcrumbs(getBreadcrumbs());

        setResponsePage(next);
    }

    /**
     * Returns exception, always use with `throw`.
     */
    public RestartResponseException redirectBackViaRestartResponseException() {
        return createRestartResponseExceptionWithBreadcrumb(2);
    }

    /**
     * Returns exception, always use with `throw`.
     */
    public RestartResponseException restartResponseExceptionToReload() {
        return createRestartResponseExceptionWithBreadcrumb(1);
    }

    /**
     * Returns restart exception to reload the page that is `backStep` from the end of the breadcrumbs.
     * 1 means the last page (current).
     */
    private RestartResponseException createRestartResponseExceptionWithBreadcrumb(int backStep) {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (breadcrumbs.size() < backStep) {
            return new RestartResponseException(getApplication().getHomePage());
        }

        Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - backStep);
        redirectBackToBreadcrumb(breadcrumb);
        return breadcrumb.getRestartResponseException();
    }

    public void redirectBackToBreadcrumb(Breadcrumb breadcrumb) {
        // we're preparing list of breadcrumbs for next page - we're still on "current" page and don't want to
        // change breadcrumbs on current page, so we have to copy the list
        List<Breadcrumb> copied = new ArrayList<>(getBreadcrumbs());

        removeAllAfterBreadcrumb(copied, breadcrumb);

        WebPage page = breadcrumb.redirect();
        if (page == null) {
            throw new RestartResponseException(getApplication().getHomePage());
        }

        if (page instanceof PageBase) {
            PageBase base = (PageBase) page;
            base.setBreadcrumbs(copied);
        }

        setResponsePage(page);
    }

    private void removeAllAfterBreadcrumb(Breadcrumb breadcrumb) {
        removeAllAfterBreadcrumb(getBreadcrumbs(), breadcrumb);
    }

    private void removeAllAfterBreadcrumb(List<Breadcrumb> breadcrumbs, Breadcrumb breadcrumb) {
        Validate.notNull(breadcrumb, "Breadcrumb must not be null");

        boolean found = false;

        //we remove all breadcrumbs that are after "breadcrumb"
        Iterator<Breadcrumb> iterator = breadcrumbs.iterator();
        while (iterator.hasNext()) {
            Breadcrumb b = iterator.next();
            if (found) {
                iterator.remove();
            } else if (b.equals(breadcrumb)) {
                found = true;
            }
        }
    }

    public void removeLastBreadcrumb() {
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        if (canRedirectBack(DEFAULT_BREADCRUMB_STEP)) {
            Breadcrumb breadcrumb = breadcrumbs.get(breadcrumbs.size() - DEFAULT_BREADCRUMB_STEP);
            removeAllAfterBreadcrumb(breadcrumb);
        } else {
            clearBreadcrumbs();
        }
    }

    protected void setTimeZone() {
        String timeZone = null;
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null && principal.getCompiledGuiProfile() != null) {
            timeZone = principal.getCompiledGuiProfile().getDefaultTimezone();
        }
        if (timeZone != null) {
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(TimeZone.getTimeZone(timeZone));
        }
    }

    public void setBreadcrumbs(List<Breadcrumb> breadcrumbs) {
        getBreadcrumbs().clear();

        if (breadcrumbs != null) {
            getBreadcrumbs().addAll(breadcrumbs);
        }
    }

    public List<Breadcrumb> getBreadcrumbs() {
        if (breadcrumbs == null) {
            breadcrumbs = new ArrayList<>();
        }
        return breadcrumbs;
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

    public void clearBreadcrumbs() {
        getBreadcrumbs().clear();
    }

    public boolean isLogoLinkEnabled() {
        return true;
    }

    public String determineDataLanguage() {
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
        target.add(get(createComponentPath(ID_MAIN_HEADER)));
    }

    public AsyncWebProcessManager getAsyncWebProcessManager() {
        return MidPointApplication.get().getAsyncWebProcessManager();
    }

    @Override
    public Locale getLocale() {
        return getSession().getLocale();
    }

    //REGISTRY

    public <C extends Containerable> Panel initContainerValuePanel(String id, IModel<PrismContainerValueWrapper<C>> model,
            ItemPanelSettings settings) {
        //TODO find from registry first
        return new PrismContainerValuePanel<>(id, model, settings) {
            @Override
            protected boolean isRemoveButtonVisible() {
                return false;
            }
        };
    }

    private LeftMenuPanel getSideBarMenuPanel() {
        return (LeftMenuPanel) get(ID_SIDEBAR_MENU);
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

    protected MessagePanel createMessagePanel(String panelId, MessagePanel.MessagePanelType type, String message, Object... params) {
        MessagePanel panel = new MessagePanel(panelId, type,
                createStringResource(message, params), false);
        return panel;
    }

    public TaskAwareExecutor taskAwareExecutor(@NotNull AjaxRequestTarget target, @NotNull String operationName) {
        return new TaskAwareExecutor(this, target, operationName);
    }
}
