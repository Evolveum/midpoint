
package com.evolveum.prism.xml.ns._public.query_2;

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
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xjc.PrismForJAXBUtil;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
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
 * 
 *                 TODO
 *             
 * 
 * <p>Java class for QueryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="condition" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element ref="{http://prism.evolveum.com/xml/ns/public/query-2}filter"/>
 *         &lt;element name="paging" type="{http://prism.evolveum.com/xml/ns/public/query-2}PagingType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryType", propOrder = {
    "description",
    "condition",
    "filter",
    "paging"
})
public class QueryType implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    protected String description;
    protected Object condition;
    
    @XmlAnyElement
    protected Element filter;
    @XmlTransient
    protected MapXNode xfilter;
    
    protected PagingType paging;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "QueryType");
    public final static QName F_DESCRIPTION = new QName(PrismConstants.NS_QUERY, "description");
    public final static QName F_FILTER = new QName(PrismConstants.NS_QUERY, "filter");
    public final static QName F_CONDITION = new QName(PrismConstants.NS_QUERY, "condition");
    public final static QName F_PAGING = new QName(PrismConstants.NS_QUERY, "paging");

    /**
     * Creates a new {@code QueryType} instance.
     * 
     */
    public QueryType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code QueryType} instance by deeply copying a given {@code QueryType} instance.
     * 
     * 
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public QueryType(final QueryType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'QueryType' from 'null'.");
        }
        // CBuiltinLeafInfo: java.lang.String
        this.description = ((o.description == null)?null:o.getDescription());
        // CBuiltinLeafInfo: java.lang.Object
        this.condition = ((o.condition == null)?null:copyOf(o.getCondition()));
        // CWildcardTypeInfo: org.w3c.dom.Element
        this.filter = ((o.filter == null)?null:((o.getFilter() == null)?null:((Element) o.getFilter().cloneNode(true))));
        // CClassInfo: com.evolveum.prism.xml.ns._public.query_2.PagingType
        this.paging = ((o.paging == null)?null:((o.getPaging() == null)?null:o.getPaging().clone()));
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the condition property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getCondition() {
        return condition;
    }

    /**
     * Sets the value of the condition property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setCondition(Object value) {
        this.condition = value;
    }

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link Element }
     *     
     */
    public Element getFilter() {
        if (xfilter == null) {
        	return null;
        } else {
        	DomParser domParser = PrismUtil.getDomParser(null);
        	try {
				return domParser.serializeToElement(xfilter, F_FILTER);
			} catch (SchemaException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
        }
    }
    
    public MapXNode getXFilter() {
    	return this.xfilter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param element
     *     allowed object is
     *     {@link Element }
     *     
     */
    public void setFilter(Element element) {
    	if (element == null) {
    		this.xfilter = null;
    	} else {
    		DomParser domParser = PrismUtil.getDomParser(null);
    		try {
				this.xfilter = domParser.parseElementAsMap(element);
			} catch (SchemaException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
    	}
    }
    
    public void setFilter(XNode xnode) {
    	if (xnode == null || xnode.isEmpty()) {
    		this.xfilter = null;
    	} else {
    		if (xnode instanceof MapXNode) {
    			this.xfilter = (MapXNode) xnode;
	    	} else {
	    		throw new IllegalArgumentException("Cannot parse filter from "+xnode);
	    	}
    	}
    }

    /**
     * Gets the value of the paging property.
     * 
     * @return
     *     possible object is
     *     {@link PagingType }
     *     
     */
    public PagingType getPaging() {
        return paging;
    }

    /**
     * Sets the value of the paging property.
     * 
     * @param value
     *     allowed object is
     *     {@link PagingType }
     *     
     */
    public void setPaging(PagingType value) {
        this.paging = value;
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

    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = 1;
        {
            String theDescription;
            theDescription = this.getDescription();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "description", theDescription), currentHashCode, theDescription);
        }
        {
            Object theCondition;
            theCondition = this.getCondition();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "condition", theCondition), currentHashCode, theCondition);
        }
        {
            Element theFilter;
            theFilter = this.getFilter();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "filter", theFilter), currentHashCode, theFilter);
        }
        {
            PagingType thePaging;
            thePaging = this.getPaging();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "paging", thePaging), currentHashCode, thePaging);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof QueryType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final QueryType that = ((QueryType) object);
        {
            String lhsDescription;
            lhsDescription = this.getDescription();
            String rhsDescription;
            rhsDescription = that.getDescription();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "description", lhsDescription), LocatorUtils.property(thatLocator, "description", rhsDescription), lhsDescription, rhsDescription)) {
                return false;
            }
        }
        {
            Object lhsCondition;
            lhsCondition = this.getCondition();
            Object rhsCondition;
            rhsCondition = that.getCondition();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "condition", lhsCondition), LocatorUtils.property(thatLocator, "condition", rhsCondition), lhsCondition, rhsCondition)) {
                return false;
            }
        }
        {
            Element lhsFilter;
            lhsFilter = this.getFilter();
            Element rhsFilter;
            rhsFilter = that.getFilter();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "filter", lhsFilter), LocatorUtils.property(thatLocator, "filter", rhsFilter), lhsFilter, rhsFilter)) {
                return false;
            }
        }
        {
            PagingType lhsPaging;
            lhsPaging = this.getPaging();
            PagingType rhsPaging;
            rhsPaging = that.getPaging();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "paging", lhsPaging), LocatorUtils.property(thatLocator, "paging", rhsPaging), lhsPaging, rhsPaging)) {
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
    public QueryType clone() {
        try {
            {
                // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
                final QueryType clone = ((QueryType) super.clone());
                // CBuiltinLeafInfo: java.lang.String
                clone.description = ((this.description == null)?null:this.getDescription());
                // CBuiltinLeafInfo: java.lang.Object
                clone.condition = ((this.condition == null)?null:copyOf(this.getCondition()));
                // CWildcardTypeInfo: org.w3c.dom.Element
                clone.filter = ((this.filter == null)?null:((this.getFilter() == null)?null:((Element) this.getFilter().cloneNode(true))));
                // CClassInfo: com.evolveum.prism.xml.ns._public.query_2.PagingType
                clone.paging = ((this.paging == null)?null:((this.getPaging() == null)?null:this.getPaging().clone()));
                return clone;
            }
        } catch (CloneNotSupportedException e) {
            // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
            throw new AssertionError(e);
        }
    }

}
