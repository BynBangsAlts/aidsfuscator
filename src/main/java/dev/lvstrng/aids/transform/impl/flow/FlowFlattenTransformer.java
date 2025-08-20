package dev.lvstrng.aids.transform.impl.flow;

import dev.lvstrng.aids.analysis.frames.FrameAnalyzer;
import dev.lvstrng.aids.analysis.misc.Local;
import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.ASMUtils;
import me.coley.analysis.value.AbstractValue;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.*;

public class FlowFlattenTransformer implements Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            for(var method : classNode.methods) {
                flatten(classNode, method);
            }
        }
    }

    public static void flatten(ClassNode classNode, MethodNode method) {
        var frames = ASMUtils.analyzeMethod(classNode, method);
        if(frames == null)
            return;

        var initializedThis = false; //init check
        var groups = new HashMap<String, List<LabelNode>>(); //frame, label
        for(var insn : method.instructions) {
            if(insn == method.instructions.getFirst() || insn == method.instructions.getLast())
                continue;

            if(method.name.equals("<init>")) {
                if(!initializedThis && insn instanceof MethodInsnNode call) {
                    initializedThis = call.getOpcode() == INVOKESPECIAL && call.owner.equals(classNode.superName) && call.name.equals("<init>");
                    continue;
                }

                if(!initializedThis)
                    continue;
            }

            var frame = frames.get(insn);
            if(frame == null)
                continue;

            if(frame.getStackSize() != 0)
                continue;

            if(isReachedByTCB(frames, insn))
                continue;

            var lbl = new LabelNode();
            method.instructions.insertBefore(insn, lbl);
            groups.computeIfAbsent(
                    FrameAnalyzer.generateMap(frame),
                    _ -> new ArrayList<>()
            ).add(lbl);
        }

        var entries = new ArrayList<>(groups.entrySet());
        Collections.shuffle(entries);

        for(var group : entries) {
            var labels = group.getValue();
            if(labels.isEmpty())
                continue;

            var local = Local.alloc(method, Type.INT_TYPE);
            var dispatcher = new LabelNode();
            var cases = new HashMap<LabelNode, Integer>();

            labels.forEach(e -> {
                var key = random.nextInt();
                var list = new InsnList();

                cases.putIfAbsent(e, key);

                list.add(ASMUtils.pushInt(cases.get(e)));
                list.add(local.store());
                list.add(new JumpInsnNode(GOTO, dispatcher));

                method.instructions.insertBefore(e, list);
            });

            var lookup = new LookupSwitchInsnNode(
                    labels.getFirst(),
                    cases.values().stream().mapToInt(Integer::intValue).toArray(),
                    cases.keySet().toArray(new LabelNode[0])
            );
            ASMUtils.sortSwitch(lookup);

            var list = new InsnList();
            list.add(dispatcher);
            list.add(local.load());
            list.add(lookup);
            method.instructions.add(list);
        }
    }

    private static boolean isReachedByTCB(Map<AbstractInsnNode, Frame<AbstractValue>> frames, AbstractInsnNode insn) {
        var frame = frames.get(insn);
        if (frame == null)
            return false;

        for (int i = 0; i < frame.getStackSize(); i++) {
            if (checkThrowable(frame.getStack(i)))
                return true;
        }

        for (int i = 0; i < frame.getLocals(); i++) {
            if (checkThrowable(frame.getLocal(i)))
                return true;
        }

        return false;
    }

    private static boolean checkThrowable(AbstractValue value) {
        var type = value.getType();
        if (type == null || type.getSort() != Type.OBJECT)
            return false;

        var current = type.getClassName().replace('.', '/');
        while (current != null && !current.equals("java/lang/Object")) {
            if (current.equals("java/lang/Throwable"))
                return true;

            var clazz = Jar.getClassAll(current);
            if (clazz == null)
                return false;

            current = clazz.superName;
        }

        return false;
    }
}