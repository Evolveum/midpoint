/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.component.breadcrumbs.BreadcrumbPageClass;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.SecurityUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.IPageFactory;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.flow.RedirectToUrlException;

import java.io.Serializable;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainMenuPanel extends BasePanel<MainMenuItem> {
	private static final long serialVersionUID = 1L;

    private static final String ID_ITEM = "item";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";
    private static final String ID_ICON = "icon";
    private static final String ID_SUBMENU = "submenu";
    private static final String ID_ARROW = "arrow";
    private static final String ID_BUBBLE = "bubble";
    private static final String ID_SUB_ITEM = "subItem";
    private static final String ID_SUB_LINK = "subLink";
    private static final String ID_SUB_LABEL = "subLabel";

    private static final Trace LOGGER = TraceManager.getTrace(MainMenuPanel.class);

    public MainMenuPanel(String id, IModel<MainMenuItem> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        final MainMenuItem menu = getModelObject();

        WebMarkupContainer item = new WebMarkupContainer(ID_ITEM);
        item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
            public String getObject() {
                if (menu.isMenuActive((WebPage) getPage())) {
                    return "active";
                }

                for (MenuItem item : menu.getItems()) {
                    if (item.isMenuActive((WebPage) getPage())) {
                        return "active";
                    }
                }

                return !menu.getItems().isEmpty() ? "treeview" : null;
            }
        }));
        add(item);

        WebMarkupContainer link;
        if (menu.getPageClass() != null) {
            link = new AjaxLink(ID_LINK) {
            	private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    mainMenuPerformed(menu);
                }
            };
        } else if (menu instanceof AdditionalMenuItem){
            link = new AjaxLink(ID_LINK) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    additionalMenuPerformed(menu);
                }
            };
        } else {
            link = new WebMarkupContainer(ID_LINK);
        }
        item.add(link);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeModifier.replace("class", new PropertyModel<>(menu, MainMenuItem.F_ICON_CLASS)));
        link.add(icon);

        Label label = new Label(ID_LABEL, menu.getNameModel());
        link.add(label);

        final PropertyModel<String> bubbleModel = new PropertyModel<>(menu, MainMenuItem.F_BUBBLE_LABEL);

        Label bubble = new Label(ID_BUBBLE, bubbleModel);
        bubble.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return bubbleModel.getObject() != null;
			}
        });
        link.add(bubble);

        WebMarkupContainer arrow = new WebMarkupContainer(ID_ARROW);
        arrow.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
		    public boolean isVisible() {
		        return !menu.getItems().isEmpty() && bubbleModel.getObject() == null;
		    }
		});
        link.add(arrow);

        WebMarkupContainer submenu = new WebMarkupContainer(ID_SUBMENU);
        submenu.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
		    public boolean isVisible() {
		        return !menu.getItems().isEmpty();
		    }
		});
        item.add(submenu);

        ListView<MenuItem> subItem = new ListView<MenuItem>(ID_SUB_ITEM, new Model((Serializable) menu.getItems())) {

            @Override
            protected void populateItem(ListItem<MenuItem> listItem) {
                createSubmenu(listItem);
            }
        };
        submenu.add(subItem);
    }

    private void createSubmenu(final ListItem<MenuItem> listItem) {
        final MenuItem menuItem = listItem.getModelObject();

        listItem.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return menuItem.isMenuActive((WebPage) getPage()) ? "active" : null;
            }
        }));

        Link<String> subLink = new Link<String>(ID_SUB_LINK) {
        	private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                menuItemPerformed(menuItem);
            }
        };
        listItem.add(subLink);

        Label subLabel = new Label(ID_SUB_LABEL, menuItem.getNameModel());
        subLink.add(subLabel);

        listItem.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                MenuItem mi = listItem.getModelObject();

                boolean visible = true;
                if (mi.getVisibleEnable() != null) {
                    visible = mi.getVisibleEnable().isVisible();
                }

                return visible && SecurityUtils.isMenuAuthorized(mi);
            }

            @Override
            public boolean isEnabled() {
                MenuItem mi = listItem.getModelObject();

                if (mi.getVisibleEnable() == null) {
                    return true;
                }

                return mi.getVisibleEnable().isEnabled();
            }
        });
    }

    private void menuItemPerformed(MenuItem menu) {
    	LOGGER.trace("menuItemPerformed: {}", menu);

        IPageFactory pFactory = Session.get().getPageFactory();
        WebPage page = pFactory.newPage(menu.getPageClass(), menu.getParams());
        if (!(page instanceof PageBase)) {
            setResponsePage(page);
            return;
        }

        PageBase pageBase = (PageBase) page;

        // IMPORTANT: we need to re-bundle the name to a new models
        // that will not be connected to the old page reference
        // otherwise the old page will somehow remain in the memory
        // I have no idea how it could do that and especially how
        // several old pages can remain in memory. But if the model
        // is not re-bundled here then the page size grows and never
        // falls.
        MainMenuItem mainMenuItem = getModelObject();
        String name = mainMenuItem.getNameModel().getObject();
        Breadcrumb bc = new Breadcrumb(new Model<>(name));
        bc.setIcon(new Model<>(mainMenuItem.getIconClass()));
        pageBase.addBreadcrumb(bc);

        List<MenuItem> items = mainMenuItem.getItems();
        if (!items.isEmpty() && mainMenuItem.isInsertDefaultBackBreadcrumb()) {
            MenuItem first = items.get(0);

            IModel<String> nameModel = first.getNameModel();
            BreadcrumbPageClass invisibleBc = new BreadcrumbPageClass(new Model<>(nameModel.getObject()), first.getPageClass(),
                    first.getParams());
            invisibleBc.setVisible(false);
            pageBase.addBreadcrumb(invisibleBc);
        }

        setResponsePage(page);
    }

    private void mainMenuPerformed(MainMenuItem menu) {
    	LOGGER.trace("mainMenuPerformed: {}", menu);

        if (menu.getParams() == null) {
            setResponsePage(menu.getPageClass());
        } else {
            setResponsePage(menu.getPageClass(), menu.getParams());
        }
    }

    private void additionalMenuPerformed(MainMenuItem menu) {
    	LOGGER.trace("additionalMenuPerformed: {}", menu);

        if (menu.getPageClass() != null) {
            setResponsePage(menu.getPageClass());
        } else {
            throw new RedirectToUrlException(((AdditionalMenuItem)menu).getTargetUrl());
        }
    }
}
