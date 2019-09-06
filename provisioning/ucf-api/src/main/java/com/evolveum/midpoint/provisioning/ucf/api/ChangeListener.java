/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

/**
 * Processes changes encountered on a resource
 */
public interface ChangeListener {

	/**
	 * Called when the connector learns about a resource change.
	 * @param change The change.
	 * @return true if the change was successfully processed and can be acknowledged on the resource;
	 * false (or a runtime exception) should be returned otherwise
	 *
	 * TODO add operation result here? Beware of simultaneous firing of changes. OperationResult is not thread-safe yet.
	 */
	boolean onChange(Change change);
}
