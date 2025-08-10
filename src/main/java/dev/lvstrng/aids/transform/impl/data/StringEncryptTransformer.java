package dev.lvstrng.aids.transform.impl.data;

import dev.lvstrng.aids.analysis.misc.Local;
import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.AESUtil;
import dev.lvstrng.aids.utils.ASMUtils;
import dev.lvstrng.aids.utils.Dictionary;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

//TODO concatenation + different decrypt methods (255, 64 etc. etc.)
public class StringEncryptTransformer extends Transformer {
    @Override
    public void transform() {
        for (var classNode : Jar.getClasses()) {
            var fieldName = Dictionary.FIELD.getNewName(classNode);
            var methodName = Dictionary.METHOD.getNewName(classNode);
            var pool = new ArrayList<String>();

            var keys = createKeys();
            int dflt = random.nextInt(255);
            int idxXor = random.nextInt();

            for (var method : classNode.methods) {
                ASMUtils.translateConcatenation(method);

                for (var ain : method.instructions) {
                    if (!(ain instanceof LdcInsnNode ldc))
                        continue;

                    if (!(ldc.cst instanceof String str))
                        continue;

                    int idx = pool.size();
                    int key = random.nextInt();
                    var list = new InsnList();

                    list.add(ASMUtils.pushInt(idx ^ idxXor));
                    list.add(ASMUtils.pushInt(key));
                    list.add(new MethodInsnNode(INVOKESTATIC, classNode.name, methodName, "(II)Ljava/lang/String;"));

                    method.instructions.insertBefore(ldc, list);
                    method.instructions.remove(ldc);

                    pool.add(xor255Runtime(str, key, keys, dflt));
                }
            }

            if (!pool.isEmpty()) {
                generateClinit(classNode, fieldName, pool);
                classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, fieldName, "[Ljava/lang/String;", null, null));
                create255Decrypt(classNode, keys, dflt, fieldName, methodName, idxXor);
            }
        }
    }

    // Runtime decrypt method (255 switch keys)
    private static void create255Decrypt(ClassNode classNode, int[] keys, int dflt, String fieldName, String methodName, int xorIdx) {
        var method = new MethodNode(
                ACC_PRIVATE | ACC_STATIC,
                methodName,
                "(II)Ljava/lang/String;",
                null, null
        );

        // ---- LOCALS ---- (we don't need to use the Local class because this is a new method anyway)
        int idxKey = 0; //param
        int key = 1; //param

        int charArr = 2; //chars to return
        int i = 3; //for loop
        int key2 = 4; //xor

        // ---- CODE ----
        var list = new InsnList();

        list.add(new FieldInsnNode(GETSTATIC, classNode.name, fieldName, "[Ljava/lang/String;"));
        list.add(new VarInsnNode(ILOAD, idxKey));
        list.add(ASMUtils.pushInt(xorIdx));
        list.add(new InsnNode(IXOR));
        list.add(new InsnNode(AALOAD)); //str
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C"));
        list.add(new VarInsnNode(ASTORE, charArr));

        // ---- CODE - FOR LOOP ----
        var forStart = new LabelNode();
        var forEnd = new LabelNode();

        list.add(new InsnNode(ICONST_0));
        list.add(new VarInsnNode(ISTORE, i));

        list.add(forStart);
        list.add(new VarInsnNode(ILOAD, i));
        list.add(new VarInsnNode(ALOAD, charArr));
        list.add(new InsnNode(ARRAYLENGTH));
        list.add(new JumpInsnNode(IF_ICMPGE, forEnd));

        list.add(new VarInsnNode(ILOAD, i));
        list.add(new IntInsnNode(SIPUSH, 255));
        list.add(new InsnNode(IAND));

        var dfltLbl = new LabelNode();
        var ex = new LabelNode();
        var labels = new LabelNode[255];
        for (int k = 0; k < 255; k++) {
            labels[k] = new LabelNode();
        }
        list.add(new TableSwitchInsnNode(0, 254, dfltLbl, labels));

        for (int k = 0; k < 255; k++) {
            list.add(labels[k]);
            list.add(ASMUtils.pushInt(keys[k]));
            list.add(new JumpInsnNode(GOTO, ex));
        }

        list.add(dfltLbl);
        list.add(ASMUtils.pushInt(dflt));
        list.add(ex);
        list.add(new VarInsnNode(ISTORE, key2));

        list.add(new VarInsnNode(ALOAD, charArr));
        list.add(new VarInsnNode(ILOAD, i));

        list.add(new InsnNode(DUP2));
        list.add(new InsnNode(CALOAD));
        list.add(new VarInsnNode(ILOAD, key));
        list.add(new InsnNode(IXOR));
        list.add(new VarInsnNode(ILOAD, key2));
        list.add(new InsnNode(IXOR));
        list.add(new InsnNode(CASTORE));

        list.add(new IincInsnNode(i, 1));
        list.add(new JumpInsnNode(GOTO, forStart));
        list.add(forEnd);

        // ---- CODE - RETURN ----
        list.add(new TypeInsnNode(NEW, "java/lang/String"));
        list.add(new InsnNode(DUP));
        list.add(new VarInsnNode(ALOAD, charArr));
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;"));
        list.add(new InsnNode(ARETURN));

        method.instructions.add(list);
        classNode.methods.add(method);
    }

    private static String xor255Runtime(String string, int key, int[] keys, int dflt) {
        var chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            int key2 = i == 255 ? dflt : keys[i & 255];
            chars[i] = (char) (chars[i] ^ key ^ key2);
        }

        return new String(chars);
    }

    private int[] createKeys() {
        var rand = new SecureRandom();
        var keys = new int[255];

        for (int i = 0; i < 255; i++) {
            keys[i] = rand.nextInt(255);
        }

        return keys;
    }

    @SuppressWarnings("all")
    private void generateClinit(ClassNode classNode, String fieldName, List<String> pool) {
        var method = classNode.methods.stream()
                .filter(e -> e.name.equals("<clinit>"))
                .findAny().orElseGet(() -> {
                    var node = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
                    classNode.methods.add(node);
                    return node;
                });

        // ---- INIT ----
        var aesKey = AESUtil.getKey().getEncoded();
        var iv = AESUtil.getIv();

        int key1 = random.nextInt();
        int key2 = random.nextInt();

        var sb = new StringBuilder();
        var lenArr = new char[pool.size()]; // sadly won't support strings with length > 65535 (but allows storing of more strings)
        for (int i = 0; i < pool.size(); i++) {
            var str = pool.get(i);

            sb.append(str);
            lenArr[i] = (char) str.length();
        }

        var theString = AESUtil.encrypt(xor(sb.toString(), key1, key2), aesKey, iv);

        // ---- LOCALS ----
        var alloc = Local.allocObject(method);              // stream
        var lenStrVar = Local.allocObject(method);          // lengths
        var keyVar = Local.allocObject(method);             // key bytes
        var ivVar = Local.allocObject(method);              // iv bytes
        var ciphVar = Local.allocObject(method);            // cipher

        // (for loop)
        var charArr = Local.allocObject(method);            // charArr
        var strArr = Local.allocObject(method);             // stringArr
        var iVar = Local.alloc(method, Type.INT_TYPE);      // index

        // (do-while loop for storing strings)
        var ptrVar = Local.alloc(method, Type.INT_TYPE);    // ptr
        var lenVar = Local.alloc(method, Type.INT_TYPE);    // len

        // ---- CODE ----
        var list = new InsnList();

        list.add(new LdcInsnNode(theString));
        list.add(alloc.store());

        list.add(new LdcInsnNode(new String(lenArr)));
        list.add(lenStrVar.store());

        list.add(new LdcInsnNode(new String(aesKey, StandardCharsets.ISO_8859_1)));
        list.add(new LdcInsnNode("ISO-8859-1"));
        list.add(new InsnNode(DUP_X1));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
        list.add(keyVar.store());

        list.add(new LdcInsnNode(new String(iv, StandardCharsets.ISO_8859_1)));
        list.add(new InsnNode(SWAP));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
        list.add(ivVar.store());

        // ---- CODE - CIPHER -----
        list.add(new LdcInsnNode("AES/CBC/PKCS5Padding"));
        list.add(new MethodInsnNode(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;"));
        list.add(ciphVar.store());

        // --------------
        list.add(ciphVar.load());
        list.add(new InsnNode(ICONST_2)); // decrypt mode

        list.add(new TypeInsnNode(NEW, "javax/crypto/spec/SecretKeySpec"));
        list.add(new InsnNode(DUP));
        list.add(keyVar.load());
        list.add(new LdcInsnNode("AES"));
        list.add(new MethodInsnNode(INVOKESPECIAL, "javax/crypto/spec/SecretKeySpec", "<init>", "([BLjava/lang/String;)V")); //secretKeySpec

        list.add(new TypeInsnNode(NEW, "javax/crypto/spec/IvParameterSpec"));
        list.add(new InsnNode(DUP));
        list.add(ivVar.load());
        list.add(new MethodInsnNode(INVOKESPECIAL, "javax/crypto/spec/IvParameterSpec", "<init>", "([B)V")); // iv

        list.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V"));

        list.add(new TypeInsnNode(NEW, "java/lang/String"));
        list.add(new InsnNode(DUP));
        list.add(ciphVar.load());
        list.add(new MethodInsnNode(INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;"));
        list.add(alloc.load());
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "doFinal", "([B)[B"));
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"));
        list.add(alloc.store()); // decrypted AES

        list.add(alloc.load());
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C"));
        list.add(charArr.store()); // char array

        list.add(ASMUtils.pushInt(pool.size()));
        list.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        list.add(strArr.store()); // string array

        // ---- CODE - XOR ----
        var forStart = new LabelNode();
        var forEnd = new LabelNode();

        list.add(new InsnNode(ICONST_0));
        list.add(iVar.store());

        list.add(forStart);
        list.add(iVar.load());
        list.add(charArr.load());
        list.add(new InsnNode(ARRAYLENGTH));
        list.add(new JumpInsnNode(IF_ICMPGE, forEnd));

        list.add(charArr.load());
        list.add(iVar.load());
        list.add(new InsnNode(DUP2));
        list.add(new InsnNode(CALOAD));

        list.add(ASMUtils.pushInt(key1));
        list.add(new InsnNode(IXOR));
        list.add(ASMUtils.pushInt(key2));
        list.add(new InsnNode(IXOR));
        list.add(new InsnNode(I2C));
        list.add(new InsnNode(CASTORE));

        list.add(new IincInsnNode(iVar.getIndex(), 1));
        list.add(new JumpInsnNode(GOTO, forStart));

        list.add(forEnd);
        list.add(new TypeInsnNode(NEW, "java/lang/String"));
        list.add(new InsnNode(DUP));
        list.add(charArr.load());
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"));
        list.add(alloc.store());

        // ---- CODE - DO-WHILE LOOP ----
        list.add(new InsnNode(ICONST_0));
        list.add(new InsnNode(DUP));
        list.add(iVar.store());
        list.add(ptrVar.store());

        var lbl = new LabelNode();
        list.add(lbl);

        list.add(lenStrVar.load());
        list.add(iVar.load());
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C"));
        list.add(lenVar.store());

        list.add(strArr.load());
        list.add(iVar.load());

        list.add(alloc.load());
        list.add(ptrVar.load());
        list.add(new InsnNode(DUP));
        list.add(lenVar.load());
        list.add(new InsnNode(IADD));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;"));
        list.add(new InsnNode(AASTORE));

        list.add(new IincInsnNode(iVar.getIndex(), 1));
        list.add(ptrVar.load());
        list.add(lenVar.load());
        list.add(new InsnNode(IADD));
        list.add(ptrVar.store());

        list.add(iVar.load());
        list.add(lenStrVar.load());
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C"));
        list.add(new InsnNode(ARRAYLENGTH));
        list.add(new JumpInsnNode(IF_ICMPLT, lbl));

        list.add(strArr.load());
        list.add(new FieldInsnNode(PUTSTATIC, classNode.name, fieldName, "[Ljava/lang/String;"));

        if (method.instructions.size() == 0) {
            list.add(new InsnNode(RETURN));
        }

        method.instructions.insert(list);
    }

    public static String xor(String str, int key, int key2) {
        var arr = str.toCharArray();

        for (int i = 0; i < arr.length; i++) {
            arr[i] = (char) (arr[i] ^ key ^ key2);
        }

        return new String(arr);
    }
}