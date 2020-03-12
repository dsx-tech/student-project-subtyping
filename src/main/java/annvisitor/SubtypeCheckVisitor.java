package annvisitor;

import ann.Subtype;
import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementScanner7;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.ListIterator;

public class SubtypeCheckVisitor extends ElementScanner7<Void, Void> {
    private final Trees mTrees;

    public SubtypeCheckVisitor(ProcessingEnvironment processingEnvironment) {
        super();
        this.mTrees = Trees.instance(processingEnvironment);
    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        CompilationUnitTree cut = mTrees.getPath(e).getCompilationUnit();

        new TreeScanner<Void, Void>() {

            @Override
            public Void visitReturn(ReturnTree node, Void aVoid) {
                if (e.getAnnotation(Subtype.class) != null) {
                    // TODO: implement the case which checks conformity return value with annotation on method
                }
                return super.visitReturn(node, aVoid);
            }

            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
                // list of expr for args
                List<? extends ExpressionTree> actualParams = node.getArguments();
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                // list of elements for args
                List<? extends VariableElement> formalParams = invokedMethod.getParameters();

                if (!actualParams.isEmpty()) {
                    ListIterator<? extends VariableElement> iterator1 = formalParams.listIterator();
                    ListIterator<? extends ExpressionTree> iterator2 = actualParams.listIterator();

                    while (iterator1.hasNext() && iterator2.hasNext()) {
                        VariableElement var1 = iterator1.next();
                        Element var2 = mTrees.getElement(mTrees.getPath(cut, iterator2.next()));
                        Subtype ann1 = var1.getAnnotation(Subtype.class);
                        Subtype ann2 = var2.getAnnotation(Subtype.class);

                        //
                        if (ann1 == null ^ ann2 == null) {
                            mTrees.printMessage(Diagnostic.Kind.ERROR,
                                    "No necessary annotation on formal or actual parameter",
                                    node,
                                    cut);
                        } else if (ann1 != null) {
                            if (!ann1.value().equalsIgnoreCase(ann2.value())) {
                                mTrees.printMessage(Diagnostic.Kind.ERROR,
                                        "Annotation on actual parameter doesn't corresponds to formal's",
                                        node,
                                        cut);
                            }

                        }

                    }

                }

                return super.visitMethodInvocation(node, aVoid);
            }

            @Override
            public Void visitNewClass(NewClassTree node, Void aVoid) {
                // list of expr for args
                List<? extends ExpressionTree> actualParams = node.getArguments();
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                // list of elements for args
                List<? extends VariableElement> formalParams = invokedMethod.getParameters();

                if (!actualParams.isEmpty()) {
                    ListIterator<? extends VariableElement> iterator1 = formalParams.listIterator();
                    ListIterator<? extends ExpressionTree> iterator2 = actualParams.listIterator();

                    while (iterator1.hasNext() && iterator2.hasNext()) {
                        VariableElement var1 = iterator1.next();
                        Element var2 = mTrees.getElement(mTrees.getPath(cut, iterator2.next()));
                        Subtype ann1 = var1.getAnnotation(Subtype.class);
                        Subtype ann2 = var2.getAnnotation(Subtype.class);

                        //
                        if (ann1 == null ^ ann2 == null) {
                            mTrees.printMessage(Diagnostic.Kind.ERROR,
                                    "No necessary annotation on formal or actual parameter",
                                    node,
                                    cut);
                        } else if (ann1 != null) {
                            if (!ann1.value().equalsIgnoreCase(ann2.value())) {
                                mTrees.printMessage(Diagnostic.Kind.ERROR,
                                        "Annotation on actual parameter doesn't corresponds to formal's",
                                        node,
                                        cut);
                            }

                        }

                    }

                }

                return super.visitNewClass(node, aVoid);
            }

            @Override
            public Void visitVariable(VariableTree node, Void aVoid) {
                // TODO: implement acts which check the initialization expression of corresponding local variable
                return super.visitVariable(node, aVoid);
            }

        }.scan(mTrees.getTree(e), null);

        return super.visitExecutable(e, aVoid);
    }
}
