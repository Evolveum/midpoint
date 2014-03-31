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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;

import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;


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
public class ItemPathType implements Serializable, Equals, Cloneable {
	
	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-2", "ItemPathType");
	
	public static final QName F_PATH = new QName("http://prism.evolveum.com/xml/ns/public/types-2", "path");

	@XmlTransient
	private ItemPath itemPath;
	
	@XmlElementRef(name = "path", namespace = "http://prism.evolveum.com/xml/ns/public/types-2", type = JAXBElement.class)
    @XmlMixed
    @XmlAnyElement(lax = true)
    protected List<Object> content;

    public ItemPathType() {
    	// Nothing to do
    	if (content == null){
    		content = new ContentList();
    	}
//    	System.out.println("content after: " + content);
    }
    
    public ItemPathType(ItemPath itemPath) {
		this.itemPath = itemPath;
	}

	public ItemPath getItemPath() {
		if (itemPath == null){
			getContent();
		}
		return itemPath;
	}
	
	public void setItemPath(ItemPath itemPath){
		this.itemPath = itemPath;
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
        if (!(content instanceof ContentList)) {
            content = new ContentList();
        }
        return this.content;
    }

    public ItemPathType clone() {
    	ItemPathType clone = new ItemPathType();
        if (itemPath != null) {
    	    clone.setItemPath(itemPath.clone());
        }
    	for (Object o : getContent()){
    		clone.getContent().add(o);
    	}
//    	clone.getContent().addAll(content);
    	return clone;
    }
    
    @Override
    public boolean equals(Object obj) {
    	final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
    	return equals(null, null, obj, strategy);
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
    	
    	if (thisPath != null){
    		return thisPath.equivalent(otherPath);
    	}
    	
    	List<Object> thsContent = getContent();
    	List<Object> othContent = other.getContent();
    	
    	return equalsStrategy.equals(thisLocator, thatLocator, thsContent, othContent);
    	
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((itemPath == null) ? 0 : itemPath.hashCode());
		return result;
	}
	
	class ContentList implements List<Object>, Serializable {

			@Override
			public int size() {
				if (itemPath != null){
					return 1;
				}
				return 0;
			}

			@Override
			public boolean isEmpty() {
				return itemPath == null;
//				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public boolean contains(Object o) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public Iterator<Object> iterator() {
				return new Iterator<Object>() {
					int i = 0;
					@Override
					public boolean hasNext() {
						return i==0;
					}

					@Override
					public Object next() {
						if (i== 0){
							i++;
							//TODO it should be itemPathType not string..
							XPathHolder holder = new XPathHolder(itemPath);	
//							return holder.getXPathWithDeclarations();
							return new JAXBElement<String>(F_PATH, String.class, holder.getXPath());
//							return itemPath;
						} 
						return null;
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException("nto supported yet");
					}
					
				};
//				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public Object[] toArray() {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public <T> T[] toArray(T[] a) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public boolean add(Object e) {
				if (e instanceof String){
					XPathHolder holder = new XPathHolder((String) e);
					itemPath = holder.toItemPath();
					return true;
				} else if (e instanceof QName){
					itemPath = new ItemPath((QName) e);
					return true;
				} else if (e instanceof JAXBElement){
					JAXBElement jaxb = (JAXBElement) e;
					// TODO: after refactoring next method, change to item path type
					String s = (String)((JAXBElement) e).getValue();
					XPathHolder holder = new XPathHolder(s);
					itemPath = holder.toItemPath();
					return true;
				}
				throw new IllegalArgumentException("PATH ADD: "+e+" "+e.getClass());
			}

			@Override
			public boolean remove(Object o) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public boolean containsAll(Collection<?> c) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public boolean addAll(Collection<? extends Object> c) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public boolean addAll(int index, Collection<? extends Object> c) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public boolean removeAll(Collection<?> c) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public boolean retainAll(Collection<?> c) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public void clear() {
				itemPath = null;
			}

			@Override
			public Object get(int index) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public Object set(int index, Object element) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public void add(int index, Object element) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public Object remove(int index) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public int indexOf(Object o) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public int lastIndexOf(Object o) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public ListIterator<Object> listIterator() {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public ListIterator<Object> listIterator(int index) {
				throw new UnsupportedOperationException("nto supported yet");
			}

			@Override
			public List<Object> subList(int fromIndex, int toIndex) {
				throw new UnsupportedOperationException("nto supported yet");
			}
        	
        
	}

    @Override
    public String toString() {
        return "ItemPathType{" +
                "itemPath=" + getItemPath() +
                '}';
    }

    //	@Override
//	public int hashCode(ObjectLocator locator, HashCodeStrategy hashCodeStrategy) {
//		final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
////		hashCodeStrategy.
//		return 0;
//	}
    
}
