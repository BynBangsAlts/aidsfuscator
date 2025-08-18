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
public class MethodRenameTransformer extends Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            if((classNode.access & ACC_ANNOTATION) != 0)
                continue;

            for(var method : classNode.methods) {
                if(method.name.startsWith("<"))
                    continue;

                if(method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
                    continue;

                if(isInheritedFromLibrary(classNode, method.name, method.desc))
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
                    if(isInheritedFromLibrary(owner, name, descriptor))
                        return super.mapMethodName(owner, name, descriptor);

                    return super.mapMethodName(owner, Mappings.METHOD.newOrCurrent(name), descriptor);
                }
            }));

            newMap.put(remapped.name, remapped);
        }

        Jar.getClassMap().clear();
        Jar.getClassMap().putAll(newMap);
    }

    public static boolean isInheritedFromLibrary(String owner, String methodName, String descriptor) {
        return isInheritedFromLibrary(Jar.getClassAll(owner), methodName, descriptor);
    }

    public static boolean isInheritedFromLibrary(ClassNode owner, String methodName, String descriptor) {
        if(owner == null)
            return true;

        if(Jar.isLib(owner))
            return true;

        if(checkInterface(owner, methodName, descriptor))
            return true;

        var current = owner.superName; // don't check current
        while (current != null && !current.equals("java/lang/Object")) {
            var clazz = Jar.getClassAll(current);
            if(clazz == null)
                continue;

            if(Jar.isLib(current)) { //only check for libraries, don't rename the methods there
                if(checkInterface(owner, methodName, descriptor))
                    return true;

                if(HierarchyUtils.getMethodsWithDesc(clazz).contains(methodName + descriptor))
                    return true;
            }

            current = clazz.superName;
        }

        return false;
    }

    private static boolean checkInterface(ClassNode owner, String methodName, String descriptor) {
        for(var itf : owner.interfaces) {
            var clazz = Jar.getClassAll(itf);
            if(clazz == null)
                continue;

            if(HierarchyUtils.getMethodsWithDesc(clazz).contains(methodName + descriptor))
                continue;

            if(Jar.isLib(clazz)) {
                if(isInheritedFromLibrary(clazz, methodName, descriptor))
                    continue;

                if(checkInterface(clazz, methodName, descriptor))
                    continue;
            }

            return true;
        }

        return false;
    }
}
