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
public class ObjectDeltaPanel implements Serializable {
	private PrismObject oldDelta;
	private ObjectDelta newDelta;

	public ObjectDeltaPanel(PrismObject oldDelta) {
		Validate.notNull(oldDelta, "OldObject must not be null.");
		this.oldDelta = oldDelta;
	}
	
	public PrismObject getOldDelta() {
		return oldDelta;
	}

	public ObjectDelta getNewDelta() {
		return newDelta;
	}

	public void setDelta(ObjectDelta newDelta) {
		this.newDelta =newDelta;
	}

}
