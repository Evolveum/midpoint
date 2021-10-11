/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.schema.internals.InternalInspector;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public class CountingInspector implements InternalInspector, DebugDumpable {

    @SuppressWarnings("rawtypes")
    private Map<Class, Integer> readMap = new HashMap<>();

    private Map<NamedObjectKey, Integer> roleEvaluationMap = new HashMap<>();

    @Override
    public <O extends ObjectType> void inspectRepositoryRead(Class<O> type, String oid) {
        Integer i = readMap.get(type);
        if (i == null) {
            i = 0;
        }
        i++;
        readMap.put(type, i);
    }

    public <O extends ObjectType> void assertRead(Class<O> type, int expectedCount) {
        assertEquals("Unexpected number of reads of " + type.getSimpleName(), (Integer) expectedCount, readMap.get(type));
    }

    @Override
    public <F extends AssignmentHolderType> void inspectRoleEvaluation(F target, boolean fullEvaluation) {
        NamedObjectKey key = new NamedObjectKey(target);
        Integer i = roleEvaluationMap.get(key);
        if (i == null) {
            i = 0;
        }
        i++;
        roleEvaluationMap.put(key, i);
    }

    public void assertRoleEvaluations(String roleOid, int expectedCount) {
        for (Entry<NamedObjectKey, Integer> entry : roleEvaluationMap.entrySet()) {
            if (roleOid.equals(entry.getKey().oid)) {
                assertEquals("Wrong role evaluation count for role " + roleOid,
                        (Integer) expectedCount, entry.getValue());
                return;
            }
        }
        if (expectedCount != 0) {
            AssertJUnit.fail("No evaluation count found for role " + roleOid);
        }
    }

    public void reset() {
        readMap = new HashMap<>();
        roleEvaluationMap = new HashMap<>();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "read", readMap, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "roleEvaluation", roleEvaluationMap, indent + 1);
        return sb.toString();
    }

    private class NamedObjectKey {
        private String oid;
        private String name;

        public NamedObjectKey(ObjectType object) {
            this.oid = object.getOid();
            this.name = object.getName().getOrig();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((oid == null) ? 0 : oid.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            NamedObjectKey other = (NamedObjectKey) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            if (oid == null) {
                if (other.oid != null) {
                    return false;
                }
            } else if (!oid.equals(other.oid)) {
                return false;
            }
            return true;
        }

        private CountingInspector getOuterType() {
            return CountingInspector.this;
        }

        @Override
        public String toString() {
            return "(" + oid + ":" + name + ")";
        }
    }
}
