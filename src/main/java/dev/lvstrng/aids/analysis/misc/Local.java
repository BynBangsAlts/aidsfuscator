package dev.lvstrng.aids.analysis.misc;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Local {
    private final MethodNode method;
    private final int idx;
    private final Type type;

    private final int store, load;

    private Local(MethodNode method, int idx, Type type) {
        this.method = method;
        this.idx = idx;
        this.type = type;

        switch (type.getSort()) {
            case Type.INT, Type.BYTE, Type.CHAR, Type.SHORT, Type.BOOLEAN -> {
                store = Opcodes.ISTORE;
                load = Opcodes.ILOAD;
            }
            case Type.LONG -> {
                store = Opcodes.LSTORE;
                load = Opcodes.LLOAD;
            }
            case Type.DOUBLE -> {
                store = Opcodes.DSTORE;
                load = Opcodes.DLOAD;
            }
            case Type.FLOAT -> {
                store = Opcodes.FSTORE;
                load = Opcodes.FLOAD;
            }
            default -> {
                store = Opcodes.ASTORE;
                load = Opcodes.ALOAD;
            }
        }
    }

    public static Local alloc(MethodNode method, Type type) {
        return new Local(method, method.maxLocals += type.getSize(), type);
    }

    public static Local allocObject(MethodNode method) {
        var type = Type.getObjectType("java/lang/String"); //this won't change shit, literally only use it for .getSize() and determining if it's an obj or prim
        return alloc(method, type);
    }

    public int getIndex() {
        return idx;
    }

    public MethodNode getMethod() {
        return method;
    }

    public Type getType() {
        return type;
    }

    public int getLoad() {
        return load;
    }

    public int getStore() {
        return store;
    }

    public VarInsnNode load() {
        return new VarInsnNode(getLoad(), getIndex());
    }

    public VarInsnNode store() {
        return new VarInsnNode(getStore(), getIndex());
    }
}
