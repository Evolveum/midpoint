/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

/**
 *
 */
public interface UcfExpressionEvaluatorAware {

    @SuppressWarnings("unused")
    UcfExpressionEvaluator getUcfExpressionEvaluator();

    void setUcfExpressionEvaluator(UcfExpressionEvaluator evaluator);
}
