/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */


package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.JaxbVisitable;
import com.evolveum.midpoint.prism.JaxbVisitor;
import com.evolveum.midpoint.prism.Raw;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.MiscUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;


/**
 *
 * THIS IS NOT A GENERATED CLASS. It was heavily modified after it was originally generated long time ago.
 *
 *                 Describe a change to a single attribute.
 *                 In this case the path expression used in the "property"
 *                 attribute must select exactly one property.
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ItemDeltaType", propOrder = {
    "modificationType",
    "path",
    "value",
    "estimatedOldValue"
})
public class ItemDeltaType implements Serializable, Cloneable, JaxbVisitable {

	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "ItemDeltaType");
	public static final QName F_PATH = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "path");
	public static final QName F_VALUE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "value");

    @XmlElement(required = true)
    protected ModificationTypeType modificationType;
//    @XmlAnyElement
    protected ItemPathType path;

    @XmlElement(required = true)
    @Raw
    @NotNull
    protected final List<Object> value = new ArrayList<>();           // Object is here to show as xsd:anyType in WSDL

    @XmlElement(required = true)
    @Raw
    @NotNull
    protected final List<Object> estimatedOldValue = new ArrayList<>();           // Object is here to show as xsd:anyType in WSDL

    /**
     * Gets the value of the modificationType property.
     *
     * @return
     *     possible object is
     *     {@link ModificationTypeType }
     *
     */
    public ModificationTypeType getModificationType() {
        return modificationType;
    }

    /**
     * Sets the value of the modificationType property.
     *
     * @param value
     *     allowed object is
     *     {@link ModificationTypeType }
     *
     */
    public void setModificationType(ModificationTypeType value) {
        this.modificationType = value;
    }

    /**
     * Gets the value of the path property.
     *
     * @return
     *     possible object is
     *     {@link Element }
     *
     */
    public ItemPathType getPath() {
        return path;
    }

    /**
     * Sets the value of the path property.
     *
     * @param value
     *     allowed object is
     *     {@link Element }
     *
     */
    public void setPath(ItemPathType value) {
        this.path = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return
     *     possible object is
     *     {@link ItemDeltaType.Value }
     *
     */
    @NotNull
    public List<RawType> getValue() {
        return (List<RawType>) (List) value;        // brutal hack
    }

//    public List<Object> getAnyValues(){
//    	List<Object> vals = new ArrayList<Object>();
//    	for (Object raw : value){
//    		vals.addAll(((RawType) raw).getContent());
//    	}
//    	return vals;
//    }

//    /**
//     * Sets the value of the value property.
//     *
//     * @param value
//     *     allowed object is
//     *     {@link ItemDeltaType.Value }
//     *
//     */
//    public void setValue(List<RawType> value) {
//        this.value = value;
//    }


	@NotNull
    public List<RawType> getEstimatedOldValue() {
        return (List<RawType>) (List) estimatedOldValue;        // brutal hack
    }

    @Override
    public ItemDeltaType clone() {
        ItemDeltaType clone = new ItemDeltaType();
        clone.setModificationType(getModificationType());
        clone.setPath(getPath());  //TODO clone path
		// Proper cloning of inner raw values.
        List<RawType> cloneValue = clone.getValue();
		for (RawType rawType : getValue()) {
			cloneValue.add(rawType.clone());
		}
//        delta.setValue(value != null ? value.clone() : null);
        clone.getEstimatedOldValue().addAll(getEstimatedOldValue());
        return clone;
    }

    /**
     * Copies all values of property {@code Content} deeply.
     *
     * @param source
     *     The source to copy from.
     * @param target
     *     The target to copy {@code source} to.
     * @throws NullPointerException
     *     if {@code target} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static void copyContent(final List<Object> source, final List<Object> target) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if ((source!= null)&&(!source.isEmpty())) {
            for (final Iterator<?> it = source.iterator(); it.hasNext(); ) {
                final Object next = it.next();
                if (next instanceof JAXBElement) {
                    // Referenced elements without classes.
                    if (((JAXBElement) next).getValue() instanceof String) {
                        // CElementInfo: javax.xml.bind.JAXBElement<java.lang.String>
                        target.add(copyOfStringElement(((JAXBElement) next)));
                        continue;
                    }
                }
                if (next instanceof String) {
                    // CBuiltinLeafInfo: java.lang.String
                    target.add(((String) next));
                    continue;
                }
                if (next instanceof Object) {
                    // CBuiltinLeafInfo: java.lang.Object
                    target.add(copyOf(((Object) next)));
                    continue;
                }
                // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                throw new AssertionError((("Unexpected instance '"+ next)+"' for property 'Content' of class 'com.evolveum.prism.xml.ns._public.types_3.PolyStringType'."));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object copyOf(final Object o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        try {
            if (o!= null) {
                if (o.getClass().isPrimitive()) {
                    return o;
                }
                if (o.getClass().isArray()) {
                    return copyOfArray(o);
                }
                // Immutable types.
                if (o instanceof Boolean) {
                    return o;
                }
                if (o instanceof Byte) {
                    return o;
                }
                if (o instanceof Character) {
                    return o;
                }
                if (o instanceof Double) {
                    return o;
                }
                if (o instanceof Enum) {
                    return o;
                }
                if (o instanceof Float) {
                    return o;
                }
                if (o instanceof Integer) {
                    return o;
                }
                if (o instanceof Long) {
                    return o;
                }
                if (o instanceof Short) {
                    return o;
                }
                if (o instanceof String) {
                    return o;
                }
                if (o instanceof BigDecimal) {
                    return o;
                }
                if (o instanceof BigInteger) {
                    return o;
                }
                if (o instanceof UUID) {
                    return o;
                }
                if (o instanceof QName) {
                    return o;
                }
                if (o instanceof Duration) {
                    return o;
                }
                if (o instanceof Currency) {
                    return o;
                }
                // String based types.
                if (o instanceof File) {
                    return new File(o.toString());
                }
                if (o instanceof URI) {
                    return new URI(o.toString());
                }
                if (o instanceof URL) {
                    return new URL(o.toString());
                }
                if (o instanceof MimeType) {
                    return new MimeType(o.toString());
                }
                // Cloneable types.
                if (o instanceof XMLGregorianCalendar) {
                    return ((XMLGregorianCalendar) o).clone();
                }
                if (o instanceof Date) {
                    return ((Date) o).clone();
                }
                if (o instanceof Calendar) {
                    return ((Calendar) o).clone();
                }
                if (o instanceof TimeZone) {
                    return ((TimeZone) o).clone();
                }
                if (o instanceof Locale) {
                    return ((Locale) o).clone();
                }
                if (o instanceof Element) {
                    return ((Element)((Element) o).cloneNode(true));
                }
                if (o instanceof JAXBElement) {
                    return copyOf(((JAXBElement) o));
                }
                try {
                    return o.getClass().getMethod("clone", ((Class[]) null)).invoke(o, ((Object[]) null));
                } catch (NoSuchMethodException e) {
                    if (o instanceof Serializable) {
                        return copyOf(((Serializable) o));
                    }
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (IllegalAccessException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (InvocationTargetException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (SecurityException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (IllegalArgumentException e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                } catch (ExceptionInInitializerError e) {
                    // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                    throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
                }
            }
            return null;
        } catch (MalformedURLException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        } catch (URISyntaxException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        } catch (MimeTypeParseException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        }
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static Object copyOfArray(final Object array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            if (array.getClass() == boolean[].class) {
                return copyOf(((boolean[]) array));
            }
            if (array.getClass() == byte[].class) {
                return copyOf(((byte[]) array));
            }
            if (array.getClass() == char[].class) {
                return copyOf(((char[]) array));
            }
            if (array.getClass() == double[].class) {
                return copyOf(((double[]) array));
            }
            if (array.getClass() == float[].class) {
                return copyOf(((float[]) array));
            }
            if (array.getClass() == int[].class) {
                return copyOf(((int[]) array));
            }
            if (array.getClass() == long[].class) {
                return copyOf(((long[]) array));
            }
            if (array.getClass() == short[].class) {
                return copyOf(((short[]) array));
            }
            final int len = Array.getLength(array);
            final Object copy = Array.newInstance(array.getClass().getComponentType(), len);
            for (int i = (len- 1); (i >= 0); i--) {
                Array.set(copy, i, copyOf(Array.get(array, i)));
            }
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given {@code javax.xml.bind.JAXBElement<java.lang.String>} instance.
     *
     * @param e
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code e} or {@code null} if {@code e} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement<String> copyOfStringElement(final JAXBElement<String> e) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (e!= null) {
            final JAXBElement<String> copy = new JAXBElement<>(e.getName(), e.getDeclaredType(), e.getScope(), e.getValue());
            copy.setNil(e.isNil());
            // CBuiltinLeafInfo: java.lang.String
            copy.setValue(((String) copy.getValue()));
            return copy;
        }
        return null;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((modificationType == null) ? 0 : modificationType.hashCode());
		result = prime * result + value.hashCode();
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
		ItemDeltaType other = (ItemDeltaType) obj;
		if (modificationType != other.modificationType)
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		// use of isEmpty is a hack: should be fixed soon!
		if (!MiscUtil.unorderedCollectionEquals(value, other.value))
			return false;
		if (!MiscUtil.unorderedCollectionEquals(estimatedOldValue, other.estimatedOldValue))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ItemDeltaType(modificationType=" + modificationType
				+ ", path=" + path + ", value=" + value + ", estimatedOldValue=" + estimatedOldValue + ")";
	}

	@Override
	public void accept(JaxbVisitor visitor) {
		visitor.visit(this);
		if (path != null) {
			path.accept(visitor);
		}
		for (Object o : value) {
			if (o instanceof JaxbVisitable) {
				((JaxbVisitable) o).accept(visitor);
			}
		}
		for (Object o : estimatedOldValue) {
			if (o instanceof JaxbVisitable) {
				((JaxbVisitable) o).accept(visitor);
			}
		}
	}
}
