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

package com.evolveum.midpoint.prism.path;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *
 */
public interface ItemPathFactory {

	UniformItemPath emptyPath();

	UniformItemPath create(QName... qnames);

	UniformItemPath create(String... names);

	UniformItemPath create(Object... namesOrIdsOrSegments);

	UniformItemPath create(@NotNull ItemPath components);

	UniformItemPath create(UniformItemPath parentPath, QName subName);

	UniformItemPath create(UniformItemPath parentPath, UniformItemPath childPath);

	UniformItemPath create(List<ItemPathSegment> segments);

	UniformItemPath create(List<ItemPathSegment> segments, ItemPathSegment subSegment);

	UniformItemPath create(List<ItemPathSegment> segments, QName subName);

	UniformItemPath create(List<ItemPathSegment> segments, List<ItemPathSegment> additionalSegments);

	UniformItemPath create(ItemPathSegment... segments);

	UniformItemPath create(UniformItemPath parentPath, ItemPathSegment subSegment);

}
