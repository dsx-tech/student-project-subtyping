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
            private ResultKind checkExecutable(ExpressionTree node,
                                            List<? extends ExpressionTree> acParams,
                                            List<? extends VariableElement> fParams,
                                            Void p) {
                if (acParams == null ^ fParams == null) {
                    return ResultKind.NULLITY;
                }

                if (acParams != null) {
                    if (acParams.size() != fParams.size()) {
                        return ResultKind.DIFFERENT_SIZE;
                    } else {    // lists of actual and formal parameters != null and have the same size
                        Iterator<? extends ExpressionTree> it1 = acParams.iterator();
                        Iterator<? extends VariableElement> it2 = fParams.iterator();

                        while (it1.hasNext() && it2.hasNext()) {
                            ExpressionTree var1 = it1.next();
                            VariableElement var2 = it2.next();

                            String type1 = scan(var1, p);
                            Subtype ann2 = var2.getAnnotation(Subtype.class);
                            String type2 = ann2 != null ? ann2.value() : "unit";

                            if (checkCompatibility(type1, type2)) {
                                return ResultKind.TYPE_MISMATCH;
                            }
                        }
                    }
                }

                return ResultKind.OK;
            }
            // TODO: check subtype
            private String reduceTypes(String r, String l) {
                return r;
            }

            // TODO: check subtype
            private boolean checkCompatibility(String r, String l) {
                return !r.equals(l);
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

                if (res == null) {
                   res = "unit";
                }

                return res;
            }

            @Override
            public String visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
                List<? extends ExpressionTree> actualParams = node.getArguments(); // list of expr for args
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node)); // element of a current invoked method
                List<? extends VariableElement> formalParams = invokedMethod.getParameters(); // list of elements for args

                switch (checkExecutable(node, actualParams, formalParams, aVoid)) {
                    case NULLITY:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Reference to actual or formal parameters list is null",
                                node,
                                cut);
                        break;
                    case DIFFERENT_SIZE:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Count of actual parameters does not correspond to formal's",
                                node,
                                cut);
                        break;
                    case TYPE_MISMATCH:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Types on actual parameter does not corresponds to formal's",
                                node,
                                cut);
                        break;
                    case OK:
                    default:
                }

                //String res = super.visitMethodInvocation(node, aVoid);
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

                switch (checkExecutable(node, actualParams, formalParams, aVoid)) {
                    case NULLITY:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Reference to actual or formal parameters list is null",
                                node,
                                cut);
                        break;
                    case DIFFERENT_SIZE:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Count of actual parameters does not correspond to formal's",
                                node,
                                cut);
                        break;
                    case TYPE_MISMATCH:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Types on actual parameter does not corresponds to formal's",
                                node,
                                cut);
                        break;
                    case OK:
                    default:
                }
                //String res = super.visitNewClass(node, aVoid);
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
                } else {
                    res = "unit";
                }

                return res;
            }

            @Override
            public String visitLiteral(LiteralTree node, Void aVoid) {
                return "unit";
            }

            @Override
            public String visitBinary(BinaryTree node, Void aVoid) {
                String l = this.scan((Tree)node.getLeftOperand(), aVoid);
                String r = this.scan((Tree)node.getRightOperand(), aVoid);

                if (checkCompatibility(r, l)) {
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
                /* if (!res.equals("unit")) {
                    mTrees.printMessage(Diagnostic.Kind.NOTE, res,
                            node,
                            cut);
                }

                 */
                return res;
            }

        }.scan(mTrees.getTree(e), null);

        return super.visitExecutable(e, aVoid);
    }

    public enum ResultKind {
        NULLITY,
        DIFFERENT_SIZE,
        TYPE_MISMATCH,
        OK
    }
}
