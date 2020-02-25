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
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
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
        AceEditorPanel conditionPanel  =  new AceEditorPanel(panelCtx.getComponentId(), null, new ConditionExpressionModel(panelCtx.getRealValueModel(), panelCtx.getPageBase()), 200);
//        conditionPanel.getEditor().add(new OnChangeAjaxBehavior() {
//            @Override
//            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
//
//            }
//        });
        conditionPanel.getEditor().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        conditionPanel.getEditor().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        return conditionPanel;
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        ItemPath assignmentConditionPath  = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_CONDITION, MappingType.F_EXPRESSION);
        ItemPath inducementConditionPath  = ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONDITION, MappingType.F_EXPRESSION);

        ItemPath wrapperPath = wrapper.getPath().namedSegmentsOnly();
        return wrapper instanceof PrismPropertyWrapper && (inducementConditionPath.isSubPathOrEquivalent(wrapperPath) || assignmentConditionPath.isSubPathOrEquivalent(wrapperPath) || QNameUtil.match(AssignmentType.F_CONDITION, wrapper.getItemName()));
    }
}
