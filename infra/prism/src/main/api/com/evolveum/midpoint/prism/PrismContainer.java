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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import java.util.*;
import java.util.function.Consumer;

/**
 * <p>
 * Property container groups properties into logical blocks.The reason for
 * grouping may be as simple as better understandability of data structure. But
 * the group usually means different meaning, source or structure of the data.
 * For example, the property container is frequently used to hold properties
 * that are dynamic, not fixed by a static schema. Such grouping also naturally
 * translates to XML and helps to "quarantine" such properties to avoid Unique
 * Particle Attribute problems.
 * </p><p>
 * Property Container contains a set of (potentially multi-valued) properties or inner property containers.
 * The order of properties is not significant, regardless of the fact that it
 * may be fixed in the XML representation. In the XML representation, each
 * element inside Property Container must be either Property or a Property
 * Container.
 * </p><p>
 * Property Container is mutable.
 * </p>
 *
 * @author Radovan Semancik
 */
public interface PrismContainer<C extends Containerable> extends Item<PrismContainerValue<C>,PrismContainerDefinition<C>>, PrismContainerable<C> {

//    public PrismContainer(QName name) {
//        super(name);
//    }
//
//    public PrismContainer(QName name, PrismContext prismContext) {
//        super(name, prismContext);
//    }
//
//    public PrismContainer(QName name, Class<C> compileTimeClass) {
//        super(name);
//		if (Modifier.isAbstract(compileTimeClass.getModifiers())) {
//            throw new IllegalArgumentException("Can't use class '" + compileTimeClass.getSimpleName() + "' as compile-time class for "+name+"; the class is abstract.");
//        }
//        this.compileTimeClass = compileTimeClass;
//    }
//
//    public PrismContainer(QName name, Class<C> compileTimeClass, PrismContext prismContext) {
//        this(name, compileTimeClass);
//        this.prismContext = prismContext;
//		if (prismContext != null) {
//			try {
//				prismContext.adopt(this);
//			} catch (SchemaException e) {
//				throw new SystemException("Schema exception when adopting freshly created PrismContainer: " + this);
//			}
//		}
//    }
//
//
//    public PrismContainer(QName name, PrismContainerDefinition<C> definition, PrismContext prismContext) {
//        super(name, definition, prismContext);
//    }

    Class<C> getCompileTimeClass();

	/**
	 * Returns true if this object can represent specified compile-time class.
	 * I.e. this object can be presented in the compile-time form that is an
	 * instance of a specified class.
	 */
	boolean canRepresent(Class<?> compileTimeClass);

	boolean canRepresent(QName type);

	@NotNull
    @Override
	Collection<C> getRealValues();

	@Override
	C getRealValue();

	PrismContainerValue<C> getOrCreateValue();

	PrismContainerValue<C> getValue();

	void setValue(@NotNull PrismContainerValue<C> value) throws SchemaException;

	@Override
	boolean add(@NotNull PrismContainerValue newValue, boolean checkUniqueness) throws SchemaException;

	@Override
	PrismContainerValue<C> getPreviousValue(PrismValue value);

	@Override
	PrismContainerValue<C> getNextValue(PrismValue value);

    PrismContainerValue<C> getValue(Long id);

	<T> void setPropertyRealValue(QName propertyName, T realValue) throws SchemaException;

	<T> void setPropertyRealValues(QName propertyName, T... realValues) throws SchemaException;

	<T> T getPropertyRealValue(ItemPath propertyPath, Class<T> type);

	/**
     * Convenience method. Works only on single-valued containers.
     */
	void add(Item<?, ?> item) throws SchemaException;

	PrismContainerValue<C> createNewValue();

	void mergeValues(PrismContainer<C> other) throws SchemaException;

	void mergeValues(Collection<PrismContainerValue<C>> otherValues) throws SchemaException;

	void mergeValue(PrismContainerValue<C> otherValue) throws SchemaException;

	/**
     * Remove all empty values
     */
	void trim();

	/**
     * Returns applicable property container definition.
     * <p>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property container definition
     */
	PrismContainerDefinition<C> getDefinition();

	/**
     * Sets applicable property container definition.
     *
     * @param definition the definition to set
     */
	void setDefinition(PrismContainerDefinition<C> definition);

	@Override
	void applyDefinition(PrismContainerDefinition<C> definition) throws SchemaException;

	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findItem(QName itemQName, Class<I> type);

	/**
	 * Returns true if the object and all contained prisms have proper definition.
	 */
	@Override
	boolean hasCompleteDefinition();

	@Override
	Object find(ItemPath path);

	@Override
	<IV extends PrismValue,ID extends ItemDefinition> PartiallyResolvedItem<IV,ID> findPartial(ItemPath path);

	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findCreateItem(QName itemQName, Class<I> type, boolean create) throws SchemaException;

	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findItem(ItemPath path, Class<I> type);

	<IV extends PrismValue,ID extends ItemDefinition> Item<IV,ID> findItem(ItemPath path);

	boolean containsItem(ItemPath itemPath, boolean acceptEmptyItem) throws SchemaException;

	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findCreateItem(ItemPath itemPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException;

	PrismContainerValue<C> findValue(long id);

	<T extends Containerable> PrismContainer<T> findContainer(ItemPath path);

	<I extends Item<?,?>> List<I> getItems(Class<I> type);

	@SuppressWarnings("unchecked")
	List<PrismContainer<?>> getContainers();

	<T> PrismProperty<T> findProperty(ItemPath path);

	PrismReference findReference(ItemPath path);

	PrismReference findReferenceByCompositeObjectElementName(QName elementName);

	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath containerPath,
			Class<I> type) throws SchemaException;

	// The "definition" parameter provides definition of item to create, in case that the container does not have
    // the definition (e.g. in case of "extension" containers)
	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I findOrCreateItem(ItemPath containerPath,
			Class<I> type, ID definition) throws SchemaException;

	<T extends Containerable> PrismContainer<T> findOrCreateContainer(UniformItemPath containerPath) throws SchemaException;

	<T extends Containerable> PrismContainer<T> findOrCreateContainer(QName containerName) throws SchemaException;

	<T> PrismProperty<T> findOrCreateProperty(ItemPath propertyPath) throws SchemaException;

	PrismReference findOrCreateReference(UniformItemPath propertyPath) throws SchemaException;

	PrismReference findOrCreateReference(QName propertyName) throws SchemaException;

	/**
     * Convenience method. Works only on single-valued containers.
     */
	void remove(Item<?, ?> item);

	void removeProperty(ItemPath path);

	void removeContainer(ItemPath path);

	void removeReference(ItemPath path);

	<IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> void removeItem(ItemPath path, Class<I> itemType);

	// Expects that the "self" path segment is NOT included in the basePath
    // is this method used anywhere?
//    void addItemPathsToList(ItemPath basePath, Collection<ItemPath> list) {
//    	boolean addIds = true;
//    	if (getDefinition() != null) {
//    		if (getDefinition().isSingleValue()) {
//    			addIds = false;
//    		}
//    	}
//    	for (PrismContainerValue<V> pval: getValues()) {
//    		ItemPath subpath = null;
//    		ItemPathSegment segment = null;
//    		if (addIds) {
//    			subpath = basePath.subPath(new IdItemPathSegment(pval.getId())).subPath(new NameItemPathSegment(getElementName()));
//    		} else {
//    			subpath = basePath.subPath(new NameItemPathSegment(getElementName()));
//    		}
//    		pval.addItemPathsToList(subpath, list);
//    	}
//    }

    @Override
    ContainerDelta<C> createDelta();

	@Override
	ContainerDelta<C> createDelta(ItemPath path);

	boolean isEmpty();

	@Override
	void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions,
			boolean prohibitRaw, ConsistencyCheckScope scope);

    @Override
    void assertDefinitions(boolean tolarateRaw, String sourceDescription) throws SchemaException;

	ContainerDelta<C> diff(PrismContainer<C> other);

	ContainerDelta<C> diff(PrismContainer<C> other, boolean ignoreMetadata, boolean isLiteral);

	List<? extends ItemDelta> diffModifications(PrismContainer<C> other);

	List<? extends ItemDelta> diffModifications(PrismContainer<C> other, boolean ignoreMetadata, boolean isLiteral);

	@Override
	PrismContainer<C> clone();

	@Override
	PrismContainer<C> cloneComplex(CloneStrategy strategy);

    PrismContainerDefinition<C> deepCloneDefinition(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction);

    @Override
    boolean containsEquivalentValue(PrismContainerValue<C> value);

	@Override
	boolean containsEquivalentValue(PrismContainerValue<C> value, Comparator<PrismContainerValue<C>> comparator);

	@Override
	void accept(Visitor visitor, ItemPath path, boolean recursive);

	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost precisely equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent().
	 */
	@Override
	int hashCode();

	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost precisely equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent().
	 */
	@Override
	boolean equals(Object obj);

	/**
     * This method ignores some part of the object during comparison (e.g. source demarcation in values)
     * These methods compare the "meaningful" parts of the objects.
     */
	boolean equivalent(Object obj);

	@Override
	String toString();

	@Override
	String debugDump(int indent);

	static <V extends Containerable> PrismContainer<V> newInstance(PrismContext prismContext, QName type) throws SchemaException {
        PrismContainerDefinition<V> definition = prismContext.getSchemaRegistry().findContainerDefinitionByType(type);
        if (definition == null) {
            throw new SchemaException("Definition for " + type + " couldn't be found");
        }
        return definition.instantiate();
    }

	static <V extends PrismContainerValue> void createParentIfNeeded(V value, ItemDefinition definition) throws SchemaException {
		if (value.getParent() != null) {
			return;
		}
		if (!(definition instanceof PrismContainerDefinition)) {
			throw new SchemaException("Missing or invalid definition for a PrismContainer: " + definition);
		}
		PrismContainer<?> rv = (PrismContainer) definition.instantiate();
		rv.add(value);
	}

	/**
	 * Optimizes (trims) definition tree by removing any definitions not corresponding to items in this container.
	 * Works recursively by sub-containers of this one.
	 * USE WITH CARE. Make sure the definitions are not shared by other objects!
	 */
	void trimDefinitionTree(Collection<UniformItemPath> alwaysKeep);

}
