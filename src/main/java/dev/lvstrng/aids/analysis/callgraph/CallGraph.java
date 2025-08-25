package dev.lvstrng.aids.analysis.callgraph;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.utils.HierarchyUtils;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class CallGraph {
    private final List<CallNode> nodes = new ArrayList<>();

    public void create() {
        for(var classNode : Jar.getClasses()) {
            for(var method : classNode.methods) {
                for(var insn : method.instructions) {
                    if(insn instanceof MethodInsnNode call) {
                        var clazz = Jar.getClass(call.owner);
                        if(clazz == null)
                            continue;

                        var node = HierarchyUtils.getMethod(clazz, call.name, call.desc);
                        if(node == null)
                            continue;

                        nodes.add(new CallNode(method, node, call));
                    }

                    if(insn instanceof InvokeDynamicInsnNode indy) {
                        var clazz = Jar.getClass(indy.bsm.getOwner());
                        if(clazz != null) {
                            var node = HierarchyUtils.getMethod(clazz, indy.bsm.getName(), indy.bsm.getDesc());
                            if(node != null) {
                                nodes.add(new CallNode(method, node, indy));
                            }
                        }

                        for(var arg : indy.bsmArgs) {
                            if(!(arg instanceof Handle handle))
                                continue;

                            var klass = Jar.getClass(handle.getOwner());
                            if(klass == null)
                                continue;

                            var node = HierarchyUtils.getMethod(klass, handle.getName(), handle.getDesc());
                            if(node == null)
                                continue;

                            nodes.add(new CallNode(method, node, indy));
                        }
                    }
                }
            }
        }
    }

    public List<CallNode> getCallsFor(MethodNode method) {
        return nodes.stream().filter(e -> e.callee() == method).toList();
    }

    public List<CallNode> getCallsFor(String method) {
        return nodes.stream().filter(e -> (e.callee().name + e.callee().desc).equals(method)).toList();
    }

    public List<CallNode> getNodes() {
        return nodes;
    }
}
