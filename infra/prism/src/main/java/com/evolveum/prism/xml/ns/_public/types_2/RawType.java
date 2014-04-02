package com.evolveum.prism.xml.ns._public.types_2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.parser.PrismBeanConverter;
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
 * 
 *                 TODO
 *             
 * 
 * <p>Java class for RawType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RawType">
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
@XmlType(name = "RawType", propOrder = {
    "content"
})
public class RawType implements Serializable, Cloneable, Equals {
	private static final long serialVersionUID = 4430291958902286779L;
	
	public RawType() {
	}

    public RawType(XNode xnode) {
        this.xnode = xnode;
    }

    /*
     *  At most one of these two values (xnode, parsed) should be set.
     *  Content is present when parsing via JAXB, and hold there.
     *  It is *NOT* updated on xnode/parsed changes, which are forbidden anyway.
     */

    @XmlTransient
	private XNode xnode;
	
	@XmlTransient
	private PrismValue parsed;
	
    @XmlMixed
    @XmlAnyElement
    protected List<Object> content;
    
	public XNode getXnode() {
        parseContentListIfNeeded();
		return xnode;
	}

    public List<Object> getContent() {
        if (content == null) {
            ContentList newContentList = new ContentList();
            try {
                newContentList.fillIn();
            } catch (SchemaException e) {
                throw new SystemException("Couldn't prepare RawType contents: " + e.getMessage(), e);
            }
            content = newContentList;
        }
        return content;
    }

    public XNode serializeToXNode() throws SchemaException {
        parseContentListIfNeeded();
        if (xnode != null) {
            return xnode;
        } else if (parsed != null) {
            return parsed.getPrismContext().getXnodeProcessor().serializeItemValue(parsed);
        } else {
            return null;            // or an exception here?
        }
    }

    // BRUTAL HACK until I figure out why my getContentList() and add() are not being called
    private void parseContentListIfNeeded() {
        if (xnode == null && parsed == null && content != null) {
            for (Object o : content) {
                addObject(o);
            }
        }
    }

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
            xnode = null;
            parsed = null;
            super.clear();
	    }

        void fillIn() throws SchemaException {
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

    private void addObject(Object e) {
        if (e instanceof String) {
            if (!StringUtils.isBlank((String) e)) {
                addString((String) e);
            }
        } else if (e instanceof Element) {
            addElement((Element) e);
        } else if (e instanceof JAXBElement) {
            addJaxbElement((JAXBElement) e);
        } else {
            throw new IllegalArgumentException("RAW TYPE ADD: "+e+" "+e.getClass());
        }
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

    private void addString(final String val) {
        ValueParser valueParser = new ValueParser() {

            @Override
            public Object parse(QName typeName)
                    throws SchemaException {
                return XmlTypeConverter.toJavaValue(val, typeName);
            }

            @Override
            public boolean isEmpty() {
                return StringUtils.isEmpty(val);
            }

            @Override
            public String getStringValue() {
                return val;
            }
        };
        if (xnode != null || parsed != null) {
            throw new IllegalStateException("Trying to add text value to already filled-in RawType. Value being added = " + val);
        }
        xnode = new PrimitiveXNode();
        ((PrimitiveXNode)xnode).setValueParser(valueParser);
    }


    // itemDefinition may be null; in that case we do the best what we can
	public <V extends PrismValue> V getParsedValue(ItemDefinition itemDefinition, QName itemName) throws SchemaException {
        parseContentListIfNeeded();
        if (parsed != null) {
			return (V) parsed;
		} else if (xnode != null) {
            V value = null;
			if (itemDefinition != null) {
				PrismContext prismContext = itemDefinition.getPrismContext();
				Item<V> subItem = prismContext.getXnodeProcessor().parseItem(xnode, itemDefinition.getName(), itemDefinition);
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
        item.add(newValue);
        return item;
    }

    public RawType clone() {
    	RawType clone = new RawType();
        parseContentListIfNeeded();
        if (xnode != null) {
    	    clone.xnode = xnode.clone();
        } else if (parsed != null) {
            clone.parsed = parsed.clone();
        }
        // contents cannot be cloned, because copying it would result in re-adding existing contents to the clone
    	return clone;
    }
    
	@Override
	public int hashCode() {
        parseContentListIfNeeded();
		final int prime = 31;
		int result = 1;
		result = prime * result + ((xnode == null) ? 0 : xnode.hashCode());
        result = prime * result + ((parsed == null) ? 0 : parsed.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
        parseContentListIfNeeded();
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

}
