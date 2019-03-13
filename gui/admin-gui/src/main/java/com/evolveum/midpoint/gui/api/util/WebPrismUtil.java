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
package com.evolveum.midpoint.gui.api.util;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;

/**
 * @author katka
 *
 */
public class WebPrismUtil {
	
	public static <ID extends ItemDefinition<I>, I extends Item<V, ID>, V extends PrismValue> String getHelpText(ID def) {
		String doc = def.getHelp();
        if (StringUtils.isEmpty(doc)) {
        	doc = def.getDocumentation();
        	if (StringUtils.isEmpty(doc)) {
            	return null;
            }
        }
        
        return doc.replaceAll("\\s{2,}", " ").trim();
	}

}
