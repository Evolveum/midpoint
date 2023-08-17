
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism;

import java.io.Serializable;
import java.util.Set;

public class DataPoint implements Clusterable, Serializable {

    //(USER Type)
    Set<String> members;

    //(ROLE Type)
    Set<String> properties;

    int membersCount;

    public DataPoint(Set<String> members, Set<String> properties) {
        this.members = members;
        this.properties = properties;
        this.membersCount = members.size();
    }

    @Override
    public Set<String> getPoint() {
        return properties;
    }

    @Override
    public int getMembersCount() {
        return membersCount;
    }

    public Set<String> getMembers() {
        return members;
    }

    public Set<String> getProperties() {
        return properties;
    }

}
