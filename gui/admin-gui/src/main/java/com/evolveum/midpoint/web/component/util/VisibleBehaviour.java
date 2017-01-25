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

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.util.Producer;
import org.jetbrains.annotations.NotNull;

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class VisibleBehaviour extends VisibleEnableBehaviour {

	@NotNull private final SerializableBooleanProducer visibility;

	public VisibleBehaviour(@NotNull SerializableBooleanProducer visibility) {
		this.visibility = visibility;
	}

	@Override
	public boolean isVisible() {
		return visibility.run();
	}
}
