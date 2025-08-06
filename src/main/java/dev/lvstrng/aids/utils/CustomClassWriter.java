package dev.lvstrng.aids.utils;

import dev.lvstrng.aids.jar.DependencyMissingException;
import dev.lvstrng.aids.jar.Jar;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class CustomClassWriter extends ClassWriter {
    public CustomClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        var first = resolve(type1);
        var second = resolve(type2);

        try {
            if (first == null)
                throw new DependencyMissingException(type1);

            if (second == null)
                throw new DependencyMissingException(type2);

            return HierarchyUtils.getCommonSuperClass(first, second);
        } catch (Exception e) {
            System.err.println("Error thrown when reading libraries: " + e.getMessage());
            return super.getCommonSuperClass(type1, type2);
        }
    }

    private ClassNode resolve(String name) {
        var node = Jar.getClass(name);
        if (node != null)
            return node;

        node = Jar.getLibrary(name);
        if (node != null)
            return node;

        return Jar.getArtificial(name);
    }
}
