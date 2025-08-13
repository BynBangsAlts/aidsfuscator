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

        if(isInAnInterface(owner, name, desc))
            return true;

        //superclass check
        var current = owner.superName;
        while (current != null && !current.equals("java/lang/Object")) {
            var clazz = Jar.getClassAll(current);
            if(clazz == null)
                return false; //possibly missing library?

            if(Jar.isLib(clazz)) {
                var list = HierarchyUtils.getFieldsWithDesc(clazz);
                if (list.contains(name + "." + desc))
                    return true; //field is inherited from library, return
            }

            if(isInAnInterface(clazz, name, desc))
                return true;

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

    //holy recursion
    private boolean isInAnInterface(ClassNode owner, String name, String desc) {
        if(owner == null)
            return false;

        for(var itf : owner.interfaces) {
            var clazz = Jar.getClassAll(itf);
            if(clazz == null)
                continue;

            if(Jar.isLib(clazz)) {
                if (HierarchyUtils.getFieldsWithDesc(clazz).contains(name + "." + desc))
                    return true;
            }

            if(isInAnInterface(clazz, name, desc) || isFieldFromLibrary(clazz, name, desc))
                return true;
        }

        return false;
    }

    private boolean isInAnInterface(String owner, String name, String desc) {
        return isInAnInterface(Jar.getClassAll(owner), name, desc);
    }
}
