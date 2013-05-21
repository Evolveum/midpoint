/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.prism.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class NamedNodeMapEmptyImpl implements NamedNodeMap {

	public static final NamedNodeMap IMMUTABLE_EMPTY_NAMED_NODE_MAP = new NamedNodeMapEmptyImpl();

	@Override
	public Node getNamedItem(String name) {
		return null;
	}

	@Override
	public Node setNamedItem(Node arg) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

	@Override
	public Node removeNamedItem(String name) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

	@Override
	public Node item(int index) {
		return null;
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	@Override
	public Node setNamedItemNS(Node arg) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

	@Override
	public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
		throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, "This NodeMap is immutable");
	}

}
