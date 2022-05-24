/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.component.ContainersPopupDto;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvenanceMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

public class PrismValueMetadataPanel extends BasePanel<ValueMetadataWrapperImpl> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismValueMetadataPanel.class);

    private static final String ID_BUTTONS_CONTAINER="buttonsContainer";
    private static final String ID_BUTTONS="buttons";
    private static final String ID_BUTTON="button";
    private static final String ID_CARD="card";

    private static final String ID_PROVENANCE_METADATA = "provenanceMetadata";
    private static final String ID_METADATA = "metadata";
    private static final String ID_METADATA_LIST = "metadataList";
    private static final String ID_METADATA_QNAME = "metadataQName";

    public PrismValueMetadataPanel(String id, IModel<ValueMetadataWrapperImpl> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "metadata"));
        setOutputMarkupId(true);

        IModel<List<ContainersPopupDto>> list = createMetadataListModel();

        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsContainer.setOutputMarkupId(true);
        buttonsContainer.add(new VisibleBehaviour(() -> list.getObject().stream().filter(c -> c.isSelected()).count() == 0));
        add(buttonsContainer);

        ListView<ContainersPopupDto> buttons = new ListView<>(ID_BUTTONS, list) {
            @Override
            protected void populateItem(ListItem<ContainersPopupDto> item) {
                AjaxButton button  = new AjaxButton(ID_BUTTON, createStringResource(item.getModelObject().getItemName())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        item.getModelObject().setSelected(true);

                        setContainersToShow(item.getModelObject(), target);
                    }
                };
                button.add(AttributeAppender.append("class", createButtonClassModel(item.getModel())));
                item.add(button);
            }
        };
        buttonsContainer.add(buttons);

        WebMarkupContainer card = new WebMarkupContainer(ID_CARD);
        card.add(new VisibleBehaviour(() -> list.getObject().stream().filter(c -> c.isSelected()).count() > 0));
        add(card);

        createMetadataNavigationPanel(card, list);

        ProvenanceMetadataPanel provenanceMetadataPanel =
                new ProvenanceMetadataPanel(ID_PROVENANCE_METADATA, getModel(),createItemPanelSettings());
        provenanceMetadataPanel.add(new VisibleBehaviour(this::shouldShowProvenanceMetadataDetails));
        provenanceMetadataPanel.setOutputMarkupId(true);
        card.add(provenanceMetadataPanel);

        MetadataContainerPanel<? extends Containerable> valueMetadataPanel =
                new MetadataContainerPanel<>(ID_METADATA, createMetadataNoProvenanceModel(),createItemPanelSettings());
        valueMetadataPanel.add(new VisibleBehaviour(this::shouldShowMetadataDetails));
        valueMetadataPanel.setOutputMarkupId(true);
        card.add(valueMetadataPanel);
    }

    private ItemPanelSettings createItemPanelSettings() {
        return new ItemPanelSettingsBuilder()
                .editabilityHandler(wrapper -> true)
                .headerVisibility(false)
                .visibilityHandler(w -> createItemVisibilityBehavior(w))
                .build();
    }

    private ItemVisibility createItemVisibilityBehavior(ItemWrapper<?, ?> w) {
        if (getModelObject().isShowMetadataDetails()) {
            return w instanceof PrismContainerWrapper ? ItemVisibility.HIDDEN : ItemVisibility.AUTO;
        }
        return w.isShowMetadataDetails() ? ItemVisibility.AUTO : ItemVisibility.HIDDEN;
    }

    private IModel<PrismContainerWrapper<Containerable>> createMetadataNoProvenanceModel() {
        return () -> getModelObject() != null ? (PrismContainerWrapper<Containerable>) getModelObject().getSelectedChild() : null;
    }

    private void createMetadataNavigationPanel(WebMarkupContainer card, IModel<List<ContainersPopupDto>> listModel) {
        ListView<ContainersPopupDto> metadataList = new ListView<>(ID_METADATA_LIST, listModel) {

            @Override
            protected void populateItem(ListItem<ContainersPopupDto> listItem) {
                AjaxButton button  = createNavigationItem(ID_METADATA_QNAME, listItem.getModel());

                listItem.setOutputMarkupId(true);
                listItem.add(button);
            }
        };
        card.add(metadataList);
        metadataList.setOutputMarkupId(true);
    }

    private AjaxButton createNavigationItem(String id, IModel<ContainersPopupDto> model) {
        AjaxButton button  = new AjaxButton(id, createStringResource(model.getObject().getItemName())) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                ContainersPopupDto container = model.getObject();
                container.setSelected(true);
                setContainersToShow(container, ajaxRequestTarget);
            }
        };
        button.add(AttributeAppender.append("class", createButtonClassModel(model)));
        button.setOutputMarkupId(true);
        button.setOutputMarkupPlaceholderTag(true);

        return button;
    }

    private boolean shouldShowMetadataDetails() {
        return isAnyMetadataSelected() && !containsProvenanceMetadata();
    }

    private boolean shouldShowProvenanceMetadataDetails() {
        return isAnyMetadataSelected() && containsProvenanceMetadata();
    }

    private boolean isAnyMetadataSelected() {
        if (getModelObject().isShowMetadataDetails()) {
            return true;
        }
        for (PrismContainerValueWrapper<ValueMetadataType> value : getValueMetadata().getValues()) {
            for (PrismContainerWrapper<? extends Containerable> metadataContainer : value.getContainers()) {
                if (metadataContainer.isShowMetadataDetails()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean containsProvenanceMetadata() {
        //TODO fix findContainer in prismContainerWrppaer when no value id specified
        for (PrismContainerValueWrapper<ValueMetadataType> value : getValueMetadata().getValues()) {
            PrismContainerWrapper<ProvenanceMetadataType> provenanceWrapper = null;
            try {
                provenanceWrapper = value.findContainer(ValueMetadataType.F_PROVENANCE);
            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot find provenance metadata wrapper", e);
            }
            if (provenanceWrapper != null && !provenanceWrapper.getValues().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private IModel<?> createButtonClassModel(IModel<ContainersPopupDto> model) {
        return () -> {
            String primaryButton = getPrimaryButton(getValueMetadata(), model.getObject());

            return primaryButton == null ? "" : primaryButton;
        };
    }

    private String getPrimaryButton(PrismContainerWrapper<ValueMetadataType> valueMetadata, ContainersPopupDto containersPopupDto) {
        if (QNameUtil.match(ValueMetadataType.COMPLEX_TYPE, containersPopupDto.getTypeName()) && valueMetadata.isShowMetadataDetails()) {
            return "active";
        }

        for (PrismContainerValueWrapper<ValueMetadataType> value : valueMetadata.getValues()) {
            for (PrismContainerWrapper<? extends Containerable> container : value.getContainers()) {
                if (!QNameUtil.match(containersPopupDto.getTypeName(), container.getTypeName())) {
                    continue;
                }

                if (container.isShowMetadataDetails()) {
                    return "active";
                }
            }
        }

        return null;
    }

    private void setContainersToShow(ContainersPopupDto containersToShow, AjaxRequestTarget ajaxRequestTarget) {
        if (QNameUtil.match(ValueMetadataType.COMPLEX_TYPE, containersToShow.getDef().getTypeName())) {
            getValueMetadata().setShowMetadataDetails(true);
        } else {
            getValueMetadata().setShowMetadataDetails(false);
        }

        for (PrismContainerValueWrapper<ValueMetadataType> values : getValueMetadata().getValues()) {
            for (PrismContainerWrapper<? extends Containerable> container : values.getContainers()) {
                if (QNameUtil.match(container.getTypeName(), containersToShow.getDef().getTypeName())) {
                    container.setShowMetadataDetails(true);
                } else {
                    container.setShowMetadataDetails(false);
                }
            }
        }

        ajaxRequestTarget.add(PrismValueMetadataPanel.this);
    }

    private IModel<List<ContainersPopupDto>> createMetadataListModel() {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<ContainersPopupDto> load() {
                ValueMetadataWrapperImpl metadataWrapper = getValueMetadata();

                List<PrismContainerDefinition<? extends Containerable>> childContainers;
                try {
                    childContainers = metadataWrapper != null ? metadataWrapper.getChildContainers() : Collections.emptyList();
                } catch (SchemaException e) {
                    LOGGER.error("Cannot get child containers: {}", e.getMessage(), e);
                    childContainers = Collections.emptyList();
                }

                List<ContainersPopupDto> navigation = childContainers.stream().map(c -> new ContainersPopupDto(false, c)).collect(Collectors.toList());

                List<? extends ItemDefinition> childNonContainers = metadataWrapper != null ? metadataWrapper.getChildNonContainers()
                        : Collections.emptyList();
                if (!childNonContainers.isEmpty()) {
                    navigation.add(new ContainersPopupDto(false, metadataWrapper));
                }

                return navigation;
            }
        };
    }

    private ValueMetadataWrapperImpl getValueMetadata() {
        return getModelObject();
    }
}
