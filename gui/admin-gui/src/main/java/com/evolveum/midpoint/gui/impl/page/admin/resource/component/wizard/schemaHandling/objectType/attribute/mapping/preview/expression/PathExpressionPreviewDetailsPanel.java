/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.expression;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Preview panel for path expressions. Extracts and displays the
 * {@link ItemPathType} value from the expression.
 */
public class PathExpressionPreviewDetailsPanel extends BasePanel<ExpressionType> {

    private static final Trace LOGGER = TraceManager.getTrace(PathExpressionPreviewDetailsPanel.class);

    private static final String ID_PATH_VALUE = "pathValue";

    public PathExpressionPreviewDetailsPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new Label(ID_PATH_VALUE, Model.of(getPathValue())));
    }

    private String getPathValue() {
        ExpressionType expression = getModelObject();
        if (expression == null) {
            return "";
        }

        try {
            ItemPathType path = ExpressionUtil.getPathExpressionValue(expression);
            if (path == null) {
                return "";
            }
            return String.valueOf(path);
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get path expression value: {}", ex.getLocalizedMessage(), ex);
            return "";
        }
    }
}
