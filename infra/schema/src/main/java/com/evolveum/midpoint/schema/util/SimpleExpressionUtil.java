/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import javax.xml.bind.JAXBElement;

/**
 * Very simple expression utils. More advanced ones are to be found in upper layers.
 *
 * EXPERIMENTAL. Later will be reconsidered.
 *
 * @author mederly
 */
@Experimental
public class SimpleExpressionUtil {

    public static Object getConstantIfPresent(ExpressionType expression) {
        if (expression == null || expression.getExpressionEvaluator().size() != 1) {
            return null;
        }
        JAXBElement<?> jaxb = expression.getExpressionEvaluator().get(0);
        if (QNameUtil.match(jaxb.getName(), SchemaConstants.C_VALUE)) {
            return jaxb.getValue();
        } else {
            return null;
        }
    }
}
