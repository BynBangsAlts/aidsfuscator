package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.Dependencies;
import dev.lvstrng.aids.transform.impl.data.ArithmeticTransformer;
import dev.lvstrng.aids.transform.impl.data.IntegerEncryptTransformer;
import dev.lvstrng.aids.transform.impl.data.StringEncryptTransformer;
import dev.lvstrng.aids.transform.impl.flow.FlowFlattenTransformer;
import dev.lvstrng.aids.transform.impl.flow.IfConfuser;
import dev.lvstrng.aids.transform.impl.flow.LightFlowTransformer;
import dev.lvstrng.aids.transform.impl.flow.UselessExceptionTransformer;
import org.objectweb.asm.ClassWriter;

public class Main {
    public static void main(String[] args) {
        var obfuscator = new Obfuscator("in.jar", ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        Dependencies.analyze("dependencies/");

        obfuscator.readInput();
        obfuscator.obfuscate(
                new FlowFlattenTransformer(),
                new IfConfuser()
        );
        obfuscator.saveOutput("out.jar");
    }
}
