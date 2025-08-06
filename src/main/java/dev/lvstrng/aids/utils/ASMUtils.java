package dev.lvstrng.aids.utils;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.value.AbstractValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ASMUtils implements Opcodes {
    public static Map<AbstractInsnNode, Frame<AbstractValue>> analyzeMethod(ClassNode owner, MethodNode method) {
        var analyzer = new SimAnalyzer(new SimInterpreter());
        analyzer.setSkipDeadCodeBlocks(true);

        try {
            var frames = new HashMap<AbstractInsnNode, Frame<AbstractValue>>();
            var arr = analyzer.analyzeAndComputeMaxs(owner.name, method);

            for(int i = 0; i < method.instructions.size(); i++) {
                var insn = method.instructions.get(i);
                frames.put(insn, arr[i]);
            }

            return frames;
        } catch (AnalyzerException _) {}

        return null;
    }

    public static int getInt(AbstractInsnNode insn) {
        if(insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer num) {
            return num;
        }

        if(insn instanceof IntInsnNode node && node.getOpcode() != NEWARRAY) {
            return node.operand;
        }

        return insn.getOpcode() - ICONST_0;
    }

    public static AbstractInsnNode pushInt(int num) {
        if(num <= 5 && num >= -1) {
            return new InsnNode(ICONST_0 + num);
        }

        if(num <= Byte.MAX_VALUE && num >= Byte.MIN_VALUE) {
            return new IntInsnNode(BIPUSH, num);
        }

        if(num <= Short.MAX_VALUE && num >= Short.MIN_VALUE) {
            return new IntInsnNode(SIPUSH, num);
        }

        return new LdcInsnNode(num);
    }

    public static boolean isReturn(AbstractInsnNode insn) {
        return insn.getOpcode() >= IRETURN && insn.getOpcode() <= RETURN;
    }
}
