package dev.lvstrng.aids.transform.impl.flow;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.ASMUtils;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;

/**
 * @author lvstrng
 * Wraps useless exceptions around jump instructions. Since I do `athrow` in the handler:
 * CFR deletes this TCB, but keeps the break statements.
 * Procyon does a weird loadexception() call.
 * Vienflower just makes the code hard to read
 */
public class UselessExceptionTransformer extends Transformer {
    private final String[] handlerTypes = {
            "java/io/IOException",
            "java/lang/InterruptedException",
            "java/lang/RuntimeException",
            "java/lang/Exception",
            "java/lang/IllegalStateException",
            null
    };

    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            for(var method : classNode.methods) {
                var frames = ASMUtils.analyzeMethod(classNode, method);
                if(frames == null)
                    continue;

                for(var insn : method.instructions) {
                    if(!(insn instanceof JumpInsnNode jmp))
                        continue;

                    if(jmp.getOpcode() == GOTO)
                        continue;

                    var frame = frames.get(insn);
                    if(frame == null || frame.getStackSize() != ASMUtils.getJumpConsumer(jmp.getOpcode()))
                        continue;

                    var start = new LabelNode();
                    var end = new LabelNode();
                    var handler = new LabelNode();
                    var exit = new LabelNode();

                    var list = new InsnList();
                    list.add(start);
                    list.add(new JumpInsnNode(jmp.getOpcode(), jmp.label));
                    list.add(end);
                    list.add(new JumpInsnNode(GOTO, exit));
                    list.add(handler);
                    list.add(new InsnNode(ATHROW));
                    list.add(exit);

                    method.instructions.insertBefore(insn, list);
                    method.instructions.remove(insn);

                    var type = handlerTypes[random.nextInt(handlerTypes.length)];
                    var tcb = new TryCatchBlockNode(start, end, handler, type);
                    if (method.tryCatchBlocks == null)
                        method.tryCatchBlocks = new ArrayList<>();

                    method.tryCatchBlocks.add(tcb);
                }
            }
        }
    }
}
