package us.shiroyama.android.cuckoo.processor;

import android.annotation.TargetApi;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import us.shiroyama.android.cuckoo.annotations.By;
import us.shiroyama.android.cuckoo.annotations.Delegate;

/**
 * Annotation Processor
 *
 * @author Fumihiko Shiroyama
 */

@AutoService(Processor.class)
public class DelegationProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Types typeUtils;
    private Messager messager;
    private Filer filer;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return ImmutableSet.of(Delegate.class.getName(), By.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);

        elementUtils = env.getElementUtils();
        typeUtils = env.getTypeUtils();
        messager = env.getMessager();
        filer = env.getFiler();
    }

    @TargetApi(24)
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (annotations.size() == 0) {
            return true;
        }

        roundEnvironment.getElementsAnnotatedWith(Delegate.class)
                .stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .forEach(typeElement -> {
                    TypeElement originalClass = (TypeElement) typeElement;
                    String targetPackage = elementUtils.getPackageOf(originalClass).getQualifiedName().toString();

                    List<MethodSpec> methodSpecs = Stream.concat(
                            getConstructorSpecs(getConstructorElements(originalClass), originalClass).stream(),
                            getMethodSpecs(getOverridingElements(originalClass), getMethodToFieldNameMap(roundEnvironment)).stream()
                    ).collect(Collectors.toList());

                    TypeSpec typeSpec = TypeSpec
                            .classBuilder(getSubclassName(originalClass))
                            .addModifiers(getModifiers(originalClass))
                            .superclass(ClassName.get(originalClass))
                            .addMethods(methodSpecs)
                            .build();
                    JavaFile javaFile = JavaFile
                            .builder(targetPackage, typeSpec)
                            .addFileComment("This is auto-generated code. Do not modify this directly.")
                            .build();
                    try {
                        javaFile.writeTo(filer);
                    } catch (IOException e) {
                        messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    }
                });

        return true;
    }

    @TargetApi(24)
    private List<ExecutableElement> getConstructorElements(TypeElement originalClass) {
        return originalClass.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.CONSTRUCTOR)
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toList());
    }

    @TargetApi(24)
    private List<ExecutableElement> getOverriddenElements(TypeElement originalClass) {
        return originalClass.getEnclosedElements()
                .stream()
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .filter(element -> element.getAnnotation(Override.class) != null)
                .map(element -> (ExecutableElement) element)
                .collect(Collectors.toList());
    }

    @TargetApi(24)
    private List<ExecutableElement> getOverridingElements(TypeElement originalClass) {
        return originalClass.getInterfaces()
                .stream()
                .filter(typeMirror -> typeMirror.getKind() == TypeKind.DECLARED)
                .map(typeMirror -> typeUtils.asElement(typeMirror))
                .flatMap(element -> element.getEnclosedElements().stream())
                .filter(element -> element.getKind() == ElementKind.METHOD)
                .map(element -> (ExecutableElement) element)
                .filter(method -> !sameMethodExisting(getOverriddenElements(originalClass), method))
                .collect(Collectors.toList());
    }

    private Map<ExecutableElement, String> getMethodToFieldNameMap(RoundEnvironment roundEnvironment) {
        final Map<ExecutableElement, String> methodToFieldNameMap = new HashMap<>();

        for (Element byElement : roundEnvironment.getElementsAnnotatedWith(By.class)) {
            if (byElement.getKind() != ElementKind.FIELD) {
                continue;
            }
            VariableElement byField = (VariableElement) byElement;
            for (Element element : typeUtils.asElement(byField.asType()).getEnclosedElements()) {
                if (element.getKind() != ElementKind.METHOD) {
                    continue;
                }
                ExecutableElement method = (ExecutableElement) element;
                methodToFieldNameMap.put(method, byField.getSimpleName().toString());
            }
        }

        return methodToFieldNameMap;
    }

    @TargetApi(24)
    private List<MethodSpec> getConstructorSpecs(List<ExecutableElement> constructorElements, TypeElement originalClass) {
        return constructorElements.stream()
                .map(constructor -> {
                    List<String> parameterNames = constructor.getParameters()
                            .stream()
                            .map(parameter -> parameter.getSimpleName().toString())
                            .collect(Collectors.toList());

                    List<ParameterSpec> parameters = constructor.getParameters()
                            .stream()
                            .map(ParameterSpec::get)
                            .collect(Collectors.toList());

                    MethodSpec.Builder constructorBuilder = MethodSpec
                            .constructorBuilder()
                            .addModifiers(Modifier.PRIVATE)
                            .addParameters(parameters)
                            .addStatement("super($L)", String.join(",", parameterNames));

                    MethodSpec.Builder initializerBuilder = MethodSpec
                            .methodBuilder("newInstance")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(TypeName.get(originalClass.asType()))
                            .addParameters(parameters)
                            .addStatement("return new $L($L)", getSubclassName(originalClass), String.join(",", parameterNames));

                    return Arrays.asList(constructorBuilder.build(), initializerBuilder.build());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @TargetApi(24)
    private List<MethodSpec> getMethodSpecs(List<ExecutableElement> overridingMethods, Map<ExecutableElement, String> methodToFieldMap) {
        return overridingMethods
                .stream()
                .map(method -> {
                    List<String> parameterNames = method.getParameters()
                            .stream()
                            .map(parameter -> parameter.getSimpleName().toString())
                            .collect(Collectors.toList());

                    StringBuilder statementBuilder = (method.getReturnType().getKind() != TypeKind.VOID) ? new StringBuilder("return ") : new StringBuilder();
                    statementBuilder.append(String.format("$L.$L(%s)", String.join(",", parameterNames)));

                    String fieldName = getFieldName(methodToFieldMap, method);
                    return MethodSpec
                            .overriding(method)
                            .addStatement(statementBuilder.toString(), fieldName, method.getSimpleName())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private String getFieldName(Map<ExecutableElement, String> methodToFieldMap, ExecutableElement method) {
        for (Map.Entry<ExecutableElement, String> entry : methodToFieldMap.entrySet()) {
            ExecutableElement methodInField = entry.getKey();
            if (hasTheSameSignatures(method, methodInField)) {
                return entry.getValue();
            }
        }
        RuntimeException error = new RuntimeException("no corresponding field containing method: " + method.getSimpleName());
        messager.printMessage(Diagnostic.Kind.ERROR, error.getMessage());
        throw error;
    }

    private String getSubclassName(TypeElement originalClass) {
        return originalClass.getSimpleName() + "Impl";
    }

    private boolean hasTheSameSignatures(ExecutableElement left, ExecutableElement right) {
        boolean hasTheSameName = left.getSimpleName().equals(right.getSimpleName());
        boolean isSubsignature = typeUtils.isSubsignature((ExecutableType) left.asType(), (ExecutableType) right.asType());
        return hasTheSameName && isSubsignature;
    }

    @TargetApi(24)
    private boolean sameMethodExisting(List<ExecutableElement> methods, ExecutableElement method) {
        return methods
                .stream()
                .reduce(false,
                        (hasTheSameSignature, executableElement) -> hasTheSameSignature || hasTheSameSignatures(executableElement, method),
                        (before, after) -> before || after);
    }

    @TargetApi(24)
    private Modifier[] getModifiers(TypeElement originalClass) {
        List<Modifier> modifiers = originalClass.getModifiers()
                .stream()
                .filter(modifier -> modifier != Modifier.ABSTRACT)
                .collect(Collectors.toList());

        modifiers.add(0, Modifier.FINAL);
        return modifiers.stream().toArray(Modifier[]::new);
    }

}
