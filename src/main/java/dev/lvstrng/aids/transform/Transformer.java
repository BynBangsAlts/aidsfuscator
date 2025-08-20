package dev.lvstrng.aids.transform;

import org.objectweb.asm.Opcodes;

import java.util.concurrent.ThreadLocalRandom;

public interface Transformer extends Opcodes {
    ThreadLocalRandom random = ThreadLocalRandom.current();

    void transform();
}
