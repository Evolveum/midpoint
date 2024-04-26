/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

/**
 * @author lazyman, katkav
 */
@Component
public class NameResolutionHelper {

    private static final int MAX_OIDS_TO_RESOLVE_AT_ONCE = 200;

    @Autowired private PrismContext prismContext;

    // TODO keep names between invocations (e.g. when called from searchObjects/searchContainers)
    public void resolveNamesIfRequested(EntityManager em, PrismContainerValue<?> containerValue, Collection<SelectorOptions<GetOperationOptions>> options) {
        resolveNamesIfRequested(em, Collections.singletonList(containerValue), options);
    }

    public void resolveNamesIfRequested(EntityManager em, List<? extends PrismContainerValue> containerValues, Collection<SelectorOptions<GetOperationOptions>> options) {

        List<? extends ItemPath> pathsToResolve = getPathsToResolve(options);
        if (pathsToResolve.isEmpty()) {
            return;
        }

        final Set<String> oidsToResolve = new HashSet<>();
        Visitor oidExtractor = visitable -> {
            if (visitable instanceof PrismReferenceValue) {
                PrismReferenceValue value = (PrismReferenceValue) visitable;
                if (!ItemPathCollectionsUtil.containsSubpathOrEquivalent(pathsToResolve, value.getPath())) {
                    return;
                }
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
                Query query = em.createNamedQuery("resolveReferences");
                query.setParameter("oid", batch);

                @SuppressWarnings({ "unchecked", "raw" })
                List<Object[]> results = query.getResultList();            // returns oid + name
                for (Object[] result : results) {
                    String oid = (String) result[0];
                    RPolyString name = (RPolyString) result[1];
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

    @NotNull
    private List<? extends ItemPath> getPathsToResolve(Collection<SelectorOptions<GetOperationOptions>> options) {
        final UniformItemPath emptyPath = prismContext.emptyPath();
        List<UniformItemPath> rv = new ArrayList<>();
        for (SelectorOptions<GetOperationOptions> option : CollectionUtils.emptyIfNull(options)) {
            if (GetOperationOptions.isResolveNames(option.getOptions())) {
                rv.add(option.getItemPath(emptyPath));
            }
        }
        return rv;
    }
}
