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
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Transformer;

/**
 * @author semancik
 *
 */
public abstract class XNode implements DebugDumpable {
	
	public static final QName KEY_OID = new QName(null, "oid");
	public static final QName KEY_VERSION = new QName(null, "version");
	public static final QName KEY_CONTAINER_ID = new QName(null, "id");
	public static final QName KEY_REFERENCE_OID = new QName(null, "oid");
	public static final QName KEY_REFERENCE_TYPE = new QName(null, "type");
	public static final QName KEY_REFERENCE_RELATION = new QName(null, "relation");
	public static final QName KEY_REFERENCE_DESCRIPTION = new QName(null, "description");
	public static final QName KEY_REFERENCE_FILTER = new QName(null, "filter");
	public static final QName KEY_REFERENCE_OBJECT = new QName(null, "object");

	// Common fields
	private XNode parent;
	
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
	
	// These may be deteceted in parsed file and
	// are also used for serialization
	private QName typeQName;
	private Integer maxOccurs;

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

	public QName getTypeQName() {
		return typeQName;
	}

	public void setTypeQName(QName typeQName) {
		this.typeQName = typeQName;
	}

	public Integer getMaxOccurs() {
		return maxOccurs;
	}

	public void setMaxOccurs(Integer maxOccurs) {
		this.maxOccurs = maxOccurs;
	}

	public boolean isExplicitTypeDeclaration() {
		return explicitTypeDeclaration;
	}

	public void setExplicitTypeDeclaration(boolean explicitTypeDeclaration) {
		this.explicitTypeDeclaration = explicitTypeDeclaration;
	}
	
	public XNode cloneTransformKeys(Transformer<QName> keyTransformer) {
		return cloneTransformKeys(keyTransformer, this);
	}
	
	private <X extends XNode> X cloneTransformKeys(Transformer<QName> keyTransformer, X xnode) {
		if (xnode instanceof PrimitiveXNode<?>) {
			return xnode;
		} else if (xnode instanceof MapXNode) {
			MapXNode xmap = (MapXNode)xnode;
			MapXNode xclone = new MapXNode();
			for (Entry<QName,XNode> entry: xmap.entrySet()) {
				QName key = entry.getKey();
				QName newKey = keyTransformer.transform(key);
				if (newKey != null) {
					XNode value = entry.getValue();
					XNode newValue = cloneTransformKeys(keyTransformer, value);
					xclone.put(newKey, newValue);
				}
			}
			return (X) xclone;
		} else if (xnode instanceof ListXNode) {
			ListXNode xclone = new ListXNode();
			for (XNode xsubnode: ((ListXNode)xnode)) {
				xclone.add(cloneTransformKeys(keyTransformer, xsubnode));
			}
			return (X) xclone;
		} else {
			throw new IllegalArgumentException("Unknown xnode "+xnode);
		}
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

//	@Override
	public String dump() {
		return debugDump();
	}
	
	public abstract String getDesc();
	
	protected String dumpSuffix() {
		StringBuilder sb = new StringBuilder();
		if (typeQName != null) {
			sb.append(" type=").append(typeQName);
		}
		if (maxOccurs != null) {
			sb.append(" maxOccurs=").append(maxOccurs);
		}
		return sb.toString();
	}
}
