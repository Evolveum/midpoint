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

package com.evolveum.midpoint.prism;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface TypeDefinition extends Definition {

	/**
	 * Returns compile-time class, if this type has any. For example, UserType.class, ObjectType.class, ExtensionType.class.
	 */
	@Nullable
	Class<?> getCompileTimeClass();

	/**
	 * Name of super type of this complex type definition. E.g. c:ObjectType is a super type for
	 * c:FocusType which is a super type for c:UserType. Or (more complex example) ri:ShadowAttributesType
	 * is a super type of ri:AccountObjectClass. (TODO is this really true?)
	 */
	@Nullable
	QName getSuperType();
}
