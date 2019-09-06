/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskBindingType;

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
