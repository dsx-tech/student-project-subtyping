package annvisitor;

import ann.Subtype;
import ann.subtype.Top;
import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementScanner7;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SubtypeCheckVisitor extends ElementScanner7<Void, Void> {
    private final Trees mTrees;
    private final ProcessingEnvironment procEnv;

    public SubtypeCheckVisitor(ProcessingEnvironment processingEnvironment) {
        super();
        this.mTrees = Trees.instance(processingEnvironment);
        this.procEnv = processingEnvironment;
    }

    //using non annotated but subtyped local vars
    @Override
    public Void visitExecutable(ExecutableElement e, Void aVoid) {
        CompilationUnitTree cut = mTrees.getPath(e).getCompilationUnit();
        Map<Element, String> localVarsTypes = new HashMap<>();

        new TreeScanner<String, Void>() {
            private ResultKind checkParamsMatching(List<? extends ExpressionTree> actualParams,
                                                   ExecutableElement invokedMethod,
                                                   Void p) {
                List<? extends VariableElement> formalParams = invokedMethod.getParameters();

                if (actualParams == null ^ formalParams == null) {
                    return ResultKind.NULLITY;
                }

                if (actualParams != null) {
                    if (actualParams.size() != formalParams.size()) {
                        return ResultKind.DIFFERENT_SIZE;
                    } else {    // lists of actual and formal parameters != null and have the same size
                        Iterator<? extends ExpressionTree> it1 = actualParams.iterator();
                        Iterator<? extends VariableElement> it2 = formalParams.iterator();

                        while (it1.hasNext() && it2.hasNext()) {
                            ExpressionTree var1 = it1.next();
                            VariableElement var2 = it2.next();

                            String type1 = scan(var1, p);
                            String type2 = Top.class.getName();
                            Subtype ann2 = var2.getAnnotation(Subtype.class);
                            TypeMirror value = null;
                            if (ann2 != null) {
                                try {
                                    ann2.value();
                                } catch (MirroredTypeException mte) {
                                    value = mte.getTypeMirror();
                                }
                                if (value != null) {
                                    type2 = value.toString();
                                }
                            }
                            // TODO: add checks subtype
                            if (!type1.equals(type2)) {
                                return ResultKind.TYPE_MISMATCH_PARAMS;
                            }
                        }
                    }
                }

                return ResultKind.OK;
            }

            private void printResultInfo(Tree node, ResultKind res) {
                switch (res) {
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
                    case TYPE_MISMATCH_PARAMS:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Types on actual parameter does not corresponds to formal's",
                                node,
                                cut);
                        break;
                    case TYPE_MISMATCH_OPERAND:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Incompatible operands types",
                                node,
                                cut);
                        break;
                    case INCORRECT_RETURN_TYPE:
                        mTrees.printMessage(Diagnostic.Kind.ERROR,
                                "Incorrect return type",
                                node,
                                cut);
                    case OK:
                    default:
                }
            }

            @Override
            public String visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                List<? extends ExpressionTree> actualParams = node.getArguments();
                printResultInfo(node, checkParamsMatching(actualParams, invokedMethod, aVoid));

                Subtype ann = invokedMethod.getAnnotation(Subtype.class);
                String type = Top.class.getName();
                TypeMirror value = null;
                if (ann != null) {
                    try {
                        ann.value();
                    } catch (MirroredTypeException mte) {
                        value = mte.getTypeMirror();
                    }
                    if (value != null) {
                        type = value.toString();
                    }
                }
                return type;
            }

            @Override
            public String visitNewClass(NewClassTree node, Void aVoid) {
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                List<? extends ExpressionTree> actualParams = node.getArguments();

                printResultInfo(node, checkParamsMatching(actualParams, invokedMethod, aVoid));
                return Top.class.getName();
            }

            @Override
            public String visitReturn(ReturnTree node, Void aVoid) {
                String type = super.visitReturn(node, aVoid);
                Subtype ann = e.getAnnotation(Subtype.class);
                TypeMirror value = null;
                if (ann != null) {
                    try {
                        ann.value();
                    } catch (MirroredTypeException mte) {
                        value = mte.getTypeMirror();
                    }
                    if (value != null) {
                        if (type == null || !value.toString().equals(type)) {
                            printResultInfo(node, ResultKind.INCORRECT_RETURN_TYPE);
                        }
                        type = value.toString();
                    }
                }

                if (type == null) {
                    type = Top.class.getName();
                }

                return type;
            }

            @Override
            public String visitVariable(VariableTree node, Void aVoid) {
                String type = Top.class.getName();
                if (node.getInitializer() != null) {
                    // inferred type
                    type = scan(node.getInitializer(), aVoid);
                }
                // variable declaration
                Element var = mTrees.getElement(mTrees.getPath(cut, node));
                Subtype ann = var.getAnnotation(Subtype.class);
                TypeMirror value = null;
                if (ann != null) {
                    try {
                        ann.value();
                    } catch (MirroredTypeException mte) {
                        value = mte.getTypeMirror();
                    }
                    if (value != null) {
                        localVarsTypes.put(var, value.toString());
                        type = value.toString();
                    }
                } else {
                    localVarsTypes.put(var, type);
                }

                return type;
            }

            @Override
            public String visitLiteral(LiteralTree node, Void aVoid) {
                String res = Top.class.getName();
                /*
                switch (node.getKind()) {
                    case INT_LITERAL:
                        res = Int.class;
                        break;
                    case CHAR_LITERAL:
                        res = Char.class;
                        break;
                    case LONG_LITERAL:
                        res = Long.class;
                        break;
                    case FLOAT_LITERAL:
                        res = Float.class;
                        break;
                    case DOUBLE_LITERAL:
                        res = Double.class;
                        break;
                    case BOOLEAN_LITERAL:
                        res = Bool.class;
                        break;
                    case STRING_LITERAL:
                        res = Str.class;
                        break;
                    default:
                }

                 */
                return res;
            }

            @Override
            public String visitBinary(BinaryTree node, Void aVoid) {
                String l = this.scan(node.getLeftOperand(), aVoid);
                String r = this.scan(node.getRightOperand(), aVoid);
                /*
                if (node.getKind() == Tree.Kind.PLUS) {
                    if (isSubtype(l, Str.class) || isSubtype(r, Str.class)) {
                        Class<?> commonAnc = generalizeTypes(l, r);
                        if (commonAnc.getName().equals(Top.class.getName())) {
                            return Str.class;
                        } else {
                            return commonAnc;
                        }
                    }
                }

                 */

                // TODO: check that l and r are subtype of int/double/...
                if (!l.equals(r)) {
                    printResultInfo(node, ResultKind.TYPE_MISMATCH_OPERAND);
                }
/*
                if (
                        node.getKind() == Tree.Kind.ASSIGNMENT ||
                        node.getKind() == Tree.Kind.DIVIDE_ASSIGNMENT ||
                        node.getKind() == Tree.Kind.REMAINDER_ASSIGNMENT ||
                        node.getKind() == Tree.Kind.MULTIPLY_ASSIGNMENT ||
                        node.getKind() == Tree.Kind.PLUS_ASSIGNMENT ||
                        node.getKind() == Tree.Kind.MINUS_ASSIGNMENT
                ) {
                    mTrees.printMessage(Diagnostic.Kind.NOTE, node.getLeftOperand().getKind().name(), node, cut);
                }

 */
                // TODO: add correct return statement
                return l;
            }

            @Override
            public String visitIdentifier(IdentifierTree node, Void aVoid) {
                String res = Top.class.getName();

                Element var = mTrees.getElement(mTrees.getPath(cut, node));
                Subtype ann = var.getAnnotation(Subtype.class);
                TypeMirror value = null;
                if (ann != null) {
                    try {
                        ann.value();
                    } catch (MirroredTypeException mte) {
                        value = mte.getTypeMirror();
                    }
                    if (value != null) {
                        res = value.toString();
                    }
                } else if (localVarsTypes.containsKey(var)) {
                    res = localVarsTypes.get(var);
                }
                return res;
            }

        }.scan(mTrees.getTree(e), null);

        return super.visitExecutable(e, aVoid);
    }

    public enum ResultKind {
        NULLITY,
        DIFFERENT_SIZE,
        TYPE_MISMATCH_PARAMS,
        TYPE_MISMATCH_OPERAND,
        INCORRECT_RETURN_TYPE,
        OK
    }
}
