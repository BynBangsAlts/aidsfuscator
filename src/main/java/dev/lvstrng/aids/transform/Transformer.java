package dev.lvstrng.aids.transform;

import org.objectweb.asm.Opcodes;

import java.util.concurrent.ThreadLocalRandom;

public abstract class Transformer implements Opcodes {
    public static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public abstract void transform();
}
