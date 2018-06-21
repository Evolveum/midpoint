/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.common.mapping;

/**
 * @author semancik
 *
 */
public enum MappingEvaluationState {
	/**
	 * Nothing is initialized, we are just preparing the mapping.
	 */
	UNINITIALIZED,

	/**
	 * Prepared for evaluation. Declarative part of mapping is
	 * set and parsed. Variables are set. Source are set, but they can still change.
	 * The purpose of this state is that we can check if a mapping is activated
	 * (i.e. if the input changes will "trigger" the mapping). And we can sort
	 * mapping evaluation according to their dependencies.
	 */
	PREPARED,

	/**
	 * Mapping was evaluated, results are produced.
	 */
	EVALUATED;
}
