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

package com.evolveum.midpoint.repo.sql.helpers;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lazyman, katkav, mederly
 */
@Component
public class NameResolutionHelper {

    // TODO keep names between invocations (e.g. when called from searchObjects/searchContainers)
    public <C extends Containerable> void resolveNamesIfRequested(Session session, PrismContainerValue<C> containerValue, Collection<SelectorOptions<GetOperationOptions>> options) {
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (GetOperationOptions.isResolveNames(rootOptions)) {
            final List<String> oidsToResolve = new ArrayList<>();
            Visitor oidExtractor = new Visitor() {
                @Override
                public void visit(Visitable visitable) {
                    if (visitable instanceof PrismReferenceValue) {
                        PrismReferenceValue value = (PrismReferenceValue) visitable;
                        if (value.getTargetName() != null) {    // just for sure
                            return;
                        }
                        if (value.getObject() != null) {        // improbable but possible
                            value.setTargetName(value.getObject().getName());
                            return;
                        }
                        if (value.getOid() == null) {           // shouldn't occur as well
                            return;
                        }
                        oidsToResolve.add(value.getOid());
                    }
                }
            };
            containerValue.accept(oidExtractor);

			if (!oidsToResolve.isEmpty()) {
				Query query = session.getNamedQuery("resolveReferences");
				query.setParameterList("oid", oidsToResolve);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

				List<Map<String, Object>> results = query.list();
				final Map<String, PolyString> oidNameMap = consolidateResults(results);

                Visitor nameSetter = new Visitor() {
                    @Override
                    public void visit(Visitable visitable) {
                        if (visitable instanceof PrismReferenceValue) {
                            PrismReferenceValue value = (PrismReferenceValue) visitable;
                            if (value.getTargetName() == null && value.getOid() != null) {
                                value.setTargetName(oidNameMap.get(value.getOid()));
                            }
                        }
                    }
                };
                containerValue.accept(nameSetter);
			}
        }
    }

    private Map<String, PolyString> consolidateResults(List<Map<String, Object>> results) {
    	Map<String, PolyString> oidNameMap = new HashMap<>();
    	for (Map<String, Object> map : results) {
    		PolyStringType name = RPolyString.copyToJAXB((RPolyString) map.get("1"));
    		oidNameMap.put((String)map.get("0"), new PolyString(name.getOrig(), name.getNorm()));
    	}
    	return oidNameMap;
    }
}
