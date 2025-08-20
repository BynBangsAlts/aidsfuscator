package dev.lvstrng.aids.transform.impl.data;

import dev.lvstrng.aids.analysis.callgraph.CallGraph;
import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.ASMUtils;
import dev.lvstrng.aids.utils.HierarchyUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.util.HashMap;

/**
 * A transformer that adds an extra {@code int} parameter to methods in order to obfuscate integers.
 * <p>
 * Inspired by Zelix KlassMaster.
 * </p>
 *
 * <h2>Example (before obfuscation)</h2>
 * <pre>
 * Main.someMethod();
 *
 * public static void someMethod() {
 *     for (int i = 0; i < 100; i++) {
 *         System.out.println(i);
 *     }
 * }
 * </pre>
 *
 * <h2>Example (after obfuscation)</h2>
 * <pre>
 * Main.someMethod(23847826);
 *
 * public static void someMethod(int n) {
 *     for (int i = 0; i < 23847926 ^ n; i++) {
 *         System.out.println(i);
 *     }
 * }
 * </pre>
 *
 * <p>
 * If the containing method already has a seed, the inserted parameter can also be obfuscated,
 * for example:
 * </p>
 *
 * <pre>
 * Main.someMethod(384523 ^ n);
 * </pre>
 *
 * <h2>Usage</h2>
 * <p>
 * This transformer is intended to be used as a final obfuscation layer. When applied on top of
 * other transformers such as integer/string encryption or flattening,
 * the code becomes extremely difficult to deobfuscate.
 * </p>
 *
 * @author lvstrng
 */
public class MethodParameterChangeTransformer implements Transformer {
    @Override
    public void transform() {
        var graph = new CallGraph();
        graph.create();

        var seeds = new HashMap<String, Integer>();
        var indexes = new HashMap<MethodNode, Integer>();
        for(var classNode : Jar.getClasses()) {
            for(var method : classNode.methods) {
                if(HierarchyUtils.isInheritedFromLibrary(classNode, method.name, method.desc))
                    continue;

                if (blacklisted(classNode, method))
                    continue;

                seeds.put(method.name, random.nextInt());
            }
        }

        for (var classNode : Jar.getClasses()) {
            for (var method : classNode.methods) {
                if(HierarchyUtils.isInheritedFromLibrary(classNode, method.name, method.desc))
                    continue;

                if (blacklisted(classNode, method))
                    continue;

                int seed = seeds.get(method.name);
                var calls = graph.getCallsFor(method);
                if (calls == null)
                    continue;

                if(calls.stream().anyMatch(e -> e.insn() instanceof InvokeDynamicInsnNode))
                    continue;

                int varIndex = calculateMax(method);
                var newDesc = method.desc.replace(")", "I)");

                for (var caller : calls) {
                    var mInsn = (MethodInsnNode) caller.insn();
                    mInsn.desc = newDesc;

                    var callOwner = caller.caller();
                    callOwner.instructions.insertBefore(caller.insn(), ASMUtils.pushInt(seed));
                }

                for (var insn : method.instructions) {
                    if (insn instanceof VarInsnNode var) {
                        if (var.var < varIndex)
                            continue;

                        var.var++;
                    } else if (insn instanceof IincInsnNode var) {
                        if (var.var < varIndex)
                            continue;

                        var.var++;
                    }
                }

                method.desc = newDesc;
                indexes.put(method, varIndex);
            }
        }

        for(var method : indexes.keySet()) {
            var varIndex = indexes.get(method);
            var seed = seeds.get(method.name);

            if(varIndex == null)
                continue;

            if(seed == null)
                continue;

            for(var insn : method.instructions) {
                if(ASMUtils.isValidIntPush(insn)) {
                    var list = new InsnList();
                    var num = ASMUtils.getInt(insn);

                    list.add(ASMUtils.pushInt(num ^ seed));
                    list.add(new VarInsnNode(ILOAD, varIndex));
                    list.add(new InsnNode(IXOR));

                    method.instructions.insertBefore(insn, list);
                    method.instructions.remove(insn);
                } else if (ASMUtils.isValidLongPush(insn)) {
                    var list = new InsnList();
                    var num = ASMUtils.getLong(insn);

                    list.add(ASMUtils.pushLong(num ^ seed));
                    list.add(new VarInsnNode(ILOAD, varIndex));
                    list.add(new InsnNode(I2L));
                    list.add(new InsnNode(LXOR));

                    method.instructions.insertBefore(insn, list);
                    method.instructions.remove(insn);
                }
            }
        }
    }

    private boolean blacklisted(ClassNode classNode, MethodNode method) {
        if(method.name.equals("<clinit>"))
            return true;

        if(method.name.equals("main") && method.desc.equals("([Ljava/lang/String;)V"))
            return true;

        return method.name.contains("$");
    }

    private int calculateMax(MethodNode method) {
        var num = 0;
        if(!Modifier.isStatic(method.access))
            num++;

        for(var arg : Type.getArgumentTypes(method.desc)) {
            num += arg.getSize();
        }

        return num;
    }
}