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
package com.evolveum.midpoint.prism;

import org.w3c.dom.Element;

/**
 * @author semancik
 *
 */
public abstract class PrismValue {
	
	private SourceType type;
    private Objectable source;
    private Item parent;
    protected Element domElement = null;
    
    PrismValue() {
		super();
	}
    
    PrismValue(SourceType type, Objectable source) {
		super();
		this.type = type;
		this.source = source;
	}
    
    PrismValue(SourceType type, Objectable source, Item parent) {
		super();
		this.type = type;
		this.source = source;
		this.parent = parent;
	}

	public void setSource(Objectable source) {
        this.source = source;
    }

    public void setType(SourceType type) {
        this.type = type;
    }
    
    public SourceType getType() {
        return type;
    }

    public Objectable getSource() {
        return source;
    }
    
	public Item getParent() {
		return parent;
	}

	public void setParent(Item parent) {
		this.parent = parent;
	}
	
	public Element asDomElement() {
		if (domElement == null) {
			domElement = createDomElement();
		}
		return domElement;
	}

	//protected abstract Element createDomElement();
	protected Element createDomElement() {return null;};

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrismValue other = (PrismValue) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		// Following != is there by purpose to avoid loops. This check is sufficient here.
		} else if (parent != other.parent)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	
}
