/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism.show;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ValueMetadataWrapperImpl;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class VisualizationItemLinePanel extends BasePanel<VisualizationItemLineDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_NAME_CONTAINER = "nameContainer";
    private static final String ID_NAME = "name";
    private static final String ID_OLD_VALUE_CONTAINER = "oldValueContainer";
    private static final String ID_OLD_VALUE_IMAGE = "oldValueImage";
    private static final String ID_OLD_VALUE = "oldValue";
    private static final String ID_NEW_VALUE_CONTAINER = "newValueContainer";
    private static final String ID_NEW_VALUE_SUB_CONTAINER = "newValueSubContainer";
    private static final String ID_NEW_VALUE_IMAGE = "newValueImage";
    private static final String ID_NEW_VALUE = "newValue";
    private static final String ID_OLD_METADATA = "oldMetadata";
    private static final String ID_NEW_METADATA = "newMetadata";

    public VisualizationItemLinePanel(String id, IModel<VisualizationItemLineDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer nameCell = new WebMarkupContainer(ID_NAME_CONTAINER);
        nameCell.add(new AttributeModifier("rowspan", () -> getModelObject().getNumberOfLines()));

        Label label = new Label(ID_NAME, createStringResource("${name}", getModel()));
        nameCell.add(label);
        nameCell.add(new VisibleBehaviour(() -> getModelObject().isFirst()));
        add(nameCell);

        WebMarkupContainer oldValueCell = new WebMarkupContainer(ID_OLD_VALUE_CONTAINER);
        oldValueCell.add(new VisibleBehaviour(() -> getModelObject().oldValueIsUnknown() || getModelObject().isDelta()));

        Component sivp;
        if (getModelObject().oldValueIsUnknown()) {
            sivp = new Label(ID_OLD_VALUE, createStringResource("SceneItemLinePanel.unknownLabel"));
        } else {
            sivp = new VisualizationItemValuePanel(ID_OLD_VALUE, () -> getModel().getObject().getOldValue());
        }
        sivp.setRenderBodyOnly(true);
        oldValueCell.add(sivp);

        IconComponent oldValueImagePanel = new IconComponent(ID_OLD_VALUE_IMAGE, Model.of(GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER),
                createStringResource("SceneItemLinePanel.removedValue"));
        oldValueImagePanel.add(new VisibleBehaviour(
                () -> {
                    VisualizationItemValue val = getModelObject().getOldValue();
                    return val != null && val.getSourceValue() != null && !getModelObject().oldValueIsUnknown();
                }));
        oldValueCell.add(oldValueImagePanel);

        add(oldValueCell);

        IModel<String> newValueIconModel;
        IModel<String> newValueTitleModel;
        if (getModelObject().oldValueIsUnknown()) {
            if (getModelObject().isAdd()) {
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS);
                newValueTitleModel = createStringResource("SceneItemLinePanel.addedValue");
            } else if (getModelObject().isDelete()) {
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER);
                newValueTitleModel = createStringResource("SceneItemLinePanel.removedValue");
            } else if (getModelObject().isReplace()) {
                newValueIconModel = Model.of(GuiStyleConstants.CLASS_CIRCLE_FULL);
                newValueTitleModel = createStringResource("SceneItemLinePanel.unchangedValue");
            } else {
                newValueIconModel = Model.of((String) null);
                newValueTitleModel = Model.of((String) null);
            }
        } else {
            if (getModelObject().isDescriptive()) {
                newValueIconModel = Model.of((String) null);
                newValueTitleModel = Model.of((String) null);
            } else {
                newValueIconModel = !getModelObject().isDelta() && getModelObject().isDeltaVisualization() ?
                        Model.of(GuiStyleConstants.CLASS_CIRCLE_FULL) :
                        Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS);
                newValueTitleModel = !getModelObject().isDelta() && getModelObject().isDeltaVisualization() ?
                        createStringResource("SceneItemLinePanel.unchangedValue")
                        : createStringResource("SceneItemLinePanel.addedValue");
            }
        }

        WebMarkupContainer newValueCell = new WebMarkupContainer(ID_NEW_VALUE_CONTAINER);
        newValueCell.add(AttributeModifier.replace("colspan",
                () -> !getModelObject().isDelta() && !getModelObject().oldValueIsUnknown() && getModelObject().isDeltaVisualization() ? 2 : 1));
        newValueCell.add(AttributeModifier.replace("align",
                () -> !getModelObject().isDelta() && !getModelObject().oldValueIsUnknown() && getModelObject().isDeltaVisualization() ? "center" : null));

        WebMarkupContainer newValueSubContainer = new WebMarkupContainer(ID_NEW_VALUE_SUB_CONTAINER);
        newValueSubContainer.add(AttributeModifier.append("class",
                () -> !getModelObject().isDelta()
                        && !getModelObject().oldValueIsUnknown()
                        && getModelObject().isDeltaVisualization()
                        ? "justify-content-center" : null));
        newValueCell.add(newValueSubContainer);

        sivp = new VisualizationItemValuePanel(ID_NEW_VALUE, () -> getModel().getObject().getNewValue());
        sivp.setRenderBodyOnly(true);
        newValueSubContainer.add(sivp);

        IconComponent newValueImagePanel = new IconComponent(ID_NEW_VALUE_IMAGE, newValueIconModel, newValueTitleModel);
        newValueImagePanel.add(new VisibleBehaviour(() -> {
            VisualizationItemValue val = getModelObject().getNewValue();
            return newValueIconModel.getObject() != null && val != null && val.getSourceValue() != null;
        }));
        newValueSubContainer.add(newValueImagePanel);

        add(newValueCell);

        initMetadataButton(oldValueCell, ID_OLD_METADATA, () -> getModelObject().getOldValue());
        initMetadataButton(newValueSubContainer, ID_NEW_METADATA, () -> getModelObject().getNewValue());
    }

    private void initMetadataButton(WebMarkupContainer parent, String id, IModel<VisualizationItemValue> model) {
        IModel<ValueMetadataWrapperImpl> metadataModel = createMetadataModel(model);

        AjaxIconButton metadata = new AjaxIconButton(id, Model.of("fa fa-sm fa-tag"),
                createStringResource("VisualizationItemLinePanel.metadata")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showMetadata(target, metadataModel);
            }
        };
        // todo MID-8658 not finished yet, hidden for 4.7 release
        metadata.add(VisibleBehaviour.ALWAYS_INVISIBLE);
//        metadata.add(new VisibleBehaviour(() -> {
//            VisualizationItemValue val = model.getObject();
//            return val != null && val.getSourceValue() != null && !getModelObject().isNullEstimatedOldValues() && metadataModel.getObject() != null;
//        }));
        parent.add(metadata);
    }

    private IModel<ValueMetadataWrapperImpl> createMetadataModel(IModel<VisualizationItemValue> model) {
        return new LoadableDetachableModel() {
            @Override
            protected Object load() {
                VisualizationItemValue item = model.getObject();
                if (item == null) {
                    return null;
                }

                return VisualizationUtil.createValueMetadataWrapper(item.getSourceValue(), getPageBase());
            }
        };
    }

    private void showMetadata(AjaxRequestTarget target, IModel<ValueMetadataWrapperImpl> model) {
        PageBase page = getPageBase();
        page.showMainPopup(new MetadataPopup(page.getMainPopupBodyId(), model), target);
    }

//    private ItemPanelSettings createMetadataPanelSettings() {
//        return new ItemPanelSettingsBuilder()
//                .editabilityHandler(wrapper -> true)
//                .headerVisibility(false)
//                .visibilityHandler(w -> createItemVisibilityBehavior(w))
//                .build();
//    }
//
//    private ItemVisibility createItemVisibilityBehavior(ItemWrapper<?, ?> w) {
//        if (getModelObject().isShowMetadataDetails()) {
//            return w instanceof PrismContainerWrapper ? ItemVisibility.HIDDEN : ItemVisibility.AUTO;
//        }
//        return w.isShowMetadataDetails() ? ItemVisibility.AUTO : ItemVisibility.HIDDEN;
//    }
}
