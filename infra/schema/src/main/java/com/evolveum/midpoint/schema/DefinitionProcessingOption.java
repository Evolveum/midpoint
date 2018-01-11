/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DefinitionProcessingOptionType;

/**
 * How should be definitions processed when object is to be retrieved. Currently applies at the model level;
 * but in the future it might be used also elsewhere.
 */

public enum DefinitionProcessingOption {

	/**
	 * Full definition processing for the specified item(s) is to be done.
	 * This applies recursively also to sub-items.
	 */
	FULL,

	/**
	 * Full definition processing for the specified item(s) is to be done, but only if the item(s) exist.
	 * This applies recursively also to sub-items.
	 *
	 * Currently supported on root level only.
	 */
	ONLY_IF_EXISTS,

	/**
	 * Definition for the specified item(s) is to be excluded from the resulting object, even if the item(s) do exist.
	 *
	 * NOT IMPLEMENTED YET
	 */
	NONE;

	
	public static DefinitionProcessingOption toDefinitionProcessingOption(DefinitionProcessingOptionType definitionProcessingOptionType) {
		if (definitionProcessingOptionType == null) {
			return null;
		}
		
		switch (definitionProcessingOptionType) {
			case FULL: return DefinitionProcessingOption.FULL;
			case ONLY_IF_EXISTS: return DefinitionProcessingOption.ONLY_IF_EXISTS;
			case NONE : return DefinitionProcessingOption.NONE;
			default:
				throw new IllegalStateException("Unknown definition processing option: " + definitionProcessingOptionType);
		}
	}
	
	public static DefinitionProcessingOptionType toDefinitionProcessingOptionType(DefinitionProcessingOption definitionProcessingOption) {
		if (definitionProcessingOption == null) {
			return null;
		}
	
		switch (definitionProcessingOption) {
			case FULL : return DefinitionProcessingOptionType.FULL;
			case ONLY_IF_EXISTS : return DefinitionProcessingOptionType.ONLY_IF_EXISTS;
			case NONE : return DefinitionProcessingOptionType.NONE;
			default:
				throw new IllegalStateException("Unknown definition processing option: " + definitionProcessingOption);
		}
		
	}
}
