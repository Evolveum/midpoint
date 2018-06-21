package com.evolveum.midpoint.provisioning.api;

import java.io.Serializable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ResourceEventDescription implements Serializable, DebugDumpable{

	private PrismObject<ShadowType> oldShadow;
	private PrismObject<ShadowType> currentShadow;
	private ObjectDelta delta;
	private String sourceChannel;
//	private PrismObject<ResourceType> resource;


	public PrismObject<ShadowType> getCurrentShadow() {
		return currentShadow;
	}

	public PrismObject<ShadowType> getOldShadow() {
		return oldShadow;
	}

	public ObjectDelta getDelta() {
		return delta;
	}

	public String getSourceChannel() {
		return sourceChannel;
	}
//	public PrismObject<ResourceType> getResource() {
//		return resource;
//	}

	public void setDelta(ObjectDelta delta) {
		this.delta = delta;
	}

	public void setOldShadow(PrismObject<ShadowType> oldShadow) {
		this.oldShadow = oldShadow;
	}

	public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
		this.currentShadow = currentShadow;
	}

	public void setSourceChannel(String sourceChannel) {
		this.sourceChannel = sourceChannel;
	}

	 public boolean isProtected() {
	    	if ((currentShadow != null && ShadowUtil.isProtected(currentShadow))
	    			|| (oldShadow != null && ShadowUtil.isProtected(oldShadow))) {
	    		return true;
	    	}
	    	if (delta != null && delta.isAdd() && ShadowUtil.isProtected(delta.getObjectToAdd())) {
	    		return true;
	    	}
	    	return false;
	    }

	 @Override
		public String toString() {
			return "ResourceEventDescription(delta=" + delta + ", currentShadow="
					+ SchemaDebugUtil.prettyPrint(currentShadow) + ", oldShadow=" + SchemaDebugUtil.prettyPrint(oldShadow) + ", sourceChannel=" + sourceChannel
					+ ")";
		}

		/* (non-Javadoc)
		 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
		 */
		@Override
		public String debugDump() {
			return debugDump(0);
		}

		/* (non-Javadoc)
		 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
		 */
		@Override
		public String debugDump(int indent) {
			StringBuilder sb = new StringBuilder();
			SchemaDebugUtil.indentDebugDump(sb, indent);
			sb.append("ResourceEventDescription(");
			sb.append(sourceChannel);
			sb.append(")\n");

			SchemaDebugUtil.indentDebugDump(sb, indent+1);

			sb.append("Delta:");
			if (delta == null) {
				sb.append(" null");
			} else {
				sb.append(delta.debugDump(indent+2));
			}
			sb.append("\n");
			SchemaDebugUtil.indentDebugDump(sb, indent+1);

			sb.append("oldShadow:");
			if (oldShadow == null) {
				sb.append(" null");
			} else {
				sb.append(oldShadow.debugDump(indent+2));
			}

			sb.append("\n");
			SchemaDebugUtil.indentDebugDump(sb, indent+1);

			sb.append("currentShadow:");
			if (currentShadow == null) {
				sb.append(" null\n");
			} else {
				sb.append("\n");
				sb.append(currentShadow.debugDump(indent+2));
			}

			return sb.toString();
		}
//	public void setResource(PrismObject<ResourceType> resource) {
//		this.resource = resource;
//	}
//
//

	public PrismObject<ShadowType> getShadow() {
		PrismObject<ShadowType> shadow;
		if (getCurrentShadow() != null) {
			shadow = getCurrentShadow();
		} else if (getOldShadow() != null) {
			shadow = getOldShadow();
		} else if (getDelta() != null && getDelta().isAdd()) {
			if (getDelta().getObjectToAdd() == null) {
				throw new IllegalStateException("Found ADD delta, but no object to add was specified.");
			}
			shadow = getDelta().getObjectToAdd();
		} else {
			throw new IllegalStateException("Resource event description does not contain neither old shadow, nor current shadow, nor shadow in delta");
		}
		return shadow;
	}
}
