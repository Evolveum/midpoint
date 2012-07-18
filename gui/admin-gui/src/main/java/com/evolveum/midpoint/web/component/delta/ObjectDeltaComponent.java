/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.delta;

import java.io.Serializable;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

/**
 * @author mserbak
 */
public class ObjectDeltaComponent implements Serializable {
	private PrismObject oldObject;
	private ObjectDelta newDelta;
	
	public ObjectDeltaComponent(PrismObject oldObject, ObjectDelta newDelta) {
		Validate.notNull(oldObject, "OldObject must not be null.");
		Validate.notNull(newDelta, "NewDelta must not be null.");
		this.oldObject = oldObject;
		this.newDelta = newDelta;
	}

	public PrismObject getOldObject() {
		return oldObject;
	}

	public ObjectDelta getNewDelta() {
		return newDelta;
	}
}
