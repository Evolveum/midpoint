/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism;

/**
 * @author Pavol Mederly
 */
public class SerializationOptions implements Cloneable {

	private boolean serializeCompositeObjects;
    private boolean serializeReferenceNames;
	private ItemNameQualificationStrategy itemNameQualificationStrategy;
//	private NameQualificationStrategy itemTypeQualificationStrategy;
//	private NameQualificationStrategy itemPathQualificationStrategy;
//	private NameQualificationStrategy genericQualificationStrategy;

    public boolean isSerializeReferenceNames() {
        return serializeReferenceNames;
    }

    public void setSerializeReferenceNames(boolean serializeReferenceNames) {
        this.serializeReferenceNames = serializeReferenceNames;
    }

    public static SerializationOptions createSerializeReferenceNames(){
    	SerializationOptions serializationOptions = new SerializationOptions();
    	serializationOptions.setSerializeReferenceNames(true);
    	return serializationOptions;
    }

    public static boolean isSerializeReferenceNames(SerializationOptions options) {
        return options != null && options.isSerializeReferenceNames();
    }

	public boolean isSerializeCompositeObjects() {
		return serializeCompositeObjects;
	}

	public void setSerializeCompositeObjects(boolean serializeCompositeObjects) {
		this.serializeCompositeObjects = serializeCompositeObjects;
	}

	public static SerializationOptions createSerializeCompositeObjects() {
		SerializationOptions serializationOptions = new SerializationOptions();
		serializationOptions.setSerializeCompositeObjects(true);
		return serializationOptions;
	}

	public static boolean isSerializeCompositeObjects(SerializationOptions options) {
		return options != null && options.isSerializeCompositeObjects();
	}

	//	public ItemNameQualificationStrategy getItemNameQualificationStrategy() {
//		return itemNameQualificationStrategy;
//	}
//
//	public void setItemNameQualificationStrategy(ItemNameQualificationStrategy itemNameQualificationStrategy) {
//		this.itemNameQualificationStrategy = itemNameQualificationStrategy;
//	}
//
//	public NameQualificationStrategy getItemTypeQualificationStrategy() {
//		return itemTypeQualificationStrategy;
//	}
//
//	public void setItemTypeQualificationStrategy(NameQualificationStrategy itemTypeQualificationStrategy) {
//		this.itemTypeQualificationStrategy = itemTypeQualificationStrategy;
//	}
//
//	public NameQualificationStrategy getItemPathQualificationStrategy() {
//		return itemPathQualificationStrategy;
//	}
//
//	public void setItemPathQualificationStrategy(NameQualificationStrategy itemPathQualificationStrategy) {
//		this.itemPathQualificationStrategy = itemPathQualificationStrategy;
//	}
//
//	public NameQualificationStrategy getGenericQualificationStrategy() {
//		return genericQualificationStrategy;
//	}
//
//	public void setGenericQualificationStrategy(NameQualificationStrategy genericQualificationStrategy) {
//		this.genericQualificationStrategy = genericQualificationStrategy;
//	}

	public static SerializationOptions createQualifiedNames() {
		SerializationOptions opts = new SerializationOptions();
		opts.itemNameQualificationStrategy = ItemNameQualificationStrategy.ALWAYS_USE_FULL_URI;
//		opts.itemPathQualificationStrategy = NameQualificationStrategy.ALWAYS;
//		opts.itemTypeQualificationStrategy = NameQualificationStrategy.ALWAYS;
//		opts.genericQualificationStrategy = NameQualificationStrategy.ALWAYS;
		return opts;
	}

	public static boolean isFullItemNameUris(SerializationOptions opts) {
		return opts != null && opts.itemNameQualificationStrategy != ItemNameQualificationStrategy.ALWAYS_USE_FULL_URI;
	}

	public static boolean isUseNsProperty(SerializationOptions opts) {
		return opts == null || opts.itemNameQualificationStrategy == null || opts.itemNameQualificationStrategy == ItemNameQualificationStrategy.USE_NS_PROPERTY;
	}

	@Override
	protected SerializationOptions clone() {
		SerializationOptions clone = null;
		try {
			clone = (SerializationOptions) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		clone.serializeReferenceNames = this.serializeReferenceNames;
		clone.itemNameQualificationStrategy = itemNameQualificationStrategy;
		return clone;
	}
}
