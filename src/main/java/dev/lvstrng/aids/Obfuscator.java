package dev.lvstrng.aids;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.jar.hierarchy.Hierarchy;
import dev.lvstrng.aids.jar.resources.ResourceHandler;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.CustomClassWriter;
import dev.lvstrng.aids.utils.ZipUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Obfuscator {
    private final File input;
    public static int writerFlags;

    public static Hierarchy hierarchy;

    public Obfuscator(String input, int flags) {
        this.input = new File(input);
        writerFlags = flags;
    }

    public void readInput() {
        try (var zip = new ZipFile(input)) {
            for(var entry : zip.stream().toList()) {
                var in = zip.getInputStream(entry);

                if(entry.getName().contains(".class") || entry.getName().contains(".class/")) {
                    var node = readClass(in);
                    Jar.addClass(node);
                } else {
                    Jar.addResource(entry.getName(), in.readAllBytes());
                }
            }
        } catch (IOException _) {}

        hierarchy = new Hierarchy();
    }

    public void obfuscate(Transformer... transformers) {
        if(transformers == null)
            return;

        for(var transformer : transformers) {
            transformer.transform();
        }
    }

    public void saveOutput(String name) {
        var classes = new ArrayList<>(Jar.getClasses());
        classes.addAll(Jar.getArtificials().values());

        try (var zos = new ZipOutputStream(new FileOutputStream(name))) {
            for(var classNode : classes) {
                ZipUtils.writeZipEntry(zos, classNode.name + ".class", classToBytes(classNode));
            }
            
            new ResourceHandler().handle(zos);
            zos.finish();
        } catch (IOException _) {}
    }

    public static ClassNode readClass(InputStream is) throws IOException {
        var node = new ClassNode();
        new ClassReader(is).accept(node, 0);
        return node;
    }

    public static byte[] classToBytes(ClassNode classNode) {
        var writer = new CustomClassWriter(writerFlags); // why didn't we use this earlier...?
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
