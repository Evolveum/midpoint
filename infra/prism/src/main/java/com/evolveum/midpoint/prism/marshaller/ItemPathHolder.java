/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.marshaller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.*;

import com.evolveum.midpoint.util.QNameUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.xml.GlobalDynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Holds internal (parsed) form of midPoint-style XPath-like expressions.
 * It is able to retrieve/export these expressions from/to various forms (text, text in XML document,
 * XPathSegment list, prism path specification).
 * 
 * Assumes relative XPath, but somehow can also work with absolute XPaths.
 * 
 * @author semancik
 * @author mederly
 */
public class ItemPathHolder {

	private static final Trace LOGGER = TraceManager.getTrace(ItemPathHolder.class);
	public static final String DEFAULT_PREFIX = "c";
	private boolean absolute;
	private List<PathHolderSegment> segments;
	private final Map<String, String> explicitNamespaceDeclarations = new HashMap<>();

    // Part 1: Import from external representations.

	/**
	 * Sets "current node" Xpath.
	 */
	public ItemPathHolder() {
		absolute = false;
		segments = new ArrayList<>();
	}

	// This should not really be used. There should always be a namespace
	public ItemPathHolder(String xpath) {
		parse(xpath, null, null);
	}

	public ItemPathHolder(String xpath, Map<String, String> namespaceMap) {
		parse(xpath, null, namespaceMap);
	}

	public ItemPathHolder(Element domElement) {

		String xpath = ".";
		if (domElement != null) {
			xpath = domElement.getTextContent();
		}

		parse(xpath, domElement, null);
	}

	public ItemPathHolder(String xpath, Node domNode) {
		parse(xpath, domNode, null);
	}

    /**
     * Parses XPath-like expression (midPoint flavour), with regards to domNode from where the namespace declarations
     * (embedded in XML using xmlns attributes) are taken.
     *
     * @param itemPath text representation of the item path
     * @param domNode context (DOM node from which the expression was taken)
     * @param namespaceMap externally specified namespaces
     */
	private void parse(String itemPath, Node domNode, Map<String, String> namespaceMap) {

		segments = new ArrayList<>();
		absolute = false;

		if (".".equals(itemPath)) {
			return;
		}

		// Check for explicit namespace declarations.
		TrivialItemPathParser parser = TrivialItemPathParser.parse(itemPath);
		explicitNamespaceDeclarations.putAll(parser.getNamespaceMap());

		// Continue parsing with Xpath without the "preamble"
		itemPath = parser.getPureItemPathString();

		String[] segArray = itemPath.split("/");
		for (int i = 0; i < segArray.length; i++) {
			if (segArray[i] == null || segArray[i].isEmpty()) {
				if (i == 0) {
					absolute = true;
					// ignore the first empty segment of absolute path
					continue;
				} else {
					throw new IllegalArgumentException("ItemPath " + itemPath + " has an empty segment (number " + i + ")");
				}
			}

			String segmentStr = segArray[i];
            PathHolderSegment idValueFilterSegment;

            // is ID value filter attached to this segment?
            int idValuePosition = segmentStr.indexOf('[');
            if (idValuePosition >= 0) {
                if (!segmentStr.endsWith("]")) {
                    throw new IllegalArgumentException("ItemPath " + itemPath + " has a ID segment not ending with ']': '" + segmentStr + "'");
                }
                String value = segmentStr.substring(idValuePosition+1, segmentStr.length()-1);
                segmentStr = segmentStr.substring(0, idValuePosition);
                idValueFilterSegment = new PathHolderSegment(value);
            } else {
                idValueFilterSegment = null;
            }

            // processing the rest (i.e. the first part) of the segment

            boolean variable = false;
            if (segmentStr.startsWith("$")) {
                // We have variable here
                variable = true;
                segmentStr = segmentStr.substring(1);
            }

            String[] qnameArray = segmentStr.split(":");
            if (qnameArray.length > 2) {
                throw new IllegalArgumentException("Unsupported format: more than one colon in XPath segment: "
                        + segArray[i]);
            }
            QName qname;
            if (qnameArray.length == 1 || qnameArray[1] == null || qnameArray[1].isEmpty()) {
				if (ParentPathSegment.SYMBOL.equals(qnameArray[0])) {
					qname = ParentPathSegment.QNAME;
				} else if (ObjectReferencePathSegment.SYMBOL.equals(qnameArray[0])) {
					qname = ObjectReferencePathSegment.QNAME;
				} else if (IdentifierPathSegment.SYMBOL.equals(qnameArray[0])) {
					qname = IdentifierPathSegment.QNAME;
				} else {
					// default namespace <= empty prefix
					String namespace = findNamespace(null, domNode, namespaceMap);
					qname = new QName(namespace, qnameArray[0]);
				}
            } else {
                String namespacePrefix = qnameArray[0];
                String namespace = findNamespace(namespacePrefix, domNode, namespaceMap);
                if (namespace == null) {
                    QNameUtil.reportUndeclaredNamespacePrefix(namespacePrefix, itemPath);
                    namespacePrefix = QNameUtil.markPrefixAsUndeclared(namespacePrefix);
                }
                qname = new QName(namespace, qnameArray[1], namespacePrefix);
            }
            segments.add(new PathHolderSegment(qname, variable));
            if (idValueFilterSegment != null) {
                segments.add(idValueFilterSegment);
            }
		}
	}

	private String findNamespace(String prefix, Node domNode, Map<String, String> namespaceMap) {

		String ns = null;

		if (explicitNamespaceDeclarations != null) {
			if (prefix == null) {
				ns = explicitNamespaceDeclarations.get("");
			} else {
				ns = explicitNamespaceDeclarations.get(prefix);
			}
			if (ns != null) {
				return ns;
			}
		}

		if (namespaceMap != null) {
			if (prefix == null) {
				ns = namespaceMap.get("");
			} else {
				ns = namespaceMap.get(prefix);
			}
			if (ns != null) {
				return ns;
			}
		}

		if (domNode != null) {
			if (StringUtils.isNotEmpty(prefix)) {
                ns = domNode.lookupNamespaceURI(prefix);
            } else {
                // we don't want the default namespace declaration (xmlns="...") to propagate into path expressions
                // so here we do not try to obtain the namespace from the document
			}
			if (ns != null) {
				return ns;
			}
		}

		return ns;
	}

    public ItemPathHolder(List<PathHolderSegment> segments) {
        this(segments, false);
    }

    public ItemPathHolder(List<PathHolderSegment> segments, boolean absolute) {
        this.segments = new ArrayList<>();
        for (PathHolderSegment segment : segments) {
            if (segment.getQName() != null && StringUtils.isEmpty(segment.getQName().getPrefix())) {
                QName qname = segment.getQName();
                this.segments.add(new PathHolderSegment(new QName(qname.getNamespaceURI(), qname.getLocalPart())));
            } else {
                this.segments.add(segment);
            }
        }

        // this.segments = segments;
        this.absolute = absolute;
    }

    public ItemPathHolder(QName... segmentQNames) {
        this.segments = new ArrayList<>();
        for (QName segmentQName : segmentQNames) {
            PathHolderSegment segment = new PathHolderSegment(segmentQName);
            this.segments.add(segment);
        }

        this.absolute = false;
    }

    public ItemPathHolder(ItemPath itemPath) {
		this(itemPath, false);
    }

    public ItemPathHolder(ItemPath itemPath, boolean forceExplicitNamespaceDeclarations) {
		if (itemPath.getNamespaceMap() != null) {
			this.explicitNamespaceDeclarations.putAll(itemPath.getNamespaceMap());
		}
        this.segments = new ArrayList<>();
        for (ItemPathSegment segment: itemPath.getSegments()) {
            PathHolderSegment xsegment;
            if (segment instanceof NameItemPathSegment) {
            	boolean variable = segment.isVariable();
	            QName name = ((NameItemPathSegment) segment).getName();
	            xsegment = new PathHolderSegment(name, variable);
	            if (forceExplicitNamespaceDeclarations && StringUtils.isNotEmpty(name.getPrefix())) {
	            	this.explicitNamespaceDeclarations.put(name.getPrefix(), name.getNamespaceURI());
	            }
            } else if (segment instanceof IdItemPathSegment) {
                xsegment = new PathHolderSegment(idToString(((IdItemPathSegment) segment).getId()));
            } else if (segment instanceof ObjectReferencePathSegment) {
				xsegment = new PathHolderSegment(PrismConstants.T_OBJECT_REFERENCE, false);
			} else if (segment instanceof ParentPathSegment) {
				xsegment = new PathHolderSegment(PrismConstants.T_PARENT, false);
			} else if (segment instanceof IdentifierPathSegment) {
				xsegment = new PathHolderSegment(PrismConstants.T_ID, false);
			} else {
				throw new IllegalStateException("Unknown segment: " + segment);
			}
            this.segments.add(xsegment);
        }
        this.absolute = false;
    }

    // Part 2: Export to external representations.

	public String getXPath() {
		StringBuilder sb = new StringBuilder();

//		addPureXpath(sb);
		sb.append(getXPathWithDeclarations());
		return sb.toString();
	}

    public String getXPathWithoutDeclarations() {
        StringBuilder sb = new StringBuilder();
		addPureXpath(sb);
        return sb.toString();
    }

	public String getXPathWithDeclarations() {
		StringBuilder sb = new StringBuilder();
		addExplicitNsDeclarations(sb);
		addPureXpath(sb);
		return sb.toString();
	}

	private void addPureXpath(StringBuilder sb) {
		if (!absolute && segments.isEmpty()) {
			// Empty segment list gives a "local node" XPath
			sb.append(".");
			return;
		}

		if (absolute) {
			sb.append("/");
		}

        boolean first = true;

		for (PathHolderSegment seg : segments) {

            if (seg.isIdValueFilter()) {

                sb.append("[");
                sb.append(seg.getValue());
                sb.append("]");

            } else {

                if (!first) {
                    sb.append("/");
                } else {
                    first = false;
                }

                if (seg.isVariable()) {
                    sb.append("$");
                }
                QName qname = seg.getQName();

				if (ObjectReferencePathSegment.QNAME.equals(qname)) {
					sb.append(ObjectReferencePathSegment.SYMBOL);
				} else if (ParentPathSegment.QNAME.equals(qname)) {
					sb.append(ParentPathSegment.SYMBOL);
				} else if (IdentifierPathSegment.QNAME.equals(qname)) {
					sb.append(IdentifierPathSegment.SYMBOL);
				} else if (!StringUtils.isEmpty(qname.getPrefix())) {
                    sb.append(qname.getPrefix()).append(':').append(qname.getLocalPart());
                } else {
                    if (StringUtils.isNotEmpty(qname.getNamespaceURI())) {
                        String prefix = GlobalDynamicNamespacePrefixMapper.getPreferredPrefix(qname.getNamespaceURI());
                        seg.setQNamePrefix(prefix);     // hack - we modify the path segment here (only the form, not the meaning), but nevertheless it's ugly
                        sb.append(seg.getQName().getPrefix()).append(':').append(seg.getQName().getLocalPart());
                    } else {
                        // no namespace, no prefix
                        sb.append(qname.getLocalPart());
                    }
                }
            }
		}
	}
	
	public String toCanonicalPath(Class objectType, PrismContext prismContext) {
		StringBuilder sb = new StringBuilder("\\");
		
        boolean first = true;
        
        PrismObjectDefinition objDef = null;
        if (objectType != null) {
        	 objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(objectType);
        }
        ItemDefinition def = null;
		for (PathHolderSegment seg : segments) {

            if (seg.isIdValueFilter()) {
            	//for now, we don't want to save concrete id, just the path 
                continue;

            } else {

            	QName qname = seg.getQName();
            	
                if (!first) {
                    sb.append("\\");
                    if (StringUtils.isBlank(qname.getNamespaceURI()) && objDef != null) {
                        if (def instanceof PrismContainerDefinition) {
                        	PrismContainerDefinition containerDef = (PrismContainerDefinition) def;
                        	def = containerDef.findItemDefinition(qname);
                        }
                    	
                    	if (def != null) {
                    		qname = def.getName();
                    	}
                    }
                } else {
                	if (StringUtils.isBlank(qname.getNamespaceURI()) && objDef != null) {
                    	def = objDef.findItemDefinition(qname);
                    	if (def != null) {
                    		qname = def.getName();
                    	}
                    }
                    first = false;
                }

                
                
				sb.append(QNameUtil.qNameToUri(qname));
            }
		}
		
		return sb.toString();
	}

	public Map<String, String> getNamespaceMap() {

		Map<String, String> namespaceMap = new HashMap<>();
		Iterator<PathHolderSegment> iter = segments.iterator();
		while (iter.hasNext()) {
			PathHolderSegment seg = iter.next();
			QName qname = seg.getQName();
            if (qname != null) {
                if (qname.getPrefix() != null && !qname.getPrefix().isEmpty()) {
                    namespaceMap.put(qname.getPrefix(), qname.getNamespaceURI());
                }
                // this code seems to be currently of no use
//                else {
//                    // Default namespace
//                    // HACK. See addPureXpath method
//                    namespaceMap.put(DEFAULT_PREFIX, qname.getNamespaceURI());
//                }
            }
		}

		return namespaceMap;
	}

	public Element toElement(String elementNamespace, String localElementName) {
		// TODO: is this efficient?
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return toElement(elementNamespace, localElementName, loader.newDocument());
		} catch (ParserConfigurationException ex) {
			throw new AssertionError("Error on creating XML document " + ex.getMessage());
		}
	}

	public Element toElement(QName elementQName, Document document) {
		return toElement(elementQName.getNamespaceURI(), elementQName.getLocalPart(), document);
	}

    // really ugly implementation... (ignores overall context of serialization, so produces <c:path> elements even if common is default namespace) TODO rework [med]
	public Element toElement(String elementNamespace, String localElementName, Document document) {
		Element element = document.createElementNS(elementNamespace, localElementName);
		if (!StringUtils.isBlank(elementNamespace)) {
			String prefix = GlobalDynamicNamespacePrefixMapper.getPreferredPrefix(elementNamespace);
			if (!StringUtils.isBlank(prefix)) {
				try {
					element.setPrefix(prefix);
				} catch (DOMException e) {
					throw new SystemException("Error setting XML prefix '"+prefix+"' to element {"+elementNamespace+"}"+localElementName+": "+e.getMessage(), e);
				}
			}
		}
		element.setTextContent(getXPathWithDeclarations());
		Map<String, String> namespaceMap = getNamespaceMap();
		if (namespaceMap != null) {
			for (Entry<String, String> entry : namespaceMap.entrySet()) {
				DOMUtil.setNamespaceDeclaration(element, entry.getKey(), entry.getValue());
			}
		}
		return element;
	}

	public List<PathHolderSegment> toSegments() {
		// FIXME !!!
		return Collections.unmodifiableList(segments);
	}

	@NotNull
    public ItemPath toItemPath() {
        List<PathHolderSegment> xsegments = toSegments();
        List<ItemPathSegment> segments = new ArrayList<>(xsegments.size());
        for (PathHolderSegment segment : xsegments) {
            if (segment.isIdValueFilter()) {
                segments.add(new IdItemPathSegment(idToLong(segment.getValue())));
            } else {
                QName qName = segment.getQName();
                boolean variable = segment.isVariable();
				segments.add(ItemPath.createSegment(qName, variable));
            }
        }
        ItemPath path = new ItemPath(segments);
        path.setNamespaceMap(explicitNamespaceDeclarations);
        return path;
    }

    // Part 3: Various

	/**
	 * Returns new XPath with a specified element prepended to the path. Useful
	 * for "transposing" relative paths to a absolute root.
	 */
	public ItemPathHolder transposedPath(QName parentPath) {
		PathHolderSegment segment = new PathHolderSegment(parentPath);
		List<PathHolderSegment> segments = new ArrayList<>();
		segments.add(segment);
		return transposedPath(segments);
	}

	/**
	 * Returns new XPath with a specified element prepended to the path. Useful
	 * for "transposing" relative paths to a absolute root.
	 */
	public ItemPathHolder transposedPath(List<PathHolderSegment> parentPath) {
		List<PathHolderSegment> allSegments = new ArrayList<>();
		allSegments.addAll(parentPath);
		allSegments.addAll(toSegments());
		return new ItemPathHolder(allSegments);
	}
	
	

	private void addExplicitNsDeclarations(StringBuilder sb) {
		if (explicitNamespaceDeclarations != null) {
			for (String prefix : explicitNamespaceDeclarations.keySet()) {
				sb.append("declare ");
				if (prefix.equals("")) {
					sb.append("default namespace '");
					sb.append(explicitNamespaceDeclarations.get(prefix));
					sb.append("'; ");
				} else {
					sb.append("namespace ");
					sb.append(prefix);
					sb.append("='");
					sb.append(explicitNamespaceDeclarations.get(prefix));
					sb.append("'; ");
				}
			}
		}
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}

    @Override
    public String toString() {
        // TODO: more verbose toString later
        return getXPath();
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (absolute ? 1231 : 1237);
		result = prime * result + ((segments == null) ? 0 : segments.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		// Special case
		if (obj instanceof QName) {
			if (segments.size() != 1) {
				return false;
			}
			PathHolderSegment segment = segments.get(0);
			return segment.getQName().equals((QName)obj);
		}
		
		if (getClass() != obj.getClass())
			return false;
		ItemPathHolder other = (ItemPathHolder) obj;
		if (absolute != other.absolute)
			return false;
		if (segments == null) {
			if (other.segments != null)
				return false;
		} else if (!segments.equals(other.segments))
			return false;
		return true;
	}

	/**
	 * Returns true if this path is below a specified path.
	 */
	public boolean isBelow(ItemPathHolder path) {
		if (this.segments.size() < 1){
			return false;
		}
		for(int i = 0; i < path.segments.size(); i++) {
			if (i > this.segments.size()) {
				// We have run beyond all of local segments, therefore
				// this path cannot be below specified path
				return false;
			}
			if (!this.segments.get(i).equals(path.segments.get(i))) {
				// Segments don't match. We are not below.
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns a list of segments that are the "tail" after specified path.
	 * The path in the parameter is assumed to be a "superpath" to this path, e.i.
	 * this path is below specified path. This method returns all the segments 
	 * of this path that are below the specified path.
	 * Returns null if the assumption is false.
	 */
	public List<PathHolderSegment> getTail(ItemPathHolder path) {
		int i = 0;
		while(i < path.segments.size()) {
			if (i > this.segments.size()) {
				// We have run beyond all of local segments, therefore
				// this path cannot be below specified path
				return null;
			}
			if (!this.segments.get(i).equals(path.segments.get(i))) {
				// Segments don't match. We are not below.
				return null;
			}
			i++;
		}
		return segments.subList(i, this.segments.size());
	}

	public static boolean isDefault(Element pathElement) {
		if (pathElement == null) {
			return true;
		}
		ItemPathHolder xpath = new ItemPathHolder(pathElement);
		if (xpath.isEmpty()) {
			return true;
		}
		return false;
	}

	
	private Long idToLong(String stringVal) {
		if (stringVal == null) {
			return null;
		}
		return Long.valueOf(stringVal);
	}
	
	private String idToString(Long longVal) {
		if (longVal == null) {
			return null;
		}
		return longVal.toString();
	}

}
