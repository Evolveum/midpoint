/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

public class SimpleValueExpressionPanel extends BasePanel<ExpressionType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LITERAL_VALUE_INPUT = "literalValueInput";
    private static final String ID_LITERAL_VALUE_CONTAINER = "literalValueContainer";
    private static final String ID_FEEDBACK = "feedback";

    private static final Trace LOGGER = TraceManager.getTrace(SimpleValueExpressionPanel.class);

    public SimpleValueExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        FeedbackPanel feedback = new FeedbackPanel(ID_FEEDBACK);
        feedback.setOutputMarkupPlaceholderTag(true);
        add(feedback);

        WebMarkupContainer literalValueContainer = new WebMarkupContainer(ID_LITERAL_VALUE_CONTAINER);
        literalValueContainer.setOutputMarkupId(true);
        literalValueContainer.add(new VisibleBehaviour(() -> isLiteralExpressionValueNotEmpty()));
        add(literalValueContainer);

        MultiValueTextPanel<String> literalValueInput = new MultiValueTextPanel<String>(ID_LITERAL_VALUE_INPUT,
                new IModel<List<String>>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public List<String> getObject() {
                        return getLiteralValues();
                    }

                    @Override
                    public void setObject(List<String> strings) {
                        ExpressionUtil.updateLiteralExpressionValue(getModelObject(), strings, SimpleValueExpressionPanel.this.getPageBase().getPrismContext());
                    }

                    @Override
                    public void detach() {

                    }
                },
                new NonEmptyLoadableModel<Boolean>(false) {
                    @NotNull
                    @Override
                    protected Boolean load() {
                        return false;
                    }
                },
                false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void modelObjectUpdatePerformed(AjaxRequestTarget target, List<String> modelObject) {
                ExpressionUtil.updateLiteralExpressionValue(SimpleValueExpressionPanel.this.getModelObject(),
                        modelObject, SimpleValueExpressionPanel.this.getPageBase().getPrismContext());
            }
        };
        literalValueInput.add(new VisibleBehaviour(() -> isLiteralExpressionValueNotEmpty()));
        literalValueInput.setOutputMarkupId(true);
        literalValueContainer.add(literalValueInput);
    }

    private List<String> getLiteralValues() {
        List<String> literalValueList = new ArrayList<>();
        try {
            return ExpressionUtil.getLiteralExpressionValues(getModelObject());
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get literal expression value: {}", ex.getLocalizedMessage());
        }
        return literalValueList;
    }

    private boolean isLiteralExpressionValueNotEmpty() {
        try {
            return ExpressionUtil.isLiteralExpressionValueNotEmpty(SimpleValueExpressionPanel.this.getModelObject());
        } catch (SchemaException ex) {
            LOGGER.error("Unable to load literal expression value: {}", ex.getLocalizedMessage());
        }
        return false;
    }
}
