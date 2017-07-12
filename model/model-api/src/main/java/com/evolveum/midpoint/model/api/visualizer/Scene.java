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

package com.evolveum.midpoint.model.api.visualizer;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * @author mederly
 */
public interface Scene extends Serializable, DebugDumpable {

	Name getName();
	ChangeType getChangeType();

	@NotNull List<? extends Scene> getPartialScenes();
	@NotNull List<? extends SceneItem> getItems();

	boolean isOperational();

	Scene getOwner();

	/**
	 * Scene root path, relative to the owning scene root path.
	 */
	ItemPath getSourceRelPath();

	ItemPath getSourceAbsPath();

	/**
	 * Source container value where more details can be found.
	 * (For scenes that display object or value add.)
	 */
	PrismContainerValue<?> getSourceValue();

	PrismContainerDefinition<?> getSourceDefinition();

	/**
	 * Source object delta where more details can be found.
	 * (For scenes that display an object delta.)
	 */
	ObjectDelta<?> getSourceDelta();

	boolean isEmpty();
}
