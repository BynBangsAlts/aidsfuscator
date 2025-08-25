package dev.lvstrng.aids.jar.hierarchy;

import org.objectweb.asm.tree.ClassNode;

import java.util.HashSet;
import java.util.Set;

public class Tree {
    private final Set<String> parentClasses = new HashSet<>(), subClasses = new HashSet<>();
    private final ClassNode classNode;
    private final Hierarchy hierarchy;

    public Tree(Hierarchy hierarchy, ClassNode classNode) {
        this.hierarchy = hierarchy;
        this.classNode = classNode;
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public Set<String> getParents() {
        return parentClasses;
    }

    public Set<String> getChildren() {
        return subClasses;
    }

    public Set<String> getAllParents() {
        var set = new HashSet<String>();
        search(this, set);
        return set;
    }

    private void search(Tree tree, Set<String> set) {
        for(var parent : tree.getParents()) {
            if(set.add(parent))
                continue;

            var parentTree = hierarchy.getTree(parent);
            if(parentTree == null)
                continue;

            search(parentTree, set);
        }
    }
}
