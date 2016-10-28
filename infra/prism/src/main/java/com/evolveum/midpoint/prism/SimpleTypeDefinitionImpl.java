/*
 * Copyright (c) 2010-2016 Evolveum
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

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class SimpleTypeDefinitionImpl extends TypeDefinitionImpl implements SimpleTypeDefinition {

	public SimpleTypeDefinitionImpl(QName typeName, PrismContext prismContext) {
		super(typeName, prismContext);
	}

	@Override
	public void revive(PrismContext prismContext) {
	}

	@Override
	protected String getDebugDumpClassName() {
		return "STD";
	}

	@Override
	public String getDocClassName() {
		return "simple type";
	}

	@NotNull
	@Override
	public SimpleTypeDefinitionImpl clone() {
		SimpleTypeDefinitionImpl clone = new SimpleTypeDefinitionImpl(typeName, prismContext);
		super.copyDefinitionData(clone);
		return clone;
	}
}
