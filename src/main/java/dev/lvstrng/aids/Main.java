package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.dependencies.Dependencies;
import dev.lvstrng.aids.transform.impl.rename.ClassRenameTransformer;
import dev.lvstrng.aids.transform.impl.rename.FieldRenameTransformer;
import dev.lvstrng.aids.transform.impl.rename.MethodRenameTransformer;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void main(String[] args) {
        var obfuscator = new Obfuscator("evaluator-2.jar", ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        Dependencies.analyze("dependencies/");

        obfuscator.readInput();
        obfuscator.obfuscate(
                new MethodRenameTransformer(),
                new FieldRenameTransformer(),
                new ClassRenameTransformer()
        );
        obfuscator.saveOutput("out.jar");
    }
}

