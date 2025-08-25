package dev.lvstrng.aids.transform.impl.rename;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.Dictionary;
import dev.lvstrng.aids.utils.HierarchyUtils;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

public class FieldRenameTransformer implements Transformer {
    @Override
    public void transform() {
        // register all mappings
        for(var classNode : Jar.getClasses()) {
            for(var field : classNode.fields) {
                if(HierarchyUtils.isFieldFromLibrary(classNode.name, field.name, field.desc))
                    continue;

                Mappings.FIELD.register(field.name, Dictionary.FIELD.getNewName());
            }
        }

        // remap
        var newClasses = new HashMap<String, ClassNode>();
        for(var classNode : Jar.getClasses()) {
            var remapped = new ClassNode();

            classNode.accept(new ClassRemapper(remapped, new Remapper() {
                @Override
                public String mapFieldName(String owner, String name, String descriptor) {
                    if(HierarchyUtils.isFieldFromLibrary(owner, name, descriptor))
                        return super.mapFieldName(owner, name, descriptor);

                    return super.mapFieldName(owner, Mappings.FIELD.newOrCurrent(name), descriptor);
                }
            }));

            newClasses.put(remapped.name, remapped);
        }

        // apply
        Jar.getClassMap().clear();
        Jar.getClassMap().putAll(newClasses);
    }
}
