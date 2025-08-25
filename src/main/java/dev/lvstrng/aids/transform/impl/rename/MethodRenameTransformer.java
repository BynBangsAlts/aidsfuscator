package dev.lvstrng.aids.transform.impl.rename;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.Dictionary;
import dev.lvstrng.aids.utils.HierarchyUtils;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;

//TODO proper inheritance check
public class MethodRenameTransformer implements Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            for(var method : classNode.methods) {
                if(isBlacklisted(classNode, method))
                    continue;

                Mappings.METHOD.register(method.name + "_" + method.desc, Dictionary.METHOD.getNewName() + "_" + method.desc);
            }
        }

        var newMap = new HashMap<String, ClassNode>();
        for(var classNode : Jar.getClasses()) {
            var remapped = new ClassNode();

            classNode.accept(new ClassRemapper(remapped, new Remapper() {
                @Override
                public String mapMethodName(String owner, String name, String descriptor) {
                    if(name.equals("clone"))
                        return super.mapMethodName(owner, name, descriptor);

                    if(HierarchyUtils.isMethodFromLibrary(owner, name, descriptor))
                        return super.mapMethodName(owner, name, descriptor);

                    return super.mapMethodName(owner, Mappings.METHOD.newOrCurrent(name + "_" + descriptor).split("_")[0], descriptor);
                }
            }));

            newMap.put(remapped.name, remapped);
        }

        Jar.getClassMap().clear();
        Jar.getClassMap().putAll(newMap);
    }

    private boolean isBlacklisted(ClassNode classNode, MethodNode method) {
        if(method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
            return true;

        if(method.name.contains("<"))
            return true;

        if((classNode.access & ACC_ENUM) != 0) {
            if(method.name.equals("values") || method.name.equals("valueOf"))
                return true;
        }

        if(HierarchyUtils.isMethodFromLibrary(classNode.name, method.name, method.desc))
            return true;

        //somehow fixes crash for eval-2 jar (???) somebody fix plzplzplz
        if(HierarchyUtils.hasAnnotation(classNode, "java/lang/FunctionalInterface"))
            return true;

        return (classNode.access & ACC_ANNOTATION) != 0;
    }
}

