package dev.lvstrng.aids.utils;

import org.objectweb.asm.tree.ClassNode;

/**
 * Gives a new, short name for members
 */
public enum Dictionary {
    CLASS,
    METHOD,
    FIELD;

    private int counter = 0;

    /**
     * Returns a new name, no matter if it's used or not
     * @return new name as String
     */
    public String getNewName() {
        var dictionary = "abcdefghijklmnopqrstuvwxyz";
        var len = dictionary.length();
        var sb = new StringBuilder();

        int idx = counter;
        while (idx >= 0) {
            sb.append(dictionary.charAt(idx % len));
            idx = (idx / len) - 1;
        }

        counter++;
        return sb.reverse().toString();
    }

    /**
     * @param clazz class that contains member (can only be null if using for CLASSES)
     * @return new UNUSED name for a member
     */
    public String getNewName(ClassNode clazz) {
        //list to check for member names
        var list = (this == METHOD) ?
                HierarchyUtils.getMethods(clazz) :
                (this == FIELD) ? HierarchyUtils.getFields(clazz) : HierarchyUtils.getClasses();

        String name;
        do {
            name = getNewName();
        } while (list.contains(name));

        return name;
    }
}
