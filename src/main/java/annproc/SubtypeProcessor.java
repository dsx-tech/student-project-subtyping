package annproc;

import ann.Subtype;
import annvisitor.SubtypeCheckVisitor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.LinkedHashSet;
import java.util.Set;

public class SubtypeProcessor extends AbstractProcessor {
    private Messager messager;
    private SubtypeCheckVisitor subtypeCheckVisitor;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (annotations.isEmpty()) {
            return false;
        }

        Set<? extends Element> annotatedDeclarations = roundEnvironment.getElementsAnnotatedWith(Subtype.class);

        /*
        //search for annotated fields and methods
        for (Element element : annotatedDeclarations) {
            Element enclosingElement = element.getEnclosingElement();
        }

         */

        subtypeCheckVisitor = new SubtypeCheckVisitor(processingEnv);
        // obtain the root classes of a current round environment
        Set<? extends Element> classes = ElementFilter.typesIn(roundEnvironment.getRootElements());
        for (Element clazz : classes) {
            clazz.getEnclosedElements()
                    .stream()
                    .filter(e -> e.getKind() == ElementKind.CONSTRUCTOR || e.getKind() == ElementKind.METHOD)
                    .forEach(e -> e.accept(subtypeCheckVisitor, null));
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Subtype.class.getCanonicalName());
        return annotations;
    }
}
