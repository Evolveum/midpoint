package com.evolveum.prism.xml.ns._public.types_2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.PrismBeanConverter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.parser.DomParser;
import com.evolveum.midpoint.prism.parser.XNodeProcessor;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * A class used to hold raw XNodes until the definition for such an object is known.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RawType", propOrder = {
    "content"
})
public class RawType implements Serializable, Cloneable, Equals {
	private static final long serialVersionUID = 4430291958902286779L;
	
	public RawType() {
	}

    public RawType(XNode xnode) {
        this.xnode = xnode;
        ((ContentList) content).fillIn();
    }

    /*
     *  At most one of these two values (xnode, parsed) should be set.
     */

    /**
     * Unparsed value. It is set either on RawType instance construction
     * or gradually constructed when parsing via JAXB (see ContentList class).
     *
     * Note that its type QName is coupled with the "type" attribute.
     */
    @XmlTransient
	private XNode xnode;

    /**
     * Parsed value. It is computed when calling getParsedValue/getParsedItem methods.
     *
     * Beware: At most one of these fields (xnode, parsed) may be non-null at any instant.
     */
	@XmlTransient
	private PrismValue parsed;

    /**
     * Raw content (mix of strings, DOM elements and probably JAXB elements).
     * It is set either when parsing via JAXB or when receiving an XNode value.
     *
     * It is *NOT* updated on xnode/parsed changes, which are forbidden anyway.
     */
    @XmlMixed
    @XmlAnyElement
    protected List<Object> content = new ContentList();   // must be here, otherwise JAXB provides its own implementation of the list

    /**
     * Raw attributes: set when parsing via JAXB.
     *
     * Attributes are not serialized when marshalling this object via JAXB (except xsiType one).
     * All information is marshalled into XML elements, regardless of whether they originate
     * from elements or attributes. We hope the JAXB will be abandoned soon, so this is not
     * a big problem.
     */
    @XmlAnyAttribute
    private Map<QName, String> attributes = new AttributesMap();

    /**
     * Explicit designation of the value type.
     * It is set either when parsing via JAXB or when receiving XNode value.
     *
     * It is *NOT* updated on xnode/parsed changes, which are forbidden anyway.
     *
     * Will be removed when we get rid of JAXB.
     */
    @XmlAttribute(name = "xsiType")
    private QName xsiType;

    //region General getters/setters
    public XNode getXnode() {
        return xnode;
    }

    public List<Object> getContent() {
        return content;                     // content is initialized at instantiation time
    }

    public QName getXsiType() {
        if (xnode != null) {
            return xnode.getTypeQName();
        } else {
            return xsiType;
        }
    }

    public void setXsiType(QName type) {
        this.xsiType = type;
        if (xnode != null) {
            xnode.setTypeQName(type);
        }
    }
    //endregion

    //region ContentList management
    /**
     * We do not maintain ContentList after any changes in xnode or parsed are made.
     */
    class ContentList extends ArrayList<Object> implements Serializable {

        @Override
        public boolean add(Object e) {
            addObject(e);
            return super.add(e);
        }

        @Override
        public void clear() {
            removeContent(false);
            super.clear();
	    }

        void fillIn() {
            try {
                fillInWithSchemaException();
            } catch (SchemaException e) {
                throw new SystemException("Couldn't prepare RawType contents: " + e.getMessage(), e);
            }
        }

        private void fillInWithSchemaException() throws SchemaException {
            DomParser domParser;
            XNode xnodeToSerialize;
            if (parsed != null) {
                PrismContext prismContext = parsed.getPrismContext();
                xnodeToSerialize = prismContext.getXnodeProcessor().serializeItemValue(parsed);
                domParser = prismContext.getParserDom();
            } else {
                xnodeToSerialize = xnode;
                domParser = new DomParser(null);
            }
            if (xnode != null) {
                Element rootElement = domParser.serializeToElement(xnodeToSerialize, new QName("dummy"));
                NodeList children = rootElement.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element) {
                        DOMUtil.fixNamespaceDeclarations((Element) child);
                        super.add(child);
                    } else if (child instanceof Text) {
                        super.add(((Text) child).getData());
                    } else if (child instanceof Attr) {
                        // attributes are ignored (xmlns have been already copied to child)
                    } else {
                        System.out.println("fillIn: ignoring " + child);        // TODO remove this eventually
                    }
                }
            }
        }

        @Override
        public Object set(int index, Object element) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }

        @Override
        public Object remove(int index) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }

        @Override
        public boolean addAll(Collection<?> c) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }

        @Override
        public boolean addAll(int index, Collection<?> c) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }

        @Override
        protected void removeRange(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("This is not a supported way of dealing with RawType internal contents.");
        }
    }

    private void removeContent(boolean removeAttributes) {
        if (parsed != null) {
            throw new UnsupportedOperationException("Clearing content is unsupported if the content is already parsed");
        } else if (xnode != null) {
            if (xnode instanceof PrimitiveXNode) {
                if (!removeAttributes) {
                    xnode = null;           // primitive xnode of this kind got here from the content (not from attributes), so remove it
                }
            } else if (xnode instanceof MapXNode) {
                Iterator<Map.Entry<QName, XNode>> iterator = ((MapXNode) xnode).entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<QName, XNode> entry = iterator.next();
                    boolean entryIsAttribute = (entry.getValue() instanceof PrimitiveXNode && ((PrimitiveXNode) entry.getValue()).isAttribute());
                    if (removeAttributes == entryIsAttribute) {
                        iterator.remove();
                    }
                }
            } else {
                throw new IllegalStateException("Unsupported xnode type: " + xnode);
            }
        } else {
            // no content, nothing to remove
        }
    }

    private void addObject(Object e) {
        if (e instanceof String) {
            if (!StringUtils.isBlank((String) e)) {
                addStringContent((String) e);
            }
        } else if (e instanceof Element) {
            addElement((Element) e);
        } else if (e instanceof JAXBElement) {
            addJaxbElement((JAXBElement) e);
        } else {
            throw new IllegalArgumentException("RAW TYPE ADD: "+e+" "+e.getClass());
        }
        updateXNodeType();
    }

    private void addJaxbElement(JAXBElement jaxb) {
        PrismBeanConverter converter = new PrismBeanConverter(null);
        XNode newXNode;
        try {
            newXNode = converter.marshall(jaxb.getValue());
        } catch (SchemaException ex) {
            throw new IllegalArgumentException("Cannot parse element: "+ex+" Reason: "+ex.getMessage(), ex);
        }
        MapXNode mapXNode = prepareMapXNode();
        mapXNode.put(jaxb.getName(), newXNode);
    }

    private void addElement(Element e) {
        DomParser domParser = new DomParser(null);
        MapXNode newXnode;
        try {
            newXnode = domParser.parseElementAsMap(e);
        } catch (SchemaException ex) {
            throw new IllegalArgumentException("Cannot parse element: "+e+" Reason: "+ex.getMessage(), ex);
        }
        MapXNode mapXNode = prepareMapXNode();
        mapXNode.merge(newXnode);
    }

    private MapXNode prepareMapXNode() {
        if (xnode == null) {
            xnode = new MapXNode();
        } else if (!(xnode instanceof MapXNode)) {
            throw new IllegalStateException("xnode is not a MapXNode, aren't you mixing text with XML elements in this RawType?");
        }
        return (MapXNode) xnode;
    }

    class StringValueParser implements ValueParser {
        String val;

        StringValueParser(String val) {
            this.val = val;
        }

        @Override
        public Object parse(QName typeName) throws SchemaException {
            // XmlTypeConverter is not able to deal with QNames, so we have to do it here
            // Currently we know nothing about prefixes (although this might change in the future),
            // so we'll simply strip them away.
            //
            // TODO deal with ItemPathType as well
            if (DOMUtil.XSD_QNAME.equals(typeName)) {
                if (val != null) {
                    int i = val.indexOf(':');
                    if (i >= 0) {
                        return new QName(val.substring(i+1));
                    } else {
                        return new QName(val);
                    }
                } else {
                    return null;
                }
            } else {
                return XmlTypeConverter.toJavaValue(val, typeName);
            }
        }

        @Override
        public boolean isEmpty() {
            return StringUtils.isEmpty(val);
        }

        @Override
        public String getStringValue() {
            return val;
        }
    }

    private void addStringContent(final String val) {
        ValueParser valueParser = new StringValueParser(val);
        if (xnode != null || parsed != null) {
            throw new IllegalStateException("Trying to add text value to already filled-in RawType. Value being added = " + val);
        }
        PrimitiveXNode newXNode = new PrimitiveXNode();
        newXNode.setValueParser(valueParser);
        newXNode.setAttribute(false);
        xnode = newXNode;
        updateXNodeType();
    }

    //endregion

    //region AttributesMap management
    class AttributesMap implements Map<QName, String>, Serializable {

        private Map<QName, String> map = new HashMap<>();

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        @Override
        public String get(Object key) {
            return map.get(key);
        }

        @Override
        public String put(QName key, String value) {
            addStringFromAttribute(key, value);
            System.out.println("Adding attribute " + key + " = " + value);
            return map.put(key, value);
        }

        @Override
        public String remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends QName, ? extends String> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            removeContent(true);
        }

        @Override
        public Set<QName> keySet() {
            return map.keySet();
        }

        @Override
        public Collection<String> values() {
            return map.values();
        }

        @Override
        public Set<Entry<QName, String>> entrySet() {
            return map.entrySet();
        }
    }

    private void addStringFromAttribute(QName attributeName, String val) {
        ValueParser valueParser = new StringValueParser(val);
        PrimitiveXNode newXNode = new PrimitiveXNode();
        newXNode.setValueParser(valueParser);
        newXNode.setAttribute(true);
        xnode = prepareMapXNode();
        ((MapXNode) xnode).put(attributeName, newXNode);
    }
    //endregion

    //region Parsing and serialization
    // itemDefinition may be null; in that case we do the best what we can
	public <V extends PrismValue> V getParsedValue(ItemDefinition itemDefinition, QName itemName) throws SchemaException {
        if (parsed != null) {
			return (V) parsed;
		} else if (xnode != null) {
            V value;
			if (itemDefinition != null) {
				PrismContext prismContext = itemDefinition.getPrismContext();
                if (itemName == null) {
                    itemName = itemDefinition.getName();
                }
				Item<V> subItem = prismContext.getXnodeProcessor().parseItem(xnode, itemName, itemDefinition);
				value = subItem.getValue(0);
			} else {
				PrismProperty<V> subItem = XNodeProcessor.parsePrismPropertyRaw(xnode, itemName);
				value = (V) subItem.getValue();
			}
            xnode = null;
            parsed = value;
            return (V) parsed;
		} else {
		    return null;
        }
	}

    public <V extends PrismValue> Item<V> getParsedItem(ItemDefinition itemDefinition) throws SchemaException {
        Validate.notNull(itemDefinition);
        return getParsedItem(itemDefinition, itemDefinition.getName());
    }

    public <V extends PrismValue> Item<V> getParsedItem(ItemDefinition itemDefinition, QName itemName) throws SchemaException {
        Validate.notNull(itemDefinition);
        Validate.notNull(itemName);
        Item<V> item = itemDefinition.instantiate();
        V newValue = getParsedValue(itemDefinition, itemName);
        item.add((V) newValue.clone());
        return item;
    }

    private void updateXNodeType() {
        if (xsiType != null && xnode != null) {
            xnode.setTypeQName(xsiType);
        }
    }

    public XNode serializeToXNode() throws SchemaException {
        if (xnode != null) {
            return xnode;
        } else if (parsed != null) {
            return parsed.getPrismContext().getXnodeProcessor().serializeItemValue(parsed);
        } else {
            return null;            // or an exception here?
        }
    }
    //endregion

    //region Cloning, comparing, dumping (TODO)
    public RawType clone() {
    	RawType clone = new RawType();
        clone.xsiType = CloneUtil.clone(xsiType);
        if (xnode != null) {
    	    clone.xnode = xnode.clone();
            clone.updateXNodeType();
        } else if (parsed != null) {
            clone.parsed = parsed.clone();
        }
        // contents cannot be cloned, because copying it would result in re-adding existing contents to the clone
    	return clone;
    }
    
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xnode == null) ? 0 : xnode.hashCode());
        result = prime * result + ((parsed == null) ? 0 : parsed.hashCode());
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
		RawType other = (RawType) obj;
		if (xnode != null && other.xnode != null) {
            return xnode.equals(other.xnode);
        } else if (parsed != null && other.parsed != null) {
            return parsed.equals(other.parsed);
		} else {
            return xnodeSerializationsAreEqual(other);
        }
    }

    private boolean xnodeSerializationsAreEqual(RawType other) {
        try {
            return serializeToXNode().equals(other.serializeToXNode());
        } catch (SchemaException e) {
            // or should we silently return false?
            throw new SystemException("Couldn't serialize RawType to XNode when comparing them", e);
        }
    }

	@Override
	public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object that,
			EqualsStrategy equalsStrategy) {
		return equals(that);
	}
    //endregion
}
