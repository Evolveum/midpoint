/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serial;
import java.io.Serializable;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import jakarta.annotation.PostConstruct;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

@Component
public class RoleAnalysisItemPathPanelFactory extends ItemPathPanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisItemPathPanelFactory.class);

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        ItemPath itemPath = wrapper.getPath().namedSegmentsOnly();
        ItemPath wrapperPath = ItemPath.create(RoleAnalysisSessionType.F_MATCHING_RULE, RoleAnalysisMatchingRuleType.F_ITEM_PATH);
        return super.match(wrapper, valueWrapper) && itemPath.equivalent(wrapperPath);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        RoleAnalysisSessionType objectType = getObjectType(panelCtx);
        return new ItemPathPanel(panelCtx.getComponentId(), panelCtx.getRealValueModel().getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                panelCtx.getRealValueModel().setObject(new ItemPathType(itemPathDto.toItemPath()));
            }

            @Override
            protected boolean isNamespaceVisible() {
                return true;
            }

            @Override
            protected boolean isNamespaceEnable() {
                return false;
            }

            @Override
            protected boolean setDefaultItemPath() {
                RoleAnalysisProcessModeType processMode = resolveProcessMode(objectType);

                if (processMode != null) {
                    if (processMode == RoleAnalysisProcessModeType.ROLE) {
                        getModelObject().setObjectType(RoleType.COMPLEX_TYPE);
                    } else if (processMode == RoleAnalysisProcessModeType.USER) {
                        getModelObject().setObjectType(UserType.COMPLEX_TYPE);
                    }
                }

                return super.setDefaultItemPath();
            }
        };
    }

    @Override
    public Integer getOrder() {
        return 10000;
    }

    private RoleAnalysisSessionType getObjectType(PrismPropertyPanelContext<ItemPathType> panelCtx) {
        PrismPropertyWrapper<ItemPathType> item = panelCtx.unwrapWrapperModel();

        if (item.getParent() != null
                && item.getParent().getParent() != null
                && item.getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent() != null
                && item.getParent().getParent().getParent().getParent() != null) {

            try {
                PrismValueWrapper<?> value = item.getParent().getParent().getParent().getParent().getValue();
                Object realValue = value.getRealValue();
                if (realValue instanceof RoleAnalysisSessionType) {
                    return (RoleAnalysisSessionType) realValue;
                }
            } catch (SchemaException e) {
                LOGGER.warn("Cannot get object type from RoleAnalysisSessionType item path: {}", e.getMessage(), e);
                return null;
            }

        }
        return null;
    }

    private RoleAnalysisProcessModeType resolveProcessMode(RoleAnalysisSessionType objectType) {
        if (objectType != null) {
            RoleAnalysisOptionType analysisOption = objectType.getAnalysisOption();
            if (analysisOption != null) {
                return analysisOption.getProcessMode();
            }
        }
        return null;
    }

}
