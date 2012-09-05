/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ACAttributeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_VALUE = "value";
    public static final String F_EXPRESSION = "expression";

    private PrismPropertyDefinition definition;
    private String value;
    private String expression;

    public ACAttributeDto(PrismPropertyDefinition definition) {
        this(definition, null, null);
    }

    public ACAttributeDto(PrismPropertyDefinition definition, String value, String expression) {
        Validate.notNull(definition, "Prism property definition must not be null.");

        this.definition = definition;
        this.expression = expression;
        this.value = value;
    }

    public PrismPropertyDefinition getDefinition() {
        return definition;
    }

    public String getName() {
        return definition.getDisplayName(); //todo better impl
    }

    public String getExpression() {
        return expression;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public boolean isExpressionUsed() {
        return StringUtils.isNotEmpty(expression);
    }
}
