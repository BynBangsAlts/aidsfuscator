package dev.lvstrng.aids.utils;

import dev.lvstrng.aids.Obfuscator;
import dev.lvstrng.aids.jar.Jar;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HierarchyUtils {

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

    public static boolean isMethodFromLibrary(String owner, String name, String desc) {
        var clazz = Jar.getClassAll(owner);
        if (Jar.isLib(clazz)) {
            var list = clazz.methods.stream()
                    .map(e -> e.name + e.desc)
                    .toList();

            if (list.contains(name + desc))
                return true;
        }

        var tree = Obfuscator.hierarchy.getTree(owner);
        for (var parent : tree.getParents()) {
            if (isMethodFromLibrary(parent, name, desc))
                return true;
        }

        return false;
    }

    public static boolean isFieldFromLibrary(String owner, String name, String desc) {
        var clazz = Jar.getClassAll(owner);
        if (Jar.isLib(clazz)) {
            var list = clazz.fields.stream()
                    .map(e -> e.name + " " + e.desc)
                    .toList();

            if (list.contains(name + " " + desc))
                return true;
        }

        var tree = Obfuscator.hierarchy.getTree(owner);
        for (var parent : tree.getParents()) {
            if (isFieldFromLibrary(parent, name, desc))
                return true;
        }

        return false;
    }

    public static boolean hasAnnotation(ClassNode classNode, String annotation) {
        return checkAnnotation(classNode.visibleAnnotations, annotation)
                || checkAnnotation(classNode.invisibleAnnotations, annotation);
    }

    //eeeeheheheeeee
    private static boolean checkAnnotation(List<AnnotationNode> annotations, String annotation) {
        if(annotations == null)
            return false;

        for(var ann : annotations) {
            var yeah = ann.desc.equals(annotation)
                    || ann.desc.equals("L" + annotation.replace('.', '/') + ";")
                    || ann.desc.equals(annotation.replace('.', '/'));

            if(yeah)
                return true;
        }

        return false;
    }
}
