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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lazyman, katkav, mederly
 */
@Component
public class NameResolutionHelper {

	private static final int MAX_OIDS_TO_RESOLVE_AT_ONCE = 200;

	// TODO keep names between invocations (e.g. when called from searchObjects/searchContainers)
    public void resolveNamesIfRequested(Session session, PrismContainerValue<?> containerValue, Collection<SelectorOptions<GetOperationOptions>> options) {
    	resolveNamesIfRequested(session, Collections.singletonList(containerValue), options);
	}

    public void resolveNamesIfRequested(Session session, List<? extends PrismContainerValue> containerValues, Collection<SelectorOptions<GetOperationOptions>> options) {
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
		if (!GetOperationOptions.isResolveNames(rootOptions)) {
			return;
		}

		final Set<String> oidsToResolve = new HashSet<>();
		Visitor oidExtractor = visitable -> {
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
		};
		Set<PrismContainerValue> roots = containerValues.stream().map(pcv -> pcv.getRootValue()).collect(Collectors.toSet());
		roots.forEach(root -> root.accept(oidExtractor));

		Map<String, PolyString> oidNameMap = new HashMap<>();
		List<String> batch = new ArrayList<>();
		for (Iterator<String> iterator = oidsToResolve.iterator(); iterator.hasNext(); ) {
			batch.add(iterator.next());
			if (batch.size() >= MAX_OIDS_TO_RESOLVE_AT_ONCE || !iterator.hasNext()) {
				Query query = session.getNamedQuery("resolveReferences");
				query.setParameterList("oid", batch);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

				@SuppressWarnings({ "unchecked", "raw" })
				List<Map<String, Object>> results = query.list();			// returns oid + name
				for (Map<String, Object> result : results) {
					String oid = (String) result.get("0");
					RPolyString name = (RPolyString) result.get("1");
					oidNameMap.put(oid, new PolyString(name.getOrig(), name.getNorm()));
				}
				batch.clear();
			}
		}
		if (!oidNameMap.isEmpty()) {
			Visitor nameSetter = visitable -> {
				if (visitable instanceof PrismReferenceValue) {
					PrismReferenceValue value = (PrismReferenceValue) visitable;
					if (value.getTargetName() == null && value.getOid() != null) {
						value.setTargetName(oidNameMap.get(value.getOid()));
					}
				}
			};
			roots.forEach(root -> root.accept(nameSetter));
		}
	}
}
