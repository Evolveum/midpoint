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

/**
 * @author semancik
 *
 */
public interface Containerable {
	
	
	public PrismContainerValue asPrismContainerValue();
	
	/**
	 * Setup value to the containerable representation. This is used to after (empty) containerable is created to
	 * initialize it with a correct prism container value.
	 * Note: This method DOES NOT change the container value parent.
	 */
	public void setupContainerValue(PrismContainerValue container);
	
}
