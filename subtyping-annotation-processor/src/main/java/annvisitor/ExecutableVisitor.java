package annvisitor;

import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementScanner7;

public class ExecutableVisitor extends ElementScanner7<Void, Void> {
    private final ProcessingEnvironment processingEnvironment;
    private final Trees mTrees;

    public ExecutableVisitor(ProcessingEnvironment processingEnvironment) {
        this.processingEnvironment = processingEnvironment;
        this.mTrees = Trees.instance(processingEnvironment);
    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        AnnotationValueTypeTreeScanner
                .getInstance(processingEnvironment, mTrees.getPath(e).getCompilationUnit())
                .scan(mTrees.getTree(e), null);
        return super.visitExecutable(e, aVoid);
    }
}
