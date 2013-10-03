package com.evolveum.midpoint.web.page.admin.home.dto;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SystemInfoDto implements Serializable {

    private int activeUsers;
    private int maxActiveUsers;

    private int activeTasks;
    private int maxActiveTasks;

    private double serverLoad;

    private double usedMemory;
    private String usedMemoryUnit;

    public SystemInfoDto(int activeTasks, int activeUsers, int maxActiveTasks, int maxActiveUsers,
                         double serverLoad, double usedMemory, String usedMemoryUnit) {
        this.activeTasks = activeTasks;
        this.activeUsers = activeUsers;
        this.maxActiveTasks = maxActiveTasks;
        this.maxActiveUsers = maxActiveUsers;
        this.serverLoad = serverLoad;
        this.usedMemory = usedMemory;
        this.usedMemoryUnit = usedMemoryUnit;
    }

    public int getActiveTasks() {
        return activeTasks;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public int getMaxActiveTasks() {
        return maxActiveTasks;
    }

    public int getMaxActiveUsers() {
        return maxActiveUsers;
    }

    public double getServerLoad() {
        return serverLoad;
    }

    public double getUsedMemory() {
        return usedMemory;
    }

    public String getUsedMemoryUnit() {
        return usedMemoryUnit;
    }
}
