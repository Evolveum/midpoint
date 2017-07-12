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

package com.evolveum.midpoint.gui.api.model;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

/**
 * Loadable model whose object is always not null.
 * Used to reduce checks of the 'model.getObject() != null' kind.
 *
 * TODO remove redundant checks after annotations are checked at runtime (needs to be done in maven build)
 *
 * @author mederly
 */
public abstract class NonEmptyLoadableModel<T> extends LoadableModel<T> implements NonEmptyModel<T> {

	public NonEmptyLoadableModel(boolean alwaysReload) {
		super(alwaysReload);
	}

	@NotNull
    public T getObject() {
        T object = super.getObject();
		if (object == null) {
			throw new IllegalStateException("Model object is null");
		}
		return object;
    }

    public void setObject(@NotNull T object) {
        Validate.notNull(object, "Model object is to be set to null");
		super.setObject(object);
    }

	@NotNull
	abstract protected T load();
}
