/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisAttributeProgressBarDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisObjectDetailsTablePopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.util.TooltipBehavior;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisProgressBarDto;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RoleAnalysisAttributeProgressBar extends AbstractRoleAnalysisProgressBar<RoleAnalysisAttributeProgressBarDto> {

    private static final String ID_BAR_TITTLE_DATA = "progressBarDetails";
    private static final String ID_CONTAINER_TITLE_DATA = "details-container";
    private static final String ID_BAR_TITLE = "progressBarTitle";

    public RoleAnalysisAttributeProgressBar(String id, IModel<RoleAnalysisAttributeProgressBarDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    protected boolean isTitleContainerVisible() {
        return true;
    }

    @Override
    protected String getProgressBarContainerCssClass() {
        return null;
    }

    @Override
    protected @NotNull WebMarkupContainer buildTitleContainer(String idTitleContainer) {
        WebMarkupContainer titleContainer = new WebMarkupContainer(idTitleContainer);
        titleContainer.setOutputMarkupId(true);
        add(titleContainer);

        Component component = buildTitleComponent(ID_BAR_TITLE, getPageBase());
        titleContainer.add(component);

        resolveTitleDataLabel(titleContainer);
        return titleContainer;
    }

    public Component buildTitleComponent(String id, PageBase pageBase) {
        if (getModelObject().isLinkTitle()) {
            List<PrismObject<ObjectType>> prismObjects = getModelObject().getObjectValues();
            return buildAjaxLinkTitlePanel(id, pageBase, prismObjects);
        } else {
            IconWithLabel progressBarTitle = new IconWithLabel(id, new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_BAR_TITLE)) {
                @Override
                protected @NotNull String getIconCssClass() {
                    return "";
                }
            };
            progressBarTitle.setOutputMarkupId(true);
            if (getModelObject().isUnusual()) {
                progressBarTitle.add(new TooltipBehavior());
                progressBarTitle.add(AttributeModifier.replace("title",
                        pageBase.createStringResource("Unusual value")));
            }
            return progressBarTitle;
        }
    }

    private @NotNull AjaxLinkPanel buildAjaxLinkTitlePanel(String id, PageBase pageBase, List<PrismObject<ObjectType>> prismObjects) {
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(id, new PropertyModel<>(getModel(), RoleAnalysisProgressBarDto.F_BAR_TITLE)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisObjectDetailsTablePopupPanel detailsPanel = new RoleAnalysisObjectDetailsTablePopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        pageBase.createStringResource("RoleAnalysis.analyzed.members.details.panel"),
                        prismObjects) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };

                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }
        };
        ajaxLinkPanel.setOutputMarkupId(true);
        return ajaxLinkPanel;
    }

    private void resolveTitleDataLabel(@NotNull WebMarkupContainer titleContainer) {
        PropertyModel<Object> helpTooltip = new PropertyModel<>(getModel(), RoleAnalysisAttributeProgressBarDto.F_HELP_TOOLTIP);
        String helpTextValue = helpTooltip.getObject().toString();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER_TITLE_DATA);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpTextValue)));
        titleContainer.add(container);

        Label help = new Label(ID_BAR_TITTLE_DATA);
        help.add(AttributeModifier.replace("data-original-title",
                Model.of(helpTextValue)));
        help.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(helpTextValue)));
        help.setOutputMarkupId(true);
        if (getModelObject().isUnusual()) {
            help.add(AttributeModifier.append("class", "fa-exclamation-triangle text-warning"));
        } else {
            help.add(AttributeModifier.append("class", " fa-info-circle text-info"));
        }
        container.add(help);
    }

    protected boolean isWider() {
        return true;
    }

    @Override
    protected void initProgressValueLabel(String componentId, @NotNull WebMarkupContainer container) {
        super.initProgressValueLabel(componentId, container);
    }
}
