/*
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensObjectDeltaOperationType;

/**
 * @author semancik
 *
 */
public class LensObjectDeltaOperation<T extends ObjectType> extends ObjectDeltaOperation<T> implements Serializable {

	private boolean audited = false;
	
	public LensObjectDeltaOperation() {
		super();
	}

	public LensObjectDeltaOperation(ObjectDelta<T> objectDelta) {
		super(objectDelta);
	}

	public boolean isAudited() {
		return audited;
	}

	public void setAudited(boolean audited) {
		this.audited = audited;
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append(super.debugDump(indent));
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "audited", audited, indent + 1);
		return sb.toString();
	}
	
	@Override
	protected String getDebugDumpClassName() {
        return "LensObjectDeltaOperation";
    }

    public LensObjectDeltaOperationType toLensObjectDeltaOperationType() throws SchemaException {
        LensObjectDeltaOperationType retval = new LensObjectDeltaOperationType();
        retval.setObjectDeltaOperation(DeltaConvertor.toObjectDeltaOperationType(this));
        retval.setAudited(audited);
        return retval;
    }

    public static LensObjectDeltaOperation fromLensObjectDeltaOperationType(LensObjectDeltaOperationType jaxb, PrismContext prismContext) throws SchemaException {

        ObjectDeltaOperation odo = DeltaConvertor.createObjectDeltaOperation(jaxb.getObjectDeltaOperation(), prismContext);
        LensObjectDeltaOperation retval = new LensObjectDeltaOperation();
        retval.setObjectDelta(odo.getObjectDelta());
        retval.setExecutionResult(odo.getExecutionResult());
        retval.setAudited(jaxb.isAudited());
        return retval;
    }
}
