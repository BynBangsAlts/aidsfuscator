package dev.lvstrng.aids.transform.impl.Misc;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * @author BynBang
 * Marks all methods and fields as synthetic to hide original code
 */
public class AccsesTransformer extends Transformer {

    @Override
    public void transform() {
        for (ClassNode classNode : Jar.getClasses()) {
            classNode.version = Math.max(classNode.version, Opcodes.V11);

            for (MethodNode method : classNode.methods) {
                if ((method.access & Opcodes.ACC_SYNTHETIC) == 0) {
                    method.access |= Opcodes.ACC_SYNTHETIC;
                }
            }

            for (FieldNode field : classNode.fields) {
                if ((field.access & Opcodes.ACC_SYNTHETIC) == 0) {
                    field.access |= Opcodes.ACC_SYNTHETIC;
                }
            }
        }
    }
}
