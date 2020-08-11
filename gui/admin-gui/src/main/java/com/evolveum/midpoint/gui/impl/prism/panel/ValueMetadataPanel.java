/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.awt.*;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.form.Form;

import com.evolveum.midpoint.web.component.prism.ItemVisibility;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismContainerPanelContext;
import com.evolveum.midpoint.gui.impl.prism.panel.component.ListContainersPopup;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author katka
 *
 */
public class ValueMetadataPanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>> extends PrismContainerValuePanel<C, CVW> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_HEADER = "header";

    public ValueMetadataPanel(String id, IModel<CVW> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void addToHeader(WebMarkupContainer header) {
        LoadableDetachableModel<String> headerLabelModel = getLabelModel();
        Label labelComponent = new Label(ID_LABEL, headerLabelModel);
        labelComponent.setOutputMarkupId(true);
        labelComponent.setOutputMarkupPlaceholderTag(true);
        labelComponent.add(new VisibleBehaviour(() -> getModelObject() != null && getModelObject().getParent() != null && getModelObject().getParent().isMultiValue()));
        header.add(labelComponent);
    }

//    @Override
//    protected Component createDefaultPanel(String id) {
//
//        if (getModelObject() == null) {
//            return new WebMarkupContainer(id);
//        }
//
//        ListView<PrismContainerWrapper> containers = new ListView<PrismContainerWrapper>(id, new PropertyModel<>(getModel(), "containers")) {
//
//            @Override
//            protected void populateItem(ListItem<PrismContainerWrapper> listItem) {
//                ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
//                listItem.add(new MetadataContainerPanel(ID_CONTAINER, listItem.getModel(), settings));
//            }
//        };
//
//        add(containers);
//
//        return containers;
//    }\

    @Override
    protected Component createDefaultPanel(String id) {
        MetadataContainerValuePanel<C, CVW> panel = new MetadataContainerValuePanel<>(id, getModel(), getSettings());
        panel.setOutputMarkupId(true);
        return panel;
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismContainerWrapper<C> itemWrapper) {
        return null;
    }

    @Override
    protected void removeValue(CVW valueToRemove, AjaxRequestTarget target) throws SchemaException {

    }

//    private List<ITab> createTabs() {
//        List<ITab> tabs = new ArrayList<>();
//
//        for (PrismContainerWrapper w : getModelObject().getContainers()) {
//            tabs.add(new PanelTab(createStringResource(w.getDisplayName())) {
//                @Override
//                public WebMarkupContainer createPanel(String panelId) {
//                    ItemPanelSettings settings = getSettings() != null ? getSettings().copy() : null;
//                    settings.setHeaderVisible(false);
//                    return new MetadataContainerPanel(panelId, Model.of(w), settings);
//                }
//            });
//        }
//
//        if (CollectionUtils.isNotEmpty(getModelObject().getNonContainers())) {
//            tabs.add(new PanelTab(createStringResource(getModelObject().getDisplayName())) {
//                @Override
//                public WebMarkupContainer createPanel(String panelId) {
//                    ItemPanelSettings s = getSettings() != null ? getSettings().copy() : null;
//                    s.setVisibilityHandler(wrapper -> {
//                        if (wrapper instanceof PrismContainerWrapper) {
//                            return ItemVisibility.HIDDEN;
//                        }
//                        return ItemVisibility.AUTO;
//                    });
//                    return new PrismContainerValuePanel(panelId, getModel(), s) {
//
//                    };
//                }
//            });
//        }
//        return tabs;
//    }

    @Override
    protected void createMetadataPanel(Form form) {

    }

    @Override
    protected boolean isRemoveButtonVisible() {
        return false;
    }
}
