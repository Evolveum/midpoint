/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import javax.xml.bind.JAXBElement;

/**
 * Very simple expression utils. More advanced ones are to be found in upper layers.
 *
 * EXPERIMENTAL. Later will be reconsidered.
 *
 * @author mederly
 */
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
