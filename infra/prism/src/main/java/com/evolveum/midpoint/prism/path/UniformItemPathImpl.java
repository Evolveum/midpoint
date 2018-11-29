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
package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author semancik
 *
 */
public class UniformItemPathImpl implements UniformItemPath {

	public static final UniformItemPathImpl EMPTY_PATH = UniformItemPathImpl.createEmpty();

	private static UniformItemPathImpl createEmpty() {
		UniformItemPathImpl empty = new UniformItemPathImpl();
		empty.segments = Collections.emptyList();			// to ensure it won't get modified in no case
		return empty;
	}

	@NotNull private List<ItemPathSegment> segments;
	private Map<String, String> namespaceMap;

	@NotNull
	public static UniformItemPathImpl fromItemPath(ItemPath itemPath) {
		if (itemPath == null) {
			return EMPTY_PATH;
		} else if (itemPath instanceof UniformItemPathImpl) {
			return (UniformItemPathImpl) itemPath;
		} else {
			return new UniformItemPathImpl(itemPath);
		}
	}

	public static ItemPathSegment createSegment(QName qname, boolean variable) {
		if (ParentPathSegment.QNAME.equals(qname)) {
			return new ParentPathSegment();
		} else if (ObjectReferencePathSegment.QNAME.equals(qname)) {
			return new ObjectReferencePathSegment();
		} else if (IdentifierPathSegment.QNAME.equals(qname)) {
			return new IdentifierPathSegment();
		} else if (variable) {
			return new VariableItemPathSegment(qname);
		} else {
			return new NameItemPathSegment(qname);
		}
	}

	public void setNamespaceMap(Map<String, String> namespaceMap) {
		this.namespaceMap = namespaceMap;
	}

	public Map<String, String> getNamespaceMap() {
		return namespaceMap;
	}

	// use ItemPath.EMPTY_PATH from outside clients to avoid unnecessary instantiation
	private UniformItemPathImpl() {
		segments = new ArrayList<>();		// to provide room for growth
	}

	public UniformItemPathImpl(QName... qnames) {
		this.segments = new ArrayList<>(qnames.length);
		for (QName qname : qnames) {
			add(qname);
		}
	}

    public UniformItemPathImpl(String... names) {
        this.segments = new ArrayList<>(names.length);
        for (String name : names) {
            add(stringToQName(name));
        }
    }

    public UniformItemPathImpl(@NotNull ItemPath itemPath) {
		if (itemPath instanceof UniformItemPathImpl) {
			UniformItemPathImpl itemPathImpl = (UniformItemPathImpl) itemPath;
			this.segments = new ArrayList<>(itemPathImpl.size());
			this.segments.addAll(itemPathImpl.segments);
		} else {
			List<?> components = itemPath.getSegments();
			this.segments = new ArrayList<>();  // todo
			addAll(components);
		}
    }

	public UniformItemPathImpl(Object... namesOrIdsOrSegments) {
		this.segments = new ArrayList<>(namesOrIdsOrSegments.length);
		addAll(namesOrIdsOrSegments);
	}

	private QName stringToQName(String name) {
		Validate.notNull(name, "name");
		switch (name) {
			case ParentPathSegment.SYMBOL:
				return ParentPathSegment.QNAME;
			case ObjectReferencePathSegment.SYMBOL:
				return ObjectReferencePathSegment.QNAME;
			case IdentifierPathSegment.SYMBOL:
				return IdentifierPathSegment.QNAME;
			default:
				return new QName(name);
		}
	}

	public UniformItemPathImpl(UniformItemPath parentPath, QName subName) {
		this.segments = new ArrayList<>(parentPath.getSegments().size()+1);
		segments.addAll(parentPath.getSegments());
		add(subName);
	}

	public UniformItemPathImpl(UniformItemPath parentPath, UniformItemPath childPath) {
		this.segments = new ArrayList<>(parentPath.getSegments().size()+childPath.getSegments().size());
		segments.addAll(parentPath.getSegments());
		segments.addAll(childPath.getSegments());
	}

	public UniformItemPathImpl(List<ItemPathSegment> segments) {
		this.segments = new ArrayList<>(segments.size());
		this.segments.addAll(segments);
	}

	public UniformItemPathImpl(List<ItemPathSegment> segments, ItemPathSegment subSegment) {
		this.segments = new ArrayList<>(segments.size()+1);
		this.segments.addAll(segments);
		this.segments.add(subSegment);
	}

	public UniformItemPathImpl(List<ItemPathSegment> segments, QName subName) {
		this.segments = new ArrayList<>(segments.size()+1);
		this.segments.addAll(segments);
		add(subName);
	}

	public UniformItemPathImpl(List<ItemPathSegment> segments, List<ItemPathSegment> additionalSegments) {
		this.segments = new ArrayList<>(segments.size()+additionalSegments.size());
		this.segments.addAll(segments);
		this.segments.addAll(additionalSegments);
	}

	public UniformItemPathImpl(ItemPathSegment... segments) {
		this.segments = new ArrayList<>(segments.length);
		Collections.addAll(this.segments, segments);
	}

	public UniformItemPathImpl(UniformItemPath parentPath, ItemPathSegment subSegment) {
		this.segments = new ArrayList<>(parentPath.getSegments().size() + 1);
		this.segments.addAll(parentPath.getSegments());
		this.segments.add(subSegment);
	}

	@NotNull
	public UniformItemPath append(Object... components) {
		return new UniformItemPathImpl(segments, components);
	}

	public UniformItemPath append(Long id) {
		return append(new IdItemPathSegment(id));
	}

	public UniformItemPath append(ItemPathSegment subSegment) {
		return new UniformItemPathImpl(segments, subSegment);
	}

	public UniformItemPath append(UniformItemPath subPath) {
		UniformItemPathImpl newPath = new UniformItemPathImpl(segments);
		newPath.segments.addAll(subPath.getSegments());
		return newPath;
	}

	private void addAll(Collection<?> objects) {
		for (Object object : objects) {
			add(object);
		}
	}

	private void addAll(Object[] objects) {
		for (Object object : objects) {
			add(object);
		}
	}

	private void add(Object object) {
		if (object instanceof UniformItemPathImpl) {
			segments.addAll(((UniformItemPathImpl) object).segments);
		} else if (object instanceof ItemPath) {
			addAll(((ItemPath) object).getSegments());
		} else if (object instanceof ItemPathSegment) {
			add((ItemPathSegment) object);
		} else if (object instanceof QName) {
			add((QName) object);
		} else if (object instanceof String) {
			add(stringToQName((String) object));
		} else if (object == null || object instanceof Long) {
			this.segments.add(new IdItemPathSegment((Long) object));
		} else if (object instanceof Integer) {
			this.segments.add(new IdItemPathSegment(((Integer) object).longValue()));
		} else if (object instanceof Collection) {
			addAll((Collection<?>) object);
		} else if (object instanceof Object[]) {            // todo what about other kinds of array ?
			addAll((Object[]) object);
		} else {
			throw new IllegalArgumentException("Invalid item path segment value: " + object);
		}
	}

	private void add(QName qname) {
		this.segments.add(createSegment(qname, false));
	}

	private void add(ItemPathSegment segment) {
		this.segments.add(segment);
	}

	@NotNull
	public List<ItemPathSegment> getSegments() {
		return segments;
	}

	public ItemPathSegment first() {
		if (segments.isEmpty()) {
			return null;
		}
		return segments.get(0);
	}

	@NotNull
	public UniformItemPath rest() {
		return rest(1);
	}

	@NotNull
	@Override
	public UniformItemPath rest(int n) {
		if (n == 0) {
			return this;
		} else if (segments.size() < n) {
			return EMPTY_PATH;
		} else {
			return subPath(n, size());
		}
	}

	@NotNull
	public UniformItemPath tail(int n) {
		return rest(n);
	}

	private NameItemPathSegment lastNamed() {
        for (int i = segments.size()-1; i >= 0; i--) {
            if (segments.get(i) instanceof NameItemPathSegment) {
                return (NameItemPathSegment) segments.get(i);
            }
        }
        return null;
    }

    @Nullable
	public ItemPathSegment last() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(segments.size()-1);
	}

	public UniformItemPath firstAsPath() {
		return new UniformItemPathImpl(first());
	}

	@NotNull
	public UniformItemPath allExceptLast() {
		return subPath(0, segments.size()-1);
	}

    @NotNull
    public UniformItemPath allUpToLastName() {
		return subPath(0, lastNameIndex());
    }

    public UniformItemPath allUpToIncluding(int i) {
        return subPath(0, i+1);
    }

    public int size() {
		return segments.size();
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}

	/**
	 * Makes the path "normal" by inserting null Id segments where they were omitted.
	 */
	public UniformItemPathImpl normalize() {
		UniformItemPathImpl normalizedPath = new UniformItemPathImpl();
		ItemPathSegment lastSegment = null;
		Iterator<ItemPathSegment> iterator = segments.iterator();
		while (iterator.hasNext()) {
			ItemPathSegment origSegment = iterator.next();
			if (lastSegment != null && !(lastSegment instanceof IdItemPathSegment) &&
					!(origSegment instanceof IdItemPathSegment)) {
				normalizedPath.segments.add(new IdItemPathSegment());
			}
			normalizedPath.segments.add(origSegment);
			lastSegment = origSegment;
		}
		if (lastSegment != null && !(lastSegment instanceof IdItemPathSegment) &&
				// Make sure we do not insert the Id segment as a last one. That is not correct and it would spoil comparing paths
				iterator.hasNext()) {
			normalizedPath.segments.add(new IdItemPathSegment());
		}
		return normalizedPath;
	}

	@NotNull
	public UniformItemPath removeIds() {
		UniformItemPathImpl rv = new UniformItemPathImpl();
		for (ItemPathSegment segment : segments) {
			if (!(segment instanceof IdItemPathSegment)) {
				rv.add(segment);
			}
		}
		return rv;
	}

    @NotNull
    public UniformItemPath namedSegmentsOnly() {
        UniformItemPathImpl rv = new UniformItemPathImpl();
        for (ItemPathSegment segment : segments) {
            if (segment instanceof NameItemPathSegment) {
                rv.add(((NameItemPathSegment) segment).getName());
            }
        }
        return rv;
    }

	@NotNull
	public UniformItemPath stripVariableSegment() {
		return startsWithVariable() ? rest() : this;
	}

	public UniformItemPath append(QName childName) {
		return new UniformItemPathImpl(this, childName);
	}

	public UniformItemPath remainder(ItemPath prefix) {
		return new UniformItemPathImpl(ItemPathComparatorUtil.remainder(this, prefix));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + segments.hashCode();
		return result;
	}

    /**
     * More strict version of ItemPath comparison. Does not use any normalization
     * nor approximate matching QNames via QNameUtil.match.
     *
     * For semantic-level comparison, please use equivalent(..) method.
     */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UniformItemPathImpl other = (UniformItemPathImpl) obj;
		if (!segments.equals(other.segments))
			return false;
		return true;
	}

    public UniformItemPath clone() {
        UniformItemPathImpl clone = new UniformItemPathImpl();
        for (ItemPathSegment segment : segments) {
            clone.segments.add(segment.clone());
        }
        if (namespaceMap != null) {
            clone.namespaceMap = new HashMap<>(namespaceMap);
        }
        return clone;
    }

	public ItemPathType asItemPathType() {
		return new ItemPathType(this);
	}

	@NotNull
	@Override
	public UniformItemPath toUniform(PrismContext prismContext) {
		return this;
	}

	@Override
	public ItemName lastName() {
		NameItemPathSegment lastNamed = lastNamed();
		return lastNamed != null ? lastNamed.getName() : null;
	}

	@Override
	public UniformItemPathImpl subPath(int from, int to) {
		int fromClipped = Math.max(0, from);
		int toClipped = Math.min(to, segments.size());
		if (fromClipped >= toClipped) {
			return EMPTY_PATH;
		} else {
			return new UniformItemPathImpl(segments.subList(fromClipped, toClipped));
		}
	}

	@Override
	public ItemPathSegment getSegment(int i) {
		return segments.get(i);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		shortDump(sb);
		return sb.toString();
	}
}
