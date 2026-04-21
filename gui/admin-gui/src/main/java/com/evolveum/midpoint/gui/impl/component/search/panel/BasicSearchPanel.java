/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.button.SelectableItemListPopoverPanel;
import com.evolveum.midpoint.gui.impl.component.search.SearchItemWrapperComparator;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.FilterableSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.BasicQueryWrapper;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.model.Model;

public class BasicSearchPanel extends BasePanel<BasicQueryWrapper> {

    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_MORE = "more";
    private static final String ID_MORE_PROPERTIES_POPOVER = "morePropertiesPopover";
    private static final String ID_MORE_PROPERTIES_POPOVER_STATUS = "popoverStatus";
    private LoadableDetachableModel<List<FilterableSearchItemWrapper<?>>> basicSearchItemsModel;
    private LoadableDetachableModel<List<FilterableSearchItemWrapper<?>>> morePopupModel;
    private boolean isPopoverOpen = false;
    private int currentBiggestDisplayOrder;

    public BasicSearchPanel(String id, IModel<BasicQueryWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initBasicSearchItemsModel();
        initMorePopupModel();
        initLayout();
    }

    private void initBasicSearchItemsModel() {
        basicSearchItemsModel = new LoadableDetachableModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<FilterableSearchItemWrapper<?>> load() {
                return getModelObject().getItemsList();
            }
        };
    }

    private void initMorePopupModel() {
        morePopupModel = new LoadableDetachableModel<>() {
            @Override
            protected List<FilterableSearchItemWrapper<?>> load() {
                return getModelObject().getItemsList();
            }
        };
    }

    private void initLayout() {

        ListView<FilterableSearchItemWrapper<?>> items = new ListView<>(ID_ITEMS, basicSearchItemsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<FilterableSearchItemWrapper<?>> item) {
                AbstractSearchItemPanel searchItemPanel = createSearchItemPanel(ID_ITEM, item.getModel());
                searchItemPanel.setOutputMarkupId(true);
                searchItemPanel.add(new VisibleBehaviour(() -> item.getModelObject().isVisible()));
                item.add(searchItemPanel);
            }
        };
        add(items);

        currentBiggestDisplayOrder = calculateCurrentBiggestDisplayOrder();

        // it is needed to sort search items after removal of displayOrder for not visible search items,
        // so they are properly sorted in More button alphabetically
        // this case occurs when you switch between Saved filters
        sortItems();

        WebMarkupContainer propertiesStatus = new WebMarkupContainer(ID_MORE_PROPERTIES_POPOVER_STATUS, Model.of(""));
        propertiesStatus.setOutputMarkupId(true);
        add(propertiesStatus);

        SelectableItemListPopoverPanel<FilterableSearchItemWrapper<?>> popoverPanel =
                new SelectableItemListPopoverPanel<>(ID_MORE_PROPERTIES_POPOVER, morePopupModel) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void addItemsPerformed(List<FilterableSearchItemWrapper<?>> itemList, AjaxRequestTarget target) {
                        addItemPerformed(itemList, target);
                        sortItems();
                        isPopoverOpen = false;
                        String message;
                        if (itemList.size() == 1) {
                            message = getString("SelectableItemListPopoverPanel.added.single");
                        } else if (itemList.size() > 1) {
                            message = getString("SelectableItemListPopoverPanel.added.plural", itemList.size());
                        } else {
                            message = getString("SelectableItemListPopoverPanel.closed");
                        }
                        addPopoverStatusMessage(target, propertiesStatus.getMarkupId(), message, 200);
                    }

                    @Override
                    protected Component getPopoverReferenceComponent() {
                        return getMoreButtonComponent();
                    }

                    @Override
                    protected String getItemName(FilterableSearchItemWrapper<?> item) {
                        return item.getName().getObject();
                    }

                    @Override
                    protected String getItemHelp(FilterableSearchItemWrapper<?> item) {
                        return item.getHelp().getObject();
                    }

                    @Override
                    protected IModel<String> getPopoverTitleModel() {
                        return createStringResource("SearchPanel.properties");
                    }
                    @Override
                    protected void closeMorePopoverPerformed(AjaxRequestTarget target) {
                        togglePopover(target);
                        addPopoverStatusMessage(target, propertiesStatus.getMarkupId(), getString("SelectableItemListPopoverPanel.closed"), 50);
                        isPopoverOpen = false;
                    }
                };
        popoverPanel.setOutputMarkupId(true);
        add(popoverPanel);

        AjaxLink<Void> more = new AjaxLink<Void>(ID_MORE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                popoverPanel.togglePopover(target);
                String message;
                if (isPopoverOpen) {
                    message = getString("SelectableItemListPopoverPanel.closed");
                } else {
                    message = getString("SelectableItemListPopoverPanel.opened");
                }
                addPopoverStatusMessage(target, propertiesStatus.getMarkupId(), message, 50);
                isPopoverOpen = !isPopoverOpen;
            }
        };
        more.add(AttributeAppender.append("aria-expanded", "false"));
        more.add(AttributeAppender.append("aria-controls", popoverPanel.getPopoverMarkupId()));
        more.add(new VisibleBehaviour(this::moreDropDownButtonVisible));
        more.setOutputMarkupId(true);
        add(more);
    }

    public void sortItems() {
        getModelObject().getItemsList().sort(new SearchItemWrapperComparator<>());
    }

    /**
     * Calculates Current Biggest Display Order of visible Search panel items.
     * It updates displayOrder of some search items before calculation to ensure that all visible items have displayOrder set and
     * all not visible items have displayOrder null.
     * Setting of displayOrder only for all visible items is crucial to preserve their order after adding of new items by More button.
     *
     * @return the biggest displayOrder of item in search box
     */
    private int calculateCurrentBiggestDisplayOrder() {
        return getModelObject().getItemsList().stream()
                .peek(this::updateDisplayOrderIfNeeded)
                .map(FilterableSearchItemWrapper::getDisplayOrder)
                .filter(Objects::nonNull)
                .reduce(0, Integer::max);
    }

    /**
     * Updates displayOrder of search item if needed to ensure that visible item have displayOrder set and
     * not visible item have displayOrder null.
     *
     * @param item for update of displayOrder if needed
     */
    private void updateDisplayOrderIfNeeded(FilterableSearchItemWrapper<?> item) {
        // add displayOrder for default search items which are visible, so they are properly sorted before items added by More button
        // this case occurs when only default search items are in search box
        if (item.isVisible() && item.getDisplayOrder() == null) {
            item.setDisplayOrder(++currentBiggestDisplayOrder);
        }
        // it is needed to remove displayOrder for not visible search items, so they are properly sorted in More button alphabetically
        // this case occurs when you switch between Saved filters
        if (!item.isVisible() && item.getDisplayOrder() != null) {
            item.setDisplayOrder(null);
        }
    }

    private void addPopoverStatusMessage(AjaxRequestTarget target, String markupId, String message, int refreshInMillis) {
        target.appendJavaScript(String.format("MidPointTheme.updateStatusMessage('%s', '%s', %d);",
                markupId, message, refreshInMillis));
    }

    private Component getMoreButtonComponent() {
        return get(ID_MORE);
    }

    public <SIP extends AbstractSearchItemPanel<S>, S extends FilterableSearchItemWrapper> SIP createSearchItemPanel(String panelId, IModel<S> searchItemModel) {
        Class<?> panelClass = searchItemModel.getObject().getSearchItemPanelClass();
        Constructor<?> constructor;
        try {
            constructor = panelClass.getConstructor(String.class, IModel.class);
            return (SIP) constructor.newInstance(panelId, searchItemModel);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {
            throw new SystemException("Cannot instantiate " + panelClass, e);
        }
    }

    private void addItemPerformed(List<FilterableSearchItemWrapper<?>> itemList, AjaxRequestTarget target) {
        if (itemList == null) {
            itemList = morePopupModel.getObject();
        }
        itemList.forEach(item -> {
            if (item.isSelected()) {
                item.setVisible(true);
                item.setSelected(false);
                // in corner case when currentBiggestDisplayOrder reaches Integer.MAX_VALUE,
                // the variable will contains the smallest Integer values,
                // so added items will be added from the beginning of the search box items
                item.setDisplayOrder(++currentBiggestDisplayOrder);
            }
        });
        target.add(BasicSearchPanel.this);
        target.add(getParent());
    }

    private boolean moreDropDownButtonVisible() {
        return morePopupPropertyListIsNotEmpty() && getModelObject().isAllowToConfigureSearchItems();
    }

    private boolean morePopupPropertyListIsNotEmpty() {
        return CollectionUtils.isNotEmpty(morePopupModel.getObject()) &&
                morePopupModel.getObject().stream().anyMatch(property -> !property.isVisible());
    }
}
