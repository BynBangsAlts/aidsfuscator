package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.dependencies.Dependencies;
import dev.lvstrng.aids.transform.impl.misc.MethodParameterChangeTransformer;
import dev.lvstrng.aids.transform.impl.misc.MethodParameterObfuscateTransformer;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void main(String[] args) {
        var obfuscator = new Obfuscator("snake_game.jar", ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        Dependencies.analyze("dependencies/");

        obfuscator.readInput();
        obfuscator.obfuscate(
                new MethodParameterChangeTransformer(),
                new MethodParameterObfuscateTransformer()
        );
        obfuscator.saveOutput("out.jar");
    }
}

