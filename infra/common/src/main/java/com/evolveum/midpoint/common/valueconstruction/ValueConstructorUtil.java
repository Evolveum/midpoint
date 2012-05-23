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
package com.evolveum.midpoint.common.valueconstruction;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DOMUtil;

/**
 * @author semancik
 *
 */
public class ValueConstructorUtil {
	
    // Black magic hack follows. This is TODO to refactor to a cleaner state.
    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> toOutputTriple(PrismValueDeltaSetTriple<V> resultTriple, 
    		ItemDefinition outputDefinition, final PropertyPathSegment lastPathSegment, final PrismContext prismContext) {
    	Class<?> resultTripleValueClass = resultTriple.getRealValueClass();
    	if (resultTripleValueClass == null) {
    		// triple is empty. type does not matter.
    		return resultTriple;
    	}
    	PrismValueDeltaSetTriple<V> clonedTriple = resultTriple.clone();
    	if (resultTripleValueClass.equals(String.class) && outputDefinition.getTypeName().equals(PrismConstants.POLYSTRING_TYPE_QNAME)) {
    		// Have String, want PolyString
    		clonedTriple.accept(new Visitor() {
				@Override
				public void visit(Visitable visitable) {
					if (visitable instanceof PrismPropertyValue<?>) {
						PrismPropertyValue<Object> pval = (PrismPropertyValue<Object>)visitable;
						String realVal = (String)pval.getValue();
						PolyString polyStringVal = new PolyString(realVal);
						polyStringVal.recompute(prismContext.getDefaultPolyStringNormalizer());
						pval.setValue(polyStringVal);
					}
				}
			});
    	}
    	if (resultTripleValueClass.equals(PolyString.class) && outputDefinition.getTypeName().equals(DOMUtil.XSD_STRING)) {
    		// Have PolyString, want String
    		clonedTriple.accept(new Visitor() {
				@Override
				public void visit(Visitable visitable) {
					if (visitable instanceof PrismPropertyValue<?>) {
						PrismPropertyValue<Object> pval = (PrismPropertyValue<Object>)visitable;
						PolyString realVal = (PolyString)pval.getValue();
						if (realVal != null) {
							if (lastPathSegment != null && lastPathSegment.getName().equals(PrismConstants.POLYSTRING_ELEMENT_NORM_QNAME)) {
								pval.setValue(realVal.getNorm());
							} else {
								pval.setValue(realVal.getOrig());
							}
						}						
					}
				}
			});
    	}
    	return clonedTriple;
    }


}
