/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Debugging listener for reconciliation tasks.
 *
 * This is not the best place for this object. But we have to live with it now.
 *
 * @author semancik
 *
 */
public class DebugReconciliationResultListener implements
        ReconciliationResultListener, DebugDumpable {

    private final List<ReconciliationResult> results = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void process(ReconciliationResult reconResult) {
        results.add(reconResult);
    }

    public void clear() {
        results.clear();
    }

    public void assertResult(String resourceOid, long expectedUnOpsCount, long expectedResourceReconCount, long expectedResourceReconErrors,
            long expectedShadowReconCount) {
        ReconciliationResult result = findResult(resourceOid);
        assert result != null : "No recon result for resource "+resourceOid;
        PrismAsserts.assertEquals("Wrong unOpsCount in recon result for resource "+resourceOid,
                expectedUnOpsCount, result.getUnOpsCount());
        PrismAsserts.assertEquals("Wrong resourceReconCount in recon result for resource "+resourceOid,
                expectedResourceReconCount, result.getResourceReconCount());
        PrismAsserts.assertEquals("Wrong resourceReconErrors in recon result for resource "+resourceOid,
                expectedResourceReconErrors, result.getResourceReconErrors());
        PrismAsserts.assertEquals("Wrong shadowReconCount in recon result for resource "+resourceOid,
                expectedShadowReconCount, result.getShadowReconCount());
    }

    private ReconciliationResult findResult(String resourceOid) {
        for (ReconciliationResult result: results) {
            if (resourceOid.equals(result.getResourceOid())) {
                return result;
            }
        }
        return null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(DebugReconciliationResultListener.class, indent);
        DebugUtil.debugDumpWithLabel(sb, "results", results, indent + 1);
        return sb.toString();
    }

}
