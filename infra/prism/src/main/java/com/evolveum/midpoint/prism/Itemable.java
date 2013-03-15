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
package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Interface for objects that behave like an item: they have a name and may have a definition.
 * 
 * Currently provides common abstraction on top of Item and ItemDelta, as both can hold values and
 * construct them in a similar way.
 * 
 * @author Radovan Semancik
 *
 */
public interface Itemable {
	
	public QName getName();
	
	public ItemDefinition getDefinition();
	
	public PrismContext getPrismContext();
	
	public ItemPath getPath();

}
