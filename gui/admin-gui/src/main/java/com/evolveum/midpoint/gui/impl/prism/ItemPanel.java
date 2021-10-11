/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumnPanel;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author katka
 *
 */
public abstract class ItemPanel<VW extends PrismValueWrapper, IW extends ItemWrapper> extends BasePanel<IW> implements RefreshableTabPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ItemPanel.class);

    private static final String DOT_CLASS = ItemPanel.class.getName() + ".";
    private static final String OPERATION_CREATE_NEW_VALUE = DOT_CLASS + "createNewValue";

    private static final String ID_HEADER = "header";
    private static final String ID_VALUES = "values";

    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_BUTTON_CONTAINER = "buttonContainer";

    private ItemPanelSettings itemPanelSettings;


    public ItemPanel(String id, IModel<IW> model, ItemPanelSettings itemPanelSettings) {
        super(id, model);
        this.itemPanelSettings = itemPanelSettings;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {

        //ugly hack TODO FIME - prism context is lost during srialization/deserialization.. find better way how to do it.
        if (getModelObject() != null) {
            getModelObject().revive(getPrismContext());
        }

        Panel headerPanel = createHeaderPanel();
        headerPanel.add(new VisibleBehaviour(() -> getHeaderVisibility()));
        add(headerPanel);

        ListView<VW> valuesPanel = createValuesPanel();
        add(valuesPanel);

    }

    protected boolean getHeaderVisibility() {
        if (!isHeaderVisible()) {
            return false;
        }
        return getParent().findParent(AbstractItemWrapperColumnPanel.class) == null;
    }

    protected abstract Panel createHeaderPanel();

    protected ListView<VW> createValuesPanel() {

        ListView<VW> values = new ListView<VW>(ID_VALUES, new PropertyModel<>(getModel(), "values")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<VW> item) {
                GuiComponentFactory componentFactory = getPageBase().getRegistry()
                        .findValuePanelFactory(ItemPanel.this.getModelObject());


                Component panel = createValuePanel(item, componentFactory, getVisibilityHandler(), getEditabilityHandler());
                createButtons(item);
            }

        };

        return values;
    }

    @Override
    public boolean isEnabled() {
        return !getModelObject().isReadOnly();
    }

    // VALUE REGION

     protected abstract Component createValuePanel(ListItem<VW> item, GuiComponentFactory componentFactory,
             ItemVisibilityHandler visibilityHandler, ItemEditabilityHandler editabilityHandler);

     protected void createButtons(ListItem<VW> item) {
         WebMarkupContainer buttonContainer = new WebMarkupContainer(ID_BUTTON_CONTAINER);
            buttonContainer.add(new AttributeModifier("class", getButtonsCssClass()));

            item.add(buttonContainer);
            // buttons
            AjaxLink<Void> addButton = new AjaxLink<Void>(ID_ADD_BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    addValue(target);
                }
            };
            addButton.add(new VisibleBehaviour(() -> isAddButtonVisible()));
            buttonContainer.add(addButton);

            AjaxLink<Void> removeButton = new AjaxLink<Void>(ID_REMOVE_BUTTON) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    try {
                        removeValue(item.getModelObject(), target);
                    } catch (SchemaException e) {
                        LOGGER.error("Cannot remove value: {}", item.getModelObject());
                        getSession().error("Cannot remove value "+ item.getModelObject());
                        target.add(getPageBase().getFeedbackPanel());
                        target.add(ItemPanel.this);
                    }
                }
            };
            removeButton.add(new VisibleBehaviour(() -> isRemoveButtonVisible()));
            buttonContainer.add(removeButton);


            item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));

            item.add(new VisibleBehaviour(() -> isVisibleValue(item.getModel())));
     }


     protected String getButtonsCssClass() {
            return"col-xs-2";
        }

      protected IModel<String> createStyleClassModel(final IModel<VW> value) {
            return new IModel<String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject() {
                    if (getIndexOfValue(value.getObject()) > 0) {
                        return getItemCssClass();
                    }

                    return null;
                }
            };
        }

      private int getIndexOfValue(VW value) {
            IW property = (IW) value.getParent();
            List<VW> values = property.getValues();
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i).equals(value)) {
                    return i;
                }
            }

            return -1;
        }

      protected String getItemCssClass() {
            return " col-sm-offset-0 col-md-offset-4 col-lg-offset-2 prism-value ";
        }

     protected void addValue(AjaxRequestTarget target) {
            IW propertyWrapper = getModel().getObject();
            PrismPropertyValue<?> newValue = getPrismContext().itemFactory().createPropertyValue();

         WebPrismUtil.createNewValueWrapper(propertyWrapper, createNewValue(propertyWrapper), getPageBase(), target);

            target.add(ItemPanel.this);
        }

        protected abstract <PV extends PrismValue> PV createNewValue(IW itemWrapper);

        protected void removeValue(VW valueToRemove, AjaxRequestTarget target) throws SchemaException {
            LOGGER.debug("Removing value of {}", valueToRemove);
            List<VW> values = getModelObject().getValues();

            switch (valueToRemove.getStatus()) {
                case ADDED:
                    values.remove(valueToRemove);
                    getModelObject().getItem().remove(valueToRemove.getOldValue());
                    getModelObject().getItem().remove(valueToRemove.getNewValue());
                    break;
                case DELETED:
                    valueToRemove.setStatus(ValueStatus.NOT_CHANGED);
                    getModelObject().getItem().add(valueToRemove.getNewValue());
                    break;
                case NOT_CHANGED:
                    getModelObject().getItem().remove(valueToRemove.getNewValue());
                    valueToRemove.setStatus(ValueStatus.DELETED);
                    break;
            }

            int count = countUsableValues(values);

            if (count == 0 && !hasEmptyPlaceholder(values)) {
                addValue(target);
            }

            target.add(ItemPanel.this);
        }

        private int countUsableValues(List<VW> values) {
            int count = 0;
            for (VW value : values) {
                if (ValueStatus.DELETED.equals(value.getStatus())) {
                    continue;
                }
                if (ValueStatus.ADDED.equals(value.getStatus())) {
                    continue;
                }
                count++;
            }
            return count;
        }

        private boolean hasEmptyPlaceholder(List<VW> values) {
            for (VW value : values) {
                if (ValueStatus.ADDED.equals(value.getStatus()) ) {//&& !value.hasValueChanged()) {
                    return true;
                }
            }

            return false;
        }

        private boolean isAddButtonVisible() {
            return getModelObject().isMultiValue();
        }



        protected boolean isRemoveButtonVisible() {
            return !getModelObject().isReadOnly();

        }


        private boolean isVisibleValue(IModel<VW> model) {
            VW value = model.getObject();
            return !ValueStatus.DELETED.equals(value.getStatus());
        }

        public ItemVisibilityHandler getVisibilityHandler() {
            if (itemPanelSettings == null) {
                return null;
            }
            return itemPanelSettings.getVisibilityHandler();
        }

        public ItemEditabilityHandler getEditabilityHandler() {
            if (itemPanelSettings == null) {
                return null;
            }
            return itemPanelSettings.getEditabilityHandler();
        }

        public ItemMandatoryHandler getMandatoryHandler() {
            if (itemPanelSettings == null) {
                return null;
            }
            return itemPanelSettings.getMandatoryHandler();
        }

        protected boolean isShowOnTopLevel() {
             if (itemPanelSettings == null) {
                 return false;
            }
             return itemPanelSettings.isShowOnTopLevel();
        }


        protected boolean isHeaderVisible() {
             if (itemPanelSettings == null) {
                 return true;
            }

             return itemPanelSettings.isHeaderVisible();
        }

    public ItemPanelSettings getSettings() {
         return itemPanelSettings;
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(this);
    }
}
