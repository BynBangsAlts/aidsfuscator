package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.Dependencies;
import dev.lvstrng.aids.transform.impl.rename.FieldRenameTransformer;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void main(String[] args) {
        var obfuscator = new Obfuscator("in.jar", ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        Dependencies.analyze("dependencies/");

        obfuscator.readInput();
        obfuscator.obfuscate(
                new FieldRenameTransformer()
        );
        obfuscator.saveOutput("out.jar");
    }
}
