package dev.lvstrng.aids.transform.impl.data;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

public class ArithmeticTransformer extends Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            for(var method : classNode.methods) {
                for(var insn : method.instructions) {
                    var list = new InsnList();

                    switch (insn.getOpcode()) {
                        case IADD -> {
                            list.add(new InsnNode(DUP2));
                            list.add(new InsnNode(IAND));
                            list.add(new InsnNode(DUP_X2));
                            list.add(new InsnNode(POP));
                            
                            list.add(new InsnNode(IOR));
                            list.add(new InsnNode(IADD));
                        }
                        case ISUB -> {
                            list.add(new InsnNode(INEG));
                            list.add(new InsnNode(DUP2));

                            list.add(new InsnNode(IXOR));
                            list.add(new InsnNode(DUP_X2));
                            list.add(new InsnNode(POP));

                            list.add(new InsnNode(IAND));
                            list.add(new InsnNode(ICONST_2));
                            list.add(new InsnNode(IMUL));
                            list.add(new InsnNode(IADD));
                        }
                        case IXOR -> {
                            list.add(new InsnNode(DUP2));
                            list.add(new InsnNode(IOR));
                            list.add(new InsnNode(DUP_X2));
                            list.add(new InsnNode(POP));
                            list.add(new InsnNode(IAND));
                            list.add(new InsnNode(ISUB));
                        }
                        case IAND -> {
                            list.add(new InsnNode(DUP2));
                            list.add(new InsnNode(IADD));
                            list.add(new InsnNode(DUP_X2));
                            list.add(new InsnNode(POP));
                            list.add(new InsnNode(IOR));
                            list.add(new InsnNode(ISUB));
                        }
                        case IOR -> {
                            list.add(new InsnNode(DUP2));

                            list.add(new InsnNode(ICONST_M1));
                            list.add(new InsnNode(IXOR));
                            list.add(new InsnNode(SWAP));
                            list.add(new InsnNode(ICONST_M1));
                            list.add(new InsnNode(IXOR));
                            list.add(new InsnNode(SWAP));
                            list.add(new InsnNode(DUP2_X2));
                            list.add(new InsnNode(POP2));

                            list.add(new InsnNode(IADD));
                            list.add(new InsnNode(ICONST_1));
                            list.add(new InsnNode(IADD));
                            list.add(new InsnNode(DUP_X2));
                            list.add(new InsnNode(POP));
                            list.add(new InsnNode(IOR));
                            list.add(new InsnNode(IADD));
                        }
                    }

                    if(list.size() != 0) {
                        method.instructions.insertBefore(insn, list);
                        method.instructions.remove(insn);
                    }
                }
            }
        }
    }
}
