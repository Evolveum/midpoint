/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.delta.builder;

import com.evolveum.midpoint.prism.PrismValue;

import java.util.Collection;

/**
 * @author mederly
 */
public interface S_ValuesEntry {

    // Note: the names in this interface are to be kept as simple as possible.
    //
    // An exception is addRealValues, deleteRealValues, replaceRealValues: they must have a different name because
    // Java cannot distinguish between Collection<? extends PrismValue> and Collection<?>.

    S_MaybeDelete add(Object... realValues);
    S_MaybeDelete addRealValues(Collection<?> realValues);
    S_MaybeDelete add(PrismValue... values);
    S_MaybeDelete add(Collection<? extends PrismValue> values);
    S_ItemEntry delete(Object... realValues);
    S_ItemEntry deleteRealValues(Collection<?> realValues);
    S_ItemEntry delete(PrismValue... values);
    S_ItemEntry delete(Collection<? extends PrismValue> values);
    S_ItemEntry replace(Object... realValues);
    S_ItemEntry replaceRealValues(Collection<?> realValues);
    S_ItemEntry replace(PrismValue... values);
    S_ItemEntry replace(Collection<? extends PrismValue> values);
    S_ValuesEntry old(Object... realValues);
    S_ValuesEntry oldRealValues(Collection<?> realValues);
    S_ValuesEntry old(PrismValue... values);
    S_ValuesEntry old(Collection<? extends PrismValue> values);
}
