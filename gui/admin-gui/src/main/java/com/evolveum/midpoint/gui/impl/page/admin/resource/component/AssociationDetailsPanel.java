/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.DefaultContainerablePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.setReadOnlyRecursively;

public class AssociationDetailsPanel extends BasePanel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> {

    private static final String ID_DETAILS_FORM = "detailsForm";
    private static final String ID_REVIEW_BUTTON = "reviewButton";

    public AssociationDetailsPanel(String id, IModel<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .visibilityHandler(wrapper -> {
                    ItemName itemName = wrapper.getPath().lastName();
                    return itemName.equivalent(ShadowAssociationTypeDefinitionType.F_NAME)
                            || itemName.equivalent(ShadowAssociationTypeDefinitionType.F_DISPLAY_NAME)
                            || itemName.equivalent(ShadowAssociationTypeDefinitionType.F_DESCRIPTION)
                            || itemName.equivalent(ShadowAssociationTypeDefinitionType.F_DOCUMENTATION)
                            || itemName.equivalent(ShadowAssociationTypeDefinitionType.F_LIFECYCLE_STATE)
                            ? ItemVisibility.AUTO
                            : ItemVisibility.HIDDEN;
                })
                .isRemoveButtonVisible(false)
                .build();

        PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> valueClone = CloneUtil.clone(getModelObject());
        setReadOnlyRecursively(valueClone);
        var detailsFormComponent = buildFormComponent(valueClone, settings);
        add(detailsFormComponent);

        AjaxIconButton reviewButton = new AjaxIconButton(ID_REVIEW_BUTTON,
                () -> "fa fa-eye",
                createStringResource("SmartAssociationTilePanel.review")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                processReviewButtonClick(target);
            }
        };

        reviewButton.setOutputMarkupId(true);
        reviewButton.showTitleAsLabel(true);
        reviewButton.add(AttributeModifier.append("class", "btn btn-sm btn-outline-primary mt-2"));
        reviewButton.add(new VisibleBehaviour(this::isSuggestion));
        add(reviewButton);
    }

    private static @NotNull Component buildFormComponent(
            PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType> valueClone,
            ItemPanelSettings settings) {
        DefaultContainerablePanel<ShadowAssociationTypeDefinitionType, PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> details =
                new DefaultContainerablePanel<>(ID_DETAILS_FORM, () -> valueClone, settings) {
                    @Override
                    protected boolean isShowMoreButtonVisible(IModel<List<ItemWrapper<?, ?>>> nonContainerWrappers) {
                        return false;
                    }
                };

        details.setOutputMarkupId(true);
        return details;
    }

    protected boolean isSuggestion() {
        return false;
    }

    protected void processReviewButtonClick(@NotNull AjaxRequestTarget target) {
    }

}
