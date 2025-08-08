package dev.lvstrng.aids.jar;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jar {
    private static final Map<String, ClassNode> classes =     new HashMap<>();
    private static final Map<String, ClassNode> artificials = new HashMap<>();
    private static final Map<String, ClassNode> libraries =   new HashMap<>();
    private static final Map<String, byte[]> resources =      new HashMap<>();

    public static ClassNode getClassAll(String name) {
        var clazz = Jar.getLibrary(name);
        if(clazz == null)
            clazz = Jar.getClass(name);

        if(clazz == null)
            clazz = Jar.getArtificial(name);

        return clazz;
    }

    public static void addResource(String name, byte[] data) {
        resources.put(name, data);
    }

    public static byte[] getResource(String name) {
        return resources.get(name);
    }

    public static Map<String, byte[]> getResources() {
        return resources;
    }

    public void addArtificial(ClassNode classNode) {
        artificials.put(classNode.name, classNode);
    }

    public static ClassNode getArtificial(String name) {
        return artificials.get(name);
    }

    public static Map<String, ClassNode> getArtificials() {
        return artificials;
    }

    public static void addLibrary(ClassNode classNode) {
        libraries.put(classNode.name, classNode);
    }

    public static Map<String, ClassNode> getLibraries() {
        return libraries;
    }

    public static ClassNode getClass(String name) {
        return classes.get(name);
    }

    public static ClassNode getLibrary(String name) {
        return libraries.get(name);
    }

    public static void addClass(ClassNode classNode) {
        classes.put(classNode.name, classNode);
    }

    public static Map<String, ClassNode> getClassMap() {
        return classes;
    }

    public static List<ClassNode> getClasses() {
        return classes.values().stream().toList();
    }
}
