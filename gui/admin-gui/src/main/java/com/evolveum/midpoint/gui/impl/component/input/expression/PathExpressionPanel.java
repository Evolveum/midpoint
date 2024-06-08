/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowOwnerReferenceSearchExpressionEvaluatorType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class PathExpressionPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(PathExpressionPanel.class);

    private static final String ID_PATH_INPUT = "pathInput";
    private static final String ID_PATH_LABEL = "pathLabel";

    public PathExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    public IModel<String> getValueContainerLabelModel() {
        return getPageBase().createStringResource("OptionObjectSelectorType.path");
    }

    protected void initLayout(MarkupContainer parent) {
        parent.add(new Label(ID_PATH_LABEL, createStringResource("OptionObjectSelectorType.path")));

        parent.add(createPathPanel());
    }

    private Component createPathPanel() {
        TextPanel<String> pathPanel = new TextPanel<>(
                ID_PATH_INPUT, Model.of(getPath(getModelObject(), getPageBase())), String.class, false);
        pathPanel.setOutputMarkupId(true);

        pathPanel.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String path = pathPanel.getBaseFormComponent().getConvertedInput();
                updateEvaluatorValue(path);
                target.add(getFeedback());
            }
        });

        return pathPanel;
    }

    private void updateEvaluatorValue(String value) {
        ExpressionType expressionType = getModelObject();
        try {
            ItemPathType evaluator = PrismContext.get().itemPathParser().asItemPathType(value);
            expressionType = ExpressionUtil.updatePathEvaluator(expressionType, evaluator);
            getModel().setObject(expressionType);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update shadowOwnerReferenceSearch expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update shadowOwnerReferenceSearch expression value: " + ex.getLocalizedMessage());
        }
    }

    //don't remove it, used by class and method name
    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        return getPath(expression, pageBase);
    }

    private static String getPath(ExpressionType expressionType, PageBase pageBase) {
        try {
            ItemPathType path = ExpressionUtil.getPathExpressionValue(expressionType);
            if (path == null) {
                return "";
            }
            return path.toString();
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get path expression value: {}", ex.getLocalizedMessage());
            pageBase.error("Couldn't get path expression value: " + ex.getLocalizedMessage());
        }
        return null;
    }
}
