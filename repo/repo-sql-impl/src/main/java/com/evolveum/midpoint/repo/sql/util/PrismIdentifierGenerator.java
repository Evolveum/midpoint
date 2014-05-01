package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.cxf.common.util.StringUtils;

import java.util.*;

/**
 * @author lazyman
 */
public class PrismIdentifierGenerator {

    public void generate(PrismObject object) {
        if (StringUtils.isEmpty(object.getOid())) {
            String oid = UUID.randomUUID().toString();
            object.setOid(oid);
        }

        generateIdForObject(object);
    }

    private void generateIdForObject(PrismObject object) {
        if (object == null) {
            return;
        }

        List<PrismContainer> containers = getChildrenContainers(object);
        Set<Long> usedIds = new HashSet<>();
        for (PrismContainer c : containers) {
            if (c == null || c.getValues() == null) {
                continue;
            }

            for (PrismContainerValue val : (List<PrismContainerValue>) c.getValues()) {
                if (val.getId() != null) {
                    usedIds.add(val.getId());
                }
            }
        }

        Long nextId = 1L;
        for (PrismContainer c : containers) {
            if (c == null || c.getValues() == null) {
                continue;
            }

            for (PrismContainerValue val : (List<PrismContainerValue>) c.getValues()) {
                if (val.getId() != null) {
                    continue;
                }

                while (usedIds.contains(nextId)) {
                    nextId++;
                }

                val.setId(nextId);
                usedIds.add(nextId);
            }
        }
    }

    private List<PrismContainer> getChildrenContainers(PrismObject parent) {
        List<PrismContainer> containers = new ArrayList<>();
        if (ObjectType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(ObjectType.F_TRIGGER));
        }

        if (FocusType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(FocusType.F_ASSIGNMENT));
        }

        if (AbstractRoleType.class.isAssignableFrom(parent.getCompileTimeClass())) {
            containers.add(parent.findContainer(AbstractRoleType.F_INDUCEMENT));
            containers.add(parent.findContainer(AbstractRoleType.F_EXCLUSION));
            containers.add(parent.findContainer(AbstractRoleType.F_AUTHORIZATION));
        }

        return containers;
    }
}
