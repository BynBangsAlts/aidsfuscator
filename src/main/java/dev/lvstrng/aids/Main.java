package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.dependencies.Dependencies;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void main(String[] args) {
        var obfuscator = new Obfuscator("eval.jar", ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        Dependencies.analyze("dependencies/");

        obfuscator.readInput();
        obfuscator.obfuscate(
                new MethodRenameTransformer()
        );
        obfuscator.saveOutput("out.jar");
    }
}
