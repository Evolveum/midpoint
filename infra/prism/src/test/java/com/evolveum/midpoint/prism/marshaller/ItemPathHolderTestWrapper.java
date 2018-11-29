/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.path.UniformItemPath;
import org.testng.AssertJUnit;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 *  ItemPathHolder should be strictly internal to prism-impl module.
 *  However, we want to test it from the schema (XPathTest).
 *  This class therefore serves as a wrapper for ItemPathHolder.
 */
public class ItemPathHolderTestWrapper {

	private ItemPathHolder itemPathHolder;

	public static ItemPathHolderTestWrapper createForTesting(String xpath) {
		ItemPathHolderTestWrapper rv = new ItemPathHolderTestWrapper();
		rv.itemPathHolder = ItemPathHolder.createForTesting(xpath);
		return rv;
	}

	public static ItemPathHolderTestWrapper createForTesting(String xpath, Map<String, String> namespaceMap) {
		ItemPathHolderTestWrapper rv = new ItemPathHolderTestWrapper();
		rv.itemPathHolder = ItemPathHolder.createForTesting(xpath, namespaceMap);
		return rv;
	}

	public static ItemPathHolderTestWrapper createForTesting(Element element) {
		ItemPathHolderTestWrapper rv = new ItemPathHolderTestWrapper();
		rv.itemPathHolder = ItemPathHolder.createForTesting(element);
		return rv;
	}

	public static ItemPathHolderTestWrapper createForTesting(List<PathHolderSegment> segments) {
		ItemPathHolderTestWrapper rv = new ItemPathHolderTestWrapper();
		rv.itemPathHolder = ItemPathHolder.createForTesting(segments);
		return rv;
	}

	public static ItemPathHolderTestWrapper createForTesting(QName... segmentQNames) {
		ItemPathHolderTestWrapper rv = new ItemPathHolderTestWrapper();
		rv.itemPathHolder = ItemPathHolder.createForTesting(segmentQNames);
		return rv;
	}

	public static ItemPathHolderTestWrapper createForTesting(UniformItemPath path) {
		ItemPathHolderTestWrapper rv = new ItemPathHolderTestWrapper();
		rv.itemPathHolder = ItemPathHolder.createForTesting(path);
		return rv;
	}

	public static void assertEquals(ItemPathHolderTestWrapper w1, ItemPathHolderTestWrapper w2) {
		AssertJUnit.assertEquals(w1.itemPathHolder, w2.itemPathHolder);
	}

	public String getXPathWithoutDeclarations() {
		return itemPathHolder.getXPathWithoutDeclarations();
	}

	public Element toElement(String elementNamespace, String localElementName, Document document) {
		return itemPathHolder.toElement(elementNamespace, localElementName, document);
	}

	public Element toElement(QName elementQName, Document document) {
		return itemPathHolder.toElement(elementQName, document);
	}

	public List<PathHolderSegment> toSegments() {
		return itemPathHolder.toSegments();
	}

	public Map<String, String> getNamespaceMap() {
		return itemPathHolder.getNamespaceMap();
	}

	public String getXPathWithDeclarations() {
		return itemPathHolder.getXPathWithDeclarations();
	}

	public UniformItemPath toItemPath() {
		return itemPathHolder.toItemPath();
	}
}
