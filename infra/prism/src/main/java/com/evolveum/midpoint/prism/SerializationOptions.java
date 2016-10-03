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
public class SerializationOptions {

    private boolean serializeReferenceNames;
	private NameQualificationStrategy itemNameQualificationStrategy;
	private NameQualificationStrategy itemTypeQualificationStrategy;
	private NameQualificationStrategy itemPathQualificationStrategy;
	private NameQualificationStrategy genericQualificationStrategy;

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

	public NameQualificationStrategy getItemNameQualificationStrategy() {
		return itemNameQualificationStrategy;
	}

	public void setItemNameQualificationStrategy(NameQualificationStrategy itemNameQualificationStrategy) {
		this.itemNameQualificationStrategy = itemNameQualificationStrategy;
	}

	public NameQualificationStrategy getItemTypeQualificationStrategy() {
		return itemTypeQualificationStrategy;
	}

	public void setItemTypeQualificationStrategy(NameQualificationStrategy itemTypeQualificationStrategy) {
		this.itemTypeQualificationStrategy = itemTypeQualificationStrategy;
	}

	public NameQualificationStrategy getItemPathQualificationStrategy() {
		return itemPathQualificationStrategy;
	}

	public void setItemPathQualificationStrategy(NameQualificationStrategy itemPathQualificationStrategy) {
		this.itemPathQualificationStrategy = itemPathQualificationStrategy;
	}

	public NameQualificationStrategy getGenericQualificationStrategy() {
		return genericQualificationStrategy;
	}

	public void setGenericQualificationStrategy(NameQualificationStrategy genericQualificationStrategy) {
		this.genericQualificationStrategy = genericQualificationStrategy;
	}
}
