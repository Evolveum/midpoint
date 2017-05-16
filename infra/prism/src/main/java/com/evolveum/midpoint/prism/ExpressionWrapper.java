/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author mederly
 */
public class ExpressionWrapper implements Cloneable {

	private QName elementName;
    private Object expression;

    public ExpressionWrapper(QName elementName, Object expression) {
		super();
		this.elementName = elementName;
		this.expression = expression;
	}

	public QName getElementName() {
		return elementName;
	}

	public Object getExpression() {
        return expression;
    }
	
	public ExpressionWrapper clone() {
		Object expressionClone = CloneUtil.clone(expression);
		return new ExpressionWrapper(elementName, expressionClone);
	}

	@Override
	public String toString() {
		return "ExpressionWrapper(" + PrettyPrinter.prettyPrint(elementName) + ":" + PrettyPrinter.prettyPrint(expression);
	}
    
    
}
