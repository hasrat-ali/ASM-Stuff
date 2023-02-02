/***
 * ASM examples: examples showing how ASM can be used
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.hasrat.gradle.task

import org.objectweb.asm.*
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor

/**
 * DependencyVisitor
 *
 * @author Eugene Kuleshov
 */
class DependencyVisitor extends ClassVisitor {
    Set<String> packages = new HashSet<String>()

    Map<String, Map<String, Integer>> groups = new HashMap<String, Map<String, Integer>>()

    Map<String, Integer> current

    Set<String> getPackages() {
        return packages
    }

    DependencyVisitor() {
        super(Opcodes.ASM5)
    }

    // ClassVisitor

    @Override
    void visit(final int version, final int access, final String name,
               final String signature, final String superName,
               final String[] interfaces) {
        String p = getGroupKey(name)
        current = groups.get(p)
        if (current == null) {
            current = new HashMap<String, Integer>()
            groups.put(p, current)
        }

        if (signature == null) {
            if (superName != null) {
                addInternalName(superName)
            }
            addInternalNames(interfaces)
        } else {
            addSignature(signature)
        }
    }

    @Override
    AnnotationVisitor visitAnnotation(final String desc,
                                      final boolean visible) {
        addDesc(desc)
        return new AnnotationDependencyVisitor()
    }

    @Override
    AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                          final TypePath typePath, final String desc, final boolean visible) {
        addDesc(desc)
        return new AnnotationDependencyVisitor()
    }

    @Override
    FieldVisitor visitField(final int access, final String name,
                            final String desc, final String signature, final Object value) {
        if (signature == null) {
            addDesc(desc)
        } else {
            addTypeSignature(signature)
        }
        if (value instanceof Type) {
            addType((Type) value)
        }
        return new FieldDependencyVisitor()
    }

    @Override
    MethodVisitor visitMethod(final int access, final String name,
                              final String desc, final String signature, final String[] exceptions) {
        if (signature == null) {
            addMethodDesc(desc)
        } else {
            addSignature(signature)
        }
        addInternalNames(exceptions)
        return new MethodDependencyVisitor()
    }

    class AnnotationDependencyVisitor extends AnnotationVisitor {

        AnnotationDependencyVisitor() {
            super(Opcodes.ASM5)
        }

        @Override
        void visit(final String name, final Object value) {
            if (value instanceof Type) {
                addType((Type) value)
            }
        }

        @Override
        void visitEnum(final String name, final String desc,
                       final String value) {
            addDesc(desc)
        }

        @Override
        AnnotationVisitor visitAnnotation(final String name,
                                          final String desc) {
            addDesc(desc)
            return this
        }

        @Override
        AnnotationVisitor visitArray(final String name) {
            return this
        }
    }

    class FieldDependencyVisitor extends FieldVisitor {

        FieldDependencyVisitor() {
            super(Opcodes.ASM5)
        }

        @Override
        AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }

        @Override
        AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                              final TypePath typePath, final String desc,
                                              final boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }
    }

    class MethodDependencyVisitor extends MethodVisitor {

        MethodDependencyVisitor() {
            super(Opcodes.ASM5)
        }

        @Override
        AnnotationVisitor visitAnnotationDefault() {
            return new AnnotationDependencyVisitor()
        }

        @Override
        AnnotationVisitor visitAnnotation(final String desc,
                                          final boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }

        @Override
        AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                              final TypePath typePath, final String desc,
                                              final boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }

        @Override
        AnnotationVisitor visitParameterAnnotation(final int parameter,
                                                   final String desc, final boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }

        @Override
        void visitTypeInsn(final int opcode, final String type) {
            addType(Type.getObjectType(type))
        }

        @Override
        void visitFieldInsn(final int opcode, final String owner,
                            final String name, final String desc) {
            addInternalName(owner)
            addDesc(desc)
        }

        @Override
        void visitMethodInsn(final int opcode, final String owner,
                             final String name, final String desc) {
            addInternalName(owner)
            addMethodDesc(desc)
        }

        @Override
        void visitInvokeDynamicInsn(String name, String desc,
                                    Handle bsm, Object... bsmArgs) {
            addMethodDesc(desc)
            addConstant(bsm)
            for (int i = 0; i < bsmArgs.length; i++) {
                addConstant(bsmArgs[i])
            }
        }

        @Override
        void visitLdcInsn(final Object cst) {
            addConstant(cst)
        }

        @Override
        void visitMultiANewArrayInsn(final String desc, final int dims) {
            addDesc(desc)
        }

        @Override
        AnnotationVisitor visitInsnAnnotation(int typeRef,
                                              TypePath typePath, String desc, boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }

        @Override
        void visitLocalVariable(final String name, final String desc,
                                final String signature, final Label start, final Label end,
                                final int index) {
            addTypeSignature(signature)
        }

        @Override
        AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
                                                       TypePath typePath, Label[] start, Label[] end, int[] index,
                                                       String desc, boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }

        @Override
        void visitTryCatchBlock(final Label start, final Label end,
                                final Label handler, final String type) {
            if (type != null) {
                addInternalName(type)
            }
        }

        @Override
        AnnotationVisitor visitTryCatchAnnotation(int typeRef,
                                                  TypePath typePath, String desc, boolean visible) {
            addDesc(desc)
            return new AnnotationDependencyVisitor()
        }
    }

    class SignatureDependencyVisitor extends SignatureVisitor {

        String signatureClassName

        SignatureDependencyVisitor() {
            super(Opcodes.ASM5)
        }

        @Override
        void visitClassType(final String name) {
            signatureClassName = name
            addInternalName(name)
        }

        @Override
        void visitInnerClassType(final String name) {
            signatureClassName = signatureClassName + "\$" + name
            addInternalName(signatureClassName)
        }
    }

    // ---------------------------------------------

    private String getGroupKey(String name) {
        int n = name.lastIndexOf('/')
        if (n > -1) {
            //name = name.substring(0, n)
        }
        packages.add(name)
        return name
    }

    private void addName(final String name) {
        if (name == null) {
            return
        }
        String p = getGroupKey(name)
        if (current.containsKey(p)) {
            current.put(p, current.get(p) + 1)
        } else {
            current.put(p, 1)
        }
    }

    void addInternalName(final String name) {
        addType(Type.getObjectType(name))
    }

    private void addInternalNames(final String[] names) {
        for (int i = 0; names != null && i < names.length; i++) {
            addInternalName(names[i])
        }
    }

    void addDesc(final String desc) {
        addType(Type.getType(desc))
    }

    void addMethodDesc(final String desc) {
        addType(Type.getReturnType(desc))
        Type[] types = Type.getArgumentTypes(desc)
        for (int i = 0; i < types.length; i++) {
            addType(types[i])
        }
    }

    void addType(final Type t) {
        switch (t.getSort()) {
            case Type.ARRAY:
                addType(t.getElementType())
                break
            case Type.OBJECT:
                addName(t.getInternalName())
                break
            case Type.METHOD:
                addMethodDesc(t.getDescriptor())
                break
        }
    }

    private void addSignature(final String signature) {
        if (signature != null) {
            new SignatureReader(signature)
                    .accept(new SignatureDependencyVisitor())
        }
    }

    void addTypeSignature(final String signature) {
        if (signature != null) {
            new SignatureReader(signature)
                    .acceptType(new SignatureDependencyVisitor())
        }
    }

    void addConstant(final Object cst) {
        if (cst instanceof Type) {
            addType((Type) cst)
        } else if (cst instanceof Handle) {
            Handle h = (Handle) cst
            addInternalName(h.getOwner())
            addMethodDesc(h.getDesc())
        }
    }
}
