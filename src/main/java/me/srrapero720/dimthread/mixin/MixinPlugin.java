package me.srrapero720.dimthread.mixin;

import me.srrapero720.dimthread.DimThread;
import me.srrapero720.dimthread.mixin.tools.Synchronized;
import me.srrapero720.dimthread.mixin.tools.Volatile;
import me.srrapero720.dimthread.mixin.tools.Volatilize;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final Marker IT = MarkerManager.getMarker("MixinPlugin");

    private static final String VOLATILE_DESC = getDescriptor(Volatile.class);
    private static final String VOLATILIZE_DESC = getDescriptor(Volatilize.class);
    private static final String SYNCHRONIZE_DESC = getDescriptor(Synchronized.class);

    @Override
    public void onLoad(String s) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (targetClass.fields != null) {
            this.applyVolatilize(targetClass, targetClass.fields, targetClass.invisibleAnnotations);
        }
        if (targetClass.methods != null) {
            this.applySynchronized(targetClass, targetClass.methods);
        }
    }

    private void applyVolatilize(ClassNode target, List<FieldNode> targetFields, List<AnnotationNode> targetAnnotations) {
        final Set<FieldNode> NODE = new HashSet<>(targetFields.size());

        // PER CLASS ANNOTATION
        if (targetAnnotations != null) {
            for (var annotation: targetAnnotations) {
                if (!VOLATILIZE_DESC.equalsIgnoreCase(annotation.desc)) continue;
                boolean nonPublic = (boolean) annotation.values.get(0); // VOLATILIZE ONLY HAS 1 VALUE

                if (nonPublic) {
                    NODE.addAll(targetFields);
                    break;
                }

                for (FieldNode field: targetFields) {
                    if (!Modifier.isPublic(field.access)) continue;
                    NODE.add(field);
                }
            }
        }
        if (NODE.size() < targetFields.size()) {
            // PER FIELD
            for (FieldNode field: targetFields) {
                if (field.invisibleAnnotations == null) continue;
                for (AnnotationNode ann: field.invisibleAnnotations) {
                    if (!VOLATILE_DESC.equalsIgnoreCase(ann.desc)) continue;

                    NODE.add(field);
                    break;
                }
            }
        }

        NODE.forEach(fieldNode -> {
            fieldNode.access |= Modifier.VOLATILE;
            DimThread.LOGGER.info(IT,"Applied @Volatile modifier into FIELD '{}' from class '{}'", fieldNode.name, target.name);
        });
    }

    private void applySynchronized(ClassNode target, List<MethodNode> targetMethods) {
        final Set<MethodNode> NODES = new HashSet<>(targetMethods.size());

        for (MethodNode method: targetMethods) {
            if (method.invisibleAnnotations == null) continue;
            for (AnnotationNode ann: method.invisibleAnnotations) {
                if (!SYNCHRONIZE_DESC.equalsIgnoreCase(ann.desc)) continue;

                NODES.add(method);
                break;
            }
        }

        NODES.forEach(methodNode -> {
            methodNode.access |= Modifier.SYNCHRONIZED;
            DimThread.LOGGER.info(IT,"Applied @Synchronized modifier into METHOD '{}' from class '{}'", methodNode.name, target.name);
        });
    }

    private static String getDescriptor(Class<? extends Annotation> clazz) {
        return "L" + clazz.getName().replace(".", "/") + ";";
    }
}
