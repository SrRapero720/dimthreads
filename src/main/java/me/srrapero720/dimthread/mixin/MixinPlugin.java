package me.srrapero720.dimthread.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import me.srrapero720.dimthread.mixin.tools.Synchronized;
import me.srrapero720.dimthread.mixin.tools.ThreadLocal;
import me.srrapero720.dimthread.mixin.tools.Volatile;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.throwables.MixinException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;

import static me.srrapero720.dimthread.DimThread.LOGGER;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final Marker IT = MarkerManager.getMarker("MixinPlugin");
    private static final String VOLATILE = getDescriptor(Volatile.class);
    private static final String SYNCHRONIZED = getDescriptor(Synchronized.class);
    private static final String THREADLOCAL = getDescriptor(ThreadLocal.class);

    @Override public void onLoad(String s) {}
    @Override public String getRefMapperConfig() {return null;}
    @Override public void acceptTargets(Set<String> set, Set<String> set1) {}
    @Override public List<String> getMixins() {return null;}


    @Override public boolean shouldApplyMixin(String s, String s1) {return true;}
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (targetClass.fields != null) {
            this.applyVolatilize(targetClass, targetClass.fields);
            this.applyThreadLocal(targetClass, targetClass.fields, targetClass.methods);
        }
        if (targetClass.methods != null) {
            this.applySynchronized(targetClass, targetClass.methods);
        }
    }

    /**
     * Adds volatile modifier on annotated {@link Volatile} fields
     * @param clazz root class
     * @param fields fields of the root class which can't be null
     */
    private void applyVolatilize(ClassNode clazz, List<FieldNode> fields) {
        for (final var f: fields) {
            if (f.invisibleAnnotations == null) continue;
            for (AnnotationNode ann: f.invisibleAnnotations) {
                if (!VOLATILE.equalsIgnoreCase(ann.desc)) continue;

                // SET VOLATILE
                f.access |= Modifier.VOLATILE;
                LOGGER.info(IT,"Applied @Volatile modifier into FIELD '{}' from class '{}'", f.name, clazz.name);
                break;
            }
        }
    }

    /**
     * Adds volatile modifier on annotated {@link Synchronized} fields
     * @param clazz root class
     * @param methods fields of the root class which can't be null
     */
    private void applySynchronized(ClassNode clazz, List<MethodNode> methods) {
        for (MethodNode m: methods) {
            if (m.invisibleAnnotations == null) continue;
            for (AnnotationNode ann: m.invisibleAnnotations) {
                if (!SYNCHRONIZED.equalsIgnoreCase(ann.desc)) continue;

                m.access |= Modifier.SYNCHRONIZED;
                LOGGER.info(IT,"Applied @Synchronized modifier into METHOD '{}' from class '{}'", m.name, clazz.name);
                break;
            }
        }
    }

    /**
     * Wraps private fields into a {@link java.lang.ThreadLocal ThreadLocal} instances for
     * non thread-safe operations
     * @param clazz root class
     * @param fields fields of the root class which can't be null
     * @param methods methods of the root class which can't be null
     */
    // TODO: please, OPTIMIZE
    private void applyThreadLocal(ClassNode clazz, List<FieldNode> fields, List<MethodNode> methods) {
        // key is the field, value is the type descriptor
        final Set<FieldNode> NODES = new ObjectArraySet<>();

        // FETCH FIELDS
        for (var field: fields) {
            if (field.invisibleAnnotations != null) {
                for (AnnotationNode annotation: field.invisibleAnnotations) {
                    if (THREADLOCAL.equals(annotation.desc)) {
                        if (!Modifier.isPrivate(field.access)) {
                            // I can't ensure public nor protected nor package-private fields are used on classes running on mixins
                            throw new MixinException("Failed during convert field '" + field.name + "' from class '" + clazz.name + "' into ThreadLocal", new IllegalArgumentException("Field is not private"));
                        }
                        NODES.add(field);
                        break;
                    }
                }
            }
        }

        // APPLY PATCH
        for (var field: NODES) {
            // FIELD APPLICATORS
            final String ogFieldDesc = field.desc;
            final Set<String> affectedMethods = new HashSet<>();
            field.desc = "Ljava/lang/ThreadLocal;";
            field.access |= Modifier.FINAL;

            MethodNode clinit = null;
            MethodNode init = null;

            // SHOTGUN THE TRANSFORMATION
            for (MethodNode method: methods) {
                // STORE INITS
                if (method.name.equals("<clinit>")) {
                    clinit = method;
                }
                if (method.name.equals("<init>")) {
                    init = method;
                }

                for (AbstractInsnNode insn: method.instructions) {
                    // IS A FIELD INSTRUCTION NODE?
                    if (!(insn instanceof FieldInsnNode fieldInsn)) continue;

                    // IS THE FIELD WE ARE LOOKING FOR?
                    if (!field.name.equals(fieldInsn.name)) continue;

                    if (fieldInsn.getOpcode() == Opcodes.GETFIELD || fieldInsn.getOpcode() == Opcodes.GETSTATIC) {
                        method.instructions.insertBefore(insn, new FieldInsnNode(fieldInsn.getOpcode(), fieldInsn.owner, fieldInsn.name, "Ljava/lang/ThreadLocal;"));
                        // REPLACE WITH ThreadLocal#get()
                        method.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/ThreadLocal", "get", "()Ljava/lang/Object;", false));
                        unwrapPrimitiveArgument(Type.getType(ogFieldDesc), method.instructions, insn);

                        // REMOVE OLD INSTRUCTION (UNCASTED)
                        method.instructions.remove(insn);

                        // DEBUG
                        affectedMethods.add(method.name);
                    } else if (fieldInsn.getOpcode() == Opcodes.PUTFIELD || fieldInsn.getOpcode() == Opcodes.PUTSTATIC) {
                        final int OPCODE = fieldInsn.getOpcode() == Opcodes.PUTSTATIC ? Opcodes.GETSTATIC : Opcodes.GETFIELD;
                        AbstractInsnNode previousInsn = insn.getPrevious();

                        // GET FIELD
                        method.instructions.insertBefore(previousInsn, new FieldInsnNode(OPCODE, fieldInsn.owner, fieldInsn.name, "Ljava/lang/ThreadLocal;"));
                        wrapPrimitiveArgument(Type.getType(ogFieldDesc), method.instructions, insn);
                        // CALL ThreadLocal#set(value) OF THE FIELD
                        method.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/ThreadLocal", "set", "(Ljava/lang/Object;)V", false));
                        // DELETE OLD PUTFIELD
                        method.instructions.remove(insn);
                        affectedMethods.add(method.name);
                    } else {
                        LOGGER.warn(IT, "Field '{}' in '{}' has a unidentified or invalid usage (opcode: {}), this might cause issues in a future", insn.getOpcode(), field.name, clazz.name);
                    }

                }
            }

            // ADD THE DECLARATOR
            if (Modifier.isStatic(field.access)) {
                if (clinit == null) {
                    // CREATE <clinit>
                    clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                    clinit.instructions.add(new InsnNode(Opcodes.RETURN)); // Placeholder
                    methods.add(clinit);
                }

                // ADD THE DECLARATOR TO <clinit>
                InsnList staticInit = new InsnList();
                staticInit.add(new TypeInsnNode(Opcodes.NEW, "java/lang/ThreadLocal")); // CREATE INSTANCE
                staticInit.add(new InsnNode(Opcodes.DUP)); // DUPLICATE
                staticInit.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/ThreadLocal", "<init>", "()V", false)); // INVOKE
                staticInit.add(new FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, field.name, field.desc));

                clinit.instructions.insertBefore(clinit.instructions.getFirst(), staticInit);
            } else {
                if (init == null) {
                    init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);

                    // ADD SUPER CALL
                    InsnList constructorInit = new InsnList();
                    constructorInit.add(new VarInsnNode(Opcodes.ALOAD, 0)); // LOAD "this"
                    constructorInit.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, clazz.superName, "<init>", "()V", false));
                    constructorInit.add(new InsnNode(Opcodes.RETURN)); // RETURN
                    init.instructions.add(constructorInit);
                    methods.add(init);
                }

                // ADD THE DECLARATOR TO <init>
                InsnList instanceInit = new InsnList();
                instanceInit.add(new VarInsnNode(Opcodes.ALOAD, 0)); // LOAD "this"
                instanceInit.add(new TypeInsnNode(Opcodes.NEW, "java/lang/ThreadLocal")); // CREATE INSTANCE
                instanceInit.add(new InsnNode(Opcodes.DUP)); // DUPLICATE
                instanceInit.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/ThreadLocal", "<init>", "()V", false));
                instanceInit.add(new FieldInsnNode(Opcodes.PUTFIELD, clazz.name, field.name, field.desc));
                init.instructions.insert(init.instructions.getFirst(), instanceInit);
            }
        }
    }

    private static void wrapPrimitiveArgument(Type targetType, InsnList instructions, AbstractInsnNode insn) {
        switch (targetType.getSort()) {
            case Type.BOOLEAN -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false)); // Z
            case Type.BYTE -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false)); // B
            case Type.CHAR -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false)); // C
            case Type.SHORT -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false)); // S
            case Type.INT -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)); // I
            case Type.FLOAT -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false)); // F
            case Type.LONG -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false)); // J
            case Type.DOUBLE -> instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false)); // D
        }
    }

    private static void unwrapPrimitiveArgument(Type targetType, InsnList instructions, AbstractInsnNode insn) {
        switch (targetType.getSort()) {
            case Type.BOOLEAN -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false));
            }

            // Z
            case Type.BYTE -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Byte"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false));
            }

            // B
            case Type.CHAR -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Character"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false));
            }

            // C
            case Type.SHORT -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Short"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false));
            }

            // S
            case Type.INT -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
            }

            // I
            case Type.FLOAT -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false));
            }

            // F
            case Type.LONG -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()L", false));
            }

            // J
            case Type.DOUBLE -> {
                instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false));
            }

            // D
            case Type.ARRAY ->
                    instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, targetType.getDescriptor()));
            default ->
                    instructions.insertBefore(insn, new TypeInsnNode(Opcodes.CHECKCAST, targetType.getInternalName()));
        }
    }

    private static String getDescriptor(Class<? extends Annotation> clazz) {
        return "L" + clazz.getName().replace(".", "/") + ";";
    }
}
