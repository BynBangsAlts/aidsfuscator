package dev.lvstrng.aids.utils;

import dev.lvstrng.aids.jar.Jar;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;

public class HierarchyUtils {
    public static String getCommonSuperClass(ClassNode node, ClassNode other) {
        if (node.name.equals(other.name)) {
            return node.name;
        }

        var supersOfNode = new HashSet<String>();
        var current = node.name;

        while (current != null) {
            supersOfNode.add(current);
            current = getSuperName(current);
        }

        current = other.name;
        while (current != null) {
            if (supersOfNode.contains(current)) {
                return current;
            }

            current = getSuperName(current);
        }

        return "java/lang/Object"; // fallback if no common superclass is found
    }

    private static String getSuperName(String internalName) {
        var cn = Jar.getClass(internalName);

        if (cn == null)
            cn = Jar.getLibrary(internalName);

        return cn != null ? cn.superName : null;
    }

    public static MethodNode getMethod(ClassNode owner, String name, String desc) {
        return owner.methods.stream()
                .filter(e -> e.name.equals(name) && e.desc.equals(desc))
                .findAny().orElse(null);
    }
}
