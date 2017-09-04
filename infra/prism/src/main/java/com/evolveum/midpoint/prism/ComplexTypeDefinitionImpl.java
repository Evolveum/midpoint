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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.PrettyPrinter;
import org.jetbrains.annotations.NotNull;
import com.evolveum.midpoint.util.QNameUtil;

import java.util.*;

import javax.xml.namespace.QName;

/**
 * TODO
 *
 * @author Radovan Semancik
 *
 */
public class ComplexTypeDefinitionImpl extends TypeDefinitionImpl implements ComplexTypeDefinition {

	private static final long serialVersionUID = 2655797837209175037L;
	@NotNull private final List<ItemDefinition> itemDefinitions = new ArrayList<>();
	private boolean containerMarker;
	private boolean objectMarker;
	private boolean xsdAnyMarker;
	private boolean listMarker;
	private QName extensionForType;

	private String defaultNamespace;
	@NotNull private List<String> ignoredNamespaces = new ArrayList<>();

	// temporary/experimental - to avoid trimming "standard" definitions
	// we reset this flag when cloning
	protected boolean shared = true;

	public ComplexTypeDefinitionImpl(@NotNull QName typeName, @NotNull PrismContext prismContext) {
		super(typeName, prismContext);
	}

	//region Trivia
	protected String getSchemaNamespace() {
		return getTypeName().getNamespaceURI();
	}

	/**
	 * Returns set of item definitions.
	 *
	 * The set contains all item definitions of all types that were parsed.
	 * Order of definitions is insignificant.
	 *
	 * @return set of definitions
	 */
	@NotNull
	@Override
	public List<? extends ItemDefinition> getDefinitions() {
		return Collections.unmodifiableList(itemDefinitions);
	}

	public void add(ItemDefinition<?> definition) {
		itemDefinitions.add(definition);
	}

	@Override
	public boolean isShared() {
		return shared;
	}

	@Override
	public QName getExtensionForType() {
		return extensionForType;
	}

	public void setExtensionForType(QName extensionForType) {
		this.extensionForType = extensionForType;
	}

	@Override
	public boolean isContainerMarker() {
		return containerMarker;
	}

	public void setContainerMarker(boolean containerMarker) {
		this.containerMarker = containerMarker;
	}

	@Override
	public boolean isObjectMarker() {
		return objectMarker;
	}

	@Override
	public boolean isXsdAnyMarker() {
		return xsdAnyMarker;
	}

	public void setXsdAnyMarker(boolean xsdAnyMarker) {
		this.xsdAnyMarker = xsdAnyMarker;
	}

	public boolean isListMarker() {
		return listMarker;
	}

	public void setListMarker(boolean listMarker) {
		this.listMarker = listMarker;
	}

	@Override
	public String getDefaultNamespace() {
		return defaultNamespace;
	}

	public void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}

	@Override
	@NotNull
	public List<String> getIgnoredNamespaces() {
		return ignoredNamespaces;
	}

	public void setIgnoredNamespaces(@NotNull List<String> ignoredNamespaces) {
		this.ignoredNamespaces = ignoredNamespaces;
	}

	public void setObjectMarker(boolean objectMarker) {
		this.objectMarker = objectMarker;
	}

	//endregion

	//region Creating definitions
	public PrismPropertyDefinitionImpl createPropertyDefinition(QName name, QName typeName) {
		PrismPropertyDefinitionImpl propDef = new PrismPropertyDefinitionImpl(name, typeName, prismContext);
		itemDefinitions.add(propDef);
		return propDef;
	}

	// Creates reference to other schema
	// TODO: maybe check if the name is in different namespace
	// TODO: maybe create entirely new concept of property reference?
	public PrismPropertyDefinition createPropertyDefinition(QName name) {
		PrismPropertyDefinition propDef = new PrismPropertyDefinitionImpl(name, null, prismContext);
		itemDefinitions.add(propDef);
		return propDef;
	}

	public PrismPropertyDefinitionImpl createPropertyDefinition(String localName, QName typeName) {
		QName name = new QName(getSchemaNamespace(), localName);
		return createPropertyDefinition(name, typeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		QName name = new QName(getSchemaNamespace(), localName);
		QName typeName = new QName(getSchemaNamespace(), localTypeName);
		return createPropertyDefinition(name, typeName);
	}
	//endregion

	//region Finding definitions

	// TODO deduplicate w.r.t. findNamedItemDefinition
	@Override
	public <T extends ItemDefinition> T findItemDefinition(@NotNull QName name, @NotNull Class<T> clazz, boolean caseInsensitive) {
		for (ItemDefinition def : getDefinitions()) {
			if (def.isValidFor(name, clazz, caseInsensitive)) {
				return (T) def;
			}
		}
		return null;
	}

	@Override
	public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
		for (;;) {
			if (path.isEmpty()) {
				throw new IllegalArgumentException("Cannot resolve empty path on complex type definition "+this);
			}
			ItemPathSegment first = path.first();
			if (first instanceof NameItemPathSegment) {
				QName firstName = ((NameItemPathSegment)first).getName();
				return findNamedItemDefinition(firstName, path.rest(), clazz);
			} else if (first instanceof IdItemPathSegment) {
				path = path.rest();
			} else if (first instanceof ParentPathSegment) {
				ItemPath rest = path.rest();
				ComplexTypeDefinition parent = getSchemaRegistry().determineParentDefinition(this, rest);
				if (rest.isEmpty()) {
					// requires that the parent is defined as an item (container, object)
					return (ID) getSchemaRegistry().findItemDefinitionByType(parent.getTypeName());
				} else {
					return parent.findItemDefinition(rest, clazz);
				}
			} else if (first instanceof ObjectReferencePathSegment) {
				throw new IllegalStateException("Couldn't use '@' path segment in this context. CTD=" + getTypeName() + ", path=" + path);
			} else {
				throw new IllegalStateException("Unexpected path segment: " + first + " in " + path);
			}
		}
    }

	// path starts with NamedItemPathSegment
	public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest, @NotNull Class<ID> clazz) {
		ID found = null;
		for (ItemDefinition def : getDefinitions()) {
			if (def.isValidFor(firstName, clazz, false)) {
				if (found != null) {
					throw new IllegalStateException("More definitions found for " + firstName + "/" + rest + " in " + this);
				}
                found = (ID) def.findItemDefinition(rest, clazz);
				if (QNameUtil.hasNamespace(firstName)) {
					return found;			// if qualified then there's no risk of matching more entries
				}
            }
        }
		return found;
	}
	//endregion

	/**
	 * Merge provided definition into this definition.
	 */
	@Override
	public void merge(ComplexTypeDefinition otherComplexTypeDef) {
		for (ItemDefinition otherItemDef: otherComplexTypeDef.getDefinitions()) {
			add(otherItemDef.clone());
		}
	}

	@Override
	public void revive(PrismContext prismContext) {
		if (this.prismContext != null) {
			return;
		}
		this.prismContext = prismContext;
		for (ItemDefinition def: itemDefinitions) {
			def.revive(prismContext);
		}
	}

	@Override
	public boolean isEmpty() {
		return itemDefinitions.isEmpty();
	}

	@NotNull
	@Override
	public ComplexTypeDefinitionImpl clone() {
		ComplexTypeDefinitionImpl clone = new ComplexTypeDefinitionImpl(this.typeName, prismContext);
		copyDefinitionData(clone);
		clone.shared = false;
		return clone;
	}

	public ComplexTypeDefinition deepClone() {
		return deepClone(new HashMap<>());
	}

	@NotNull
	@Override
	public ComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap) {
		if (ctdMap != null) {
			ComplexTypeDefinition clone = ctdMap.get(this.getTypeName());
			if (clone != null) {
				return clone; // already cloned
			}
		}
		ComplexTypeDefinitionImpl clone = clone(); // shallow
		if (ctdMap != null) {
			ctdMap.put(this.getTypeName(), clone);
		}
		clone.itemDefinitions.clear();
		for (ItemDefinition itemDef: this.itemDefinitions) {
			clone.itemDefinitions.add(itemDef.deepClone(ctdMap));
		}
		return clone;
	}

	protected void copyDefinitionData(ComplexTypeDefinitionImpl clone) {
		super.copyDefinitionData(clone);
        clone.containerMarker = this.containerMarker;
        clone.objectMarker = this.objectMarker;
        clone.xsdAnyMarker = this.xsdAnyMarker;
        clone.extensionForType = this.extensionForType;
		clone.defaultNamespace = this.defaultNamespace;
		clone.ignoredNamespaces = this.ignoredNamespaces;
        clone.itemDefinitions.addAll(this.itemDefinitions);
	}

	public void replaceDefinition(QName propertyName, ItemDefinition newDefinition) {
		for (int i=0; i<itemDefinitions.size(); i++) {
			ItemDefinition itemDef = itemDefinitions.get(i);
			if (itemDef.getName().equals(propertyName)) {
				if (!itemDef.getClass().isAssignableFrom(newDefinition.getClass())) {
					throw new IllegalArgumentException("The provided definition of class "+newDefinition.getClass().getName()+" does not match existing definition of class "+itemDef.getClass().getName());
				}
				if (!itemDef.getName().equals(newDefinition.getName())) {
					newDefinition = newDefinition.clone();
					((ItemDefinitionImpl) newDefinition).setName(propertyName);
				}
				// Make sure this is set, not add. set will keep correct ordering
				itemDefinitions.set(i, newDefinition);
				return;
			}
		}
		throw new IllegalArgumentException("The definition with name "+propertyName+" was not found in complex type "+getTypeName());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (containerMarker ? 1231 : 1237);
		result = prime * result + ((extensionForType == null) ? 0 : extensionForType.hashCode());
		result = prime * result + ((itemDefinitions == null) ? 0 : itemDefinitions.hashCode());
		result = prime * result + (objectMarker ? 1231 : 1237);
		result = prime * result + (xsdAnyMarker ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ComplexTypeDefinitionImpl other = (ComplexTypeDefinitionImpl) obj;
		if (containerMarker != other.containerMarker) {
			return false;
		}
		if (extensionForType == null) {
			if (other.extensionForType != null) {
				return false;
			}
		} else if (!extensionForType.equals(other.extensionForType)) {
			return false;
		}
		if (!itemDefinitions.equals(other.itemDefinitions)) {
			return false;
		}
		if (objectMarker != other.objectMarker) {
			return false;
		}
		if (xsdAnyMarker != other.xsdAnyMarker) {
			return false;
		}
		// TODO ignored and default namespaces
		return true;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<indent; i++) {
			sb.append(DebugDumpable.INDENT_STRING);
		}
		sb.append(toString());
		if (extensionForType != null) {
			sb.append(",ext:");
			sb.append(PrettyPrinter.prettyPrint(extensionForType));
		}
		if (ignored) {
			sb.append(",ignored");
		}
		if (containerMarker) {
			sb.append(",Mc");
		}
		if (objectMarker) {
			sb.append(",Mo");
		}
		if (xsdAnyMarker) {
			sb.append(",Ma");
		}
		extendDumpHeader(sb);
		for (ItemDefinition def : getDefinitions()) {
			sb.append("\n");
			sb.append(def.debugDump(indent+1));
			extendDumpDefinition(sb, def);
		}
		return sb.toString();
	}

	protected void extendDumpHeader(StringBuilder sb) {
		// Do nothing
	}

	protected void extendDumpDefinition(StringBuilder sb, ItemDefinition<?> def) {
		// Do nothing
	}

	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "CTD";
    }

    @Override
    public String getDocClassName() {
        return "complex type";
    }

	@Override
	public void trimTo(@NotNull Collection<ItemPath> paths) {
    	if (shared) {
    		// TODO switch this to warning before releasing this code (3.6.1 or 3.7)
    		throw new IllegalStateException("Couldn't trim shared definition: " + this);
		}
		for (Iterator<ItemDefinition> iterator = itemDefinitions.iterator(); iterator.hasNext(); ) {
			ItemDefinition<?> itemDef = iterator.next();
			ItemPath itemPath = new ItemPath(itemDef.getName());
			if (!ItemPath.containsSuperpathOrEquivalent(paths, itemPath)) {
				iterator.remove();
			} else if (itemDef instanceof PrismContainerDefinition) {
				PrismContainerDefinition<?> itemPcd = (PrismContainerDefinition<?>) itemDef;
				if (itemPcd.getComplexTypeDefinition() != null) {
					itemPcd.getComplexTypeDefinition().trimTo(ItemPath.remainder(paths, itemPath, false));
				}
			}
		}
	}

	//	@Override
//	public void accept(Visitor visitor) {
//		super.accept(visitor);
//		itemDefinitions.forEach(def -> def.accept(visitor));
//	}
}
