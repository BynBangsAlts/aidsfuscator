package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.dependencies.Dependencies;
import dev.lvstrng.aids.transform.impl.data.ArithmeticTransformer;
import dev.lvstrng.aids.transform.impl.data.IntegerEncryptTransformer;
import dev.lvstrng.aids.transform.impl.data.StringEncryptTransformer;
import dev.lvstrng.aids.transform.impl.flow.FlowFlattenTransformer;
import dev.lvstrng.aids.transform.impl.flow.IfConfuser;
import dev.lvstrng.aids.transform.impl.flow.UselessExceptionTransformer;
import dev.lvstrng.aids.transform.impl.rename.MethodRenameTransformer;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void main(String[] args) {
        var obfuscator = new Obfuscator("evaluator-2.jar", ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        Dependencies.analyze("dependencies/");

        obfuscator.readInput();
        obfuscator.obfuscate(
                new MethodRenameTransformer()
        );
        obfuscator.saveOutput("out.jar");
    }
}
