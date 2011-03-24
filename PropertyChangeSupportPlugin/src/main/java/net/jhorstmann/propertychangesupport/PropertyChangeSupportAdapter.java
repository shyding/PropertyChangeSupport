package net.jhorstmann.propertychangesupport;

import net.jhorstmann.propertychangesupport.api.PropertyChangeEventSource;
import java.beans.Introspector;
import java.util.Arrays;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import static org.objectweb.asm.Opcodes.*;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public class PropertyChangeSupportAdapter extends ClassAdapter {

    private String classname;
    private String propertyChangeSupportField;
    private boolean hasSupport;

    public PropertyChangeSupportAdapter(ClassVisitor cv) {
        super(cv);
        this.propertyChangeSupportField = "_propertyChangeSupport";
    }

    private String[] addEventSourceInterface(String[] interfaces) {
        String eventSourceDesc = PropertyChangeEventSource.class.getName().replace('.', '/');
        boolean implementsEventSource = false;
        for (String desc : interfaces) {
            if (eventSourceDesc.equals(desc)) {
                implementsEventSource = true;
                break;
            }
        }
        String[] newinterfaces;
        if (implementsEventSource) {
            newinterfaces = interfaces;
        } else {
            newinterfaces = Arrays.copyOf(interfaces, interfaces.length + 1);
            newinterfaces[interfaces.length] = eventSourceDesc;
        }
        return newinterfaces;
    }

    @Override
    public void visit(final int version, final int access, final String classname, final String signature, final String superName, final String[] interfaces) {
        super.visit(version, access, classname, signature, superName, addEventSourceInterface(interfaces));
        this.classname = classname;
    }

    @Override
    public void visitEnd() {
        if (!hasSupport) {
            super.visitField(ACC_PRIVATE | ACC_FINAL | ACC_SYNTHETIC, propertyChangeSupportField, "Ljava/beans/PropertyChangeSupport;", null, null);
            {
                MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "addPropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, classname, propertyChangeSupportField, "Ljava/beans/PropertyChangeSupport;");
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/beans/PropertyChangeSupport", "addPropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V");
                mv.visitInsn(RETURN);
                mv.visitMaxs(2, 2);
                mv.visitEnd();
            }
            {
                MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "removePropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, classname, propertyChangeSupportField, "Ljava/beans/PropertyChangeSupport;");
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/beans/PropertyChangeSupport", "removePropertyChangeListener", "(Ljava/beans/PropertyChangeListener;)V");
                mv.visitInsn(RETURN);
                mv.visitMaxs(2, 2);
                mv.visitEnd();
            }
            {
                MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "addPropertyChangeListener", "(Ljava/lang/String;Ljava/beans/PropertyChangeListener;)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, classname, propertyChangeSupportField, "Ljava/beans/PropertyChangeSupport;");
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/beans/PropertyChangeSupport", "addPropertyChangeListener", "(Ljava/lang/String;Ljava/beans/PropertyChangeListener;)V");
                mv.visitInsn(RETURN);
                mv.visitMaxs(3, 3);
                mv.visitEnd();
            }
            {
                MethodVisitor mv = super.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, "removePropertyChangeListener", "(Ljava/lang/String;Ljava/beans/PropertyChangeListener;)V", null, null);
                mv.visitCode();
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, classname, propertyChangeSupportField, "Ljava/beans/PropertyChangeSupport;");
                mv.visitVarInsn(ALOAD, 1);
                mv.visitVarInsn(ALOAD, 2);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/beans/PropertyChangeSupport", "removePropertyChangeListener", "(Ljava/lang/String;Ljava/beans/PropertyChangeListener;)V");
                mv.visitInsn(RETURN);
                mv.visitMaxs(3, 3);
                mv.visitEnd();
            }
        }
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        if (name.equals("<init>")) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            return new AdviceAdapter(mv, access, name, desc) {

                @Override
                public void onMethodEnter() {
                    visitVarInsn(ALOAD, 0);
                    visitTypeInsn(NEW, "java/beans/PropertyChangeSupport");
                    visitInsn(DUP);
                    visitVarInsn(ALOAD, 0);
                    visitMethodInsn(INVOKESPECIAL, "java/beans/PropertyChangeSupport", "<init>", "(Ljava/lang/Object;)V");
                    visitFieldInsn(PUTFIELD, classname, propertyChangeSupportField, "Ljava/beans/PropertyChangeSupport;");
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    super.visitMaxs(Math.max(maxStack, 4), maxLocals);
                }
            };
        } else if (ASMHelper.isPublicSetter(access, name, desc)) {
            final String propertyName = Introspector.decapitalize(name.substring(3));
            final String getter = "get" + name.substring(3);
            final Type type = Type.getArgumentTypes(desc)[0];
            final String arg = type.getDescriptor();

            // rename method by prepending an underscore and make it private
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

            return new AdviceAdapter(mv, access, name, desc) {
                private int oldValue;

                @Override
                public void onMethodEnter() {
                    oldValue = super.newLocal(type);
                    System.out.println(oldValue);
                    visitVarInsn(ALOAD, 0);
                    visitMethodInsn(INVOKEVIRTUAL, classname, getter, "()" + arg);
                    storeLocal(oldValue, type);
                }

                @Override
                public void onMethodExit(int opcode) {
                    //visitVarInsn(ALOAD, 0);
                    loadThis();
                    visitFieldInsn(GETFIELD, classname, propertyChangeSupportField, "Ljava/beans/PropertyChangeSupport;");
                    // Stack: _propertyChangeSupport

                    visitLdcInsn(propertyName);
                    // Stack: _propertyChangeSupport, propertyName

                    loadLocal(oldValue, type);
                    ASMHelper.generateAutoBoxIfNeccessary(this, arg);
                    // Stack: _propertyChangeSupport, propertyName, oldValue

                    visitVarInsn(type.getOpcode(ILOAD), 1);
                    ASMHelper.generateAutoBoxIfNeccessary(this, arg);
                    // Stack: _propertyChangeSupport, propertyName, oldValue, newValue

                    visitMethodInsn(INVOKEVIRTUAL, "java/beans/PropertyChangeSupport", "firePropertyChange", "(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V");
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    super.visitMaxs(Math.max(maxStack, 4), Math.max(maxLocals, 3));
                }
            };
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }
}