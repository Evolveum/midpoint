/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormMappingPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.gui.api.util.WebPrismUtil.setReadOnlyRecursively;

public class CorrelationMappingFormPanel<C extends MappingType> extends BasePanel<PrismContainerValueWrapper<C>> implements Popupable {

    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";

    private static final String ID_FORM_PANEL = "formPanel";

    private static final String ID_CANCEL = "cancel";
    private static final String ID_ADD_SELECTED_MAPPINGS = "addSelectedMappings";
    private static final String ID_BUTTONS = "buttons";

    private Fragment footerFragment;

    public CorrelationMappingFormPanel(String id, IModel<PrismContainerValueWrapper<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        applyStaticBackdrop();
        initLayout();
    }

    @Override
    protected void onDetach() {
        try {
            restoreBackdropDefaults();
        } finally {
            super.onDetach();
        }
    }

    // Ugly hack to make the backdrop static (clicking outside the popup does not close it).
    private void applyStaticBackdrop() {
        WebMarkupContainer overlay = (WebMarkupContainer) getPageBase().getMainPopup().get("overlay");
        if (overlay == null) return;

        // Bootstrap 5
        overlay.add(AttributeModifier.replace("data-bs-backdrop", "static"));
        overlay.add(AttributeModifier.replace("data-bs-keyboard", "false"));

        // Bootstrap 4
        overlay.add(AttributeModifier.replace("data-backdrop", "static"));
        overlay.add(AttributeModifier.replace("data-keyboard", "false"));
    }

    // Remove the static backdrop so future popups use their own defaults. TBD (not safe)
    private void restoreBackdropDefaults() {
        WebMarkupContainer overlay = (WebMarkupContainer) getPageBase().getMainPopup().get("overlay");
        if (overlay == null) return;

        overlay.add(AttributeModifier.remove("data-bs-backdrop"));
        overlay.add(AttributeModifier.remove("data-bs-keyboard"));
        overlay.add(AttributeModifier.remove("data-backdrop"));
        overlay.add(AttributeModifier.remove("data-keyboard"));
    }

    protected void initLayout() {
        if (isReadOnlyMapping()) {
            setReadOnlyRecursively(getModelObject());
        }

        initHeaderDescriptionPart();

        VerticalFormMappingPanel<?> formCorrelationMappingPanel = new VerticalFormMappingPanel<>(
                ID_FORM_PANEL,
                getModel(), getItemPanelSettings()){
            @Override
            protected boolean isShowEmptyButtonVisible() {
                return CorrelationMappingFormPanel.this.isShowEmptyButtonVisible();
            }
        };

        formCorrelationMappingPanel.setOutputMarkupId(true);
        add(formCorrelationMappingPanel);
    }

    protected boolean isShowEmptyButtonVisible() {
        return false;
    }

    private void initHeaderDescriptionPart() {
        Label textLabel = new Label(ID_TEXT, createStringResource("CorrelationMappingFormPanel.text"));
        textLabel.setOutputMarkupId(true);
        add(textLabel);

        Label subTextLabel = new Label(ID_SUBTEXT, createStringResource("CorrelationMappingFormPanel.subText"));
        subTextLabel.setOutputMarkupId(true);
        add(subTextLabel);
    }

    private @NotNull Fragment initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxIconButton createMappingButton = createMappingButton();
        footer.add(createMappingButton);
        AjaxLink<Object> cancelButton = new AjaxLink<>(ID_CANCEL) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCancel(target);
                hidePopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        cancelButton.add(new VisibleBehaviour(this::isCancelButtonVisible));
        footer.add(cancelButton);

        footer.setOutputMarkupId(true);
        return footer;

    }

    protected boolean isCancelButtonVisible() {
        return true;
    }

    protected void onCancel(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    private void hidePopup(AjaxRequestTarget target) {
        onDetach();
        getPageBase().hideMainPopup(target);
    }


    private @NotNull AjaxIconButton createMappingButton() {
        AjaxIconButton createMappingButton = new AjaxIconButton(ID_ADD_SELECTED_MAPPINGS,
                getConfirmButtonIcon(),
                getConfirmButtonLabel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                performCreateMapping(target);
                hidePopup(target);
            }
        };
        createMappingButton.showTitleAsLabel(true);
        createMappingButton.setOutputMarkupId(true);
        createMappingButton.add(new VisibleBehaviour(() -> !isReadOnlyMapping()));
        return createMappingButton;
    }

    protected IModel<String> getConfirmButtonLabel() {
        return createStringResource("CorrelationMappingFormPanel.button.createMapping");
    }

    protected IModel<String> getConfirmButtonIcon() {
        return Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE);
    }

    protected void performCreateMapping(AjaxRequestTarget target) {
        // Override to implement
    }

    protected static ItemPanelSettings getItemPanelSettings() {
        return new ItemPanelSettingsBuilder()
                .visibilityHandler((wrapper) -> {
                    ItemName itemName = wrapper.getPath().lastName();
                    return itemName.equivalent(MappingType.F_NAME)
                            || itemName.equivalent(MappingType.F_TARGET)
                            || itemName.equivalent(MappingType.F_EXPRESSION)
                            || itemName.equivalent(MappingType.F_LIFECYCLE_STATE)
                            || itemName.equivalent(InboundMappingType.F_USE)
                            || itemName.equivalent(ResourceAttributeDefinitionType.F_LIFECYCLE_STATE)
                            || itemName.equivalent(ResourceAttributeDefinitionType.F_REF)
                            ? ItemVisibility.AUTO
                            : ItemVisibility.HIDDEN;
                })
                .isRemoveButtonVisible(false)
                .build();
    }

    protected boolean isReadOnlyMapping() {
        return false;
    }

    @Override
    public @NotNull Fragment getFooter() {
        if (footerFragment == null) {
            footerFragment = initFooter();
        }
        return footerFragment;
    }

    @Override
    public int getWidth() {
        return 20;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CorrelationMappingFormPanel.title");
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE);
    }

    @Override
    public Component getContent() {
        this.add(AttributeModifier.append("class", "p-0"));
        return this;
    }
}
