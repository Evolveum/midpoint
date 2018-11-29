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

package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPathImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *                 Defines a type for XPath-like item pointer. It points to a specific part
 *                 of the prism object.
 *
 *
 * <p>Java class for ItemPathType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="ItemPathType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;any/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */

// TODO it is questionable whether to treat ItemPathType as XmlType any more (similar to RawType)
//   however, unlike RawType, ItemPathType is still present in externally-visible schemas (XSD, WSDL)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemPathType")
public class ItemPathType implements Serializable, Equals, Cloneable {

	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "ItemPathType");

	@XmlTransient
	private ItemPath itemPath;

    // if possible, use one of the content-filling constructors instead
    public ItemPathType() {
    }

    public ItemPathType(ItemPath itemPath) {
		this.itemPath = itemPath;
	}

    public ItemPathType(String itemPath) {
        this.itemPath = ItemPath.parseFromString(itemPath);
    }

    @NotNull
    @Contract(pure = true)
	public ItemPath getItemPath() {
		return itemPath != null ? itemPath : UniformItemPath.EMPTY_PATH;
	}

	public UniformItemPath getUniformItemPath() {
		return UniformItemPathImpl.fromItemPath(getItemPath());
	}

	public void setItemPath(ItemPath itemPath){
		this.itemPath = itemPath;
	}

    public ItemPathType clone() {
    	return new ItemPathType(itemPath);
    }

    /**
     * More strict version of ItemPathType comparison. Does not use any normalization
     * nor approximate matching QNames via QNameUtil.match.
     *
     * For example, it detects a change from xyz:name to name and vice versa
     * when editing via debug pages (MID-1969)
     *
     * For semantic-level comparison, please use equivalent(..) method.
     */

    @Override
    public boolean equals(Object obj) {
    	final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
    	return equals(null, null, obj, strategy);
    }

    public boolean equivalent(Object other) {
        if (!(other instanceof ItemPathType)) {
            return false;
        }
        ItemPath thisPath = getItemPath();
        ItemPath otherPath = ((ItemPathType) other).getItemPath();

        return thisPath.equivalent(otherPath);
    }

	@Override
	public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object that,
			EqualsStrategy equalsStrategy) {

		if (!(that instanceof ItemPathType)){
    		return false;
    	}

    	ItemPathType other = (ItemPathType) that;

    	ItemPath thisPath = getItemPath();
    	ItemPath otherPath = other.getItemPath();

        return thisPath.equals(otherPath);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemPath == null) ? 0 : itemPath.hashCode());
		return result;
	}

    @Override
    public String toString() {
        return getItemPath().toString();
    }

	public static List<UniformItemPath> toItemPathList(List<ItemPathType> list) {
    	return list.stream().map(pt -> pt.getUniformItemPath()).collect(Collectors.toList());
	}

	public static ItemPathType parseFromElement(Element element) {
    	return new ItemPathType(ItemPathHolder.parseFromElement(element));
	}

	public Element serializeToElement(QName elementName, Document ownerDocument) {
		return ItemPathHolder.serializeToElement(getUniformItemPath(), elementName, ownerDocument);
	}
}
