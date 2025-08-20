package dev.lvstrng.aids.utils;

import dev.lvstrng.aids.jar.Jar;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.List;

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

    public static FieldNode getField(ClassNode owner, String name, String desc) {
        return owner.fields.stream()
                .filter(e -> e.name.equals(name) && e.desc.equals(desc))
                .findAny().orElse(null);
    }

    public static List<String> getMethods(ClassNode clazz) {
        return clazz.methods.stream().map(e -> e.name).toList();
    }

    public static List<String> getFields(ClassNode clazz) {
        return clazz.fields.stream().map(e -> e.name).toList();
    }

    public static List<String> getClasses() {
        return Jar.getClasses().stream().map(e -> e.name).toList();
    }

    public static List<String> getFieldsWithDesc(ClassNode clazz) {
        return clazz.fields.stream().map(e -> e.name + "." + e.desc).toList();
    }

    public static List<String> getMethodsWithDesc(ClassNode clazz) {
        return clazz.methods.stream().map(e -> e.name + e.desc).toList();
    }

    public static boolean isInheritedFromLibrary(String owner, String methodName, String descriptor) {
        return isInheritedFromLibrary(Jar.getClassAll(owner), methodName, descriptor);
    }

    public static boolean isInheritedFromLibrary(ClassNode owner, String methodName, String descriptor) {
        if(owner == null)
            return true;

        if(Jar.isLib(owner))
            return true;

        if(checkInterface(owner, methodName, descriptor))
            return true;

        var current = owner.superName; // don't check current
        while (current != null && !current.equals("java/lang/Object")) {
            var clazz = Jar.getClassAll(current);
            if(clazz == null)
                continue;

            if(Jar.isLib(current)) { //only check for libraries, don't rename the methods there
                if(checkInterface(owner, methodName, descriptor))
                    return true;

                if(HierarchyUtils.getMethodsWithDesc(clazz).contains(methodName + descriptor))
                    return true;
            }

            current = clazz.superName;
        }

        return false;
    }

    private static boolean checkInterface(ClassNode owner, String methodName, String descriptor) {
        for(var itf : owner.interfaces) {
            var clazz = Jar.getClassAll(itf);
            if(clazz == null)
                continue;

            if(HierarchyUtils.getMethodsWithDesc(clazz).contains(methodName + descriptor))
                continue;

            if(Jar.isLib(clazz)) {
                if(isInheritedFromLibrary(clazz, methodName, descriptor))
                    continue;

                if(checkInterface(clazz, methodName, descriptor))
                    continue;
            }

            return true;
        }

        return false;
    }
}
