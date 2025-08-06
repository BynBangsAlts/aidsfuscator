package dev.lvstrng.aids.analysis.callgraph;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

public record CallNode(MethodNode caller, MethodNode callee, AbstractInsnNode insn) { }
