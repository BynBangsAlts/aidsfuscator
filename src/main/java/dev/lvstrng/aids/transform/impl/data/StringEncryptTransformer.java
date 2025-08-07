package dev.lvstrng.aids.transform.impl.data;

import dev.lvstrng.aids.jar.Jar;
import dev.lvstrng.aids.transform.Transformer;
import dev.lvstrng.aids.utils.AESUtil;
import dev.lvstrng.aids.utils.ASMUtils;
import dev.lvstrng.aids.utils.Dictionary;
import org.objectweb.asm.tree.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

//TODO encrypt before adding to pool and add decrypt method
public class StringEncryptTransformer extends Transformer {
    @Override
    public void transform() {
        for(var classNode : Jar.getClasses()) {
            var fieldName = Dictionary.FIELD.getNewName(classNode);
            var pool = new ArrayList<String>();

            for(var method : classNode.methods) {
                for(var insn : method.instructions) {
                    if(!(insn instanceof LdcInsnNode ldc))
                        continue;

                    if(!(ldc.cst instanceof String str))
                        continue;

                    int idx = pool.size();

                    var list = new InsnList();
                    list.add(new FieldInsnNode(GETSTATIC, classNode.name, fieldName, "[Ljava/lang/String;"));
                    list.add(ASMUtils.pushInt(idx));
                    list.add(new InsnNode(AALOAD));

                    method.instructions.insertBefore(ldc, list);
                    method.instructions.remove(ldc);

                    pool.add(str);
                }
            }

            if(!pool.isEmpty()) {
                generateClinit(classNode, fieldName, pool);
                classNode.fields.add(new FieldNode(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, fieldName, "[Ljava/lang/String;", null, null));
            }
        }
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
        var lenArr = new char[pool.size()]; //saddly won't support strings with length > 65535 (but allows storing of more strings)
        for(int i = 0; i < pool.size(); i++) {
            var str = pool.get(i);

            sb.append(str);
            lenArr[i] = (char) str.length();
        }

        var theString = AESUtil.encrypt(xor(sb.toString(), key1, key2), aesKey, iv);
        // ---- LOCALS ----
        var alloc       = method.maxLocals++;   //joined string
        var lenStrVar   = alloc + 1;            //lengths
        var keyVar      = alloc + 2;            // key bytes
        var ivVar       = alloc + 3;            // iv bytes
        var ciphVar     = alloc + 4;            //cipher

        // (for loop)
        var charArr     = alloc + 5;            //charArr
        var strArr      = alloc + 6;            //stringArr
        var iVar        = alloc + 7;            // i (for loop)

        // (do-while loop for storing strings)
        var ptrVar      = alloc + 8;            //ptr
        var lenVar      = alloc + 9;            //len

        method.maxLocals += lenVar;
        // ---- CODE ----
        var list = new InsnList();

        list.add(new LdcInsnNode(theString));
        list.add(new VarInsnNode(ASTORE, alloc));

        list.add(new LdcInsnNode(new String(lenArr)));
        list.add(new VarInsnNode(ASTORE, lenStrVar));

        list.add(new LdcInsnNode(new String(aesKey, StandardCharsets.ISO_8859_1)));
        list.add(new LdcInsnNode("ISO-8859-1"));
        list.add(new InsnNode(DUP_X1));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
        list.add(new VarInsnNode(ASTORE, keyVar));

        list.add(new LdcInsnNode(new String(iv, StandardCharsets.ISO_8859_1)));
        list.add(new InsnNode(SWAP));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "getBytes", "(Ljava/lang/String;)[B"));
        list.add(new VarInsnNode(ASTORE, ivVar));

        // ---- CODE - CIPHER -----
        list.add(new LdcInsnNode("AES/CBC/PKCS5Padding"));
        list.add(new MethodInsnNode(INVOKESTATIC, "javax/crypto/Cipher", "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;"));
        list.add(new VarInsnNode(ASTORE, ciphVar));

        // --------------
        list.add(new VarInsnNode(ALOAD, ciphVar));
        list.add(new InsnNode(ICONST_2)); //decrypt mode

        list.add(new TypeInsnNode(NEW, "javax/crypto/spec/SecretKeySpec"));
        list.add(new InsnNode(DUP));
        list.add(new VarInsnNode(ALOAD, keyVar));
        list.add(new LdcInsnNode("AES"));
        list.add(new MethodInsnNode(INVOKESPECIAL, "javax/crypto/spec/SecretKeySpec", "<init>", "([BLjava/lang/String;)V")); //secretKeySpec

        list.add(new TypeInsnNode(NEW, "javax/crypto/spec/IvParameterSpec"));
        list.add(new InsnNode(DUP));
        list.add(new VarInsnNode(ALOAD, ivVar));
        list.add(new MethodInsnNode(INVOKESPECIAL, "javax/crypto/spec/IvParameterSpec", "<init>", "([B)V")); //iv

        list.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V"));

        list.add(new TypeInsnNode(NEW, "java/lang/String"));
        list.add(new InsnNode(DUP));
        list.add(new VarInsnNode(ALOAD, ciphVar));
        list.add(new MethodInsnNode(INVOKESTATIC, "java/util/Base64", "getDecoder", "()Ljava/util/Base64$Decoder;"));
        list.add(new VarInsnNode(ALOAD, alloc));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/util/Base64$Decoder", "decode", "(Ljava/lang/String;)[B"));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "javax/crypto/Cipher", "doFinal", "([B)[B"));
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([B)V"));
        list.add(new VarInsnNode(ASTORE, alloc)); //decrypted AES

        list.add(new VarInsnNode(ALOAD, alloc));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C"));
        list.add(new VarInsnNode(ASTORE, charArr)); //char array

        list.add(ASMUtils.pushInt(pool.size()));
        list.add(new TypeInsnNode(ANEWARRAY, "java/lang/String"));
        list.add(new VarInsnNode(ASTORE, strArr)); // string array

        // ---- CODE - XOR ----
        var forStart = new LabelNode();
        var forEnd =   new LabelNode();

        list.add(new InsnNode(ICONST_0));
        list.add(new VarInsnNode(ISTORE, iVar));

        list.add(forStart);
        list.add(new VarInsnNode(ILOAD, iVar));
        list.add(new VarInsnNode(ALOAD, charArr));
        list.add(new InsnNode(ARRAYLENGTH));
        list.add(new JumpInsnNode(IF_ICMPGE, forEnd));

        list.add(new VarInsnNode(ALOAD, charArr));
        list.add(new VarInsnNode(ILOAD, iVar));
        list.add(new InsnNode(DUP2));
        list.add(new InsnNode(CALOAD));

        list.add(ASMUtils.pushInt(key1));
        list.add(new InsnNode(IXOR));
        list.add(ASMUtils.pushInt(key2));
        list.add(new InsnNode(IXOR));
        list.add(new InsnNode(I2C));
        list.add(new InsnNode(CASTORE));

        list.add(new IincInsnNode(iVar, 1));
        list.add(new JumpInsnNode(GOTO, forStart));

        list.add(forEnd);
        list.add(new TypeInsnNode(NEW, "java/lang/String"));
        list.add(new InsnNode(DUP));
        list.add(new VarInsnNode(ALOAD, charArr));
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/String", "<init>", "([C)V"));
        list.add(new VarInsnNode(ASTORE, alloc));

        //---- CODE - DO-WHILE LOOP ----
        list.add(new InsnNode(ICONST_0));
        list.add(new InsnNode(DUP));
        list.add(new VarInsnNode(ISTORE, iVar));
        list.add(new VarInsnNode(ISTORE, ptrVar));

        var lbl = new LabelNode();
        list.add(lbl);

        list.add(new VarInsnNode(ALOAD, lenStrVar));
        list.add(new VarInsnNode(ILOAD, iVar));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "charAt", "(I)C"));
        list.add(new VarInsnNode(ISTORE, lenVar));

        list.add(new VarInsnNode(ALOAD, strArr));
        list.add(new VarInsnNode(ILOAD, iVar));

        list.add(new VarInsnNode(ALOAD, alloc));
        list.add(new VarInsnNode(ILOAD, ptrVar));
        list.add(new InsnNode(DUP));
        list.add(new VarInsnNode(ILOAD, lenVar));
        list.add(new InsnNode(IADD));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "substring", "(II)Ljava/lang/String;"));

        list.add(new InsnNode(AASTORE));
        list.add(new IincInsnNode(iVar, 1));
        list.add(new VarInsnNode(ILOAD, ptrVar));
        list.add(new VarInsnNode(ILOAD, lenVar));
        list.add(new InsnNode(IADD));
        list.add(new VarInsnNode(ISTORE, ptrVar));

        list.add(new VarInsnNode(ILOAD, iVar));
        list.add(new VarInsnNode(ALOAD, lenStrVar));
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/String", "toCharArray", "()[C"));
        list.add(new InsnNode(ARRAYLENGTH));
        list.add(new JumpInsnNode(IF_ICMPLT, lbl));

        list.add(new VarInsnNode(ALOAD, strArr));
        list.add(new FieldInsnNode(PUTSTATIC, classNode.name, fieldName, "[Ljava/lang/String;"));

        if(method.instructions.size() == 0) {
            list.add(new InsnNode(RETURN));
        }

        method.instructions.insert(list);
    }

    public static String xor(String str, int key, int key2) {
        var arr = str.toCharArray();

        for(int i = 0; i < arr.length; i++) {
            arr[i] = (char) (arr[i] ^ key ^ key2);
        }

        return new String(arr);
    }

}
