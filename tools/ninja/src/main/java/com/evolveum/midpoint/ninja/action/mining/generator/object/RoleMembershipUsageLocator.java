package com.evolveum.midpoint.ninja.action.mining.generator.object;

import java.util.HashSet;
import java.util.Set;

public class RoleMembershipUsageLocator {
    private int maxUsage = 0;
    Set<String> associatedUsersId = new HashSet<>();

    public RoleMembershipUsageLocator(int maxUsage) {
        this.maxUsage = maxUsage;
    }

    public int getUsage() {
        return associatedUsersId.size();
    }

    public void setMaxUsage(int maxUsage) {
        this.maxUsage = maxUsage;
    }

    public int getMaxUsage() {
        return maxUsage;
    }

    public void addAssociatedUserId(String user) {
        associatedUsersId.add(user);
    }
}
