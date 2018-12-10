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

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface PrismReferenceDefinition extends ItemDefinition<PrismReference> {
	QName getTargetTypeName();

	QName getCompositeObjectElementName();

	boolean isComposite();

	@NotNull
	@Override
	PrismReference instantiate();

	@NotNull
	@Override
	PrismReference instantiate(QName name);

	@Override
	ItemDelta createEmptyDelta(ItemPath path);

	@NotNull
	@Override
	PrismReferenceDefinition clone();
}
