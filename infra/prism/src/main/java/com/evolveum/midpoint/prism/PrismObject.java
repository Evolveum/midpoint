/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SystemException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

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
 * This class is named MidPointObject instead of Object to avoid confusion with
 * java.lang.Object.
 *
 * @author Radovan Semancik
 *
 */
public class PrismObject<T extends Objectable> extends PrismContainer<T> {

	protected String oid;
	protected String version;
	private T objectable = null;

	public PrismObject(QName name, Class<T> compileTimeClass) {
		super(name, compileTimeClass);
	}

	public PrismObject(QName name, PrismObjectDefinition<T> definition, PrismContext prismContext) {
		super(name, definition, prismContext);
	}

	/**
	 * Returns Object ID (OID).
	 *
	 * May return null if the object does not have an OID.
	 *
	 * @return Object ID (OID)
	 */
	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public PrismObjectDefinition<T> getDefinition() {
		return (PrismObjectDefinition<T>) super.getDefinition();
	}

	public T asObjectable() {
		if (objectable != null) {
			return objectable;
		}
		Class<T> clazz = getCompileTimeClass();
        if (clazz == null) {
            throw new SystemException("Unknown compile time class of this prism object '" + getName() + "'.");
        }
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new SystemException("Can't create instance of class '" + clazz.getSimpleName() + "', it's abstract.");
        }
        try {
            objectable = clazz.newInstance();
            objectable.setupContainer(this);
            return (T) objectable;
        } catch (SystemException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SystemException("Couldn't create jaxb object instance of '" + clazz + "': "+ex.getMessage(), ex);
        }
	}

	public PrismContainer<?> getExtension() {
		return (PrismContainer<?>) getValue().findItem(new QName(getName().getNamespaceURI(), PrismConstants.EXTENSION_LOCAL_NAME), PrismContainer.class);
	}

	@Override
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
    	if (!(definition instanceof PrismObjectDefinition)) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to object");
    	}
    	super.applyDefinition(definition);
	}

	@Override
	public <I extends Item<?>> I findItem(PropertyPath path, Class<I> type) {
		try {
			return findCreateItem(path, type, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
	}

	@Override
	public Item<?> findItem(PropertyPath path) {
		try {
			return findCreateItem(path, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error: "+e.getMessage(),e);
		}
	}

	@Override
	<I extends Item<?>> I findCreateItem(PropertyPath path, Class<I> type, ItemDefinition itemDefinition, boolean create) throws SchemaException {
		// Objects are only a single-valued containers. The path of the object itself is "empty".
		// Fix this special behavior here.
		PropertyPathSegment first = path.first();
		PropertyPath rest = path.rest();
		Item<?> subitem = null; 
		if (rest.isEmpty()) {
			subitem = getValue().findCreateItem(first.getName(), Item.class, itemDefinition, create);
		} else {
			// This is intermediary item
			PrismContainerDefinition contDef = null;
			if (getDefinition() != null) {
				contDef = getDefinition().findContainerDefinition(first.getName());
				if (contDef == null) {
					throw new SchemaException("No definition for container " + first.getName() + " in " + this);
				}
				if (contDef instanceof PrismObjectDefinition) {
					throw new IllegalStateException("Got "+contDef+" as a subitem "+first.getName()+" from "+getDefinition()+
							"which was quite unexpected");
				}
			}
			subitem = getValue().findCreateItem(first.getName(), PrismContainer.class, contDef, create);
		}
		if (subitem == null) {
			return null;
		}
		if (subitem instanceof PrismContainer) {
			return ((PrismContainer<?>)subitem).findCreateItem(path, type, itemDefinition, create);
		} else if (type.isAssignableFrom(subitem.getClass())){
			return (I) subitem;
		} else {
			if (create) {
				throw new IllegalStateException("The " + type.getSimpleName() + " cannot be created because "
						+ subitem.getClass().getSimpleName() + " with the same name exists");
			}
			return null;
		}
	}

	public void addReplaceExisting(Item<?> item) throws SchemaException {
		getValue().addReplaceExisting(item);
	}

	@Override
	public PrismObject<T> clone() {
		PrismObject<T> clone = new PrismObject<T>(getName(), getDefinition(), prismContext);
		copyValues(clone);
		return clone;
	}

	protected void copyValues(PrismObject<T> clone) {
		super.copyValues(clone);
		clone.oid = this.oid;
		clone.version = this.version;
	}

	public ObjectDelta<T> diff(PrismObject<T> other) {
		return diff(other, true, false);
	}

	public ObjectDelta<T> diff(PrismObject<T> other, boolean ignoreMetadata, boolean isLiteral) {
		if (other == null) {
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(getCompileTimeClass(), ChangeType.DELETE);
			objectDelta.setOid(getOid());
			return objectDelta;
		}
		// This must be a modify
		ObjectDelta<T> objectDelta = new ObjectDelta<T>(getCompileTimeClass(), ChangeType.MODIFY);
		objectDelta.setOid(getOid());

		Collection<? extends ItemDelta> itemDeltas = new ArrayList<ItemDelta>();
		diffInternal(other, null, itemDeltas, ignoreMetadata, isLiteral);
		objectDelta.addModifications(itemDeltas);

		return objectDelta;
	}

	public ObjectDelta<T> createDelta(ChangeType changeType) {
		ObjectDelta<T> delta = new ObjectDelta<T>(getCompileTimeClass(), changeType);
		delta.setOid(getOid());
		return delta;
	}
	
	public ObjectDelta<T> createAddDelta() {
		ObjectDelta<T> delta = createDelta(ChangeType.ADD);
		// TODO: clone?
		delta.setObjectToAdd(this);
		return delta;
	}

	public ObjectDelta<T> createDeleteDelta() {
		ObjectDelta<T> delta = createDelta(ChangeType.DELETE);
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
	public PropertyPath getPath(PropertyPath pathPrefix) {
		if (pathPrefix != null && !pathPrefix.isEmpty()) {
			throw new IllegalStateException("It makes no sense to use pathPrefix for an object");
		}
		return new PropertyPath();
	}
	
	/**
	 * Returns a live DOM representation of the object.
	 * 
	 * Although the representation should be DOM-compliant, current implementation is only somehow compliant.
	 * E.g. it cannot provide owner document and the parent links may be broken in JAXB objects. But it may be
	 * good for some uses.
	 * 
	 * For a full DOM representation see {@link PrismJaxbProcessor} (serializeToDom).
	 */
	public Element asDomElement() {
		// TODO: OID
		return getValue().asDomElement();
	}

	private Collection<PropertyPath> listItemPaths() {
		List<PropertyPath> list = new ArrayList<PropertyPath>();
		addItemPathsToList(new PropertyPath(), list);
		return list;
	}

	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost preciselly equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent().
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((oid == null) ? 0 : oid.hashCode());
		return result;
	}

	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost preciselly equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent().
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrismObject other = (PrismObject) obj;
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		return true;
	}

	/**
	 * this method ignores some part of the object during comparison (e.g. source demarkation in values)
	 * These methods compare the "meaningful" parts of the objects.
	 */
	public boolean equivalent(Object obj) {
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		PrismObject other = (PrismObject) obj;
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		ObjectDelta<T> delta = diff(other, true, false);
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
		return toDebugType()+":"+getOid()+"("+getNamePropertyValue()+")";
	}
	
	private PrismProperty<String> getNameProperty() {
		QName elementName = getName();
		String myNamespace = elementName.getNamespaceURI();
		return findProperty(new QName(myNamespace, PrismConstants.NAME_LOCAL_NAME));
	}
	
	private String getNamePropertyValue() {
		PrismProperty<String> nameProperty = getNameProperty();
		if (nameProperty == null) {
			return null;
		}
		return nameProperty.getRealValue();
	}

	/**
	 * Returns short string identification of object type. It should be in a form
	 * suitable for log messages. There is no requirement for the type name to be unique,
	 * but it rather has to be compact. E.g. short element names are preferred to long
	 * QNames or URIs.
	 * @return
	 */
	public String toDebugType() {
		QName elementName = getName();
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
	protected String additionalDumpDescription() {
		return ", "+getOid();
	}

//	public Node serializeToDom() throws SchemaException {
//		Node doc = DOMUtil.getDocument();
//		serializeToDom(doc);
//		return doc;
//	}

}
