package dev.lvstrng.aids.jar.hierarchy;

import dev.lvstrng.aids.jar.Jar;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Hierarchy {
    private final Map<String, Tree> hierarchy = new ConcurrentHashMap<>();

    public Tree getTree(String clazz) {
        if(!hierarchy.containsKey(clazz))
            createTree(Jar.getClassAll(clazz), null);

        return hierarchy.get(clazz);
    }

    public void createTree(ClassNode parent, ClassNode subClass) {
        if(this.hierarchy.get(parent.name) == null) {
            var tree = new Tree(this, parent);

            if(parent.superName != null) {
                tree.getParents().add(parent.superName);
                createTree(Jar.getClassAll(parent.superName), parent);
            }

            if(parent.interfaces != null) {
                parent.interfaces.forEach(itf -> {
                    tree.getParents().add(itf);
                    createTree(Jar.getClassAll(itf), parent);
                });
            }

            hierarchy.put(parent.name, tree);
        }

        if(subClass == null)
            return;

        hierarchy.get(parent.name).getChildren().add(subClass.name);
    }
}
