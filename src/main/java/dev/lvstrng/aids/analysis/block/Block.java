package dev.lvstrng.aids.analysis.block;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

public record Block(LabelNode label, InsnList instructions) {
}
