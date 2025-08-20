package dev.lvstrng.aids.transform.impl.rename;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.Dictionary;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

public class ClassRenameTransformer implements Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            Mappings.CLASS.register(classNode.name, Dictionary.CLASS.getNewName(null));
        }

        var newMap = new HashMap<String, ClassNode>();
        for(var classNode : Jar.getClasses()) {
            var remapped = new ClassNode();

            classNode.accept(new ClassRemapper(remapped, new Remapper() {
                @Override
                public String map(String name) {
                    return super.map(Mappings.CLASS.newOrCurrent(name));
                }
            }));

            newMap.put(remapped.name, remapped);
        }

        Jar.getClassMap().clear();
        Jar.getClassMap().putAll(newMap);
    }
}
