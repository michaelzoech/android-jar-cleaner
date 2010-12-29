/**
 * Copyright 2011 Michael Zoech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitbrothers.android.cleaner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

public class ClassCleaner {

    public static void clean(InputStream in, OutputStream out) throws IOException {
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final ClassAdapter ca = new MethodCleanerAdapter(new CheckClassAdapter(cw, false));
        ClassReader cr = new ClassReader(in);
        cr.accept(ca, 0);
        out.write(cw.toByteArray());
        in.close();
    }

    private static class MethodCleanerAdapter extends ClassAdapter {

        private String superName;
        private boolean addConstructor;

        public MethodCleanerAdapter(ClassVisitor cv) {
            super(cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.superName = superName;
            this.addConstructor = (access & Opcodes.ACC_INTERFACE) != 0;
            if (this.superName == null) {
                this.superName = "java/lang/Object";
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public void visitEnd() {
            if (!addConstructor) {
                MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V");
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            cv.visitEnd();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ((Opcodes.ACC_ABSTRACT & access) != 0) {
                return cv.visitMethod(access, name, desc, signature, exceptions);
            }
            if ((Opcodes.ACC_NATIVE & access) != 0) {
                return cv.visitMethod(access, name, desc, signature, exceptions);
            }
            MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
            mv.visitCode();
            if (name.equals("<init>")) {
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superName, "<init>", "()V");
                mv.visitInsn(Opcodes.RETURN);
                if (desc.equals("()V")) {
                    addConstructor = true;
                }
            } else if (desc.endsWith(")V")) {
                mv.visitInsn(Opcodes.RETURN);
            } else if (desc.endsWith(")I") || desc.endsWith(")C") || desc.endsWith(")B") || desc.endsWith(")S")
                    || desc.endsWith(")Z")) {
                mv.visitInsn(Opcodes.ICONST_0);
                mv.visitInsn(Opcodes.IRETURN);
            } else if (desc.endsWith(")J")) {
                mv.visitInsn(Opcodes.LCONST_0);
                mv.visitInsn(Opcodes.LRETURN);
            } else if (desc.endsWith(")F")) {
                mv.visitInsn(Opcodes.FCONST_0);
                mv.visitInsn(Opcodes.FRETURN);
            } else if (desc.endsWith(")D")) {
                mv.visitInsn(Opcodes.DCONST_0);
                mv.visitInsn(Opcodes.DRETURN);
            } else {
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitInsn(Opcodes.ARETURN);
            }
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return null;
        }
    }
}
