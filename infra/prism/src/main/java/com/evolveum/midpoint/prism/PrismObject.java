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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * Common supertype for all identity objects. Defines basic properties that each
 * object must have to live in our system (identifier, name).
 *
 * Objects consists of identifier and name (see definition below) and a set of
 * properties represented as XML elements in the object's body. The attributes
 * are represented as first-level XML elements (tags) of the object XML
 * representation and may be also contained in other tags (e.g. extension,
 * attributes). The QName (namespace and local name) of the element holding the
 * property is considered to be a property name.
 *
 * This class is named PrismObject instead of Object to avoid confusion with
 * java.lang.Object.
 *
 * @author Radovan Semancik
 *
 * Class invariant: has at most one value (potentially empty).
 * When making this object immutable and there's no value, we create one; in order
 * to prevent exceptions on later getValue calls.
 */
public class PrismObject<O extends Objectable> extends PrismContainer<O> {

    private static final long serialVersionUID = 7321429132391159949L;

    private static final String PROPERTY_NAME_LOCALPART = "name";

	public PrismObject(QName name, Class<O> compileTimeClass) {
		super(name, compileTimeClass);
	}

    public PrismObject(QName name, Class<O> compileTimeClass, PrismContext prismContext) {
        super(name, compileTimeClass, prismContext);
    }

    public PrismObject(QName name, PrismObjectDefinition<O> definition, PrismContext prismContext) {
		super(name, definition, prismContext);
	}

	public PrismObjectValue<O> createNewValue() {
		checkMutability();
		PrismObjectValue<O> newValue = new PrismObjectValue<>(prismContext);
		try {
			add(newValue, false);
			return newValue;
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: " + e.getMessage(), e);
		}
	}

	@NotNull
	public PrismObjectValue<O> getValue() {
		if (values.isEmpty()) {
			return createNewValue();
		} else if (values.size() > 1) {
			throw new IllegalStateException("PrismObject with more than one value: " + values);
		}
		return (PrismObjectValue<O>) values.get(0);
	}

	@Override
	public void setValue(@NotNull PrismContainerValue<O> value) throws SchemaException {
		clear();
		add(value, false);
	}

	@Override
	public boolean add(@NotNull PrismContainerValue newValue, boolean checkUniqueness) throws SchemaException {
		if (!(newValue instanceof PrismObjectValue)) {
			throw new IllegalArgumentException("Couldn't add non-PrismObjectValue to a PrismObject: value = "
					+ newValue + ", object = " + this);
		}
		if (values.size() > 1) {
			throw new IllegalStateException("PrismObject with more than one value: " + this);
		} else if (values.size() == 1) {
			PrismObjectValue<O> value = (PrismObjectValue<O>) values.get(0);
			if (value.isEmpty() && value.getOid() == null) {
				clear();
			} else {
				throw new IllegalStateException("PrismObject cannot have more than one value. New value = " + newValue
						+ ", object = " + this);
			}
		}
		return super.add(newValue, checkUniqueness);
	}

	/**
	 * Returns Object ID (OID).
	 *
	 * May return null if the object does not have an OID.
	 *
	 * @return Object ID (OID)
	 */
	public String getOid() {
		return getValue().getOid();
	}

	public void setOid(String oid) {
		checkMutability();
		getValue().setOid(oid);
	}

	public String getVersion() {
		return getValue().getVersion();
	}

	public void setVersion(String version) {
		checkMutability();
		getValue().setVersion(version);
	}

	@Override
	public PrismObjectDefinition<O> getDefinition() {
		return (PrismObjectDefinition<O>) super.getDefinition();
	}

	@NotNull
	public O asObjectable() {
		return getValue().asObjectable();
	}

	public PolyString getName() {
		PrismProperty<PolyString> nameProperty = getValue().findProperty(getNamePropertyElementName());
		if (nameProperty == null) {
			return null;
		}
		return nameProperty.getRealValue();
	}

	private QName getNamePropertyElementName() {
		return new QName(getElementName().getNamespaceURI(), PrismConstants.NAME_LOCAL_NAME);
	}

	public PrismContainer<?> getExtension() {
		return (PrismContainer<?>) getValue().findItem(getExtensionContainerElementName(), PrismContainer.class);
	}

	public PrismContainerValue<?> getExtensionContainerValue() {
		PrismContainer<?> extension = getExtension();
		if (extension == null || extension.getValues().isEmpty()) {
			return null;
		} else {
			return extension.getValue();
		}
	}

	public <I extends Item> I findExtensionItem(QName elementName) {
		PrismContainer<?> extension = getExtension();
		if (extension == null) {
			return null;
		}
		return (I) extension.findItem(elementName);
	}

	public <I extends Item> void addExtensionItem(I item) throws SchemaException {
		PrismContainer<?> extension = getExtension();
		if (extension == null) {
			extension = createExtension();
		}
		extension.add(item);
	}

	public PrismContainer<?> createExtension() throws SchemaException {
		PrismObjectDefinition<O> objeDef = getDefinition();
		PrismContainerDefinition<Containerable> extensionDef = objeDef.findContainerDefinition(getExtensionContainerElementName());
		PrismContainer<?> extensionContainer = extensionDef.instantiate();
		getValue().add(extensionContainer);
		return extensionContainer;
	}

	private QName getExtensionContainerElementName() {
		return new QName(getElementName().getNamespaceURI(), PrismConstants.EXTENSION_LOCAL_NAME);
	}

	@Override
	public void applyDefinition(PrismContainerDefinition<O> definition) throws SchemaException {
    	if (!(definition instanceof PrismObjectDefinition)) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to object");
    	}
    	super.applyDefinition(definition);
	}

	@Override
	public <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> void removeItem(ItemPath path, Class<I> itemType) {
		// Objects are only a single-valued containers. The path of the object itself is "empty".
		// Fix this special behavior here.
		getValue().removeItem(path, itemType);
	}

	public void addReplaceExisting(Item<?,?> item) throws SchemaException {
		getValue().addReplaceExisting(item);
	}

	@Override
	public PrismObject<O> clone() {
		if (prismContext != null && prismContext.getMonitor() != null) {
			prismContext.getMonitor().beforeObjectClone(this);
		}

		PrismObject<O> clone = new PrismObject<O>(getElementName(), getDefinition(), prismContext);
		copyValues(clone);

		if (prismContext != null && prismContext.getMonitor() != null) {
			prismContext.getMonitor().afterObjectClone(this, clone);
		}

		return clone;
	}

	protected void copyValues(PrismObject<O> clone) {
		super.copyValues(clone);
	}

	public PrismObjectDefinition<O> deepCloneDefinition(boolean ultraDeep) {
		return (PrismObjectDefinition<O>) super.deepCloneDefinition(ultraDeep);
	}

	@NotNull
	public ObjectDelta<O> diff(PrismObject<O> other) {
		return diff(other, true, false);
	}

	@NotNull
	public ObjectDelta<O> diff(PrismObject<O> other, boolean ignoreMetadata, boolean isLiteral) {
		if (other == null) {
			ObjectDelta<O> objectDelta = new ObjectDelta<O>(getCompileTimeClass(), ChangeType.DELETE, getPrismContext());
			objectDelta.setOid(getOid());
			return objectDelta;
		}
		// This must be a modify
		ObjectDelta<O> objectDelta = new ObjectDelta<O>(getCompileTimeClass(), ChangeType.MODIFY, getPrismContext());
		objectDelta.setOid(getOid());

		Collection<? extends ItemDelta> itemDeltas = new ArrayList<>();
		diffInternal(other, itemDeltas, ignoreMetadata, isLiteral);
		objectDelta.addModifications(itemDeltas);

		return objectDelta;
	}

	public ObjectDelta<O> createDelta(ChangeType changeType) {
		ObjectDelta<O> delta = new ObjectDelta<>(getCompileTimeClass(), changeType, getPrismContext());
		delta.setOid(getOid());
		return delta;
	}

	public ObjectDelta<O> createAddDelta() {
		ObjectDelta<O> delta = createDelta(ChangeType.ADD);
		// TODO: clone?
		delta.setObjectToAdd(this);
		return delta;
	}

	public ObjectDelta<O> createModifyDelta() {
		ObjectDelta<O> delta = createDelta(ChangeType.MODIFY);
		delta.setOid(this.getOid());
		return delta;
	}

	public ObjectDelta<O> createDeleteDelta() {
		ObjectDelta<O> delta = createDelta(ChangeType.DELETE);
		delta.setOid(this.getOid());
		return delta;
	}

	@Override
	public void setParent(PrismValue parentValue) {
		throw new IllegalStateException("Cannot set parent for an object");
	}

	@Override
	public PrismValue getParent() {
		return null;
	}

	@Override
	public ItemPath getPath() {
		return ItemPath.EMPTY_PATH;
	}

	@Override
	public boolean equals(Object obj) {
		if (prismContext != null && prismContext.getMonitor() != null) {
			prismContext.getMonitor().recordPrismObjectCompareCount(this, obj);
		}
		return super.equals(obj);
	}

	/**
	 * this method ignores some part of the object during comparison (e.g. source demarcation in values)
	 * These methods compare the "meaningful" parts of the objects.
	 */
	public boolean equivalent(Object obj) {
		if (prismContext != null && prismContext.getMonitor() != null) {
			prismContext.getMonitor().recordPrismObjectCompareCount(this, obj);
		}
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		PrismObject other = (PrismObject) obj;
		ObjectDelta<O> delta = diff(other, true, false);
		return delta.isEmpty();
	}

	@Override
	public String toString() {
		return toDebugName();
	}

	/**
	 * Returns short string representing identity of this object.
	 * It should container object type, OID and name. It should be presented
	 * in a form suitable for log and diagnostic messages (understandable for
	 * system administrator).
	 */
	public String toDebugName() {
		return toDebugType()+":"+getOid()+"("+getNamePropertyStringValue()+")";
	}

	private PrismProperty<PolyString> getNameProperty() {
		QName elementName = getElementName();
		String myNamespace = elementName.getNamespaceURI();
		return findProperty(new QName(myNamespace, PrismConstants.NAME_LOCAL_NAME));
	}

	private String getNamePropertyStringValue() {
		PrismProperty<PolyString> nameProperty = getNameProperty();
		if (nameProperty == null) {
			return null;
		}
		PolyString realValue = nameProperty.getRealValue();
		if (realValue == null) {
			return null;
		}
		return realValue.getOrig();
	}

	/**
	 * Returns short string identification of object type. It should be in a form
	 * suitable for log messages. There is no requirement for the type name to be unique,
	 * but it rather has to be compact. E.g. short element names are preferred to long
	 * QNames or URIs.
	 * @return
	 */
	public String toDebugType() {
		QName elementName = getElementName();
		if (elementName == null) {
			return "(unknown)";
		}
		return elementName.getLocalPart();
	}

	/**
	 * Return a human readable name of this class suitable for logs.
	 */
	@Override
	protected String getDebugDumpClassName() {
		return "PO";
	}

	@Override
	protected void appendDebugDumpSuffix(StringBuilder sb) {
		sb.append("(").append(getOid());
		if (getVersion() != null) {
			sb.append(", v").append(getVersion());
		}
		PrismObjectDefinition<O> def = getDefinition();
		if (def != null) {
			sb.append(", ").append(DebugUtil.formatElementName(def.getTypeName()));
		}
		sb.append(")");
	}

    /**
	 * Return display name intended for business users of midPoint
	 */
    public String getBusinessDisplayName() {
        return getNamePropertyStringValue();
    }

	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
			ConsistencyCheckScope scope) {
		super.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
		if (size() > 1) {
			throw new IllegalStateException("PrismObject holding more than one value: " + size() + ": " + this);
		}
		getValue();			// checks the type by casting to POV
	}

	@Override
	public void setImmutable(boolean immutable) {
		if (!this.immutable && immutable && values.isEmpty()) {
			createNewValue();
		}
		super.setImmutable(immutable);
	}

	public PrismObject<O> cloneIfImmutable() {
        return isImmutable() ? clone() : this;
	}

	public PrismObject<O> createImmutableClone() {
		PrismObject<O> clone = clone();
		clone.setImmutable(true);
		return clone;
	}

	@NotNull
	public static <T extends Objectable> List<T> asObjectableList(@NotNull List<PrismObject<T>> objects) {
		return objects.stream()
				.map(PrismObject::asObjectable)
				.collect(Collectors.toList());
	}

	public static PrismObject<?> asPrismObject(Objectable o) {
		return o != null ? o.asPrismObject() : null;
	}
}
