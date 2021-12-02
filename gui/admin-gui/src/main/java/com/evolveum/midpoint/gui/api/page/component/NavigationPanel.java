/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.page.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.menu.UserMenuPanel;
import com.evolveum.midpoint.web.component.menu.top.LocalePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.PageAssignmentsList;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NavigationPanel extends BasePanel<String> {

    private static final String ID_RIGHT_MENU = "rightMenu";
    private static final String ID_LOCALE = "locale";

    private static final String ID_DEPLOYMENT_NAME = "deploymentName";
    private static final String ID_PAGE_TITLE_REAL = "pageTitleReal";
    private static final String ID_PAGE_TITLE = "pageTitle";

    private static final String ID_BREADCRUMB = "breadcrumb";
    private static final String ID_BC_LINK = "bcLink";
    private static final String ID_BC_ICON = "bcIcon";
    private static final String ID_BC_NAME = "bcName";

    private static final String ID_CART_BUTTON = "cartButton";
    private static final String ID_CART_ITEMS_COUNT = "itemsCount";

    private List<Breadcrumb> breadcrumbs;

    public NavigationPanel(String id, IModel<String> titleModel) {
        super(id, titleModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        initHeaderLayout();

        initTitleLayout();

        initBreadcrumbsLayout();

        initCartButton();
    }

    private void initHeaderLayout() {
        UserMenuPanel rightMenu = new UserMenuPanel(ID_RIGHT_MENU);
        add(rightMenu);

        LocalePanel locale = new LocalePanel(ID_LOCALE);
        add(locale);
    }

    private void initTitleLayout() {

        WebMarkupContainer pageTitle = new WebMarkupContainer(ID_PAGE_TITLE);
        add(pageTitle);

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

        Label pageTitleReal = new Label(ID_PAGE_TITLE_REAL, getModel());
        pageTitleReal.setRenderBodyOnly(true);
        pageTitle.add(pageTitleReal);
    }

    private void initBreadcrumbsLayout() {
        IModel<List<Breadcrumb>> breadcrumbsModel = new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public List<Breadcrumb> getObject() {
                return getBreadcrumbs();
            }
        };

        ListView<Breadcrumb> breadcrumbs = new ListView<>(ID_BREADCRUMB, breadcrumbsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Breadcrumb> item) {
                final Breadcrumb dto = item.getModelObject();

                AjaxLink<String> bcLink = new AjaxLink<>(ID_BC_LINK) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        redirectBackToBreadcrumb(dto);
                    }
                };
                item.add(bcLink);
                bcLink.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isEnabled() {
                        return dto.isUseLink();
                    }
                });

                WebMarkupContainer bcIcon = new WebMarkupContainer(ID_BC_ICON);
                bcIcon.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return dto.getIcon() != null && dto.getIcon().getObject() != null;
                    }
                });
                bcIcon.add(AttributeModifier.replace("class", dto.getIcon()));
                bcLink.add(bcIcon);

                Label bcName = new Label(ID_BC_NAME, dto.getLabel());
                bcLink.add(bcName);

                item.add(new VisibleEnableBehaviour() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public boolean isVisible() {
                        return dto.isVisible();
                    }
                });
            }
        };
        add(breadcrumbs);
    }

    public List<Breadcrumb> getBreadcrumbs() {
        if (breadcrumbs == null) {
            breadcrumbs = new ArrayList<>();
        }
        return breadcrumbs;
    }

    public void redirectBackToBreadcrumb(Breadcrumb breadcrumb) {
        Validate.notNull(breadcrumb, "Breadcrumb must not be null");

        boolean found = false;

        //we remove all breadcrumbs that are after "breadcrumb"
        List<Breadcrumb> breadcrumbs = getBreadcrumbs();
        Iterator<Breadcrumb> iterator = breadcrumbs.iterator();
        while (iterator.hasNext()) {
            Breadcrumb b = iterator.next();
            if (found) {
                iterator.remove();
            } else if (b.equals(breadcrumb)) {
                found = true;
            }
        }
        WebPage page = breadcrumb.redirect();
        if (page instanceof PageBase) {
            PageBase base = (PageBase) page;
            base.setBreadcrumbs(breadcrumbs);
        }

        setResponsePage(page);
    }

    private void initCartButton() {
        AjaxButton cartButton = new AjaxButton(ID_CART_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().navigateToNext(new PageAssignmentsList<>(true));
            }
        };
        cartButton.setOutputMarkupId(true);
        cartButton.add(getShoppingCartVisibleBehavior());
        add(cartButton);

        Label cartItemsCount = new Label(ID_CART_ITEMS_COUNT, new LoadableModel<String>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            public String load() {
                return Integer.toString(getAssignmentShoppingCartSize());
            }
        });
        cartItemsCount.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !(getAssignmentShoppingCartSize() == 0);
            }
        });
        cartItemsCount.setOutputMarkupId(true);
        cartButton.add(cartItemsCount);
    }

    private VisibleEnableBehaviour getShoppingCartVisibleBehavior() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL, PageSelf.AUTH_SELF_ALL_URI)
                                && getAssignmentShoppingCartSize() > 0);
            }
        };
    }

    private int getAssignmentShoppingCartSize() {
        return getPageBase().getSessionStorage().getRoleCatalog().getAssignmentShoppingCart().size();
    }

}
