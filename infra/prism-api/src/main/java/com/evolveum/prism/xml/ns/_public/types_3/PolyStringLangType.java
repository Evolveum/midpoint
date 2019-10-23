/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.prism.xml.ns._public.types_3;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;


/**
 * This is NOT a generated class.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PolyStringLangType", propOrder = {
    "any"
})
public class PolyStringLangType implements Serializable {

    public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "PolyStringLangType");

    @XmlAnyElement
    protected List<Element> any = new CustomList();

    @XmlTransient
    @NotNull
    protected Map<String,String> lang = new HashMap<>();

    @NotNull
    public Map<String,String> getLang() {
        return lang;
    }

    public void setLang(@Nullable Map<String,String> lang) {
        this.lang = lang != null ? lang : new HashMap<>();
    }

    public List<Element> getAny() {
        return this.any;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + lang.hashCode();
        return result;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return equals(obj, false);
    }

    public boolean equals(Object obj, boolean isLiteral) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PolyStringLangType other = (PolyStringLangType) obj;
        if (!lang.equals(other.getLang()))
            return false;
        return true;
    }

    public PolyStringLangType clone() {
        PolyStringLangType clone = new PolyStringLangType();
        //clone.schema = (Element) schema.cloneNode(true);
        clone.lang = new HashMap<>(lang);
        return clone;
    }

    class CustomList implements Serializable, List<Element> {

        @Override
        public int size() {
            return lang.size();
        }

        @Override
        public boolean isEmpty() {
            return lang.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            // Not efficient ... but it should work
            for (Element e: this) {
                if (o.equals(e)) {
                    return true;
                }
            }
            return false;
        }

        @NotNull
        @Override
        public CustomIterator iterator() {
            return new CustomIterator(lang.entrySet().iterator());
        }

        @NotNull
        @Override
        public Object[] toArray() {
            Object[] a = new Object[lang.size()];
            CustomIterator iterator = iterator();
            for (int i = 0; i < lang.size(); i++) {
                a[i] = iterator.next();
            }
            return a;
        }

        @NotNull
        @Override
        public <T> T[] toArray(@NotNull T[] a) {
            //noinspection unchecked
            return (T[]) toArray();
        }

        // Note: we don't stick to the contract for List.add() here. (We should always add the value, and always return true.)
        @Override
        public boolean add(Element e) {
            String key = e.getLocalName();
            String value = e.getTextContent();
            String previous = lang.put(key, value);
            return !MiscUtil.equals(value, previous);
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Element) {
                String key = ((Element)o).getLocalName();
                String previous = lang.remove(key);
                return previous != null;
            }
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object e: c) {
                if (!contains(e)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Element> c) {
            boolean changed = false;
            for (Element e: c) {
                if (add(e)) {
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public boolean addAll(int index, Collection<? extends Element> c) {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

        @Override
        public void clear() {
            lang.clear();
        }

        @Override
        public Element get(int index) {
            // Poor implementation. But we do not really need this anyway.
            String key = lang.keySet().stream().sorted().skip(index).findFirst().get();
            String value = lang.get(key);
            Element e = DOMUtil.createElement(new QName(COMPLEX_TYPE.getNamespaceURI(), key));
            e.setTextContent(value);
            return e;
        }

        @Override
        public Element set(int index, Element element) {
            add(element);
            return element;
        }

        @Override
        public void add(int index, Element element) {
            add(element);
        }

        @Override
        public Element remove(int index) {
            // Poor implementation. But we do not really need this anyway.
            String key = lang.keySet().stream().sorted().skip(index).findFirst().get();
            String previous = lang.remove(key);
            Element e = DOMUtil.createElement(new QName(COMPLEX_TYPE.getNamespaceURI(), key));
            e.setTextContent(previous);
            return e;
        }

        @Override
        public int indexOf(Object o) {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

        @NotNull
        @Override
        public ListIterator<Element> listIterator() {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

        @NotNull
        @Override
        public ListIterator<Element> listIterator(int index) {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

        @NotNull
        @Override
        public List<Element> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException("Laziness is one of the greatest virtues of a programmer");
        }

    }

    class CustomIterator implements Serializable, Iterator<Element> {

        // Very lazy implementation: TODO: cleanup if needed

        Iterator<Entry<String, String>> langIterator;

        CustomIterator(Iterator<Entry<String, String>> langIterator) {
            this.langIterator = langIterator;
        }

        @Override
        public boolean hasNext() {
            return langIterator.hasNext();
        }

        @Override
        public Element next() {
            Entry<String, String> next = langIterator.next();
            Element e = DOMUtil.createElement(new QName(COMPLEX_TYPE.getNamespaceURI(), next.getKey()));
            e.setTextContent(next.getValue());
            return e;
        }

        @Override
        public void remove() {
            langIterator.remove();
        }
    }

    public boolean isEmpty() {
        return lang.isEmpty();
    }
}
