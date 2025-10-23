/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Args;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.model.CountModelProvider;
import com.evolveum.midpoint.gui.api.model.CssIconModelProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author lazyman
 * @author Igor Vaynberg (ivaynberg)
 */
public class TabbedPanel<T extends ITab> extends Panel {

    /**
     * id used for child panels
     */
    public static final String TAB_PANEL_ID = "panel";
    public static final String ID_TABS_CONTAINER = "tabs-container";
    public static final String ID_TABS = "tabs";
    public static final String RIGHT_SIDE_TAB_ITEM_ID = "rightSideTabItem";
    public static final String RIGHT_SIDE_TAB_ID = "rightSideTab";

    protected static final String ID_TITLE = "title";
    protected static final String ID_COUNT = "count";
    protected static final String ID_LINK = "link";
    private static final String ID_ICON = "icon";

    private final IModel<List<T>> tabs;
    /**
     * the current tab
     */
    private int currentTab = -1;
    private transient VisibilityCache visibilityCache;

    public TabbedPanel(final String id, final List<T> tabs) {
        this(id, tabs, null);
    }

    public TabbedPanel(final String id, final List<T> tabs, @Nullable RightSideItemProvider rightSideItemProvider) {
        this(id, tabs, null, rightSideItemProvider);
    }

    public TabbedPanel(final String id, final List<T> tabs, IModel<Integer> model, @Nullable RightSideItemProvider rightSideItemProvider) {
        this(id, Model.ofList(tabs), model, rightSideItemProvider);
    }

    /**
     * Constructor
     *
     * @param id component id
     * @param tabs list of ITab objects used to represent tabs
     */
    public TabbedPanel(final String id, final IModel<List<T>> tabs) {
        this(id, tabs, null, null);
    }

    /**
     * Constructor
     *
     * @param id component id
     * @param tabs list of ITab objects used to represent tabs
     * @param model model holding the index of the selected tab
     */
    public TabbedPanel(final String id, final IModel<List<T>> tabs, IModel<Integer> model, RightSideItemProvider rightSideItemProvider) {
        super(id, model);

        this.tabs = Args.notNull(tabs, "tabs");

        final IModel<Integer> tabCount = new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Integer getObject() {
                return tabs.getObject().size();
            }
        };

        initDefaultComponentCssClass();

        WebMarkupContainer tabsContainer = newTabsContainer(ID_TABS_CONTAINER);
        tabsContainer.setOutputMarkupId(true);
        tabsContainer.setOutputMarkupPlaceholderTag(true);
        add(tabsContainer);

        // add the loop used to generate tab names
        Loop loop = new Loop(ID_TABS, tabCount) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final LoopItem item) {
                populateLoopItem(item);
            }

            @Override
            protected LoopItem newItem(final int iteration) {
                return newTabContainer(iteration);
            }
        };

        loop.setOutputMarkupId(true);
        loop.setOutputMarkupPlaceholderTag(true);

        tabsContainer.add(loop);

        WebMarkupContainer rightSideTabItem = new WebMarkupContainer(RIGHT_SIDE_TAB_ITEM_ID);
        Component rightSideTabPanel = rightSideItemProvider != null ? rightSideItemProvider.createRightSideItem(RIGHT_SIDE_TAB_ID) : null;
        if (rightSideTabPanel != null) {
            rightSideTabItem.add(rightSideTabPanel);
        } else {
            rightSideTabItem.setVisible(false);
        }
        tabsContainer.add(rightSideTabItem);

        add(newPanel());
    }

    protected void initDefaultComponentCssClass() {
        add(AttributeModifier.prepend("class", "card card-primary card-outline card-outline-tabs"));
    }

    protected void populateLoopItem(LoopItem item) {
        final int index = item.getIndex();
        final T tab = TabbedPanel.this.tabs.getObject().get(index);

        final WebMarkupContainer titleLink = newLink(ID_LINK, index);
        titleLink.add(AttributeAppender.append("class", () -> getSelectedTab() == index ? getSelectedTabCssClass() : ""));
        titleLink.setOutputMarkupPlaceholderTag(true);
        titleLink.setOutputMarkupId(true);
        item.add(titleLink);

        IModel<String> iconCssClass = null;
        if (tab instanceof CssIconModelProvider) {
            iconCssClass = ((CssIconModelProvider) tab).getCssIconModel();
        }
        titleLink.add(newIcon(ID_ICON, iconCssClass));
        titleLink.add(newTitle(ID_TITLE, tab.getTitle(), index));

        final IModel<String> count;
        final IModel<String> countCssClass;
        if (tab instanceof CountModelProvider) {
            CountModelProvider cmp = (CountModelProvider) tab;
            count = cmp.getCountModel();
            countCssClass = cmp.getCountCssClassModel();
        } else {
            count = null;
            countCssClass = null;
        }

        Label countLabel = new Label(ID_COUNT, count);
        countLabel.setOutputMarkupId(true);
        countLabel.setOutputMarkupPlaceholderTag(true);
        countLabel.add(AttributeModifier.append("class", () -> countCssClass != null ? countCssClass.getObject() : null));
        countLabel.add(new VisibleBehaviour(() -> count != null));
        titleLink.add(countLabel);
    }

    /**
     * Override of the default initModel behaviour. This component <strong>will not</strong> use any
     * compound model of a parent.
     *
     * @see org.apache.wicket.Component#initModel()
     */
    @Override
    protected IModel<?> initModel() {
        return new Model<>(-1);
    }

    /**
     * Generates the container for all tabs. The default container automatically adds the css
     * <code>class</code> attribute based on the return value of {@link #getTabContainerCssClass()}
     *
     * @param id container id
     * @return container
     */
    protected WebMarkupContainer newTabsContainer(final String id) {
        WebMarkupContainer tabs = new WebMarkupContainer(id);
        tabs.setOutputMarkupId(true);
        tabs.setOutputMarkupPlaceholderTag(true);
        return tabs;
    }

    /**
     * Generates a loop item used to represent a specific tab's <code>li</code> element.
     *
     * @param tabIndex
     * @return new loop item
     */
    protected LoopItem newTabContainer(final int tabIndex) {
        return new LoopItem(tabIndex) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onConfigure() {
                super.onConfigure();

                setVisible(getVisiblityCache().isVisible(tabIndex));
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);

                String cssClass = tag.getAttribute("class");
                if (cssClass == null) {
                    cssClass = " ";
                }
                cssClass += " tab" + getIndex();

                if (getVisiblityCache().getLastVisible() == getIndex()) {
                    cssClass += ' ' + getLastTabCssClass();
                }
                tag.put("class", cssClass.trim());
            }
        };
    }

    @Override
    protected void onBeforeRender() {
        int index = getSelectedTab();

        if (index == -1 || !getVisiblityCache().isVisible(index)) {
            // find first visible tab
            index = -1;
            for (int i = 0; i < tabs.getObject().size(); i++) {
                if (getVisiblityCache().isVisible(i)) {
                    index = i;
                    break;
                }
            }

            if (index != -1) {
                // found a visible tab, so select it
                setSelectedTab(index);
            }
        }

        setCurrentTab(index);

        super.onBeforeRender();
    }

    /**
     * @return the value of css class attribute that will be added to last tab. The default value is
     * <code>last</code>
     */
    protected String getLastTabCssClass() {
        return "";
    }

    /**
     * @return the value of css class attribute that will be added to a div containing the tabs. The
     * default value is <code>tab-row</code>
     */
    protected String getTabContainerCssClass() {
        return "tab-row";
    }

    /**
     * @return the value of css class attribute that will be added to selected tab. The default
     * value is <code>selected</code>
     */
    protected String getSelectedTabCssClass() {
        return "active";
    }

    /**
     * @return list of tabs that can be used by the user to add/remove/reorder tabs in the panel
     */
    public final IModel<List<T>> getTabs() {
        return tabs;
    }

    protected Component newIcon(final String id, IModel<String> iconCssClass) {
        Label label = new Label(id);
        label.add(AttributeModifier.replace("class", iconCssClass));
        label.add(new VisibleBehaviour(() -> iconCssClass != null && StringUtils.isNotEmpty(iconCssClass.getObject())));

        return label;
    }

    /**
     * Factory method for tab titles. Returned component can be anything that can attach to span
     * tags such as a fragment, panel, or a label
     *
     * @param titleId id of tiatle component
     * @param titleModel model containing tab title
     * @param index index of tab
     * @return title component
     */
    protected Component newTitle(final String titleId, final IModel<?> titleModel, final int index) {
        Label label = new Label(titleId, titleModel);
        label.setRenderBodyOnly(true);
        return label;
    }

    /**
     * Factory method for links used to switch between tabs.
     * <p/>
     * The created component is attached to the following markup. Label component with id: title
     * will be added for you by the tabbed panel.
     * <p/>
     * <pre>
     * &lt;a href=&quot;#&quot; wicket:id=&quot;link&quot;&gt;&lt;span wicket:id=&quot;title&quot;&gt;[[tab title]]&lt;/span&gt;&lt;/a&gt;
     * </pre>
     * <p/>
     * Example implementation:
     * <p/>
     * <pre>
     * protected WebMarkupContainer newLink(String linkId, final int index)
     * {
     *     return new Link(linkId)
     *    {
     *         private static final long serialVersionUID = 1L;
     *
     *         public void onClick()
     *        {
     *             setSelectedTab(index);
     *        }
     *    };
     * }
     * </pre>
     *
     * @param linkId component id with which the link should be created
     * @param index index of the tab that should be activated when this link is clicked. See
     * {@link #setSelectedTab(int)}.
     * @return created link component
     */
    protected WebMarkupContainer newLink(final String linkId, final int index) {
        return new Link<Void>(linkId) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick() {
                setSelectedTab(index);
                onTabChange(index);
            }
        };
    }

    /**
     * sets the selected tab
     *
     * @param index index of the tab to select
     * @return this for chaining
     * @throws IndexOutOfBoundsException if index is not in the range of available tabs
     */
    public TabbedPanel<T> setSelectedTab(final int index) {
        if ((index < 0) || (index >= tabs.getObject().size())) {
            throw new IndexOutOfBoundsException();
        }

        setDefaultModelObject(index);

        // force the tab's component to be aquired again if already the current tab
        currentTab = -1;
        setCurrentTab(index);

        return this;
    }

    private void setCurrentTab(int index) {
        if (this.currentTab == index) {
            // already current
            return;
        }
        this.currentTab = index;

        final Component component;

        if (currentTab == -1 || (tabs.getObject().size() == 0) || !getVisiblityCache().isVisible(currentTab)) {
            // no tabs or the current tab is not visible
            component = newPanel();
        } else {
            // show panel from selected tab
            T tab = tabs.getObject().get(currentTab);
            component = tab.getPanel(TAB_PANEL_ID);
            if (component == null) {
                throw new WicketRuntimeException("ITab.getPanel() returned null. TabbedPanel [" +
                        getPath() + "] ITab index [" + currentTab + "]");
            }
        }

        if (!component.getId().equals(TAB_PANEL_ID)) {
            throw new WicketRuntimeException(
                    "ITab.getPanel() returned a panel with invalid id [" +
                            component.getId() +
                            "]. You must always return a panel with id equal to the provided panelId parameter. TabbedPanel [" +
                            getPath() + "] ITab index [" + currentTab + "]");
        }
        component.setOutputMarkupPlaceholderTag(true);
        component.setOutputMarkupId(true);

        addOrReplace(component);
    }

    private WebMarkupContainer newPanel() {
        return new WebMarkupContainer(TAB_PANEL_ID);
    }

    /**
     * @return index of the selected tab
     */
    public final int getSelectedTab() {
        return (Integer) getDefaultModelObject();
    }

    @Override
    protected void onDetach() {
        visibilityCache = null;

        super.onDetach();
    }

    private VisibilityCache getVisiblityCache() {
        if (visibilityCache == null) {
            visibilityCache = new VisibilityCache();
        }

        return visibilityCache;
    }

    /**
     * A cache for visibilities of {@link ITab}s.
     */
    private class VisibilityCache {

        /**
         * Visibility for each tab.
         */
        private Boolean[] visibilities;

        /**
         * Last visible tab.
         */
        private int lastVisible = -1;

        public VisibilityCache() {
            visibilities = new Boolean[tabs.getObject().size()];
        }

        public int getLastVisible() {
            if (lastVisible == -1) {
                for (int t = 0; t < tabs.getObject().size(); t++) {
                    if (isVisible(t)) {
                        lastVisible = t;
                    }
                }
            }

            return lastVisible;
        }

        public boolean isVisible(int index) {
            if (visibilities.length < index + 1) {
                Boolean[] resized = new Boolean[index + 1];
                System.arraycopy(visibilities, 0, resized, 0, visibilities.length);
                visibilities = resized;
            }

            if (visibilities.length > 0) {
                Boolean visible = visibilities[index];
                if (visible == null) {
                    List<T> tabsList = tabs.getObject();
                    if (tabsList.size() <= index) {
                        return false;
                    }

                    T tab = tabs.getObject().get(index);
                    visible = tab != null && tab.isVisible();
                    if (tab != null) {
                        visibilities[index] = visible;
                    }
                }
                return visible;
            } else {
                return false;
            }
        }
    }

    /**
     * Method called after tab was changed - user clicked on link in tab header.
     *
     * @param index Index of new tab.
     */
    protected void onTabChange(int index) {
    }

    @FunctionalInterface
    public interface RightSideItemProvider extends Serializable {
        Component createRightSideItem(String id);
    }

    public void reloadCountLabels(AjaxRequestTarget target) {
        Loop tabbedPanel = ((Loop) get(ID_TABS_CONTAINER).get(ID_TABS));
        int tabsCount = tabbedPanel.getIterations();
        for (int i = 0; i < tabsCount; i++) {
            Component countLabel = tabbedPanel.get(Integer.toString(i)).get(ID_LINK).get(ID_COUNT);
            if (countLabel != null) {
                target.add(countLabel);
            }
        }
    }
}
