/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.page.admin.DetailsFragment;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AjaxLinkTruncatePanelAction extends BasePanel<AjaxLinkTruncateDto> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LINK_LABEL = "link_label";
    private static final String ID_ICON = "icon";
    private static final String ID_LINK_ICON = "link_icon";
    private static final String ID_IMAGE = "image";
    private static final String ID_CONTAINER = "container";

    public AjaxLinkTruncatePanelAction(
            @NotNull String id,
            @NotNull IModel<AjaxLinkTruncateDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        DetailsFragment detailsView = initFragment();
        detailsView.setOutputMarkupId(true);
        add(detailsView);
    }

    protected DetailsFragment initFragment() {
        AjaxLink<Void> actionLink = prepareActionLinkPanel();
        actionLink.add(new VisibleBehaviour(this::isActionEnabled));

        PanelMode panelMode = getPanelMode();
        if (panelMode.equals(PanelMode.DEFAULT)) {
            add(AttributeAppender.append("class", "col-12 p-0"));
        } else if (panelMode.equals(PanelMode.ROTATED)) {
            add(AttributeAppender.append("class", "role-mining-header-height"));
        }

        return new DetailsFragment(ID_CONTAINER, panelMode.getMode(), AjaxLinkTruncatePanelAction.this) {

            @Override
            protected void initFragmentLayout() {

                AjaxLink<String> link = new AjaxLink<>(ID_LINK) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        AjaxLinkTruncatePanelAction.this.onDisplayNameClick(target);
                    }

                };

                Label label = new Label(ID_LINK_LABEL, new PropertyModel<>(getModel(), AjaxLinkTruncateDto.F_NAME));

                if (getPopupText() != null) {
                    label.add(AttributeModifier.replace("title", getPopupText()));
                    label.add(new TooltipBehavior());
                }

                link.add(label);
                link.add(new EnableBehaviour(AjaxLinkTruncatePanelAction.this::isEnabled));
                add(link);

                add(new CompositedIconPanel(ID_ICON, new PropertyModel<>(getModel(), AjaxLinkTruncateDto.F_ICON)));

                add(actionLink);

            }
        };
    }

    @NotNull
    private AjaxLink<Void> prepareActionLinkPanel() {
        Label image = new Label(ID_IMAGE);
        image.add(AttributeModifier.replace("class",
                new PropertyModel<>(getModel(), AjaxLinkTruncateDto.F_MODE + ".displayString")));
        image.setOutputMarkupId(true);
        AjaxLink<Void> actionLink = new AjaxLink<>(ID_LINK_ICON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisOperationMode roleAnalysisOperationMode = onStatusClick(
                        target, AjaxLinkTruncatePanelAction.this.getModelObject().getMode());

                if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.EXCLUDE)) {
                    getImageComponent().add(
                            AttributeModifier.replace("class", RoleAnalysisOperationMode.INCLUDE.getDisplayString()));
                } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.INCLUDE)) {
                    getImageComponent().add(
                            AttributeModifier.replace("class", RoleAnalysisOperationMode.EXCLUDE.getDisplayString()));
                }

                target.add(getImageComponent());
            }
        };
        actionLink.add(image);
        actionLink.setOutputMarkupId(true);
        return actionLink;
    }

    private Component getImageComponent() {
        return get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_LINK_ICON, ID_IMAGE));
    }

    protected RoleAnalysisOperationMode onStatusClick(
            AjaxRequestTarget target,
            RoleAnalysisOperationMode roleAnalysisOperationMode) {
        return roleAnalysisOperationMode;
    }

    public void onDisplayNameClick(AjaxRequestTarget target) {
    }

    private @Nullable String getPopupText() {
        return AjaxLinkTruncatePanelAction.this.getModelObject().getToolTip();
    }

    private @NotNull PanelMode getPanelMode() {
        return AjaxLinkTruncatePanelAction.this.getModelObject().getPanelMode();
    }

    public enum PanelMode {
        DEFAULT("mode-first"),
        ROTATED("mode-second");

        private final String mode;

        PanelMode(String mode) {
            this.mode = mode;
        }

        public String getMode() {
            return mode;
        }
    }

    public boolean isActionEnabled() {
        return AjaxLinkTruncatePanelAction.this.getModelObject().isActionEnabled();
    }

}
