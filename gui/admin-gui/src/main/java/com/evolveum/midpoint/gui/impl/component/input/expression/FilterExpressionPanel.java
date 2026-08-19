/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.expression;

import javax.xml.namespace.QName;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.reports.component.SearchFilterConfigurationPanel;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FilterExpressionEvaluatorType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class FilterExpressionPanel extends EvaluatorExpressionPanel {

    private static final Trace LOGGER = TraceManager.getTrace(FilterExpressionPanel.class);

    private static final String ID_FILTER_INPUT = "filterInput";

    public FilterExpressionPanel(String id, IModel<ExpressionType> model, IModel<QName> expressionTargetTypeModel) {
        super(id, model, expressionTargetTypeModel);
    }

    @Override
    public IModel<String> getValueContainerLabelModel(PageBase pageBase) {
        return pageBase.createStringResource("FilterExpressionPanel.label");
    }

    @Override
    protected void initLayout(MarkupContainer parent) {
        parent.add(new SearchFilterConfigurationPanel<>(
                ID_FILTER_INPUT, null, createFilterModel(), this::getExpressionTargetType) {

            @Override
            protected StringResourceModel getConfigPanelTitle() {
                return createStringResource("FilterExpressionPanel.configureFilter");
            }
            
            @Override
            protected boolean addEmptyBlumBehaviourToTextField() {
                return true;
            }
        });
    }

    /**
     * Model of the filter of the expression. {@link LambdaModel} skips the setter when its target
     * object is null, and the expression of an empty condition is null, so the target falls back to
     * a fresh expression. Without it the first filter the user configures is silently dropped.
     */
    private IModel<SearchFilterType> createFilterModel() {
        return LambdaModel.of(
                getModel().orElseGet(ExpressionType::new), FilterExpressionPanel::readFilter, this::writeFilter);
    }

    private static SearchFilterType readFilter(ExpressionType expression) {
        FilterExpressionEvaluatorType evaluator = ExpressionUtil.getFilterExpressionValue(expression);
        return evaluator != null ? evaluator.getFilter() : null;
    }

    private void writeFilter(ExpressionType expression, SearchFilterType filter) {
        try {
            FilterExpressionEvaluatorType evaluator = filter == null
                    ? null
                    : new FilterExpressionEvaluatorType().filter(filter);
            getModel().setObject(
                    ExpressionUtil.updateFilterExpressionValue(expression, evaluator));
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't update filter expression value: {}", ex.getLocalizedMessage());
            getPageBase().error("Couldn't update filter expression value: " + ex.getLocalizedMessage());
        }
    }

    /**
     * Description of the filter shown in the tooltip of the collapsed expression. Looked up by class
     * and method name from {@link ExpressionPanel}, do not remove.
     *
     * @param expression expression the filter belongs to.
     * @param pageBase page the description is created for.
     * @return the query text of the filter, or an empty string when there is none.
     */
    public static String getInfoDescription(ExpressionType expression, PageBase pageBase) {
        FilterExpressionEvaluatorType evaluator = ExpressionUtil.getFilterExpressionValue(expression);
        if (evaluator == null || evaluator.getFilter() == null) {
            return "";
        }
        String text = evaluator.getFilter().getText();
        return text != null ? text : "";
    }
}
