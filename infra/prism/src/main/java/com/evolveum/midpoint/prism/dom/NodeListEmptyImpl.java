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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NodeListEmptyImpl implements NodeList {

	public static final NodeList IMMUTABLE_EMPTY_NODELIST = new NodeListEmptyImpl();

	@Override
	public Node item(int index) {
		return null;
	}

	@Override
	public int getLength() {
		return 0;
	}

}
