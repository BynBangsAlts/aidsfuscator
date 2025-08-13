package dev.lvstrng.aids.transform.impl.renamer;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import org.objectweb.asm.tree.*;

/**
 * @author BynBang
 * Renames all local variables to Var1000 Var1001 etc.
 */
public class LocalVariableRenamer extends Transformer {

    private int counter = 1000;

    @Override
    public void transform() {
        for (ClassNode classNode : Jar.getClasses()) {
            for (MethodNode method : classNode.methods) {
                renameVariables(method);
            }
        }
    }

    private void renameVariables(MethodNode method) {
        if (method.parameters != null) {
            method.parameters.forEach(param -> param.name = "Var" + counter++);
        }

        if (method.localVariables != null) {
            method.localVariables.forEach(local -> local.name = "Var" + counter++);
        }
    }
}
