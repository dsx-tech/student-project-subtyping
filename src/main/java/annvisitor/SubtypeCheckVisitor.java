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
import java.util.*;

public class SubtypeCheckVisitor extends ElementScanner7<Void, Void> {
    private final Trees mTrees;

    public SubtypeCheckVisitor(ProcessingEnvironment processingEnvironment) {
        super();
        this.mTrees = Trees.instance(processingEnvironment);
    }

    //using non annotated but subtyped local vars
    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        CompilationUnitTree cut = mTrees.getPath(e).getCompilationUnit();
        Map<Element, String> localVarsTypes = new HashMap<>();

        new TreeScanner<String, Void>() {
            private void checkExecutable(ExpressionTree node,
                                         List<? extends ExpressionTree> acParams,
                                         List<? extends VariableElement> fParams) {

                if (!acParams.isEmpty()) {
                    ListIterator<? extends VariableElement> iterator1 = fParams.listIterator();
                    // TODO: handle case when expression tree is a literal or non variable expression
                    ListIterator<? extends ExpressionTree> iterator2 = acParams.listIterator();

                    while (iterator1.hasNext() && iterator2.hasNext()) {
                        VariableElement var1 = iterator1.next();
                        Element var2 = mTrees.getElement(mTrees.getPath(cut, iterator2.next()));
                        Subtype ann1 = var1.getAnnotation(Subtype.class);
                        Subtype ann2 = var2.getAnnotation(Subtype.class);

                        //TODO: check a subtype
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
            }

            private String reduceTypes(String r, String l) {
                return r;
            }

            private boolean checkCompatibility(String r, String l) {
                return r.equals(l);
            }

            @Override
            public String visitReturn(ReturnTree node, Void aVoid) {
                String res = super.visitReturn(node, aVoid);
                Subtype ann = e.getAnnotation(Subtype.class);

                if (ann != null) {
                    if (res == null || !ann.value().equals(res)) {
                        mTrees.printMessage(Diagnostic.Kind.ERROR, "Incorrect return type",
                                node,
                                cut);
                    }
                    res = ann.value();
                }

                return res;
            }

            @Override
            public String visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
                // list of expr for args
                List<? extends ExpressionTree> actualParams = node.getArguments();
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                // list of elements for args
                List<? extends VariableElement> formalParams = invokedMethod.getParameters();
                checkExecutable(node, actualParams, formalParams);

                String res = super.visitMethodInvocation(node, aVoid);
                Subtype ann = invokedMethod.getAnnotation(Subtype.class);

                return ann != null ? ann.value() : "unit";
            }

            @Override
            public String visitNewClass(NewClassTree node, Void aVoid) {
                // list of expr for args
                List<? extends ExpressionTree> actualParams = node.getArguments();
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                // list of elements for args
                List<? extends VariableElement> formalParams = invokedMethod.getParameters();
                checkExecutable(node, actualParams, formalParams);
                String res = super.visitNewClass(node, aVoid);
                return "unit";
            }

            @Override
            public String visitVariable(VariableTree node, Void aVoid) {
                // inferred type
                String res = super.visitVariable(node, aVoid);
                // variable declaration
                Element var = mTrees.getElement(mTrees.getPath(cut, node));
                Subtype ann = var.getAnnotation(Subtype.class);

                if (ann != null) {
                    localVarsTypes.put(var, ann.value());
                    res = ann.value();
                } else if (res != null) {
                    localVarsTypes.put(var, res);
                }
                mTrees.printMessage(Diagnostic.Kind.NOTE, res != null ? res : "i dont know", node, cut);
                return res;
            }

            @Override
            public String visitLiteral(LiteralTree node, Void aVoid) {
                return "unit";
            }

            // TODO: check subtype
            @Override
            public String visitBinary(BinaryTree node, Void aVoid) {
                String l = this.scan((Tree)node.getLeftOperand(), aVoid);
                String r = this.scan((Tree)node.getRightOperand(), aVoid);

                if (!checkCompatibility(r, l)) {
                    mTrees.printMessage(Diagnostic.Kind.ERROR, "Incompatible types",
                            node,
                            cut);
                }

                return reduceTypes(l, r);
            }

            @Override
            public String visitIdentifier(IdentifierTree node, Void aVoid) {
                String res = "unit";

                Element var = mTrees.getElement(mTrees.getPath(cut, node));
                Subtype ann = var.getAnnotation(Subtype.class);
                if (ann != null) {
                    res = ann.value();
                } else if (localVarsTypes.containsKey(var)) {
                    res = localVarsTypes.get(var);
                }

                return res;
            }

        }.scan(mTrees.getTree(e), null);

        return super.visitExecutable(e, aVoid);
    }
}
