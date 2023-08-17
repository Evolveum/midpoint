
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism;

import java.util.Set;

public interface Clusterable {
    Set<String> getPoint();
    int getMembersCount();
}
