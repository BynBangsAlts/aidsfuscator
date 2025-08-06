package dev.lvstrng.aids.analysis.frames;

import me.coley.analysis.value.AbstractValue;
import org.objectweb.asm.tree.analysis.Frame;

public class FrameAnalyzer {
    public static String generateMap(Frame<AbstractValue> frame) {
        var builder = new StringBuilder("stack = { ");

        if(frame.getStackSize() > 0) {
            for(int i = 0; i < frame.getStackSize(); i++) {
                var stack = frame.getStack(i).getType();
                if(stack != null) {
                    builder.append("'").append(stack.getInternalName()).append("', ");
                    continue;
                }

                builder.append("'uninitialized', ");
            }
            builder.delete(builder.length() - 2, builder.length());
        }

        builder.append(" }, locals = { ");
        if(frame.getStackSize() > 0) {
            for(int i = 0; i < frame.getLocals(); i++) {
                var local = frame.getLocal(i).getType();
                if(local != null) {
                    builder.append("'").append(local.getInternalName()).append("', ");
                    continue;
                }

                builder.append("'uninitialized', ");
            }
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append(" }");

        return builder.toString();
    }

    public static boolean equals(Frame<AbstractValue> frame, Frame<AbstractValue> frame2) {
        return generateMap(frame).equals(generateMap(frame2));
    }
}
