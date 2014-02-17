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

package com.evolveum.prism.xml.ns._public.types_2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;


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
 * &lt;complexType name="ItemPathType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;any/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemPathType", propOrder = {
    "content"
})
public class ItemPathType {
	
	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-2", "ItemPathType");

	@XmlTransient
	private ItemPath itemPath;
	
    @XmlMixed
    @XmlAnyElement(lax = true)
    protected List<Object> content;

    public ItemPathType() {
    	// Nothing to do
    }
    
    public ItemPathType(ItemPath itemPath) {
		this.itemPath = itemPath;
	}

	public ItemPath getItemPath() {
		return itemPath;
	}

	/**
     * 
     *                 Defines a type for XPath-like item pointer. It points to a specific part
     *                 of the prism object.
     *             Gets the value of the content property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the content property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getContent().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * {@link String }
     * 
     * 
     */
    public List<Object> getContent() {
        if (content == null) {
            content = new List<Object>() {

				@Override
				public int size() {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public boolean isEmpty() {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean contains(Object o) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public Iterator<Object> iterator() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Object[] toArray() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public <T> T[] toArray(T[] a) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public boolean add(Object e) {
					XPathHolder holder = new XPathHolder(e.toString());
					itemPath = holder.toItemPath();
					return true;
//					throw new IllegalArgumentException("PATH ADD: "+e+" "+e.getClass());
				}

				@Override
				public boolean remove(Object o) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean containsAll(Collection<?> c) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean addAll(Collection<? extends Object> c) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean addAll(int index, Collection<? extends Object> c) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean removeAll(Collection<?> c) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public boolean retainAll(Collection<?> c) {
					// TODO Auto-generated method stub
					return false;
				}

				@Override
				public void clear() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public Object get(int index) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public Object set(int index, Object element) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public void add(int index, Object element) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public Object remove(int index) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public int indexOf(Object o) {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public int lastIndexOf(Object o) {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public ListIterator<Object> listIterator() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public ListIterator<Object> listIterator(int index) {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public List<Object> subList(int fromIndex, int toIndex) {
					// TODO Auto-generated method stub
					return null;
				}
            	
            };
        }
        return this.content;
    }

    public ItemPathType clone() {
    	ItemPathType clone = new ItemPathType();
    	// TODO
    	return clone;
    }
    
}
