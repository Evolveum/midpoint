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
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

/**
 * @author semancik
 *
 */
public interface ModelProjectionContext<O extends ObjectType> extends ModelElementContext<O> {

	/**
	 * Returns synchronization delta.
	 * 
	 * Synchronization delta describes changes that have recently happened. MidPoint reacts to these
	 * changes by "pulling them in" (e.g. using them in inbound mappings).
	 */
	public ObjectDelta<O> getSyncDelta();
	public void setSyncDelta(ObjectDelta<O> syncDelta);
	
	/**
	 * Decision regarding the account. It describes the overall situation of the account e.g. whether account
	 * is added, is to be deleted, unliked, etc.
	 * 
	 * If set to null no decision was made yet. Null is also a typical value when the context is created.
	 * 
	 * @see SynchronizationPolicyDecision
	 */
	public SynchronizationPolicyDecision getSynchronizationPolicyDecision();
	
}
