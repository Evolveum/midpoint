/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.api;

/**
 *  Contains information about object deletion result; primarily needed by repository caching algorithms.
 *  Because it is bound to the current (SQL) implementation of the repository, avoid using this information
 *  for any other purposes.
 *
 *  EXPERIMENTAL.
 */
public class DeleteObjectResult {

	private final String objectTextRepresentation;
	private final String language;

	public DeleteObjectResult(String objectTextRepresentation, String language) {
		this.objectTextRepresentation = objectTextRepresentation;
		this.language = language;
	}

	/**
	 * The textual representation of the object as stored in repository. It is to be parsed when really
	 * necessary. Note that it does not contain information that is stored elsewhere (user photo, lookup table rows,
	 * certification cases, task result, etc).
	 */
	public String getObjectTextRepresentation() {
		return objectTextRepresentation;
	}

	/**
	 * Language in which the text representation is encoded.
	 */
	public String getLanguage() {
		return language;
	}
}
