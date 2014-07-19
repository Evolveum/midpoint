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

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
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

    private static final long serialVersionUID = 7321429132391159949L;

    protected String oid;
	protected String version;
	private T objectable = null;

	public PrismObject(QName name, Class<T> compileTimeClass) {
		super(name, compileTimeClass);
	}

    public PrismObject(QName name, Class<T> compileTimeClass, PrismContext prismContext) {
        super(name, compileTimeClass, prismContext);
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
            throw new SystemException("Unknown compile time class of this prism object '" + getElementName() + "'.");
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
		return (PrismContainer<?>) getValue().findItem(getExtensionContainerElementName(), PrismContainer.class);
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
		PrismObjectDefinition<T> objeDef = getDefinition();
		PrismContainerDefinition<Containerable> extensionDef = objeDef.findContainerDefinition(getExtensionContainerElementName());
		PrismContainer<?> extensionContainer = extensionDef.instantiate();
		getValue().add(extensionContainer);
		return extensionContainer;
	}

	private QName getExtensionContainerElementName() {
		return new QName(getElementName().getNamespaceURI(), PrismConstants.EXTENSION_LOCAL_NAME);
	}

	@Override
	public void applyDefinition(ItemDefinition definition) throws SchemaException {
    	if (!(definition instanceof PrismObjectDefinition)) {
    		throw new IllegalArgumentException("Cannot apply "+definition+" to object");
    	}
    	super.applyDefinition(definition);
	}

	@Override
	public <I extends Item<?>> I findItem(ItemPath path, Class<I> type) {
		try {
			return findCreateItem(path, type, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error:(path="+path+",type="+type+"): "+e.getMessage(),e);
		}
	}

	@Override
	public Item<?> findItem(ItemPath path) {
		try {
			return findCreateItem(path, Item.class, null, false);
		} catch (SchemaException e) {
			// This should not happen
			throw new SystemException("Internal Error:(path="+path+"): "+e.getMessage(),e);
		}
	}

	@Override
	public <I extends Item<?>> void removeItem(ItemPath path, Class<I> itemType) {
		// Objects are only a single-valued containers. The path of the object itself is "empty".
		// Fix this special behavior here.
		getValue().removeItem(path, itemType);
	}

	public void addReplaceExisting(Item<?> item) throws SchemaException {
		getValue().addReplaceExisting(item);
	}

	@Override
	public PrismObject<T> clone() {
		PrismObject<T> clone = new PrismObject<T>(getElementName(), getDefinition(), prismContext);
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
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(getCompileTimeClass(), ChangeType.DELETE, getPrismContext());
			objectDelta.setOid(getOid());
			return objectDelta;
		}
		// This must be a modify
		ObjectDelta<T> objectDelta = new ObjectDelta<T>(getCompileTimeClass(), ChangeType.MODIFY, getPrismContext());
		objectDelta.setOid(getOid());

		Collection<? extends ItemDelta> itemDeltas = new ArrayList<ItemDelta>();
		diffInternal(other, itemDeltas, ignoreMetadata, isLiteral);
		objectDelta.addModifications(itemDeltas);

		return objectDelta;
	}

	public ObjectDelta<T> createDelta(ChangeType changeType) {
		ObjectDelta<T> delta = new ObjectDelta<T>(getCompileTimeClass(), changeType, getPrismContext());
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
	public ItemPath getPath() {
		return ItemPath.EMPTY_PATH;
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
	 * this method ignores some part of the object during comparison (e.g. source demarcation in values)
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
	protected String additionalDumpDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("(").append(getOid());
		if (getVersion() != null) {
			sb.append(", v").append(getVersion());
		}
		PrismObjectDefinition<T> def = getDefinition();
		if (def != null) {
			sb.append(", ").append(DebugUtil.formatElementName(def.getTypeName()));
		}
		sb.append(")");
		return sb.toString();
	}

//	public Node serializeToDom() throws SchemaException {
//		Node doc = DOMUtil.getDocument();
//		serializeToDom(doc);
//		return doc;
//	}

}
