package com.evolveum.midpoint.common.mining.objects.chunk;

import com.evolveum.midpoint.prism.path.ItemPath;

import java.io.Serializable;

public class DisplayValueOption implements Serializable {
    ItemPath roleItemValuePath;
    ItemPath userItemValuePath;


    public DisplayValueOption() {
    }
    public DisplayValueOption(ItemPath roleItemValuePath, ItemPath userItemValuePath) {
        this.roleItemValuePath = roleItemValuePath;
        this.userItemValuePath = userItemValuePath;
    }

    public ItemPath getRoleItemValuePath() {
        return roleItemValuePath;
    }

    public void setRoleItemValuePath(ItemPath roleItemValuePath) {
        this.roleItemValuePath = roleItemValuePath;
    }

    public ItemPath getUserItemValuePath() {
        return userItemValuePath;
    }

    public void setUserItemValuePath(ItemPath userItemValuePath) {
        this.userItemValuePath = userItemValuePath;
    }

}
