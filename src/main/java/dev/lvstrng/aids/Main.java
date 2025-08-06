package dev.lvstrng.aids;

import dev.lvstrng.aids.transform.impl.data.IntegerEncryptTransformer;
import dev.lvstrng.aids.transform.impl.flow.LightFlowTransformer;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void main(String[] args) {
        var obfuscator = new Obfuscator("crackme.jar", ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        obfuscator.readInput();
        obfuscator.obfuscate(
                new LightFlowTransformer(),
                new IntegerEncryptTransformer()
        );
        obfuscator.saveOutput("out.jar");
    }
}
