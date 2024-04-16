package com.evolveum.midpoint.schema.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;

public class ProtectedStringViolations {

    private List<ItemPath> encrypted = new ArrayList<>();

    private List<ItemPath> hashed = new ArrayList<>();

    private List<ItemPath> clearValue = new ArrayList<>();

    public void addEncrypted(ItemPath path) {
        encrypted.add(path);
    }

    public void addHashed(ItemPath path) {
        hashed.add(path);
    }

    public void addClearValue(ItemPath path) {
        clearValue.add(path);
    }

    public List<ItemPath> getEncrypted() {
        return Collections.unmodifiableList(encrypted);
    }

    public List<ItemPath> getHashed() {
        return Collections.unmodifiableList(hashed);
    }

    public List<ItemPath> getClearValue() {
        return Collections.unmodifiableList(clearValue);
    }
}
