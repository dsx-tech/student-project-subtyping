package annvisitor;

import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.ElementScanner7;

public class SubtypeCheckVisitor extends ElementScanner7<Void, Void> {
    private final Trees mTrees;
    private final ProcessingEnvironment processingEnv;

    public SubtypeCheckVisitor(ProcessingEnvironment processingEnvironment) {
        this.mTrees = Trees.instance(processingEnvironment);
        this.processingEnv = processingEnvironment;
    }

    //using non annotated but subtyped local vars
    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        SubtypeTreeScanner subtypeTreeScan = new SubtypeTreeScanner(processingEnv,
                mTrees.getPath(e).getCompilationUnit());
        subtypeTreeScan.scan(mTrees.getTree(e), null);
        return super.visitExecutable(e, aVoid);
    }
}
