package dev.lvstrng.aids.analysis.block;

import dev.lvstrng.aids.utils.ASMUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class BlockAnalyzer {
    private final ClassNode classNode;
    private final MethodNode method;

    private final List<Block> blocks = new ArrayList<>();

    public BlockAnalyzer(ClassNode classNode, MethodNode method) {
        this.classNode = classNode;
        this.method = method;
    }

    public void createIntense() {
        var frames = ASMUtils.analyzeMethod(classNode, method);
        if(frames != null) {
            for (var insn : method.instructions) {
                var frame = frames.get(insn);
                if(frame == null)
                    continue;

                if(frame.getStackSize() != 0)
                    continue;

                method.instructions.insertBefore(insn, new LabelNode());
            }
        }
        create();
    }

    public void create() {
        LabelNode current = null;
        InsnList currentInsns = null;

        for(var insn : method.instructions) {
            if(insn instanceof LabelNode lbl) {
                if(current != null)
                    blocks.add(new Block(current, currentInsns));

                current = lbl;
                currentInsns = new InsnList();
            }

            if(current == null)
                continue;

            currentInsns.add(insn);
        }
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    public MethodNode getMethod() {
        return method;
    }
}
