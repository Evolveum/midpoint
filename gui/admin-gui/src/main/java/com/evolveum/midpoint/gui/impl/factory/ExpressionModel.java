/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.bind.JAXBElement;
import java.util.List;

public class ExpressionModel implements IModel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionModel.class);

        private static final long serialVersionUID = 1L;

        private IModel<ExpressionType> baseModel;
        private PageBase pageBase;

        public ExpressionModel(IModel<ExpressionType> valueWrapper, PageBase pageBase) {
            this.baseModel = valueWrapper;
            this.pageBase = pageBase;
        }

        @Override
        public void detach() {
            // TODO Auto-generated method stub

        }

        @Override
        public String getObject() {
            try {
                ExpressionType value = baseModel.getObject();
                if (value == null) {
                    return null;
                }

                List<JAXBElement<?>> evaluatros = value.getExpressionEvaluator();
                //should be one
                if (CollectionUtils.isEmpty(evaluatros)) {
                    return null;
                }
                if (evaluatros.size() > 1) {
                    LOGGER.warn("More than one evaluator found. getting first of them");
                }

                JAXBElement<?> evaluator = evaluatros.get(0);
                if (evaluator == null) {
                    return null;
                }

                return ExpressionUtil.serialize(evaluator, pageBase.getPrismContext());

            } catch (SchemaException e) {
                // TODO handle!!!!
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
//                getSession().error("Cannot serialize filter");
            }
            return null;
        }

        @Override
        public void setObject(String object) {
            if (StringUtils.isBlank(object)) {
                baseModel.setObject(null);
                return;
            }

            try {
                ExpressionType condition = new ExpressionType();
                ExpressionUtil.parseExpressionEvaluators(object, condition, pageBase.getPrismContext());
//                ExpressionType condition = pageBase.getPrismContext().parserFor(object).parseRealValue();
                baseModel.setObject(condition);
            } catch (Exception e) {
                // TODO handle!!!!
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse filter", e);
//                getSession().error("Cannot parse filter");
            }

        }
}
