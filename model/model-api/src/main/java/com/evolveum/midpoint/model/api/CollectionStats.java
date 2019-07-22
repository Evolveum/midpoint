/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.model.api;

/**
 * Stats (summary information) about a specific collection.
 * 
 * @author semancik
 */
public class CollectionStats {
	
	private int objectCount;
	private Integer domainCount;
	
	public int getObjectCount() {
		return objectCount;
	}
	
	public void setObjectCount(int objectCount) {
		this.objectCount = objectCount;
	}
	
	public Integer getDomainCount() {
		return domainCount;
	}
	
	public void setDomainCount(Integer domainCount) {
		this.domainCount = domainCount;
	}
	
	public Float computePercentage() {
		if (domainCount == null) {
			return null;
		}
		return ((float)objectCount * 100f) / ((float)domainCount);
	}

	@Override
	public String toString() {
		return "CollectionStats(" + objectCount + "/" + domainCount + ")";
	}

}
