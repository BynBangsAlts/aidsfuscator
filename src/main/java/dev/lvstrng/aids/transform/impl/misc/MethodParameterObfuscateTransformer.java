package dev.lvstrng.aids.transform.impl.misc;

import dev.lvstrng.aids.analysis.callgraph.CallGraph;
import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.ASMUtils;
import dev.lvstrng.aids.utils.HierarchyUtils;
import dev.lvstrng.aids.utils.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MethodParameterObfuscateTransformer implements Transformer {
    @Override
    public void transform() {
        var graph = new CallGraph();
        graph.create();

        var methods = new ArrayList<Pair<ClassNode, MethodNode>>();
        var obfuscatedMethods = new HashMap<ClassNode, Set<String>>();

        for(var classNode : Jar.getClasses()) {
            obfuscatedMethods.put(classNode, new HashSet<>());

            if ((classNode.access & ACC_ANNOTATION) != 0)
                continue;

            for(var method : classNode.methods) {
                if(blacklisted(classNode, method))
                    continue;

                if(obfuscatedMethods.get(classNode).contains(method.name)) //don't obfuscate methods with same name and same parameters, will break
                    continue;

                if(graph.getCallsFor(method).stream().anyMatch(e -> e.insn() instanceof InvokeDynamicInsnNode)) //don't obfuscate invokedynamic parameters
                    continue;

                methods.add(new Pair<>(classNode, method));
                obfuscatedMethods.get(classNode).add(method.name);
            }
        }

        for(var pair : methods) {
            var method = pair.getRight();
            var calls = graph.getCallsFor(method);
            if(calls == null)
                continue;

            if(calls.stream().anyMatch(e -> e.insn() instanceof InvokeDynamicInsnNode))
                continue;

            if(blacklisted(pair.getLeft(), method))
                continue;

            method.localVariables = null;

            var isStatic =   Modifier.isStatic(method.access);
            var returnType = method.desc.substring(method.desc.indexOf(')') + 1);
            var args =       Type.getArgumentTypes(method.desc);

            var oldDesc = method.desc;
            var newDesc = "([Ljava/lang/Object;)" + returnType;
            method.desc = newDesc;

            var list = new InsnList();
            var array = isStatic ? 0 : 1; //index of obj array
            var current = array + args.length; // index of the obfuscated parameter variable

            list.add(new VarInsnNode(ALOAD, array));

            for(int i = 0; i < args.length; i++) {
                var type = args[i];

                list.add(new InsnNode(DUP));
                list.add(new LdcInsnNode(i));
                list.add(new InsnNode(AALOAD));
                ASMUtils.unbox(list, type);

                if(type.getSort() == Type.OBJECT) //cast to required type
                    list.add(new TypeInsnNode(CHECKCAST, type.getInternalName()));

                switch (type.getSort()) {
                    case Type.INT, Type.CHAR, Type.BYTE, Type.SHORT, Type.BOOLEAN ->
                            list.add(new VarInsnNode(ISTORE, current));
                    case Type.LONG ->
                            list.add(new VarInsnNode(LSTORE, current));
                    case Type.FLOAT ->
                            list.add(new VarInsnNode(FSTORE, current));
                    case Type.DOUBLE ->
                            list.add(new VarInsnNode(DSTORE, current));
                    case Type.OBJECT ->
                            list.add(new VarInsnNode(ASTORE, current));
                    case Type.ARRAY -> {
                        list.add(new TypeInsnNode(CHECKCAST, type.getInternalName()));
                        list.add(new VarInsnNode(ASTORE, current));
                    }
                    default -> throw new RuntimeException("Unhandled type: " + type.getSort());
                }

                current += type.getSize();
            }

            var insns = method.instructions.toArray();
            for (var insn : insns) {
                if (insn instanceof VarInsnNode varInsn) {
                    int var = varInsn.var;

                    if (var >= (isStatic ? 0 : 1)) {
                        varInsn.var += args.length;
                    }
                }

                if(insn instanceof IincInsnNode iinc) {
                    int var = iinc.var;

                    if(var >= (isStatic ? 0 : 1)) {
                        iinc.var += args.length;
                    }
                }
            }

            list.add(new InsnNode(POP));
            method.instructions.insert(list);

            for(var caller : calls) {
                if(!(caller.insn() instanceof MethodInsnNode call))
                    continue;

                call.desc = newDesc;

                var unbox = new InsnList();
                unbox.add(ASMUtils.pushInt(args.length));
                unbox.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));

                for(int i = args.length - 1; i >= 0; i--) {
                    var type = Type.getArgumentTypes(oldDesc)[i];

                    if(type.getSize() == 2) {
                        unbox.add(new InsnNode(DUP_X2));
                        unbox.add(new InsnNode(DUP_X2));
                        unbox.add(new InsnNode(POP));
                    } else {
                        unbox.add(new InsnNode(DUP_X1));
                        unbox.add(new InsnNode(SWAP));
                    }

                    ASMUtils.box(unbox, type);
                    unbox.add(ASMUtils.pushInt(i));
                    unbox.add(new InsnNode(SWAP));
                    unbox.add(new InsnNode(AASTORE));
                }

                caller.caller().instructions.insertBefore(call, unbox);
            }
        }
    }

    private boolean blacklisted(ClassNode classNode, MethodNode method) {
        if(HierarchyUtils.isInheritedFromLibrary(classNode, method.name, method.desc))
            return true;

        if(method.name.equals("<clinit>"))
            return true;

        if(method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
            return true;

        return method.name.contains("$");
    }
}
