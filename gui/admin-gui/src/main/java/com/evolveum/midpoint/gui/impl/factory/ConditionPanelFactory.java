/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;

@Component
public class ConditionPanelFactory extends AbstractGuiComponentFactory<ExpressionType> implements Serializable  {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ExpressionType> panelCtx) {
        AceEditorPanel conditionPanel  =  new AceEditorPanel(panelCtx.getComponentId(), null, new ExpressionModel(panelCtx.getRealValueModel(), panelCtx.getPageBase()), 200){
            @Override
            protected boolean isResizeToMaxHeight() {
                return false;
            }
        };
        conditionPanel.getEditor().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        conditionPanel.getEditor().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        return conditionPanel;
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return QNameUtil.match(ExpressionType.COMPLEX_TYPE, wrapper.getTypeName());
    }
}
