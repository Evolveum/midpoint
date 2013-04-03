/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.filter;

import java.util.List;

import com.evolveum.midpoint.prism.PrismPropertyValue;

import org.w3c.dom.Node;

/**
 * 
 * @author Igor Farinic
 * 
 */
public interface Filter {

	void setParameters(List<Object> parameters);

	List<Object> getParameters();

	<T extends  Object> PrismPropertyValue<T> apply(PrismPropertyValue<T> value);
}
