/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.JaxbVisitable;
import com.evolveum.midpoint.prism.JaxbVisitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

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
public class ItemPathType implements Serializable, Equals, Cloneable, JaxbVisitable {

    public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "ItemPathType");

    @XmlTransient
    private ItemPath itemPath;

    // if possible, use one of the content-filling constructors instead
    public ItemPathType() {
    }

    public ItemPathType(ItemPath itemPath) {
        this.itemPath = itemPath;
    }

    @NotNull
    @Contract(pure = true)
    public ItemPath getItemPath() {
        return itemPath != null ? itemPath : ItemPath.EMPTY_PATH;
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

    public static List<? extends ItemPath> toItemPathList(List<ItemPathType> list) {
        return list.stream().map(pt -> pt.getItemPath()).collect(Collectors.toList());
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        visitor.visit(this);
    }
}
