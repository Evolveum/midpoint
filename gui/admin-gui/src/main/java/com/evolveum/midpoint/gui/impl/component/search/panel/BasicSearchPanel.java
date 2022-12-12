package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.button.SelectableItemListPopoverPanel;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.OidSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.SearchConfigurationWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractSearchItemWrapper;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

public class BasicSearchPanel<C extends Containerable> extends BasePanel<SearchConfigurationWrapper<C>> {

    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_MORE = "more";
    private static final String ID_MORE_PROPERTIES_POPOVER = "morePropertiesPopover";
    private LoadableDetachableModel<List<AbstractSearchItemWrapper>> basicSearchItemsModel;
    private LoadableDetachableModel<List<AbstractSearchItemWrapper>> morePopupModel;

    public BasicSearchPanel(String id, IModel<SearchConfigurationWrapper<C>> model) {
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
            protected List<AbstractSearchItemWrapper> load() {
                return getModelObject().getItemsList()
                        .stream().filter(item
                                -> !(item instanceof OidSearchItemWrapper))
                        .collect(Collectors.toList());
            }
        };
    }

    private void initMorePopupModel() {
        morePopupModel = new LoadableDetachableModel<List<AbstractSearchItemWrapper>>() {
            @Override
            protected List<AbstractSearchItemWrapper> load() {
                return getModelObject().getItemsList();
            }
        };
    }

    public void displayedSearchItemsModelReset() {
        basicSearchItemsModel.detach();
    }

    private void initLayout() {

        ListView<AbstractSearchItemWrapper> items = new ListView<AbstractSearchItemWrapper>(ID_ITEMS, basicSearchItemsModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<AbstractSearchItemWrapper> item) {
                AbstractSearchItemPanel searchItemPanel = createSearchItemPanel(ID_ITEM, item.getModel());
                searchItemPanel.add(new VisibleBehaviour(() -> item.getModelObject().isVisible()));
                item.add(searchItemPanel);
            }
        };
//        items.add(createVisibleBehaviour(SearchBoxModeType.BASIC));
        add(items);

        SelectableItemListPopoverPanel<AbstractSearchItemWrapper> popoverPanel =
                new SelectableItemListPopoverPanel<AbstractSearchItemWrapper>(ID_MORE_PROPERTIES_POPOVER, morePopupModel) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void addItemsPerformed(List<AbstractSearchItemWrapper> itemList, AjaxRequestTarget target) {
                        addItemPerformed(itemList, target);
                    }

                    @Override
                    protected Component getPopoverReferenceComponent() {
                        return getMoreButtonComponent();
                    }

                    @Override
                    protected String getItemName(AbstractSearchItemWrapper item) {
                        return item.getName();
                    }

                    @Override
                    protected String getItemHelp(AbstractSearchItemWrapper item) {
                        return item.getHelp();
                    }

                    @Override
                    protected IModel<String> getPopoverTitleModel() {
                        return createStringResource("SearchPanel.properties");
                    }
                };
        add(popoverPanel);

        AjaxLink<Void> more = new AjaxLink<Void>(ID_MORE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                popoverPanel.togglePopover(target);
            }
        };
        more.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(morePopupModel.getObject())));
        more.setOutputMarkupId(true);
        add(more);
    }

    private Component getMoreButtonComponent() {
        return get(ID_MORE);
    }

    private void closeMorePopoverPerformed(AjaxRequestTarget target) {
        String popoverId = get(ID_MORE_PROPERTIES_POPOVER).getMarkupId();
        target.appendJavaScript("$('#" + popoverId + "').toggle();");
    }

    public <SIP extends AbstractSearchItemPanel<S>, S extends AbstractSearchItemWrapper> SIP createSearchItemPanel(String panelId, IModel<S> searchItemModel) {
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

    private void addItemPerformed(List<AbstractSearchItemWrapper> itemList, AjaxRequestTarget target) {
        if (itemList == null) {
            itemList = morePopupModel.getObject();
        }
        itemList.forEach(item -> {
            if (item.isSelected()) {
                item.setVisible(true);
                item.setSelected(false);
            }
        });
        target.add(BasicSearchPanel.this);
        target.add(getParent());
    }

}
