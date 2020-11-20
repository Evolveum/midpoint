/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.util.QNameUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceAcquisitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

public class ProvenanceMetadataPanel extends PrismContainerPanel<ValueMetadataType, ValueMetadataWrapperImpl> {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProvenanceMetadataPanel.class);
    private static final String ID_YIELD_CONTAINER = "yieldContainer";
    private static final String ID_YIELD_HEADER = "yieldHeader";
    private static final String ID_YIELD = "yield";
    private static final String ID_PROVENANCE = "provenance";
    private static final String ID_ACQUISITION_HEADER = "acquisitionHeader";
    private static final String ID_ACQUISITIONS = "acquisitions";
    private static final String ID_ACQUISITION = "acquisition";
    private static final String ID_SHOW_MORE = "showMore";
    private static final String ID_DEFAULT_PANEL = "defaultPanel";
    private static final String ID_PROVENANCE_DISPLAY = "provenanceDisplayName";
    private static final String ID_VALUE_METADATA_DETAILS = "valueMetadataDetails";


    public ProvenanceMetadataPanel(String id, IModel<ValueMetadataWrapperImpl> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected boolean getHeaderVisibility() {
        return false;
    }

    @Override
    protected Component createValuesPanel() {
        WebMarkupContainer container = new WebMarkupContainer(ID_YIELD_CONTAINER);
        container.setOutputMarkupId(true);
        container.setOutputMarkupPlaceholderTag(true);
        add(container);

        container.add(createDisplayNamePanel());
        container.add(createYieldPanel());

        return container;
    }

    private DisplayNamePanel<Containerable> createDisplayNamePanel() {
        DisplayNamePanel<Containerable> displayNamePanel = new DisplayNamePanel<Containerable>(ID_PROVENANCE_DISPLAY, new ItemRealValueModel<>(new PropertyModel<>(getModel(), "values.0"))) {

            @Override
            protected String createImageModel() {
                return "fa fa-tag";
            }

            @Override
            protected IModel<String> createHeaderModel() {
                return getHeaderModel();
            }

            @Override
            protected IModel<List<String>> getDescriptionLabelsModel() {
                return new ReadOnlyModel<>(() -> Collections.singletonList(getDescriptionLabel()));
            }

        };
        displayNamePanel.setOutputMarkupId(true);
        return displayNamePanel;
    }

    private ListView<PrismContainerValueWrapper<ValueMetadataType>> createYieldPanel() {
        ListView<PrismContainerValueWrapper<ValueMetadataType>> yield = new ListView<PrismContainerValueWrapper<ValueMetadataType>>(ID_YIELD, new PropertyModel<>(getModel(), "values")) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ValueMetadataType>> valueMetadataListItem) {
                createValuePanelWithProvenanceHeader(valueMetadataListItem);
            }
        };
        yield.setOutputMarkupId(true);
        return yield;
    }

    private void createValuePanelWithProvenanceHeader(ListItem<PrismContainerValueWrapper<ValueMetadataType>> valueMetadataListItem) {
        IModel<PrismContainerWrapper<ProvenanceMetadataType>> provenanceWrapperModel = PrismContainerWrapperModel.fromContainerValueWrapper(valueMetadataListItem.getModel(), ValueMetadataType.F_PROVENANCE);
        ListView<PrismContainerValueWrapper<ProvenanceMetadataType>> provenanceValues = new ListView<PrismContainerValueWrapper<ProvenanceMetadataType>>(ID_PROVENANCE, new PropertyModel<>(provenanceWrapperModel, "values")) {
            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ProvenanceMetadataType>> provenanceListItem) {
                createMetadataDetailsPanel(provenanceListItem, valueMetadataListItem);
            }

        };
        valueMetadataListItem.add(provenanceValues);
    }

    private void createMetadataDetailsPanel(ListItem<PrismContainerValueWrapper<ProvenanceMetadataType>> provenanceListItem, ListItem<PrismContainerValueWrapper<ValueMetadataType>> valueMetadataListItem) {
        WebMarkupContainer panel = createAcquisitionPanel(PrismContainerWrapperModel.fromContainerValueWrapper(provenanceListItem.getModel(), ProvenanceMetadataType.F_ACQUISITION));
        provenanceListItem.add(panel);

        ToggleIconButton<Void> showMore = createShowMoreButton(provenanceListItem.getModel());
        provenanceListItem.add(showMore);

        IModel<PrismContainerWrapper<Containerable>> detailsModel = createDetailsModel(valueMetadataListItem.getModel());
        Label label = new Label(ID_YIELD_HEADER, createDetailsDescriptionModel(detailsModel));
        provenanceListItem.add(label);
        label.add(new VisibleBehaviour(() -> provenanceListItem.getModelObject().isShowEmpty()));

        MetadataContainerPanel<Containerable> defaultPanel = createDefaultPanel(detailsModel, provenanceListItem.getModel());
        defaultPanel.add(new VisibleBehaviour(() -> !getModelObject().isShowMetadataDetails() && provenanceListItem.getModelObject().isShowEmpty()));
        provenanceListItem.add(defaultPanel);

        ItemPanelSettings settings = getSettings().copy();
        settings.setVisibilityHandler(w -> createItemVisibilityBehavior(w));
        ValueMetadataPanel<ValueMetadataType, PrismContainerValueWrapper<ValueMetadataType>> valueMetadataPanel =
                new ValueMetadataPanel<>(ID_VALUE_METADATA_DETAILS, valueMetadataListItem.getModel(), settings);
        valueMetadataPanel.add(new VisibleBehaviour(() -> getModelObject().isShowMetadataDetails() && provenanceListItem.getModelObject().isShowEmpty()));
        provenanceListItem.add(valueMetadataPanel);

    }

    private ToggleIconButton<Void> createShowMoreButton(IModel<PrismContainerValueWrapper<ProvenanceMetadataType>> provenanceModel) {
        ToggleIconButton<Void> showMore = new ToggleIconButton<Void>(ID_SHOW_MORE,
                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {

            @Override
            public boolean isOn() {
                return provenanceModel.getObject().isShowEmpty();
            }

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                PrismContainerValueWrapper<ProvenanceMetadataType> modelObject = provenanceModel.getObject();
                modelObject.setShowEmpty(!modelObject.isShowEmpty());
                ajaxRequestTarget.add(ProvenanceMetadataPanel.this);
            }
        };

        showMore.setEnabled(true);
        showMore.setOutputMarkupId(true);
        showMore.setOutputMarkupPlaceholderTag(true);
        return showMore;
    }

    private MetadataContainerPanel<Containerable> createDefaultPanel(IModel<PrismContainerWrapper<Containerable>> detailsModel, IModel<PrismContainerValueWrapper<ProvenanceMetadataType>> provenanceModel) {
        ItemPanelSettings settings = getSettings().copy();
        settings.setVisibilityHandler(w -> ItemVisibility.AUTO);
        MetadataContainerPanel<Containerable> defaultPanel = new MetadataContainerPanel<>(ID_DEFAULT_PANEL, detailsModel, settings);
        defaultPanel.setOutputMarkupPlaceholderTag(true);
        defaultPanel.setOutputMarkupId(true);
//        defaultPanel.add(new VisibleBehaviour(() -> provenanceModel.getObject().isShowEmpty()));
        return defaultPanel;
    }

    private ItemVisibility createItemVisibilityBehavior(ItemWrapper<?, ?> wrapper) {
        if (getModelObject().isShowMetadataDetails()) {
            if (wrapper instanceof PrismContainerWrapper) {
                return ItemVisibility.HIDDEN;
            }
        }
        return ItemVisibility.AUTO;
    }

    private IModel<String> createDetailsDescriptionModel(IModel<PrismContainerWrapper<Containerable>> detailsModel) {
        return new ReadOnlyModel<>(() -> {
            PrismContainerWrapper<Containerable> details = detailsModel.getObject();
            if (details == null || details.isRuntimeSchema() || QNameUtil.match(ValueMetadataType.COMPLEX_TYPE, details.getTypeName())) {
                return "";
            }

            if (hasMoreThenOneItem(details)) {
                return getString(details.getTypeName().getLocalPart() + ".description");
            }

            return getString(details.getTypeName().getLocalPart() + ".description.single");
        });
    }

    private <PCW extends PrismContainerWrapper> IModel<PCW> createDetailsModel(IModel<PrismContainerValueWrapper<ValueMetadataType>> valueMetadataModel) {
        return new ReadOnlyModel<>(() -> {
            ValueMetadataWrapperImpl valueMetadataWrapper = getModelObject();
            if (valueMetadataWrapper.isShowMetadataDetails()) {
                return (PCW) valueMetadataWrapper;
            }
            return (PCW) valueMetadataModel.getObject().getSelectedChild();
        });
    }

    private IModel<String> getHeaderModel() {
        return createStringResource("${selectedChild.displayName}", getModel());
    }

    private String getDescriptionLabel() {
            if (getModelObject() == null) {
                return "";
            }

            if (getModelObject().isShowMetadataDetails()) {
                return getString(ValueMetadataType.COMPLEX_TYPE.getLocalPart() + ".displayType");
            }

            PrismContainerWrapper<? extends Containerable> child = getModelObject().getSelectedChild();
            //TODO only for provenance?
            if (!child.isRuntimeSchema()) {
                return getString(child.getTypeName().getLocalPart() + ".displayType");
            }
            return "";
    }

    private boolean hasMoreThenOneItem(PrismContainerWrapper<Containerable> child) {
        if (!child.isSingleValue()) {
            return true;
        }

        PrismContainerValueWrapper<Containerable> childValue;
        try {
            childValue = child.getValue();
        } catch (SchemaException e) {
            LOGGER.warn("Cannot get child value, returning default label.");
            return true;
        }

        if (childValue.getItems().size() == 1) {
            ItemWrapper childChild = childValue.getItems().get(0);
            if (!(childChild instanceof PrismContainerWrapper)) {
                return false;
            }
        }

        return childValue.getItems().size() > 1;

    }

    private WebMarkupContainer createAcquisitionPanel(IModel<PrismContainerWrapper<ProvenanceAcquisitionType>> listPropertyModel) {
        WebMarkupContainer container = new WebMarkupContainer(ID_ACQUISITION_HEADER);

        ListView<PrismContainerValueWrapper<ProvenanceAcquisitionType>> acquisition =
                new ListView<PrismContainerValueWrapper<ProvenanceAcquisitionType>>(ID_ACQUISITIONS, new PropertyModel<>(listPropertyModel, "values")) {

            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<ProvenanceAcquisitionType>> listItem) {
                ProvenanceAcquisitionHeaderPanel panel = new ProvenanceAcquisitionHeaderPanel(ID_ACQUISITION, new ItemRealValueModel<>(listItem.getModel()));
                panel.setOutputMarkupId(true);
                listItem.add(panel);
            }
        };
        container.add(acquisition);
        return container;

    }

}
