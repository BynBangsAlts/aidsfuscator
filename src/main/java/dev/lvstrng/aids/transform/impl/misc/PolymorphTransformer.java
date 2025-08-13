package dev.lvstrng.aids.transform.impl.Misc;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.ASMUtils;
import org.objectweb.asm.tree.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


/**
 * @author Caesium
 * Credits To Caesium For This Transformer
 */
public class PolymorphTransformer extends Transformer {
    @Override
    public void transform() {
        for (var classNode : Jar.getClasses()) {
            for (var method : classNode.methods) {
                if (method.instructions == null || method.instructions.size() == 0)
                    continue;

                AtomicInteger index = new AtomicInteger();

                Stream.of(method.instructions.toArray().forEach(insn -> {
                            if (insn instanceof LdcInsnNode) {
                                if (random.nextBoolean()) {
                                    method.instructions.insertBefore(insn, new IntInsnNode(BIPUSH, ThreadLocalRandom.current().nextInt(-64, 64)));
                                    method.instructions.insertBefore(insn, new InsnNode(POP));
                                }
                            } else if (index.getAndIncrement() % 6 == 0) {
                                if (random.nextFloat() > 0.6) {
                                    method.instructions.insertBefore(insn, new IntInsnNode(BIPUSH, ThreadLocalRandom.current().nextInt(-27, 37)));
                                    method.instructions.insertBefore(insn, new InsnNode(POP));
                                } else {
                                    method.instructions.insertBefore(insn, new InsnNode(NOP));
                                }
                            }
                   });
            }
        }
    }
}
