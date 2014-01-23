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
package com.evolveum.midpoint.prism.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

public class DOMParser {
	
	public XNode parse(File file) {
		Document document = DOMUtil.parseFile(file);
		return parse(document);
	}
	
	public RootXNode parse(Document document) {
		Element element = DOMUtil.getFirstChildElement(document);
		RootXNode root = new RootXNode(DOMUtil.getQName(element));
		XNode xnode = parseElement(element);
		root.setSubnode(xnode);
		return root;
	}

	public XNode parseElement(Element element) {
		if (DOMUtil.hasChildElements(element)) {
			return parseSubElemets(element);
		} else {
			return parsePrimitiveElement(element);
		}
	}

	private MapXNode parseSubElemets(Element element) {
		MapXNode xmap = new MapXNode();
		QName lastElementQName = null;
		List<Element> lastElements = null;
		for (Element childElement: DOMUtil.listChildElements(element)) {
			QName childQName = DOMUtil.getQName(childElement);
			if (childQName.equals(lastElementQName)) {
				lastElements.add(childElement);
			} else {
				parseElementGroup(xmap, lastElementQName, lastElements);
				lastElementQName = childQName;
				lastElements = new ArrayList<Element>();
				lastElements.add(childElement);
			}
		}
		parseElementGroup(xmap, lastElementQName, lastElements);
		return xmap;
	}

	private void parseElementGroup(MapXNode xmap, QName elementQName, List<Element> elements) {
		if (elements == null || elements.isEmpty()) {
			return;
		}
		if (elements.size() == 1) {
			XNode xsub = parseElement(elements.get(0));
			xmap.put(elementQName, xsub);
		} else {
			ListXNode xlist = parseElementList(elements); 
			xmap.put(elementQName, xlist);
		}
	}

	/**
	 * Parses elements that all have the same element name. 
	 */
	private ListXNode parseElementList(List<Element> elements) {
		ListXNode xlist = new ListXNode();
		for (Element element: elements) {
			XNode xnode = parseElement(element);
			xlist.add(xnode);
		}
		return xlist;
	}

	private <T> PrimitiveXNode<T> parsePrimitiveElement(final Element element) {
		PrimitiveXNode<T> xnode = new PrimitiveXNode<T>();
		ValueParser<T> valueParser = new ValueParser<T>() {
			@Override
			public T parse(PrismPropertyDefinition<T> definition) {
				return parsePrimitiveElementValue(element, definition);
			}
			@Override
			public String toString() {
				return "ValueParser(DOM, "+PrettyPrinter.prettyPrint(DOMUtil.getQName(element))+": "+element.getTextContent()+")";
			}
		};
		xnode.setValueParser(valueParser);
		return xnode;
	}
	
	private <T> T parsePrimitiveElementValue(Element element, PrismPropertyDefinition<T> definition) {
		return null;
	}
	
}
