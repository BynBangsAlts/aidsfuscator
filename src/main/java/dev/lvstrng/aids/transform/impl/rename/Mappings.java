package dev.lvstrng.aids.transform.impl.rename;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lvstrng
 * Contains all new/old names for renamed members.
 */
public enum Mappings {
    CLASS,
    METHOD,
    FIELD;

    private final Map<String, String> oldToNew = new HashMap<>();
    private final Map<String, String> newToOld = new HashMap<>();

    public void register(String oldName, String newName) {
        oldToNew.put(oldName, newName);
        newToOld.put(newName, oldName);
    }

    public String newOrCurrent(String oldName) {
        return oldToNew.getOrDefault(oldName, oldName);
    }

    public String oldOrCurrent(String newName) {
        return newToOld.getOrDefault(newName, newName);
    }
}