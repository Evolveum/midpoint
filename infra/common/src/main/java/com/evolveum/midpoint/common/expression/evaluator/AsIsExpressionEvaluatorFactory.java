/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.evaluator;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AsIsExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectFactory;

/**
 * @author semancik
 *
 */
public class AsIsExpressionEvaluatorFactory implements ExpressionEvaluatorFactory {
	
	private PrismContext prismContext;

	public AsIsExpressionEvaluatorFactory(PrismContext prismContext) {
		super();
		this.prismContext = prismContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#getElementName()
	 */
	@Override
	public QName getElementName() {
		return new ObjectFactory().createAsIs(new AsIsExpressionEvaluatorType()).getName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement)
	 */
	@Override
	public <V extends PrismValue> AsIsExpressionEvaluator<V> createEvaluator(JAXBElement<?> evaluatorElement, ItemDefinition outputDefinition, String contextDescription) throws SchemaException {
		Object evaluatorTypeObject = null;
        if (evaluatorElement != null) {
        	evaluatorTypeObject = evaluatorElement.getValue();
        }
        if (evaluatorTypeObject != null && !(evaluatorTypeObject instanceof AsIsExpressionEvaluatorType)) {
            throw new SchemaException("AsIs value constructor cannot handle elements of type " + evaluatorTypeObject.getClass().getName()+" in "+contextDescription);
        }
        return new AsIsExpressionEvaluator<V>((AsIsExpressionEvaluatorType)evaluatorTypeObject, outputDefinition, prismContext);
	}

}
