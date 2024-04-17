/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.mining;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.MatchRulePanel;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForRoleAnalysis;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getAttributesForUserAnalysis;

//TODO check serializable
@Component
public class MatchRulePanelFactory extends AbstractGuiComponentFactory<MatchType> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return RoleAnalysisMatchingRuleType.F_MATCH_RULE.equals(wrapper.getItemName());
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<MatchType> panelCtx) {
        MatchRulePanel rangeSliderPanel = new MatchRulePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(),false);
        rangeSliderPanel.setOutputMarkupId(true);
        return rangeSliderPanel;
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

    public boolean isMultiValue(@NotNull PrismPropertyPanelContext<MatchType> panelCtx) {
        IModel<PrismPropertyWrapper<MatchType>> itemWrapperModel = panelCtx.getItemWrapperModel();

        if (itemWrapperModel != null) {
            PrismPropertyWrapper<MatchType> object = itemWrapperModel.getObject();
            if (object != null) {
                PrismContainerValueWrapper<?> parent = object.getParent();
                if (parent != null && parent.getRealValue() instanceof RoleAnalysisMatchingRuleType matchingRuleType) {
                    String attributeIdentifier = matchingRuleType.getAttributeIdentifier();

                    List<RoleAnalysisAttributeDef> attributes;
                    if (getProcessMode(panelCtx).equals(RoleAnalysisProcessModeType.ROLE)) {
                        attributes = getAttributesForRoleAnalysis();
                    } else {
                        attributes = getAttributesForUserAnalysis();
                    }

                    for (RoleAnalysisAttributeDef attribute : attributes) {
                        if (attribute.getDisplayValue().equals(attributeIdentifier)) {
                            return attribute.isContainer();
                        }
                    }
                }
            }
        }

        return false;
    }

    public RoleAnalysisProcessModeType getProcessMode(@NotNull PrismPropertyPanelContext<MatchType> panelCtx) {
        IModel<PrismPropertyWrapper<MatchType>> itemWrapperModel = panelCtx.getItemWrapperModel();

        if (itemWrapperModel != null) {
            PrismPropertyWrapper<MatchType> object = itemWrapperModel.getObject();
            if (object != null) {
                PrismContainerValueWrapper<?> parent = object.getParent();
                if (parent != null) {
                    if (parent.getParent() != null) {
                        if (parent.getParent().getParent() != null) {
                            Object realValue = parent.getParent().getParent().getRealValue();
                            if (realValue != null && realValue instanceof RoleAnalysisSessionType session) {
                                RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
                                return analysisOption.getProcessMode();
                            }
                        }
                    }
                }
            }
        }

        return RoleAnalysisProcessModeType.USER;
    }


}
