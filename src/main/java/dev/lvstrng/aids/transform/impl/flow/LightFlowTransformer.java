package dev.lvstrng.aids.transform.impl.flow;

import dev.lvstrng.aids.analysis.frames.FrameAnalyzer;
import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.ASMUtils;
import dev.lvstrng.aids.utils.Pair;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LightFlowTransformer extends Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            for(var method : classNode.methods) {
                var frames = ASMUtils.analyzeMethod(classNode, method);
                if(frames == null)
                    continue;

                int local = method.maxLocals++;
                int number = random.nextInt(); //we're gonna integer encrypt it anyway

                method.instructions.insert(new VarInsnNode(ISTORE, local));
                method.instructions.insert(ASMUtils.pushInt(number));

                var targets = new HashMap<String, List<LabelNode>>();
                var empty = new HashMap<String, List<LabelNode>>();

                for(var insn : method.instructions) {
                    var frame = frames.get(insn);
                    if(frame == null)
                        continue;

                    switch (frame.getStackSize()) {
                        //for gotos
                        case 0: {
                            var lbl = new LabelNode();
                            method.instructions.insertBefore(insn, lbl);

                            empty.computeIfAbsent(FrameAnalyzer.generateMap(frame), _ -> new ArrayList<>()).add(lbl);
                            break;
                        }
                        //for actual flow
                        case 1: {
                            if(frame.getStack(0).getType().getSort() == Type.OBJECT)
                                continue;

                            var lbl = new LabelNode();
                            method.instructions.insertBefore(insn, lbl);

                            targets.computeIfAbsent(FrameAnalyzer.generateMap(frame), _ -> new ArrayList<>()).add(lbl);
                            break;
                        }
                    }
                }

                //iterate second time with all jumps ready
                for(var insn : method.instructions) {
                    var frame = frames.get(insn);
                    if(frame == null)
                        continue;

                    switch (frame.getStackSize()) {
                        case 0: {
                            if(!(insn instanceof JumpInsnNode jmp && jmp.getOpcode() == GOTO))
                                continue;

                            var frameTargets = empty.get(FrameAnalyzer.generateMap(frame));
                            if(frameTargets == null)
                                continue;

                            var available = frameTargets.stream()
                                    .filter(e -> method.instructions.indexOf(e) > method.instructions.indexOf(insn))//again, only forward jumps
                                    .filter(e -> Math.abs(method.instructions.indexOf(e) - method.instructions.indexOf(insn)) > 7) //keep empty labels away!!
                                    .toList();
                            if(available.isEmpty())
                                continue;

                            if(!willHitReturn(insn))
                                continue;

                            if(isAtEndOfTcb(method, insn))
                                continue;

                            var last = available.getLast();
                            var list = new InsnList();

                            list.add(new VarInsnNode(ILOAD, local));
                            list.add(new JumpInsnNode(IFEQ, last)); //never jumps
                            list.add(new VarInsnNode(ILOAD, local));
                            list.add(new JumpInsnNode(IFNE, jmp.label)); //real deal

                            method.instructions.insertBefore(insn, list);
                            method.instructions.remove(insn);
                            frameTargets.remove(last);
                            break;
                        }
                        case 1: {
                            if(frame.getStack(0).getType().getSort() == Type.OBJECT)
                                continue;

                            var frameTargets = targets.get(FrameAnalyzer.generateMap(frame));
                            if(frameTargets == null)
                                continue;

                            var availableLabels = frameTargets.stream()
                                    .filter(e -> method.instructions.indexOf(e) > method.instructions.indexOf(insn)) //forward jumps only
                                    .filter(e -> Math.abs(method.instructions.indexOf(e) - method.instructions.indexOf(insn)) > 7) //keep empty labels away!!
                                    .toList();
                            if(availableLabels.isEmpty())
                                continue;

                            var lastLbl = availableLabels.getLast();
                            var list = new InsnList();

                            list.add(new VarInsnNode(ILOAD, local));
                            list.add(new JumpInsnNode(IFEQ, lastLbl)); //if local is 0, jump to label (should realistically never happen)

                            method.instructions.insertBefore(insn, list);
                            frameTargets.remove(lastLbl);
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean willHitReturn(AbstractInsnNode insn) {
        var current = insn;

        while (current != null) {
            if(ASMUtils.isReturn(current))
                return true;

            current = current.getNext(); //iterate forwards till hit return
        }

        return false;
    }

    //we only really need to check this, everything else is valid lol
    private boolean isAtEndOfTcb(MethodNode method, AbstractInsnNode insn) {
        if(method.tryCatchBlocks == null || method.tryCatchBlocks.isEmpty())
            return false;

        var ranges = new ArrayList<Pair<Integer, Integer>>();
        for(var tcb : method.tryCatchBlocks) {
            ranges.add(new Pair<>(method.instructions.indexOf(tcb.end), method.instructions.indexOf(tcb.handler)));
        }

        var idx = method.instructions.indexOf(insn);
        for(var range : ranges) {
            //start
            if(range.getLeft() <= idx && range.getRight() >= idx)
                return true;
        }

        return false;
    }
}
