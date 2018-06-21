
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;


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
 * &lt;complexType name="QueryType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element ref="{http://prism.evolveum.com/xml/ns/public/query-2}filter"/&gt;
 *         &lt;element name="paging" type="{http://prism.evolveum.com/xml/ns/public/query-2}PagingType" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryType", propOrder = {
    "description",
    "filter",
    "paging"
})
public class QueryType implements Serializable, Cloneable, DebugDumpable
{
    private final static long serialVersionUID = 201105211233L;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "QueryType");
    public final static QName F_DESCRIPTION = new QName(PrismConstants.NS_QUERY, "description");
    public final static QName F_FILTER = new QName(PrismConstants.NS_QUERY, "filter");
    public final static QName F_PAGING = new QName(PrismConstants.NS_QUERY, "paging");

    protected String description;
    @XmlElement(required = true)
    protected SearchFilterType filter;
    protected PagingType paging;



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

    public QueryType description(String value) {
        setDescription(value);
        return this;
    }

    /**
     * Gets the value of the filter property.
     *
     * @return
     *     possible object is
     *     {@link SearchFilterType }
     *
     */
    public SearchFilterType getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     *
     * @param value
     *     allowed object is
     *     {@link SearchFilterType }
     *
     */
    public void setFilter(SearchFilterType value) {
        this.filter = value;
    }

    public QueryType filter(SearchFilterType value) {
        setFilter(value);
        return this;
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

    public QueryType paging(PagingType value) {
        setPaging(value);
        return this;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((paging == null) ? 0 : paging.hashCode());
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
		QueryType other = (QueryType) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (paging == null) {
			if (other.paging != null)
				return false;
		} else if (!paging.equals(other.paging))
			return false;
		return true;
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
        } catch (MimeTypeParseException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        } catch (MalformedURLException e) {
            throw((AssertionError) new AssertionError((("Unexpected instance during copying object '"+ o)+"'.")).initCause(e));
        } catch (URISyntaxException e) {
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
                // CWildcardTypeInfo: org.w3c.dom.Element
                clone.filter = ((this.filter == null)?null:((this.getFilter() == null)?null:((SearchFilterType) this.getFilter().clone())));
                // CClassInfo: com.evolveum.prism.xml.ns._public.query_3.PagingType
                clone.paging = ((this.paging == null)?null:((this.getPaging() == null)?null:this.getPaging().clone()));
                return clone;
            }
        } catch (CloneNotSupportedException e) {
            // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
            throw new AssertionError(e);
        }
    }

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("QueryType");
		if (description != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "description", description, indent + 1);
		}
		if (filter != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "filter", filter, indent + 1);
		}
		if (paging != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "paging", paging.toString(), indent + 1);
		}
		return sb.toString();
	}

}
