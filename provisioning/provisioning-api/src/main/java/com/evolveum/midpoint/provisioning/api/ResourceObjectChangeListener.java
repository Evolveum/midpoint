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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;

/**
 * @author Radovan Semancik
 * 
 */
public interface ResourceObjectChangeListener {

	String CLASS_NAME_WITH_DOT = ResourceObjectChangeListener.class.getName() + ".";
	String NOTIFY_CHANGE = CLASS_NAME_WITH_DOT + "notifyChange";
	String CHECK_SITUATION = CLASS_NAME_WITH_DOT + "checkSituation";

	/**
	 * Submits notification about a specific change that happened on the
	 * resource. The call should return without a major delay. It means that the
	 * implementation can do calls to repository, but it should not
	 * (synchronously) initiate a long-running process or provisioning request.
	 * 
	 * This operation may be called multiple times with the same change, e.g. in
	 * case of failures in IDM or on the resource. The implementation must be
	 * able to handle such duplicates.
	 * 
	 * @param change
	 *            change description
	 */
	public void notifyChange(ResourceObjectShadowChangeDescriptionType change, OperationResult parentResult);

}
