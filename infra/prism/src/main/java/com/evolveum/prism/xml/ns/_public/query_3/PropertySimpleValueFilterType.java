
package com.evolveum.prism.xml.ns._public.query_3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.Raw;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import com.evolveum.midpoint.util.xml.DomAwareHashCodeStrategy;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;
import org.w3c.dom.Element;


/**
 * <p>Java class for PropertySimpleValueFilterType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PropertySimpleValueFilterType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://prism.evolveum.com/xml/ns/public/query-2}FilterType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="property" type="{http://prism.evolveum.com/xml/ns/public/types-3}XPathType"/&gt;
 *         &lt;choice&gt;
 *           &lt;element name="value" type="{http://www.w3.org/2001/XMLSchema}anyType"/&gt;
 *           &lt;any namespace='##other'/&gt;
 *         &lt;/choice&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PropertySimpleValueFilterType", propOrder = {
    "property",
    "value",
    "any"
})
public class PropertySimpleValueFilterType
    extends FilterClauseType
    implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    @XmlAnyElement
    protected Element property;
    @Raw
    protected Object value;
    @XmlAnyElement(lax = true)
    protected Object any;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "PropertySimpleValueFilterType");
    public final static QName F_VALUE = new QName(PrismConstants.NS_QUERY, "value");

    /**
     * Creates a new {@code PropertySimpleValueFilterType} instance.
     *
     */
    public PropertySimpleValueFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code PropertySimpleValueFilterType} instance by deeply copying a given {@code PropertySimpleValueFilterType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public PropertySimpleValueFilterType(final PropertySimpleValueFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super(o);
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'PropertySimpleValueFilterType' from 'null'.");
        }
        // CWildcardTypeInfo: org.w3c.dom.Element
        this.property = ((o.property == null)?null:((o.getProperty() == null)?null:((Element) o.getProperty().cloneNode(true))));
        // CBuiltinLeafInfo: java.lang.Object
        this.value = ((o.value == null)?null:copyOf(o.getValue()));
        // CBuiltinLeafInfo: java.lang.Object
        this.any = ((o.any == null)?null:copyOf(o.getAny()));
    }

    /**
     * Gets the value of the property property.
     *
     * @return
     *     possible object is
     *     {@link Element }
     *
     */
    public Element getProperty() {
        return property;
    }

    /**
     * Sets the value of the property property.
     *
     * @param value
     *     allowed object is
     *     {@link Element }
     *
     */
    public void setProperty(Element value) {
        this.property = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Gets the value of the any property.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getAny() {
        return any;
    }

    /**
     * Sets the value of the any property.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setAny(Object value) {
        this.any = value;
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     *
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = super.hashCode(locator, strategy);
        {
            Element theProperty;
            theProperty = this.getProperty();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "property", theProperty), currentHashCode, theProperty);
        }
        {
            Object theValue;
            theValue = this.getValue();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "value", theValue), currentHashCode, theValue);
        }
        {
            Object theAny;
            theAny = this.getAny();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "any", theAny), currentHashCode, theAny);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof PropertySimpleValueFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!super.equals(thisLocator, thatLocator, object, strategy)) {
            return false;
        }
        final PropertySimpleValueFilterType that = ((PropertySimpleValueFilterType) object);
        {
            Element lhsProperty;
            lhsProperty = this.getProperty();
            Element rhsProperty;
            rhsProperty = that.getProperty();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "property", lhsProperty), LocatorUtils.property(thatLocator, "property", rhsProperty), lhsProperty, rhsProperty)) {
                return false;
            }
        }
        {
            Object lhsValue;
            lhsValue = this.getValue();
            Object rhsValue;
            rhsValue = that.getValue();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "value", lhsValue), LocatorUtils.property(thatLocator, "value", rhsValue), lhsValue, rhsValue)) {
                return false;
            }
        }
        {
            Object lhsAny;
            lhsAny = this.getAny();
            Object rhsAny;
            rhsAny = that.getAny();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "any", lhsAny), LocatorUtils.property(thatLocator, "any", rhsAny), lhsAny, rhsAny)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object object) {
        final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
        return equals(null, null, object, strategy);
    }

    /**
     * Creates and returns a deep copy of a given object.
     *
     * @param o
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code o} or {@code null} if {@code o} is {@code null}.
     */
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
        } catch (URISyntaxException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        } catch (MalformedURLException e) {
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
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static boolean[] copyOf(final boolean[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final boolean[] copy = ((boolean[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static byte[] copyOf(final byte[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final byte[] copy = ((byte[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static char[] copyOf(final char[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final char[] copy = ((char[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static double[] copyOf(final double[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final double[] copy = ((double[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static float[] copyOf(final float[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final float[] copy = ((float[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static int[] copyOf(final int[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final int[] copy = ((int[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static long[] copyOf(final long[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final long[] copy = ((long[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given array.
     *
     * @param array
     *     The array to copy or {@code null}.
     * @return
     *     A deep copy of {@code array} or {@code null} if {@code array} is {@code null}.
     */
    private static short[] copyOf(final short[] array) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (array!= null) {
            final short[] copy = ((short[]) Array.newInstance(array.getClass().getComponentType(), array.length));
            System.arraycopy(array, 0, copy, 0, array.length);
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given {@code JAXBElement} instance.
     *
     * @param element
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code element} or {@code null} if {@code element} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static JAXBElement copyOf(final JAXBElement element) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (element!= null) {
            final JAXBElement copy = new JAXBElement(element.getName(), element.getDeclaredType(), element.getScope(), element.getValue());
            copy.setNil(element.isNil());
            copy.setValue(copyOf(copy.getValue()));
            return copy;
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of a given {@code Serializable}.
     *
     * @param serializable
     *     The instance to copy or {@code null}.
     * @return
     *     A deep copy of {@code serializable} or {@code null} if {@code serializable} is {@code null}.
     */
    private static Serializable copyOf(final Serializable serializable) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if (serializable!= null) {
            try {
                final ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
                final ObjectOutputStream out = new ObjectOutputStream(byteArrayOutput);
                out.writeObject(serializable);
                out.close();
                final ByteArrayInputStream byteArrayInput = new ByteArrayInputStream(byteArrayOutput.toByteArray());
                final ObjectInputStream in = new ObjectInputStream(byteArrayInput);
                final Serializable copy = ((Serializable) in.readObject());
                in.close();
                return copy;
            } catch (SecurityException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (ClassNotFoundException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (InvalidClassException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (NotSerializableException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (StreamCorruptedException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (OptionalDataException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            } catch (IOException e) {
                throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ serializable)+"'.")).initCause(e));
            }
        }
        return null;
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     *
     * @return
     *     A deep copy of this object.
     */
    @Override
    public PropertySimpleValueFilterType clone() {
        {
            // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
            final PropertySimpleValueFilterType clone = ((PropertySimpleValueFilterType) super.clone());
            // CWildcardTypeInfo: org.w3c.dom.Element
            clone.property = ((this.property == null)?null:((this.getProperty() == null)?null:((Element) this.getProperty().cloneNode(true))));
            // CBuiltinLeafInfo: java.lang.Object
            clone.value = ((this.value == null)?null:copyOf(this.getValue()));
            // CBuiltinLeafInfo: java.lang.Object
            clone.any = ((this.any == null)?null:copyOf(this.getAny()));
            return clone;
        }
    }

}
