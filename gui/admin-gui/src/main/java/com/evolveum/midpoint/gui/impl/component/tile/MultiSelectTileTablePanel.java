package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;
import java.util.List;

public abstract class MultiSelectTileTablePanel<E extends Serializable, O extends ObjectType> extends SingleSelectTileTablePanel<O> {

    protected static final String ID_SELECTED_ITEMS_CONTAINER = "selectedItemsContainer";
    private static final String ID_SELECTED_ITEM_CONTAINER = "selectedItemContainer";
    private static final String ID_SELECTED_ITEM = "selectedItem";
    private static final String ID_DESELECT_BUTTON = "deselectButton";

    public MultiSelectTileTablePanel(
            String id,
            UserProfileStorage.TableId tableId) {
        this(id, Model.of(ViewToggle.TILE), tableId);
    }

    public MultiSelectTileTablePanel(
            String id,
            IModel<ViewToggle> viewToggle,
            UserProfileStorage.TableId tableId) {
        super(id, viewToggle, tableId);
    }

    @Override
    protected Fragment createHeaderFragment(String id) {
        Fragment headerFragment =  super.createHeaderFragment(id);

        headerFragment.add(AttributeAppender.replace("class", ""));


        WebMarkupContainer selectedItemsContainer = new WebMarkupContainer(ID_SELECTED_ITEMS_CONTAINER);
        selectedItemsContainer.setOutputMarkupId(true);
        selectedItemsContainer.add(new VisibleBehaviour(() -> isSelectedItemsPanelVisible()));
        headerFragment.add(selectedItemsContainer);

        ListView<E> selectedContainer = new ListView<>(
                ID_SELECTED_ITEM_CONTAINER,
                getSelectedItemsModel()) {

            @Override
            protected void populateItem(ListItem<E> item) {
                E entry = item.getModelObject();

                item.add(new Label(ID_SELECTED_ITEM, getItemLabelModel(entry)));
                AjaxButton deselectButton = new AjaxButton(ID_DESELECT_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deselectItem(entry);
                        refresh(target);
                    }
                };
                item.add(deselectButton);
            }
        };
        selectedContainer.setOutputMarkupId(true);
        selectedItemsContainer.add(selectedContainer);
        return headerFragment;
    }

    protected boolean isSelectedItemsPanelVisible() {
        return true;
    }

    @Override
    public void refresh(AjaxRequestTarget target) {
        super.refresh(target);
        target.add(getSelectedItemPanel());
    }

    protected Component getSelectedItemPanel() {
        return get(createComponentPath(ID_HEADER, ID_SELECTED_ITEMS_CONTAINER));
    }

    protected abstract void deselectItem(E entry);

    protected abstract IModel<String> getItemLabelModel(E entry);

    protected abstract IModel<List<E>> getSelectedItemsModel();

    @Override
    public SelectableBeanObjectDataProvider<O> getProvider() {
        return (SelectableBeanObjectDataProvider<O>) super.getProvider();
    }

    @Override
    protected Component createTile(String id, IModel<TemplateTile<SelectableBean<O>>> model) {

        return new SelectableFocusTilePanel<>(id, model) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                super.onClick(target);
                getModelObject().getValue().setSelected(getModelObject().isSelected());

                processSelectOrDeselectItem(getModelObject().getValue());
                target.add(getSelectedItemPanel());
            }
        };
    }

    void onSelectTableRow(IModel<SelectableBean<O>> model, AjaxRequestTarget target) {
        boolean oldState = model.getObject().isSelected();

        model.getObject().setSelected(!oldState);
        processSelectOrDeselectItem(model.getObject());
        if (model.getObject().isSelected()) {
            ((SelectableBeanDataProvider) getProvider()).getSelected().add(model.getObject().getValue());
        }

        refresh(target);
    }

    protected void processSelectOrDeselectItem(SelectableBean<O> value) {
    }
}
