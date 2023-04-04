/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.form.multivalue;

import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by honchar
 */
public abstract class MultiValueObjectChoosePanel<R extends Referencable> extends BasePanel<List<R>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_EMPTY_MODEL_CONTAINER = "emptyModelContainer";
    private static final String ID_EMPTY_MODEL_INPUT = "emptyModelInput";
    private static final String ID_ADD_WHEN_EMPTY_BUTTON = "addWhenEmptyButton";
    private static final String ID_REMOVE_WHEN_EMPTY_BUTTON = "removeWhenEmptyButton";
    private static final String ID_MULTI_SHADOW_REF_VALUE = "multiShadowRefPanel";
    private static final String ID_REFERENCE_VALUE_INPUT = "referenceValueInput";
    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";
    private static final String ID_BUTTONS_WHEN_EMPTY_CONTAINER = "buttonsWhenEmptyContainer";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    private boolean emptyObjectPanelDisplay = false;

    public MultiValueObjectChoosePanel(String id, IModel<List<R>> referenceListModel) {
        super(id, referenceListModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        initEmptyModelInputPanel();
        initMultiValuesListPanel();
    }

    private void initEmptyModelInputPanel() {
        WebMarkupContainer emptyModelContainer = new WebMarkupContainer(ID_EMPTY_MODEL_CONTAINER);
        emptyModelContainer.setOutputMarkupId(true);
        emptyModelContainer.add(new VisibleBehaviour(() -> isEmptyModel() || emptyObjectPanelDisplay));
        add(emptyModelContainer);

        ValueChoosePanel<R> emptyModelPanel = new ValueChoosePanel<>(ID_EMPTY_MODEL_INPUT, () -> null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected ObjectFilter createCustomFilter() {
                return MultiValueObjectChoosePanel.this.createCustomFilter();
            }

            @Override
            public List<QName> getSupportedTypes() {
                return MultiValueObjectChoosePanel.this.getSupportedTypes();
            }

            @Override
            protected <O extends ObjectType> void choosePerformed(AjaxRequestTarget target, O object) {
                emptyObjectPanelDisplay = false;
                chooseObjectPerformed(target, object);
                target.add(MultiValueObjectChoosePanel.this);
            }
        };
        emptyModelPanel.setOutputMarkupId(true);
        emptyModelContainer.add(emptyModelPanel);

        WebMarkupContainer buttonsWhenEmptyContainer = new WebMarkupContainer(ID_BUTTONS_WHEN_EMPTY_CONTAINER);
        buttonsWhenEmptyContainer.setOutputMarkupId(true);
        emptyModelContainer.add(buttonsWhenEmptyContainer);

        AjaxButton addWhenEmptyButton = new AjaxButton(ID_ADD_WHEN_EMPTY_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                //will be disabled all the time, nothing to do here
            }
        };
        addWhenEmptyButton.setOutputMarkupId(true);
        addWhenEmptyButton.add(new EnableBehaviour(() -> false));
        buttonsWhenEmptyContainer.add(addWhenEmptyButton);

        AjaxButton removeWhenEmptyButton = new AjaxButton(ID_REMOVE_WHEN_EMPTY_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                //will be disabled all the time, nothing to do here
            }
        };
        removeWhenEmptyButton.setOutputMarkupId(true);
        removeWhenEmptyButton.add(new EnableBehaviour(() -> false));
        buttonsWhenEmptyContainer.add(removeWhenEmptyButton);
    }

    private void initMultiValuesListPanel() {
        ListView<R> multiValuesPanel = new ListView<>(ID_MULTI_SHADOW_REF_VALUE, getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<R> item) {
                ValueChoosePanel<R> valueChoosePanel = new ValueChoosePanel<>(ID_REFERENCE_VALUE_INPUT, item.getModel()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ObjectFilter createCustomFilter() {
                        return MultiValueObjectChoosePanel.this.createCustomFilter();
                    }

                    @Override
                    public List<QName> getSupportedTypes() {
                        return MultiValueObjectChoosePanel.this.getSupportedTypes();
                    }

                    @Override
                    protected <O extends ObjectType> void choosePerformedHook(AjaxRequestTarget target, O object) {
                        removeObjectPerformed(item.getModelObject());
                        chooseObjectPerformed(target, object);
                    }
                };
                valueChoosePanel.setOutputMarkupId(true);
                item.add(valueChoosePanel);

                WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
                buttonsContainer.setOutputMarkupId(true);
                item.add(buttonsContainer);

                AjaxButton addButton = new AjaxButton(ID_ADD_BUTTON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        emptyObjectPanelDisplay = true;
                        target.add(MultiValueObjectChoosePanel.this);
//                            MultiValueObjectChoosePanel.this.getModelObject().add(null);
                    }
                };
                addButton.setOutputMarkupId(true);
                addButton.add(new EnableBehaviour(() -> item.getModelObject() != null));
                buttonsContainer.add(addButton);

                AjaxLink<Void> removeButton = new AjaxLink<>(ID_REMOVE_BUTTON) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        removeObjectPerformed(item.getModelObject());
                        target.add(MultiValueObjectChoosePanel.this);

                    }
                };
                removeButton.setOutputMarkupId(true);
                removeButton.add(new EnableBehaviour(() -> item.getModelObject() != null));
                buttonsContainer.add(removeButton);
            }
        };
        multiValuesPanel.add(new VisibleBehaviour(() -> !isEmptyModel()));
        multiValuesPanel.setOutputMarkupId(true);
        add(multiValuesPanel);
    }

    protected void removeObjectPerformed(R object) {
        MultiValueObjectChoosePanel.this.getModelObject().remove(object);
    }

    private boolean isEmptyModel() {
        return CollectionUtils.isEmpty(getModelObject());
    }

    protected ObjectFilter createCustomFilter() {
        return null;
    }

    protected <O extends ObjectType> void chooseObjectPerformed(AjaxRequestTarget target, O object) {
    }

    protected abstract List<QName> getSupportedTypes();

    protected abstract <O extends ObjectType> R createReferencableObject(O object);
}
