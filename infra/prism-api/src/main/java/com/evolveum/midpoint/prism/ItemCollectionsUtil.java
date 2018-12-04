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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.MiscUtil;

import java.util.Collection;

/**
 *
 */
public class ItemCollectionsUtil {
	public static boolean compareCollectionRealValues(Collection<? extends PrismProperty> col1, Collection<? extends PrismProperty> col2) {
		return MiscUtil.unorderedCollectionEquals(col1, col2,
				(p1, p2) -> {
					if (!p1.getElementName().equals(p2.getElementName())) {
						return false;
					}
					Collection p1RealVals = p1.getRealValues();
					Collection p2RealVals = p2.getRealValues();
					return MiscUtil.unorderedCollectionEquals(p1RealVals, p2RealVals);
				});
	}
}
