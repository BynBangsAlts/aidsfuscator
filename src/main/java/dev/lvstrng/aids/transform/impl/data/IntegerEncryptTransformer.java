package dev.lvstrng.aids.transform.impl.data;

import dev.lvstrng.aids.analysis.misc.Local;
import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.AESUtil;
import dev.lvstrng.aids.utils.ASMUtils;
import dev.lvstrng.aids.utils.Dictionary;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//TODO encrypt before adding to pool and add decrypt method
public class IntegerEncryptTransformer extends Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            if(Modifier.isInterface(classNode.access))
                continue;

            var fieldName = Dictionary.FIELD.getNewName(classNode);
            var pool = new ArrayList<Integer>();
            var key = AESUtil.getKey().getEncoded();
            var iv = AESUtil.getIv();

            for(var method : classNode.methods) {
                for(var insn : method.instructions) {
                    if(!isValidIntPush(insn))
                        continue;

                    int xor1 = random.nextInt();
                    int xor2 = random.nextInt();

                    var list = new InsnList();
                    list.add(new FieldInsnNode(GETSTATIC, classNode.name, fieldName, "[I"));
                    list.add(ASMUtils.pushInt(pool.size()));
                    list.add(new InsnNode(IALOAD));

                    var number = ASMUtils.getInt(insn);
                    pool.add(number);

                    method.instructions.insertBefore(insn, list);
                    method.instructions.remove(insn);
                }
            }

            if(!pool.isEmpty()) {
                generateClinit(classNode, fieldName, pool, key, iv);
                classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, fieldName, "[I", null, null));
            }
        }
    }

    private void generateClinit(ClassNode classNode, String fieldName, List<Integer> pool, byte[] key, byte[] iv) {
        var method = classNode.methods.stream()
                .filter(e -> e.name.equals("<clinit>"))
                .findAny().orElseGet(() -> {
                    var node = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
                    classNode.methods.add(node);
                    return node;
                });

        int xorKey = random.nextInt();
        var sb = new StringBuilder();
        for(var num : pool) {
            sb.append(new String(intToBytes(num ^ xorKey), StandardCharsets.ISO_8859_1));
        }

        var enc = AESUtil.encrypt(sb.toString(), key, iv);
        var alloc = Local.alloc(method, Type.getObjectType("java/lang/String"));

        var keyBytes =  Local.allocObject(method);
        var ivBytes =   Local.allocObject(method);
        var cipher =    Local.allocObject(method);
        var i =         Local.alloc(method, Type.INT_TYPE);
        var ptr =       Local.alloc(method, Type.INT_TYPE);
        var arr =       Local.allocObject(method);
        var bytes =     Local.allocObject(method);

        var list = new InsnList();

        list.add(new LdcInsnNode(enc));
        list.add(alloc.store());

        list.add(new LdcInsnNode(new String(key, StandardCharsets.ISO_8859_1)));
        list.add(new LdcInsnNode("ISO-8859-1"));
        list.add(new InsnNode(DUP_X1));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
        list.add(keyBytes.store());

        list.add(new LdcInsnNode(new String(iv, StandardCharsets.ISO_8859_1)));
        list.add(new InsnNode(SWAP));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
        list.add(ivBytes.store());

        list.add(new LdcInsnNode("AES/CBC/PKCS5Padding"));
        list.add(new MethodInsnNode(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;"));
        list.add(cipher.store());

        list.add(cipher.load());
        list.add(new InsnNode(ICONST_2));
        list.add(new TypeInsnNode(NEW, "javax/crypto/spec/SecretKeySpec"));
        list.add(new InsnNode(DUP));
        list.add(keyBytes.load());
        list.add(new LdcInsnNode("AES"));
        list.add(new MethodInsnNode(INVOKESPECIAL, "javax/crypto/spec/SecretKeySpec", "<init>", "([BLjava/lang/String;)V")); //secretKeySpec

        list.add(new TypeInsnNode(NEW, "javax/crypto/spec/IvParameterSpec"));
        list.add(new InsnNode(DUP));
        list.add(ivBytes.load());
        list.add(new MethodInsnNode(INVOKESPECIAL, "javax/crypto/spec/IvParameterSpec", "<init>", "([B)V"));

        list.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V"));

        list.add(new TypeInsnNode(NEW, "java/lang/String"));
        list.add(new InsnNode(DUP));
        list.add(cipher.load());
        list.add(new MethodInsnNode(INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;"));
        list.add(alloc.load());
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "doFinal", "([B)[B"));
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"));
        list.add(alloc.store());

        list.add(new InsnNode(ICONST_0));
        list.add(new InsnNode(DUP));
        list.add(i.store());
        list.add(ptr.store());

        list.add(alloc.load());
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "length", "()I"));
        list.add(new InsnNode(ICONST_4));
        list.add(new InsnNode(IDIV));
        list.add(new IntInsnNode(NEWARRAY, T_INT));
        list.add(arr.store());

        var lbl = new LabelNode();
        list.add(lbl);

        list.add(alloc.load());
        list.add(ptr.load());
        list.add(new InsnNode(DUP));
        list.add(new InsnNode(ICONST_4));
        list.add(new InsnNode(IADD));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;"));
        list.add(new LdcInsnNode("ISO-8859-1"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
        list.add(bytes.store());

        //unpack
        list.add(bytes.load());
        list.add(ASMUtils.pushInt(0));
        list.add(new InsnNode(BALOAD));
        list.add(ASMUtils.pushInt(255));
        list.add(new InsnNode(IAND));
        list.add(ASMUtils.pushInt(24));
        list.add(new InsnNode(ISHL));

        list.add(bytes.load());
        list.add(ASMUtils.pushInt(1));
        list.add(new InsnNode(BALOAD));
        list.add(ASMUtils.pushInt(255));
        list.add(new InsnNode(IAND));
        list.add(ASMUtils.pushInt(16));
        list.add(new InsnNode(ISHL));
        list.add(new InsnNode(IOR));

        list.add(bytes.load());
        list.add(ASMUtils.pushInt(2));
        list.add(new InsnNode(BALOAD));
        list.add(ASMUtils.pushInt(255));
        list.add(new InsnNode(IAND));
        list.add(ASMUtils.pushInt(8));
        list.add(new InsnNode(ISHL));
        list.add(new InsnNode(IOR));

        list.add(bytes.load());
        list.add(ASMUtils.pushInt(3));
        list.add(new InsnNode(BALOAD));
        list.add(ASMUtils.pushInt(255));
        list.add(new InsnNode(IAND));
        list.add(new InsnNode(IOR));

        list.add(ASMUtils.pushInt(xorKey));
        list.add(new InsnNode(IXOR));
        list.add(arr.load());
        list.add(i.load());
        list.add(new InsnNode(DUP2_X1));
        list.add(new InsnNode(POP2));
        list.add(new InsnNode(IASTORE));

        list.add(new IincInsnNode(i.getIndex(), 1));
        list.add(new IincInsnNode(ptr.getIndex(), 4));

        list.add(i.load());
        list.add(arr.load());
        list.add(new InsnNode(ARRAYLENGTH));
        list.add(new JumpInsnNode(IF_ICMPLT, lbl));

        list.add(arr.load());
        list.add(new FieldInsnNode(PUTSTATIC, classNode.name, fieldName, "[I"));

        if(method.instructions.size() == 0)
            list.add(new InsnNode(RETURN));

        method.instructions.insert(list);
    }

    private static boolean isValidIntPush(AbstractInsnNode insn) {
        return (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Integer) || insn.getOpcode() == SIPUSH || insn.getOpcode() == BIPUSH;
    }

    private static byte[] intToBytes(int i) {
        var bytes = new byte[4];
        bytes[0] = (byte) (i >> 24);
        bytes[1] = (byte) (i >> 16);
        bytes[2] = (byte) (i >> 8);
        bytes[3] = (byte) (i);

        return bytes;
    }

    public static int bytesToInt(byte[] bytes) {
        return ((int) bytes[0] & 0xFF) << 24 |
                ((int) bytes[1] & 0xFF) << 16 |
                ((int) bytes[2] & 0xFF) << 8 |
                ((int) bytes[3] & 0xFF);
    }
}
