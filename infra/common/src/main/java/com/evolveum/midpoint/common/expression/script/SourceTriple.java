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
package com.evolveum.midpoint.common.expression.script;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.expression.Source;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author semancik
 *
 */
public class SourceTriple<V extends PrismValue> extends PrismValueDeltaSetTriple<V> {

	private Source<V> source;

	public SourceTriple(Source<V> source) {
		super();
		this.source = source;
	}

	public SourceTriple(Source<V> source, Collection<V> zeroSet, Collection<V> plusSet, Collection<V> minusSet) {
		super(zeroSet, plusSet, minusSet);
		this.source = source;
	}
	
	public Source<V> getSource() {
		return source;
	}

	public void setSource(Source<V> source) {
		this.source = source;
	}

	public QName getName() {
		return source.getName();
	}

	public ItemPath getResidualPath() {
		return source.getResidualPath();
	}
	
}
