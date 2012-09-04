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
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * @author semancik
 *
 */
public class LensFocusContext<O extends ObjectType> extends LensElementContext<O> {


	private ObjectDeltaWaves<O> secondaryDeltas = new ObjectDeltaWaves<O>();
	
	private int getWave() {
		return getLensContext().getWave();
	}

	public LensFocusContext(Class<O> objectTypeClass, LensContext<O, ? extends ObjectType> lensContext) {
		super(objectTypeClass, lensContext);
	}

	@Override
	public void setOid(String oid) {
		super.setOid(oid);
		secondaryDeltas.setOid(oid);
	}

	public ObjectDelta<O> getWavePrimaryDelta() throws SchemaException {
    	if (getWave() == 0) {
    		return getPrimaryDelta();
    	} else {
    		return secondaryDeltas.getMergedDeltas(getPrimaryDelta(), getWave());
    	}
    }
	
	@Override
	public ObjectDelta<O> getSecondaryDelta() {
        try {
			return secondaryDeltas.getMergedDeltas();
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Unexpected delta merging problem: "+e.getMessage(), e);
		}
    }
    
    public ObjectDelta<O> getSecondaryDelta(int wave) {
    	return secondaryDeltas.get(wave);
    }
    
    public ObjectDelta<O> getWaveSecondaryDelta() throws SchemaException {
        return secondaryDeltas.get(getWave());
    }

    public void setSecondaryDelta(ObjectDelta<O> secondaryDelta, int wave) {
        this.secondaryDeltas.set(wave, secondaryDelta);
    }
    
    public void setWaveSecondaryDelta(ObjectDelta<O> secondaryDelta) {
        this.secondaryDeltas.set(getWave(), secondaryDelta);
    }
    
    public void swallowToWaveSecondaryDelta(ItemDelta<?> propDelta) throws SchemaException {
		ObjectDelta<O> secondaryDelta = getWaveSecondaryDelta();
		if (secondaryDelta == null) {
            secondaryDelta = new ObjectDelta<O>(getObjectTypeClass(), ChangeType.MODIFY, getPrismContext());        
            secondaryDelta.setOid(getOid());
            setWaveSecondaryDelta(secondaryDelta);
        }
        secondaryDelta.swallow(propDelta);
	}
    
    /**
     * Returns user delta, both primary and secondary (merged together) for a current wave.
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     */
    public ObjectDelta<O> getWaveDelta() throws SchemaException {
    	if (getWave() == 0) {
    		// Primary delta is executed only in the first wave (wave 0)
    		return ObjectDelta.union(getPrimaryDelta(), getWaveSecondaryDelta());
    	} else {
    		return getWaveSecondaryDelta();
    	}
    }
    
    /**
     * Returns delta of user assignments, both primary and secondary (merged together).
     * The returned object is (kind of) immutable. Changing it may do strange things (but most likely the changes will be lost).
     * Only works for UserType now.
     */
    public ContainerDelta<AssignmentType> getAssignmentDelta() throws SchemaException {
    	if (getObjectTypeClass() != UserType.class) {
    		throw new UnsupportedOperationException("Attempt to get assignment deltas from "+getObjectTypeClass());
    	}
        ObjectDelta<UserType> userDelta = (ObjectDelta<UserType>) getDelta();
        if (userDelta == null) {
            return createEmptyAssignmentDelta();
        }
        ContainerDelta<AssignmentType> assignmentDelta = userDelta.findContainerDelta(new PropertyPath(SchemaConstants.C_ASSIGNMENT));
        if (assignmentDelta == null) { 
            return createEmptyAssignmentDelta();
        }
        return assignmentDelta;
    }

    private ContainerDelta<AssignmentType> createEmptyAssignmentDelta() {
        return new ContainerDelta<AssignmentType>(getAssignmentContainerDefinition());
    }
    
    private PrismContainerDefinition<AssignmentType> getAssignmentContainerDefinition() {
		return getObjectDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
	}
    
    @Override
	public void adopt(PrismContext prismContext) throws SchemaException {
		super.adopt(prismContext);
		if (secondaryDeltas != null) {
			secondaryDeltas.adopt(prismContext);
		}
	}
    
    @Override
	public LensFocusContext<O> clone(LensContext lensContext) {
    	LensFocusContext<O> clone = new LensFocusContext<O>(getObjectTypeClass(), lensContext);
    	copyValues(clone, lensContext);
    	return clone;
	}

	protected void copyValues(LensFocusContext<O> clone, LensContext lensContext) {
		super.copyValues(clone, lensContext);
		if (this.secondaryDeltas != null) {
			clone.secondaryDeltas = this.secondaryDeltas.clone();
		}
	}

	@Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String dump() {
        return debugDump(0);
    }
    
    public String dump(boolean showTriples) {
        return debugDump(0, showTriples);
    }

    @Override
    public String debugDump(int indent) {
    	return debugDump(indent, true);
    }
    
    public String debugDump(int indent, boolean showTriples) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpTitle());
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("old"), getObjectOld(), indent+1);

        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("new"), getObjectNew(), indent+1);
        
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, getDebugDumpTitle("primary delta"), getPrimaryDelta(), indent+1);

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent + 1);
        sb.append(getDebugDumpTitle("secondary delta")).append(":");
        if (secondaryDeltas.isEmpty()) {
            sb.append(" empty");
        } else {
            sb.append("\n");
            sb.append(secondaryDeltas.debugDump(indent + 2));
        }

        return sb.toString();
    }

	@Override
	protected String getElementDefaultDesc() {
		return "focus";
	}

	@Override
	public String toString() {
		return "LensFocusContext(" + getObjectTypeClass().getSimpleName() + ":" + getOid() + ")";
	}
}
