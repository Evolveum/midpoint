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

package com.evolveum.midpoint.web.page;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.menu.SideBarMenuItem;
import com.evolveum.midpoint.web.component.menu.SideBarMenuPanel;
import com.evolveum.midpoint.web.component.menu.UserMenuPanel;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;
import com.evolveum.midpoint.web.component.message.MainFeedback;
import com.evolveum.midpoint.web.component.message.OpResult;
import com.evolveum.midpoint.web.component.message.TempFeedback;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.session.SessionStorage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.*;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.devutils.debugbar.DebugBar;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.FeedbackMessages;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.resource.CoreLibrariesContributor;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class PageTemplate extends WebPage {

    private static final Trace LOGGER = TraceManager.getTrace(PageTemplate.class);

    private static final String ID_TITLE = "title";
    private static final String ID_PAGE_TITLE_CONTAINER = "pageTitleContainer";
    private static final String ID_PAGE_TITLE_REAL = "pageTitleReal";
    private static final String ID_PAGE_TITLE = "pageTitle";
    private static final String ID_PAGE_SUBTITLE = "pageSubtitle";
    private static final String ID_DEBUG_PANEL = "debugPanel";
    private static final String ID_VERSION = "version";
    public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_TEMP_FEEDBACK = "tempFeedback";
    private static final String ID_DEBUG_BAR = "debugBar";
    private static final String ID_CLEAR_CACHE = "clearCssCache";
    private static final String ID_FEEDBACK_LIST = "feedbackList";
    private static final String ID_FEEDBACK_DETAILS = "feedbackDetails";
    private static final String ID_SIDEBAR_MENU = "sidebarMenu";
    private static final String ID_RIGHT_MENU = "rightMenu";
    private static final String ID_LOCALE = "locale";
    private static final String ID_MENU_TOGGLE = "menuToggle";
    private static final String ID_BREADCRUMBS="breadcrumbs";
    private static final String ID_BREADCRUMB="breadcrumb";
    private static final String ID_BC_LINK="bcLink";
    private static final String ID_BC_ICON="bcIcon";
    private static final String ID_BC_NAME="bcName";

    private PageTemplate previousPage;                  // experimental -- where to return e.g. when 'Back' button is clicked [NOT a class, in order to eliminate reinitialization when it is not needed]
    private boolean reinitializePreviousPages;      // experimental -- should we reinitialize all the chain of previous pages?

    public PageTemplate(PageParameters parameters) {
        super(parameters);

        initLayout();
    }

    public PageTemplate() {
        this(null);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        //this attaches jquery.js as first header item, which is used in our scripts.
        CoreLibrariesContributor.contribute(getApplication(), response);
    }

    @Override
    protected void onAfterRender() {
        super.onAfterRender();
        //we try to remove messages (and operation results) that were stored in session, but only
        //if all session messages were already rendered.
        boolean allRendered = true;
        FeedbackMessages messages = getSession().getFeedbackMessages();
        Iterator<FeedbackMessage> iterator = messages.iterator();
        while (iterator.hasNext()) {
            FeedbackMessage message = iterator.next();
            if (!message.isRendered()) {
                allRendered = false;
                break;
            }
        }

        if (getSession().getFeedbackMessages().size() > 0 && allRendered) {
            getSession().getFeedbackMessages().clear();
        }
    }

    private void initHeaderLayout() {
        WebMarkupContainer menuToggle = new WebMarkupContainer(ID_MENU_TOGGLE);
        menuToggle.add(createUserStatusBehaviour(true));
        add(menuToggle);

        UserMenuPanel rightMenu = new UserMenuPanel(ID_RIGHT_MENU);
        rightMenu.add(createUserStatusBehaviour(true));
        add(rightMenu);

        LocalePanel locale = new LocalePanel(ID_LOCALE);
        locale.add(createUserStatusBehaviour(false));
        add(locale);
    }

    private void initTitleLayout() {
        WebMarkupContainer pageTitleContainer = new WebMarkupContainer(ID_PAGE_TITLE_CONTAINER);
        pageTitleContainer.add(createUserStatusBehaviour(true));
        add(pageTitleContainer);

        WebMarkupContainer pageTitle = new WebMarkupContainer(ID_PAGE_TITLE);
        pageTitleContainer.add(pageTitle);
        Label pageTitleReal = new Label(ID_PAGE_TITLE_REAL, createPageTitleModel());
        pageTitleReal.setRenderBodyOnly(true);
        pageTitle.add(pageTitleReal);
        pageTitle.add(new Label(ID_PAGE_SUBTITLE, createPageSubTitleModel()));

        ListView breadcrumbs = new ListView<BreadcrumbItem>(ID_BREADCRUMB,
                new Model((Serializable) createBreadcrumbs())) {

            @Override
            protected void populateItem(ListItem<BreadcrumbItem> item) {
                final BreadcrumbItem dto = item.getModelObject();

                AjaxLink bcLink = new AjaxLink(ID_BC_LINK) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        navigateTo(target, dto);
                    }
                };
                item.add(bcLink);

                WebMarkupContainer bcIcon = new WebMarkupContainer(ID_BC_ICON);
                bcIcon.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        return dto.getIcon() != null && dto.getIcon().getObject() != null;
                    }
                });
                bcIcon.add(AttributeModifier.replace("class", dto.getIcon()));
                bcLink.add(bcIcon);

                Label bcName = new Label(ID_BC_NAME, dto.getName());
                bcLink.add(bcName);
            }
        };
        pageTitleContainer.add(breadcrumbs);
    }

    private void initLayout() {
        Label title = new Label(ID_TITLE, createPageTitleModel());
        title.setRenderBodyOnly(true);
        add(title);

        initHeaderLayout();
        initTitleLayout();
        initDebugBarLayout();

        List<SideBarMenuItem> menuItems = createMenuItems();
        SideBarMenuPanel sidebarMenu = new SideBarMenuPanel(ID_SIDEBAR_MENU, new Model((Serializable) menuItems));
        sidebarMenu.add(createUserStatusBehaviour(true));
        add(sidebarMenu);

        WebMarkupContainer version = new WebMarkupContainer(ID_VERSION) {

            @Deprecated
            public String getDescribe() {
                return PageTemplate.this.getDescribe();
            }
        };
        version.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return RuntimeConfigurationType.DEVELOPMENT.equals(getApplication().getConfigurationType());
            }
        });
        add(version);

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        add(feedbackContainer);

        MainFeedback feedback = new MainFeedback(ID_FEEDBACK);
        feedbackContainer.add(feedback);

        TempFeedback tempFeedback = new TempFeedback(ID_TEMP_FEEDBACK);
        feedbackContainer.add(tempFeedback);

//        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK_LIST);
//        feedbackList.setOutputMarkupId(true);
//        add(feedbackList);
    }

    private VisibleEnableBehaviour createUserStatusBehaviour(final boolean visibleIfLoggedIn) {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return SecurityUtils.getPrincipalUser() != null ? visibleIfLoggedIn : !visibleIfLoggedIn;
            }
        };
    }

    private void initDebugBarLayout() {
        DebugBar debugPanel = new DebugBar(ID_DEBUG_PANEL);
        add(debugPanel);

        WebMarkupContainer debugBar = new WebMarkupContainer(ID_DEBUG_BAR);
        debugBar.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                RuntimeConfigurationType runtime = getApplication().getConfigurationType();
                return RuntimeConfigurationType.DEVELOPMENT.equals(runtime);
            }
        });
        add(debugBar);

        AjaxButton clearCache = new AjaxButton(ID_CLEAR_CACHE, createStringResource("PageBase.clearCssCache")) {

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
            ObjectName objectName = ObjectName.getInstance("wro4j-idm:type=WroConfiguration");
            server.invoke(objectName, "reloadCache", new Object[]{}, new String[]{});
            if (target != null) {
                target.add(PageTemplate.this);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't clear less/js cache", ex);
            error("Error occurred, reason: " + ex.getMessage());
            if (target != null) {
                target.add(getFeedbackPanel());
            }
        }
    }

    protected List<SideBarMenuItem> createMenuItems() {
        return new ArrayList<>();
    }

    public WebMarkupContainer getFeedbackPanel() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }

    public MainFeedback getMainFeedbackPanel() {
        return (MainFeedback) getFeedbackPanel().get(ID_FEEDBACK);
    }

    public TempFeedback getTempFeedbackPanel() {
        return (TempFeedback) getFeedbackPanel().get(ID_TEMP_FEEDBACK);
    }

    public SessionStorage getSessionStorage() {
        MidPointAuthWebSession session = (MidPointAuthWebSession) getSession();
        return session.getSessionStorage();
    }

    public MidPointApplication getMidpointApplication() {
        return (MidPointApplication) getApplication();
    }

    protected IModel<String> createPageSubTitleModel() {
        String key = getClass().getSimpleName() + ".subTitle";
        return new StringResourceModel(key, this, new Model<String>(), "");
    }

    protected IModel<String> createPageTitleModel() {
        String key = getClass().getSimpleName() + ".title";
        return createStringResource(key);
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, new Model<String>(), resourceKey, objects);
    }

    public StringResourceModel createStringResource(Enum e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResource(resourceKey);
    }

    public static StringResourceModel createStringResourceStatic(Component component, String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, component, new Model<String>(), resourceKey, objects);
    }

    public static StringResourceModel createStringResourceStatic(Component component, Enum e) {
        String resourceKey = e.getDeclaringClass().getSimpleName() + "." + e.name();
        return createStringResourceStatic(component, resourceKey);
    }

    public void showResult(OperationResult result) {
        if (result == null) {
            return;
        }
        OpResult opResult = null;
        try {
            opResult = OpResult.getOpResult((PageBase) getPage(), result);
            showResult(opResult, false);
        } catch (Exception ex) {
            error(opResult);
        }
    }

    public void showResultInSession(OperationResult result) {
        if (result == null) {
            return;
        }

        OpResult opResult = null;
        try {
            opResult = OpResult.getOpResult((PageBase) getPage(), result);
            showResult(opResult, true);
        } catch (Exception ex) {
            error(opResult);
        }
    }

    private void showResult(OpResult opResult, boolean showInSession) {
        Validate.notNull(opResult, "Operation result must not be null.");
        Validate.notNull(opResult.getStatus(), "Operation result status must not be null.");

        switch (opResult.getStatus()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                if (showInSession) {
                    getSession().error(opResult);
                } else {
                    error(opResult);
                }
                break;
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                if (showInSession) {
                    getSession().info(opResult);
                } else {
                    info(opResult);
                }
                break;
            case SUCCESS:
                if (showInSession) {
                    getSession().success(opResult);
                } else {
                    success(opResult);
                }
                break;
            case UNKNOWN:
            case WARNING:
            default:
                if (showInSession) {
                    getSession().warn(opResult);
                } else {
                    warn(opResult);
                }
        }
    }

    protected String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    /**
     * It's here only because of some IDEs - it's not properly filtering resources during maven build.
     * "describe" variable is not replaced.
     *
     * @return "unknown" instead of "git describe" for current build.
     */
    @Deprecated
    public String getDescribe() {
        return getString("pageBase.unknownBuildNumber");
    }

    protected ModalWindow createModalWindow(final String id, IModel<String> title, int width, int height) {
        final ModalWindow modal = new ModalWindow(id);
        add(modal);

        modal.setResizable(false);
        modal.setTitle(title);
        modal.setCookieName(PageTemplate.class.getSimpleName() + ((int) (Math.random() * 100)));

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

    public boolean isReinitializePreviousPages() {
        return reinitializePreviousPages;
    }

    public void setReinitializePreviousPages(boolean reinitializePreviousPages) {
        this.reinitializePreviousPages = reinitializePreviousPages;
    }

    public PageTemplate getPreviousPage() {
        return previousPage;
    }

    public void setPreviousPage(PageTemplate previousPage) {
        this.previousPage = previousPage;
    }

    // experimental -- all pages should know how to reinitialize themselves (most hardcore way is to construct a new instance of themselves)
    public PageTemplate reinitialize() {
        // by default there is nothing to do -- our pages have to know how to reinitialize themselves
        LOGGER.trace("Default no-op implementation of reinitialize() called.");
        return this;
    }

    // experimental -- go to previous page (either with reinitialization e.g. when something changed, or without - typically when 'back' button is pressed)
    protected void goBack(Class<? extends Page> defaultBackPageClass) {
        LOGGER.trace("goBack called; page = {}, previousPage = {}, reinitializePreviousPages = {}", new Object[]{this, previousPage, reinitializePreviousPages});
        if (previousPage != null) {
            setResponsePage(getPreviousPageToGoTo());
        } else {
            LOGGER.trace("...going to default back page {}", defaultBackPageClass);
            setResponsePage(defaultBackPageClass);
        }
    }

    // returns previous page ready to go to (i.e. reinitialized, if necessary)
    public PageTemplate getPreviousPageToGoTo() {
        if (previousPage == null) {
            return null;
        }

        if (isReinitializePreviousPages()) {
            LOGGER.trace("...calling reinitialize on previousPage ({})", previousPage);

            previousPage.setReinitializePreviousPages(true);            // we set this flag on the original previous page...
            PageTemplate reinitialized = previousPage.reinitialize();
            reinitialized.setReinitializePreviousPages(true);           // ...but on the returned value, as it is probably different object
            return reinitialized;
        } else {
            return previousPage;
        }
    }

    // returns to previous page via restart response exception
    public RestartResponseException getRestartResponseException(Class<? extends Page> defaultBackPageClass) {
        LOGGER.trace("getRestartResponseException called; page = {}, previousPage = {}, reinitializePreviousPages = {}", new Object[]{this, previousPage, reinitializePreviousPages});
        if (previousPage != null) {
            return new RestartResponseException(getPreviousPageToGoTo());
        } else {
            LOGGER.trace("...going to default back page {}", defaultBackPageClass);
            return new RestartResponseException(defaultBackPageClass);
        }
    }

    private void navigateTo(AjaxRequestTarget target, BreadcrumbItem dto) {
        if (dto.getPageParameters() == null) {
            setResponsePage(dto.getPage());
        }

        try {
            Class clazz = dto.getPage();
            Constructor constr = clazz.getConstructor(PageParameters.class);
            WebPage page = (WebPage) constr.newInstance(dto.getPageParameters());

            setResponsePage(page);
        } catch (Exception ex) {
            LOGGER.debug("Couldn't navigate to breadcrumb item " + dto,  ex);
            error(getString("PageTemplate.couldntNavigateBreadcrumb", ex.getMessage()));
            target.add(getFeedbackPanel());
        }
    }

    public List<BreadcrumbItem> createBreadcrumbs() {
        //todo implement breadcrumb algorithm [lazyman]
        return new ArrayList<>();
    }
}
