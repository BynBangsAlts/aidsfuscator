package dev.lvstrng.aids.transform.impl.rename;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.Dictionary;
import dev.lvstrng.aids.utils.HierarchyUtils;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;

//TODO proper inheritance check
public class MethodRenameTransformer implements Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            if((classNode.access & ACC_ANNOTATION) != 0)
                continue;

            if(classNode.superName.equals("java/lang/Enum"))
                continue;

            for(var method : classNode.methods) {
                if(method.name.startsWith("<"))
                    continue;

                if(method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
                    continue;

                if(HierarchyUtils.isInheritedFromLibrary(classNode, method.name, method.desc))
                    continue;

                Mappings.METHOD.register(method.name, Dictionary.METHOD.getNewName());
            }
        }

        var newMap = new HashMap<String, ClassNode>();
        for(var classNode : Jar.getClasses()) {
            var remapped = new ClassNode();

            classNode.accept(new ClassRemapper(remapped, new Remapper() {
                @Override
                public String mapMethodName(String owner, String name, String descriptor) {
                    if(HierarchyUtils.isInheritedFromLibrary(owner, name, descriptor))
                        return super.mapMethodName(owner, name, descriptor);

                    return super.mapMethodName(owner, Mappings.METHOD.newOrCurrent(name), descriptor);
                }
            }));

            newMap.put(remapped.name, remapped);
        }

        Jar.getClassMap().clear();
        Jar.getClassMap().putAll(newMap);
    }
}

