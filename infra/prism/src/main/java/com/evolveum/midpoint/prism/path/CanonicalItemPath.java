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

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.QNameUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author katkav
 * @author mederly
 */
public class CanonicalItemPath implements Serializable {

	private static final List<Pair<String, String>> EXPLICIT_REPLACEMENTS = Arrays.asList(
			new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns/public/common/common-", "common"),
			new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns/public/connector/icf-", "icf"),
			new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns/public", "public"),
			new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns", "midpoint"),
			new ImmutablePair<>("http://prism.evolveum.com/xml/ns", "prism")
			);

	private static final String SHORTCUT_MARKER_START = "${";
	private static final String SHORTCUT_MARKER_END = "}";

	// currently we support only named segments in canonical paths
	public static class Segment implements Serializable {
		private final QName name;
		private final Integer index;		// N means this is Nth unique non-empty namespace in the path (starting from 0)
		private final Integer shortcut;		// K means this namespace is the same as the one with index=K (null if 1st occurrence)
		private Segment(QName name, Integer index, Integer shortcut) {
			this.name = name;
			this.index = index;
			this.shortcut = shortcut;
		}
		public QName getName() {
			return name;
		}
		public Integer getIndex() {
			return index;
		}
		public Integer getShortcut() {
			return shortcut;
		}
	}

	private final List<Segment> segments = new ArrayList<>();

	public static CanonicalItemPath create(ItemPath itemPath, Class<? extends Containerable> clazz, PrismContext prismContext) {
		return new CanonicalItemPath(itemPath, clazz, prismContext);
	}

	public static CanonicalItemPath create(ItemPath itemPath) {
		return new CanonicalItemPath(itemPath, null, null);
	}

	private CanonicalItemPath(List<Segment> segments) {
		this.segments.addAll(segments);
	}

	private CanonicalItemPath(ItemPath itemPath, Class<? extends Containerable> clazz, PrismContext prismContext) {
		ItemDefinition def = clazz != null && prismContext != null ?
				prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz) : null;
		while (!ItemPath.isNullOrEmpty(itemPath)) {
			ItemPathSegment first = itemPath.first();
			if (first instanceof NameItemPathSegment) {
				// TODO what about variable named item path segments?
				QName name = ((NameItemPathSegment) first).getName();
				if (def instanceof PrismContainerDefinition) {
					def = ((PrismContainerDefinition) def).findItemDefinition(name);
					if (def != null && !QNameUtil.hasNamespace(name)) {
						name = def.getName();
					}
				}
				addToSegments(name);
			} else if (first instanceof IdItemPathSegment) {
				// ignored (for now)
			} else {
				throw new UnsupportedOperationException("Canonicalization of non-name/non-ID segments is not supported: " + first);
			}
			itemPath = itemPath.rest();
		}
	}

	private void addToSegments(QName name) {
		if (!QNameUtil.hasNamespace(name)) {
			segments.add(new Segment(name, null, null));
			return;
		}
		String namespace = name.getNamespaceURI();
		int index = 0;
		Integer shortcut = null;
		for (Segment segment : segments) {
			if (namespace.equals(segment.name.getNamespaceURI())) {
				shortcut = index = segment.index;
				break;
			}
			if (QNameUtil.hasNamespace(segment.name) && segment.shortcut == null) {
				// we found a unique non-empty namespace! (so increase the index)
				index++;
			}
		}
		segments.add(new Segment(name, index, shortcut));
	}

	public List<Segment> getSegments() {
		return segments;
	}

	public int size() {
		return segments.size();
	}

	public CanonicalItemPath allUpToIncluding(int n) {
		if (n+1 < segments.size()) {
			return new CanonicalItemPath(segments.subList(0, n+1));
		} else {
			return new CanonicalItemPath(segments);
		}
	}

	public String asString() {
		StringBuilder sb = new StringBuilder();
		for (Segment segment : segments) {
			sb.append("\\");
			if (segment.shortcut == null) {        // always true for unqualified names
				sb.append(extractExplicitReplacements(QNameUtil.qNameToUri(segment.name)));
			} else {
				sb.append(SHORTCUT_MARKER_START).append(segment.shortcut).append(SHORTCUT_MARKER_END)
						.append(QNameUtil.DEFAULT_QNAME_URI_SEPARATOR_CHAR).append(segment.name.getLocalPart());
			}
		}
		return sb.toString();
	}

	private String extractExplicitReplacements(String uri) {
		for (Pair<String, String> mapping : EXPLICIT_REPLACEMENTS) {
			if (uri.startsWith(mapping.getKey())) {
				return SHORTCUT_MARKER_START + mapping.getValue() + SHORTCUT_MARKER_END + uri.substring(mapping.getKey().length());
			}
		}
		return uri;
	}

	@Override
	public String toString() {
		return asString();
	}

}
