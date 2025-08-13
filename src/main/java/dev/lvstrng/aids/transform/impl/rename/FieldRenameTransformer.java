package dev.lvstrng.aids.transform.impl.rename;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.Dictionary;
import dev.lvstrng.aids.utils.HierarchyUtils;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

public class FieldRenameTransformer extends Transformer {
    @Override
    public void transform() {
        // register all mappings
        for(var classNode : Jar.getClasses()) {
            for(var field : classNode.fields) {
                if(isFieldFromLibrary(classNode, field.name, field.desc))
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
                    if(isFieldFromLibrary(owner, name, descriptor))
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

    /**
     * We check for libraries classes that are in libraries, we will not rename those and keep their names as-is in the output jar
     *
     * @param owner class from which we're checking the field
     * @param name name of the field
     * @param desc field descriptor
     * @return whether field is from a library or not
     */
    private boolean isFieldFromLibrary(ClassNode owner, String name, String desc) {
        if(owner == null)
            return false;

        var current = owner.superName;
        while (current != null && !current.equals("java/lang/Object")) {
            var clazz = Jar.getLibrary(current);
            if(clazz == null)
                return false; //possibly missing library?

            var list = HierarchyUtils.getFieldsWithDesc(clazz);
            if(list.contains(name + "." + desc))
                return true; //field is inherited from library, return

            current = clazz.superName;
        }

        return false;
    }

    /**
     * @param owner name of class from which we're checking the field
     * @param name name of the field
     * @param desc field descriptor
     * @return whether field is from a library or not
     */
    private boolean isFieldFromLibrary(String owner, String name, String desc) {
        return isFieldFromLibrary(Jar.getClassAll(owner), name, desc);
    }
}
