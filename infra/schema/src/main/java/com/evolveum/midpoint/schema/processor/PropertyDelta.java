/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;

/**
 * @author semancik
 *
 */
public class PropertyDelta implements Dumpable, DebugDumpable {

	QName name;
	PropertyPath parentPath;
	
	Collection<Object> valuesToReplace = null;
	Collection<Object> valuesToAdd = null;
	Collection<Object> valuesToDelete = null;

	public PropertyDelta(QName name) {
		this.name = name;
		parentPath = new PropertyPath();
	}

	public PropertyDelta(PropertyPath parentPath, QName name) {
		this.name = name;
		this.parentPath = parentPath;
	}

	public PropertyDelta(PropertyPath propertyPath) {
		this.name = propertyPath.last();
		this.parentPath = propertyPath.allExceptLast();
	}
	
	public QName getName() {
		return name;
	}

	public void setName(QName name) {
		this.name = name;
	}

	public PropertyPath getParentPath() {
		return parentPath;
	}

	public void setParentPath(PropertyPath parentPath) {
		this.parentPath = parentPath;
	}
	
	public PropertyPath getPath() {
		return getParentPath().subPath(name);
	}
	
	public boolean isReplace() {
		return (valuesToReplace != null);
	}

	public Collection<Object> getValuesToAdd() {
		return valuesToAdd;
	}
	
	public Collection<Object> getValuesToDelete() {
		return valuesToDelete;
	}

	/**
	 * Returns all values regardless of whether they are added or removed or replaced.
	 * Useful for iterating over all the changed values.
	 */
	public <T> Collection<T> getValues(Class<T> type) {
		checkConsistence();
		if (valuesToReplace != null) {
			return (Collection)valuesToReplace;
		}
		return (Collection<T>) MiscUtil.union(valuesToAdd, valuesToDelete);
	}

	private void checkConsistence() {
		if (valuesToReplace !=null && (valuesToAdd != null || valuesToDelete != null)) {
			throw new IllegalStateException("The delta cannot be both 'replace' and 'add/delete' at the same time");
		}
	}

	public void merge(PropertyDelta deltaToMerge) {
		checkConsistence();
		deltaToMerge.checkConsistence();
		if (deltaToMerge.isEmpty()) {
			return;
		}
		if (deltaToMerge.valuesToReplace != null) {
			if (this.valuesToReplace != null) {
				throw new IllegalArgumentException("Cannot merge two 'replace' deltas");
			}
			this.valuesToReplace = newValueCollection();
			this.valuesToReplace.addAll(deltaToMerge.valuesToReplace);
		} else {
			addValuesToAdd(deltaToMerge.valuesToAdd);
			addValuesToDelete(deltaToMerge.valuesToDelete);
		}
	}
	
	public void applyTo(PropertyContainer propertyContainer) {
		Property property = propertyContainer.findOrCreateProperty(getParentPath(), getName());
		applyTo(property);
	}

	public void applyTo(Property property) {
		if (valuesToReplace != null) {
			property.replaceValues(valuesToReplace);
			return;
		}
		if (valuesToAdd != null) {
			property.addValues(valuesToAdd);
		}
		if (valuesToDelete != null) {
			property.deleteValues(valuesToDelete);
		}
	}

	public void addValuesToAdd(Collection<Object> newValues) {
		if (valuesToAdd == null) {
			valuesToAdd = newValueCollection();
		}
		valuesToAdd.addAll(newValues);
	}

	public void addValueToAdd(Object newValue) {
		if (valuesToAdd == null) {
			valuesToAdd = newValueCollection();
		}
		valuesToAdd.add(newValue);
	}

	public void addValuesToDelete(Collection<Object> newValues) {
		if (valuesToDelete == null) {
			valuesToDelete = newValueCollection();
		}
		valuesToDelete.addAll(newValues);
	}

	public void addValueToDelete(Object newValue) {
		if (valuesToDelete == null) {
			valuesToDelete = newValueCollection();
		}
		valuesToDelete.add(newValue);
	}

	public void setValuesToReplace(Collection<Object> newValues) {
		if (valuesToReplace == null) {
			valuesToReplace = newValueCollection();
		} else {
			valuesToReplace.clear();
		}
		valuesToReplace.addAll(newValues);
	}

	private boolean isEmpty() {
		if (valuesToAdd == null && valuesToDelete == null && valuesToReplace == null) {
			return true;
		}
		return false;
	}

	private Collection<Object> newValueCollection() {
		return new HashSet<Object>();
	}

	public boolean isValueToAdd(Object value) {
		if (valuesToAdd == null) {
			return false;
		}
		return valuesToAdd.contains(value);
	}

	public boolean isValueToDelete(Object value) {
		if (valuesToDelete == null) {
			return false;
		}
		return valuesToDelete.contains(value);
	}

	public static PropertyDelta createDelta(Class<? extends ObjectType> objectType, PropertyModificationType propMod, Schema schema) throws SchemaException {
		XPathHolder xpath = new XPathHolder(propMod.getPath());
		PropertyPath parentPath = new PropertyPath(xpath);
		PropertyContainerDefinition pcd = schema.findContainerDefinition(objectType, parentPath);
		Collection<? extends Item> items = pcd.parseItems(propMod.getValue().getAny());
		if (items.size() > 1) {
			throw new SchemaException("Expected presence of a single property in a object modification, but found "+items.size()+" instead");
		}
		if (items.size() < 1) {
			throw new SchemaException("Expected presence of a property value in a object modification, but found nothing");
		}
		Item item = items.iterator().next();
		if (!(item instanceof Property)) {
			throw new SchemaException("Expected presence of a property in a object modification, but found "+item.getClass().getSimpleName()+" instead",item.getName());
		}
		Property prop = (Property)item;
		PropertyDelta propDelta = new PropertyDelta(parentPath, prop.getName());
		if (propMod.getModificationType() == PropertyModificationTypeType.add) {
			propDelta.addValuesToAdd(prop.getValues());
		} else if (propMod.getModificationType() == PropertyModificationTypeType.delete) {
			propDelta.addValuesToDelete(prop.getValues());
		} else if (propMod.getModificationType() == PropertyModificationTypeType.replace) {
			propDelta.setValuesToReplace(prop.getValues());
		}
		
		return propDelta;
	}
	
	public Collection<PropertyModificationType> toPropertyModificationTypes() throws SchemaException {
		checkConsistence();
		Collection<PropertyModificationType> mods = new ArrayList<PropertyModificationType>();
		XPathHolder xpath = new XPathHolder(parentPath);
		Document document = DOMUtil.getDocument();
		Element xpathElement = xpath.toElement(SchemaConstants.C_PATH, document);
		if (valuesToReplace != null) {
			PropertyModificationType mod = new PropertyModificationType();
			mod.setPath(xpathElement);
			mod.setModificationType(PropertyModificationTypeType.replace);
			try {
				addModValues(mod, valuesToReplace, document);
			} catch (SchemaException e) {
				throw new SchemaException(e.getMessage()+" while converting property "+name,e);
			}
			mods.add(mod);
		}
		if (valuesToAdd != null) {
			PropertyModificationType mod = new PropertyModificationType();
			mod.setPath(xpathElement);
			mod.setModificationType(PropertyModificationTypeType.add);
			try {
				addModValues(mod, valuesToAdd, document);
			} catch (SchemaException e) {
				throw new SchemaException(e.getMessage()+" while converting property "+name,e);
			}
			mods.add(mod);
		}
		if (valuesToDelete != null) {
			PropertyModificationType mod = new PropertyModificationType();
			mod.setPath(xpathElement);
			mod.setModificationType(PropertyModificationTypeType.delete);
			try {
				addModValues(mod, valuesToDelete, document);
			} catch (SchemaException e) {
				throw new SchemaException(e.getMessage()+" while converting property "+name,e);
			}
			mods.add(mod);
		}
		return mods;
	}

	private void addModValues(PropertyModificationType mod, Collection<Object> values,
			Document document) throws SchemaException {
		Value modValue = new Value();
		mod.setValue(modValue);
		for (Object value: values) {
			modValue.getAny().add(XsdTypeConverter.toXsdElement(value, name, document));
		}
	}
	
	/**
	 * Assumes "replace" modification.
	 */
	public Property getPropertyNew(PropertyDefinition propertyDefinition) {
		if (valuesToAdd != null && valuesToDelete != null) {
			throw new IllegalStateException("Cannot fetch new property state, not a 'replace' delta");
		}
		Property prop = propertyDefinition.instantiate();
		if (valuesToReplace == null || valuesToReplace.isEmpty()) {
			return prop;
		}
		prop.getValues().addAll(valuesToReplace);
		return prop;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("PropertyDelta(");
		sb.append(parentPath).append(" / ").append(DebugUtil.prettyPrint(name));
		if (valuesToReplace != null) {
			sb.append(", REPLACE");
		}

		if (valuesToAdd != null) {
			sb.append(", ADD");
		}

		if (valuesToDelete != null) {
			sb.append(", DELETE");
		}
		sb.append(")");
		return sb.toString();
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append("PropertyDelta(");
		sb.append(parentPath).append(" / ").append(DebugUtil.prettyPrint(name)).append(")");
		
		if (valuesToReplace != null) {
			sb.append("\n");
			dumpValues(sb,"REPLACE",valuesToReplace, indent+1);
		}

		if (valuesToAdd != null) {
			sb.append("\n");
			dumpValues(sb,"ADD",valuesToAdd,indent+1);
		}

		if (valuesToDelete != null) {
			sb.append("\n");
			dumpValues(sb,"DELETE",valuesToDelete,indent+1);
		}
		
		return sb.toString();

	}

	public String dump() {
		return debugDump();
	}

	private void dumpValues(StringBuilder sb, String label, Collection<Object> values, int indent) {
		for (int i=0;i<indent;i++) {
			sb.append(INDENT_STRING);
		}
		sb.append(label).append(": ");
		if (values == null) {
			sb.append("(null)");
		} else {
			Iterator<Object> i = values.iterator();
			while(i.hasNext()) {
				sb.append(DebugUtil.prettyPrint(i.next()));
				if (i.hasNext()) {
					sb.append(", ");
				}
			}
		}
	}

}
