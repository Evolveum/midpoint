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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
public interface PrismObject<O extends Objectable> extends PrismContainer<O> {

//	public PrismObject(QName name, Class<O> compileTimeClass) {
//		super(name, compileTimeClass);
//	}
//
//    public PrismObject(QName name, Class<O> compileTimeClass, PrismContext prismContext) {
//        super(name, compileTimeClass, prismContext);
//    }
//
//    public PrismObject(QName name, PrismObjectDefinition<O> definition, PrismContext prismContext) {
//		super(name, definition, prismContext);
//	}

	PrismObjectValue<O> createNewValue();

	@NotNull
	PrismObjectValue<O> getValue();

	@Override
	void setValue(@NotNull PrismContainerValue<O> value) throws SchemaException;

	@Override
	boolean add(@NotNull PrismContainerValue newValue, boolean checkUniqueness) throws SchemaException;

	/**
	 * Returns Object ID (OID).
	 *
	 * May return null if the object does not have an OID.
	 *
	 * @return Object ID (OID)
	 */
	String getOid();

	void setOid(String oid);

	String getVersion();

	void setVersion(String version);

	@Override
	PrismObjectDefinition<O> getDefinition();

	@NotNull
	O asObjectable();

	PolyString getName();

	PrismContainer<?> getExtension();

	PrismContainerValue<?> getExtensionContainerValue();

	<I extends Item> I findExtensionItem(String elementLocalName);

	<I extends Item> I findExtensionItem(QName elementName);

	<I extends Item> void addExtensionItem(I item) throws SchemaException;

	PrismContainer<?> createExtension() throws SchemaException;

	@Override
	void applyDefinition(PrismContainerDefinition<O> definition) throws SchemaException;

	@Override
	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> void removeItem(ItemPath path, Class<I> itemType);

	void addReplaceExisting(Item<?, ?> item) throws SchemaException;

	@Override
	PrismObject<O> clone();

	@Override
	PrismObject<O> cloneComplex(CloneStrategy strategy);

	PrismObjectDefinition<O> deepCloneDefinition(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction);

	@NotNull
	ObjectDelta<O> diff(PrismObject<O> other);

	@NotNull
	ObjectDelta<O> diff(PrismObject<O> other, boolean ignoreMetadata, boolean isLiteral);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	Collection<? extends ItemDelta<?,?>> narrowModifications(Collection<? extends ItemDelta<?, ?>> modifications);

	ObjectDelta<O> createDelta(ChangeType changeType);

	ObjectDelta<O> createAddDelta();

	ObjectDelta<O> createModifyDelta();

	ObjectDelta<O> createDeleteDelta();

	@Override
	void setParent(PrismValue parentValue);

	@Override
	PrismValue getParent();

	@Override
	UniformItemPath getPath();

	@Override
	boolean equals(Object obj);

	/**
	 * this method ignores some part of the object during comparison (e.g. source demarcation in values)
	 * These methods compare the "meaningful" parts of the objects.
	 */
	boolean equivalent(Object obj);

	@Override
	String toString();

	/**
	 * Returns short string representing identity of this object.
	 * It should container object type, OID and name. It should be presented
	 * in a form suitable for log and diagnostic messages (understandable for
	 * system administrator).
	 */
	String toDebugName();

	/**
	 * Returns short string identification of object type. It should be in a form
	 * suitable for log messages. There is no requirement for the type name to be unique,
	 * but it rather has to be compact. E.g. short element names are preferred to long
	 * QNames or URIs.
	 */
	String toDebugType();

    /**
	 * Return display name intended for business users of midPoint
	 */
    String getBusinessDisplayName();

	@Override
	void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
			ConsistencyCheckScope scope);

	@Override
	void setImmutable(boolean immutable);

	PrismObject<O> cloneIfImmutable();

	PrismObject<O> createImmutableClone();

	@NotNull
	static <T extends Objectable> List<T> asObjectableList(@NotNull List<PrismObject<T>> objects) {
		return objects.stream()
				.map(o -> o.asObjectable())
				.collect(Collectors.toList());
	}

	static PrismObject<?> asPrismObject(Objectable o) {
		return o != null ? o.asPrismObject() : null;
	}

	static Objectable asObjectable(PrismObject<?> object) {
		return object != null ? object.asObjectable() : null;
	}
}
