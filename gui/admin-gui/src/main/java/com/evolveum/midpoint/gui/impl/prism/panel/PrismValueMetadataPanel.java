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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.component.ContainersPopupDto;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
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

public class PrismValueMetadataPanel extends BasePanel<ValueMetadataWrapperImpl> {

    private static final transient Trace LOGGER = TraceManager.getTrace(PrismValueMetadataPanel.class);

    private static final String ID_PROVENANCE_METADATA = "provenanceMetadata";
    private static final String ID_METADATA = "metadata";
    private static final String ID_METADATA_NAVIGATION = "metadataNav";
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
        createMetadataNavigationPanel();

        ProvenanceMetadataPanel provenanceMetadataPanel =
                new ProvenanceMetadataPanel(ID_PROVENANCE_METADATA, getModel(),createItemPanelSettings());
        provenanceMetadataPanel.add(new VisibleBehaviour(this::shouldShowProvenanceMetadataDetails));
        provenanceMetadataPanel.setOutputMarkupId(true);
        add(provenanceMetadataPanel);

        MetadataContainerPanel<Containerable> valueMetadataPanel =
                new MetadataContainerPanel<>(ID_METADATA, createMetadataNoProvenanceModel(),createItemPanelSettings());
        valueMetadataPanel.add(new VisibleBehaviour(this::shouldShowMetadataDetails));
        valueMetadataPanel.setOutputMarkupId(true);
        add(valueMetadataPanel);
    }

    private ItemPanelSettings createItemPanelSettings() {
        return new ItemPanelSettingsBuilder()
                .editabilityHandler(wrapper -> true)
                .headerVisibility(false)
                .visibilityHandler(w -> w.isShowMetadataDetails() ? ItemVisibility.AUTO : ItemVisibility.HIDDEN)
                .build();
    }

    private IModel<PrismContainerWrapper<Containerable>> createMetadataNoProvenanceModel() {
        return new ReadOnlyModel<>(() -> getModelObject() != null ? getModelObject().getSelectedChild() : null);
    }

    private void createMetadataNavigationPanel() {
        WebMarkupContainer metadataNavigation = new WebMarkupContainer(ID_METADATA_NAVIGATION);
        add(metadataNavigation);
        metadataNavigation.setOutputMarkupId(true);

        ListView<ContainersPopupDto> metadataList = new ListView<ContainersPopupDto>(ID_METADATA_LIST, createMetadataListModel()) {

            @Override
            protected void populateItem(ListItem<ContainersPopupDto> listItem) {
                AjaxButton showMetadataDetails  = new AjaxButton(ID_METADATA_QNAME,
                        createStringResource(listItem.getModelObject().getItemName())) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        ContainersPopupDto ccontainerToShow = listItem.getModelObject();
                        ccontainerToShow.setSelected(true);
                        setContainersToShow(ccontainerToShow, ajaxRequestTarget);
                    }
                };
                showMetadataDetails.add(AttributeAppender.replace("class", createButtonClassModel(listItem)));
                showMetadataDetails.setOutputMarkupId(true);
                showMetadataDetails.setOutputMarkupPlaceholderTag(true);
                listItem.setOutputMarkupId(true);
                listItem.add(showMetadataDetails);
            }
        };
        metadataNavigation.add(metadataList);
        metadataList.setOutputMarkupId(true);
        metadataList.setOutputMarkupPlaceholderTag(true);
    }

    private boolean shouldShowMetadataDetails() {
        return isAnyMetadataSelected() && !containsProvenanceMetadata();
    }

    private boolean shouldShowProvenanceMetadataDetails() {
        return isAnyMetadataSelected() && containsProvenanceMetadata();
    }

    private boolean isAnyMetadataSelected() {
        for (PrismContainerValueWrapper<ValueMetadataType> value : getValueMetadata().getValues()) {
            for (PrismContainerWrapper<Containerable> metadataContainer : value.getContainers()) {
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

    private IModel<?> createButtonClassModel(ListItem<ContainersPopupDto> selectedContainer) {

        return new ReadOnlyModel<>(() -> {

            String primaryButton = getPrimaryButton(getValueMetadata(), selectedContainer.getModelObject());

            return primaryButton == null ? "metadata-tab" : primaryButton;
        });
    }

    private String getPrimaryButton(PrismContainerWrapper<ValueMetadataType> valueMetadata, ContainersPopupDto containersPopupDto) {
        for (PrismContainerValueWrapper<ValueMetadataType> value : valueMetadata.getValues()) {
            for (PrismContainerWrapper<Containerable> container : value.getContainers()) {
                if (!QNameUtil.match(containersPopupDto.getTypeName(), container.getTypeName())) {
                    continue;
                }

                if (container.isShowMetadataDetails()) {
                    return "metadata-tab metadata-tab-active";
                }
            }
        }

        return null;
    }

    private void setContainersToShow(ContainersPopupDto containersToShow, AjaxRequestTarget ajaxRequestTarget) {
        for (PrismContainerValueWrapper<ValueMetadataType> values : getValueMetadata().getValues()) {
            for (PrismContainerWrapper<Containerable> container : values.getContainers()) {
                if (QNameUtil.match(container.getTypeName(), containersToShow.getDef().getTypeName())) {
                    container.setShowMetadataDetails(true);
                } else {
                    container.setShowMetadataDetails(false);
                }
            }
        }

        ajaxRequestTarget.add(PrismValueMetadataPanel.this);

    }

    private ReadOnlyModel<List<ContainersPopupDto>> createMetadataListModel() {
        return new ReadOnlyModel<>(() -> {
            ValueMetadataWrapperImpl metadataWrapper = getValueMetadata();

            List<PrismContainerDefinition<Containerable>> childContainers;
            try {
                childContainers = metadataWrapper.getChildContainers();
            } catch (SchemaException e) {
                LOGGER.error("Cannot get child containers: {}", e.getMessage(), e);
                childContainers = Collections.EMPTY_LIST;
            }

            return childContainers.stream().map(c -> new ContainersPopupDto(false, c)).collect(Collectors.toList());

        });
    }

    private ValueMetadataWrapperImpl getValueMetadata() {
        return getModelObject();
    }
}
