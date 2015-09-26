package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.SecurityUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.io.Serializable;

/**
 * @author Viliam Repan (lazyman)
 */
public class MainMenuPanel extends SimplePanel<MainMenuItem> {

    private static final String ID_ITEM = "item";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "label";
    private static final String ID_ICON = "icon";
    private static final String ID_SUBMENU = "submenu";
    private static final String ID_ARROW = "arrow";
    private static final String ID_SUB_ITEM = "subItem";
    private static final String ID_SUB_LINK = "subLink";
    private static final String ID_SUB_LABEL = "subLabel";

    public MainMenuPanel(String id, IModel<MainMenuItem> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        final MainMenuItem menu = getModelObject();

        WebMarkupContainer item = new WebMarkupContainer(ID_ITEM);
        item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                Class page = getPage().getClass();

                Class type = menu.getPage();
                if (type != null && page.isAssignableFrom(type)) {
                    return "active";
                }

                for (MenuItem item : menu.getItems()) {
                    if (item.getPage() != null && page.isAssignableFrom(item.getPage())) {
                        return "active";
                    }
                }

                return !menu.getItems().isEmpty() ? "treeview" : null;
            }
        }));
        add(item);

        WebMarkupContainer link;
        if (menu.getPage() != null) {
            link = new BookmarkablePageLink(ID_LINK, menu.getPage());
        } else {
            link = new WebMarkupContainer(ID_LINK);
        }
        item.add(link);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeModifier.replace("class", new PropertyModel<>(menu, MainMenuItem.F_ICON_CLASS)));
        link.add(icon);

        Label label = new Label(ID_LABEL, menu.getName());
        link.add(label);

        WebMarkupContainer arrow = new WebMarkupContainer(ID_ARROW);
        arrow.add(createMenuVisibilityBehaviour(menu));
        link.add(arrow);

        WebMarkupContainer submenu = new WebMarkupContainer(ID_SUBMENU);
        submenu.add(createMenuVisibilityBehaviour(menu));
        item.add(submenu);

        ListView<MenuItem> subItem = new ListView<MenuItem>(ID_SUB_ITEM, new Model((Serializable) menu.getItems())) {

            @Override
            protected void populateItem(ListItem<MenuItem> listItem) {
                createSubmenu(listItem);
            }
        };
        submenu.add(subItem);
    }

    private VisibleEnableBehaviour createMenuVisibilityBehaviour(final MainMenuItem menu) {
        return new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !menu.getItems().isEmpty();
            }
        };
    }

    private void createSubmenu(final ListItem<MenuItem> listItem) {
        final MenuItem menu = listItem.getModelObject();

        listItem.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                Class page = getPage().getClass();
                Class type = menu.getPage();

                return page.isAssignableFrom(type) ? "active" : null;
            }
        }));

        BookmarkablePageLink subLink = new BookmarkablePageLink(ID_SUB_LINK, menu.getPage());
        listItem.add(subLink);

        Label subLabel = new Label(ID_SUB_LABEL, menu.getName());
        subLink.add(subLabel);

        listItem.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return SecurityUtils.isMenuAuthorized(listItem.getModelObject());
            }
        });
    }
}
