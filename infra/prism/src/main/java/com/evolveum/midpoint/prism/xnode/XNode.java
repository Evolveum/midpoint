/*
 * Copyright (c) 2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.xnode;

import java.io.File;
import java.io.Serializable;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Transformer;

/**
 * @author semancik
 *
 */
public abstract class XNode implements DebugDumpable, Visitable, Cloneable, Serializable {
	
	public static final QName KEY_OID = new QName(null, "oid");
	public static final QName KEY_VERSION = new QName(null, "version");
	public static final QName KEY_CONTAINER_ID = new QName(null, "id");
	public static final QName KEY_REFERENCE_OID = new QName(null, "oid");
	public static final QName KEY_REFERENCE_TYPE = new QName(null, "type");
	public static final QName KEY_REFERENCE_RELATION = new QName(null, "relation");
	public static final QName KEY_REFERENCE_DESCRIPTION = new QName(null, "description");
	public static final QName KEY_REFERENCE_FILTER = new QName(null, "filter");
	public static final QName KEY_REFERENCE_RESOLUTION_TIME = new QName(null, "resolutionTime");
	public static final QName KEY_REFERENCE_TARGET_NAME = new QName(null, "targetName");
	public static final QName KEY_REFERENCE_OBJECT = new QName(null, "object");

	public static final QName DUMMY_NAME = new QName(null, "dummy");

	// Common fields
	protected XNode parent;
	
	/**
	 * If set to true that the element came from the explicit type definition
	 * (e.g. xsi:type in XML) on the parsing side; or that it the explicit type
	 * definition should be included on the serialization side.
	 */
	private boolean explicitTypeDeclaration = false;


	// These are set when parsing a file
	private File originFile;
	private String originDescription;
	private int lineNumber;
	
	// These may be detected in parsed file and are also used for serialization
	protected QName typeQName;
	protected QName elementName;							// Filled if and only if this is a member of heterogeneous list.
	protected Integer maxOccurs;

    // a comment that could be stored into formats that support these (e.g. XML or YAML)
    private String comment;

	public XNode getParent() {
		return parent;
	}

	public void setParent(XNode parent) {
		this.parent = parent;
	}

	public File getOriginFile() {
		return originFile;
	}

	public void setOriginFile(File originFile) {
		this.originFile = originFile;
	}

	public String getOriginDescription() {
		return originDescription;
	}

	public void setOriginDescription(String originDescription) {
		this.originDescription = originDescription;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public QName getTypeQName() {
		return typeQName;
	}

	public void setTypeQName(QName typeQName) {
		this.typeQName = typeQName;
	}

	public QName getElementName() {
		return elementName;
	}

	public void setElementName(QName elementName) {
		this.elementName = elementName;
	}

	public Integer getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(Integer maxOccurs) {
		this.maxOccurs = maxOccurs;
	}
	
	public abstract boolean isEmpty();

	public boolean isExplicitTypeDeclaration() {
		return explicitTypeDeclaration;
	}

	public void setExplicitTypeDeclaration(boolean explicitTypeDeclaration) {
		this.explicitTypeDeclaration = explicitTypeDeclaration;
	}
	
	public abstract void accept(Visitor visitor);

    public XNode clone() {
        return cloneTransformKeys(null);
    }

	public XNode cloneTransformKeys(Transformer<QName,QName> keyTransformer) {
		return cloneTransformKeys(keyTransformer, this);
	}

	private static <X extends XNode> X cloneTransformKeys(Transformer<QName,QName> keyTransformer, X xnode) {
		XNode xclone;
		if (xnode == null) {
			return null;
		} else if (xnode instanceof PrimitiveXNode<?>) {
			return (X) ((PrimitiveXNode) xnode).cloneInternal();
		} else if (xnode instanceof MapXNode) {
			MapXNode xmap = (MapXNode)xnode;
			xclone = new MapXNode();
			for (Entry<QName,XNode> entry: xmap.entrySet()) {
				QName key = entry.getKey();
				QName newKey = keyTransformer != null ? keyTransformer.transform(key) : key;
				if (newKey != null) {
					XNode value = entry.getValue();
					XNode newValue = cloneTransformKeys(keyTransformer, value);
					((MapXNode) xclone).put(newKey, newValue);
				}
			}
		} else if (xnode instanceof ListXNode) {
			xclone = new ListXNode();
			for (XNode xsubnode: ((ListXNode)xnode)) {
				((ListXNode) xclone).add(cloneTransformKeys(keyTransformer, xsubnode));
			}
		} else if (xnode instanceof RootXNode) {
			xclone = new RootXNode(((RootXNode) xnode).getRootElementName(),
					cloneTransformKeys(keyTransformer, ((RootXNode) xnode).getSubnode()));
		} else {
			throw new IllegalArgumentException("Unknown xnode "+xnode);
		}
		xclone.copyCommonAttributesFrom(xnode);
		return (X) xclone;
	}

	// filling-in other properties (we skip parent and origin-related things)
	protected void copyCommonAttributesFrom(XNode xnode) {
		explicitTypeDeclaration = xnode.explicitTypeDeclaration;
		setTypeQName(xnode.getTypeQName());
		setComment(xnode.getComment());
		setMaxOccurs(xnode.getMaxOccurs());
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	public abstract String getDesc();
	
	protected String dumpSuffix() {
		StringBuilder sb = new StringBuilder();
		if (elementName != null) {
			sb.append(" element=").append(elementName);
		}
		if (typeQName != null) {
			sb.append(" type=").append(typeQName);
		}
		if (maxOccurs != null) {
			sb.append(" maxOccurs=").append(maxOccurs);
		}
		return sb.toString();
	}

	// overriden in RootXNode
	public RootXNode toRootXNode() {
		return new RootXNode(XNode.DUMMY_NAME, this);
	}

	public boolean isHeterogeneousList() {
		return false;
	}

	public final boolean isSingleEntryMap() {
		return this instanceof MapXNode && ((MapXNode) this).size() == 1;
	}
}
