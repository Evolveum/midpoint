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
package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskBindingType;

/**
 * Binding tells about task "affinity" to a particular node.
 * 
 * @author Pavol Mederly
 * 
 */
public enum TaskBinding {

	/**
	 * TODO
	 */
	TIGHT,

	/**
	 * TODO
	 */
	LOOSE;

	public static TaskBinding fromTaskType(TaskBindingType bindingType) {
		if (bindingType == null) {
			return null;
		}
		if (bindingType == TaskBindingType.LOOSE) {
			return LOOSE;
		}
		if (bindingType == TaskBindingType.TIGHT) {
			return TIGHT;
		}
		throw new IllegalArgumentException("Unknown binding type " + bindingType);
	}

	public TaskBindingType toTaskType() {
		if (this == LOOSE) {
			return TaskBindingType.LOOSE;
		}
		if (this == TIGHT) {
			return TaskBindingType.TIGHT;
		}
		throw new IllegalArgumentException("Unknown binding type " + this);
	}
}
