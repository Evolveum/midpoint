/*
 * Copyright (c) 2010-2018 Evolveum
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

import org.apache.wicket.model.AbstractReadOnlyModel;

import java.util.Arrays;
import java.util.List;

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class ReadOnlyEnumValuesModel<E extends Enum<E>> extends AbstractReadOnlyModel<List<E>> {

	private final List<E> values;

	public ReadOnlyEnumValuesModel(Class<E> e) {
		this.values = Arrays.asList(e.getEnumConstants());
	}

	@Override
	public List<E> getObject() {
		return values;
	}
}
