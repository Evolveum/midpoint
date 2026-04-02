/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.expression.PreviewExpressionPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * UI panel for previewing a mapping in a popup dialog. Includes mapping overview,
 * expression details, optional condition, and customizable footer buttons.
 */
public class PreviewMappingPanel extends BasePanel<PrismContainerValueWrapper<MappingType>> implements Popupable {

    private static final String ID_MAPPING_NAME = "mappingName";
    private static final String ID_MAPPING_CARD_PANEL = "mappingCardPanel";
    private static final String ID_EXPRESSION_PANEL = "expressionPanel";
    private static final String ID_CONDITION_PANEL = "conditionPanel";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_REPEATER = "repeater";

    private Fragment footer;

    boolean isInbound;

    public PreviewMappingPanel(String id, IModel<PrismContainerValueWrapper<MappingType>> model, boolean isInbound) {
        super(id, model);
        this.isInbound = isInbound;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(new Label(ID_MAPPING_NAME, Model.of(getMappingName(getModelObject().getRealValue()))));

        add(new MappingPreviewCardPanel(ID_MAPPING_CARD_PANEL, this::getModelObject, isInbound));

        add(new PreviewExpressionPanel(ID_EXPRESSION_PANEL, this::getExpression));

        PreviewExpressionPanel condition = new PreviewExpressionPanel(ID_CONDITION_PANEL, this::getCondition) {
            @Override
            protected boolean isCondition() {
                return true;
            }
        };
        condition.setOutputMarkupId(true);
        condition.add(new VisibleBehaviour(() -> getCondition() != null));
        add(condition);

        initFooter();
    }

    private @Nullable ExpressionType getCondition() {
        MappingType mapping = getModelObject().getRealValue();
        return mapping != null ? mapping.getCondition() : null;
    }

    private @Nullable ExpressionType getExpression() {
        MappingType mapping = getModelObject().getRealValue();
        return mapping != null ? mapping.getExpression() : null;
    }

    protected String getMappingName(MappingType mapping) {
        if (mapping == null) {
            return "";
        }
        return StringUtils.defaultString(mapping.getName());
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        RepeatingView repeater = new RepeatingView(ID_REPEATER);
        customizeFooterButtons(repeater);
        footer.add(repeater);
    }

    public void customizeFooterButtons(@NotNull RepeatingView repeater) {
        AjaxButton cancelButton = new AjaxButton(repeater.newChildId(),
                createStringResource("PreviewMappingPanel.cancelButton")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
            }
        };
        cancelButton.setOutputMarkupId(true);
        cancelButton.add(AttributeModifier.append("class", "btn-link"));
        repeater.add(cancelButton);
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    @Override
    public int getWidth() {
        return 35;
    }

    @Override
    public int getHeight() {
        return 70;
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
        return createStringResource("PreviewMappingPanel.title");
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of("fa fa-search");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
