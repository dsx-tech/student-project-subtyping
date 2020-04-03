package annvisitor;

import ann.Subtype;
import ann.subtype.*;
import ann.subtype.Double;
import ann.subtype.Float;
import ann.subtype.Long;
import ann.subtype.Str;
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
import java.util.List;

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
        Map<Element, Class<?>> localVarsTypes = new HashMap<>();

        new TreeScanner<Class<?>, Void>() {
            // TODO: unit tests
            private boolean findPath(Class<?> c, Class<?> root,
                                     LinkedList<Class<?>> path) {
                if (c == null || root == null) {
                    return false;
                }

                while (!c.getName().equals(root.getName())) {
                    if (c.getName().equals(Top.class.getName())) {
                        return false;
                    }
                    path.addFirst(c);
                    Class<?>[] allSupInterfaces = c.getInterfaces();
                    if (allSupInterfaces.length == 0) {
                        c = Top.class;
                    } else {
                        c = allSupInterfaces[0]; // multiple inheritance is prohibited
                    }
                }
                path.addFirst(c);

                return true;
            }

            // TODO: unit tests
            private Class<?> generalizeTypes(Class<?> r, Class<?> l) {
                if (r.getName().equals(l.getName())) {
                    return r;
                }

                LinkedList<Class<?>> anc1 = new LinkedList<>();
                LinkedList<Class<?>> anc2 = new LinkedList<>();
                Class<?> commonAnc = Top.class;

                if (findPath(r, Top.class, anc1) && findPath(l, Top.class, anc2)) {
                    Iterator<Class<?>> it1 = anc1.listIterator();
                    Iterator<Class<?>> it2 = anc2.listIterator();
                    Class<?> lastCommon;

                    while (it1.hasNext() && it2.hasNext()) {
                        lastCommon = it1.next();
                        if (lastCommon.getName().equals(it2.next().getName())) {
                            commonAnc = lastCommon;
                        } else {
                            break;
                        }
                    }
                }

                return commonAnc;
            }

            private boolean isSubtype(Class<?> subt, Class<?> supt) {
                if (supt.getName().equals(Top.class.getName())) {
                    return true;
                }
                LinkedList<Class<?>> buf = new LinkedList<>();
                return findPath(subt, supt, buf);
            }

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

                            Class<?> type1 = scan(var1, p);
                            Subtype ann2 = var2.getAnnotation(Subtype.class);
                            Class<?> type2 = ann2 != null ? ann2.value() : Top.class;

                            if (!isSubtype(type1, type2)) {
                                return ResultKind.TYPE_MISMATCH_PARAMS;
                            }
                        }
                    }
                }

                return ResultKind.OK;
            }

            private void printResultInfo(ExpressionTree node, ResultKind res) {
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
                                "Incompatible operands' types",
                                node,
                                cut);
                    case OK:
                    default:
                }
            }

            @Override
            public Class<?> visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
                mTrees.printMessage(Diagnostic.Kind.NOTE, "enter in new class visitor", node, cut);
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                List<? extends ExpressionTree> actualParams = node.getArguments();
                mTrees.printMessage(Diagnostic.Kind.NOTE, "find params", node, cut);
                printResultInfo(node, checkParamsMatching(actualParams, invokedMethod, aVoid));
                Subtype ann = invokedMethod.getAnnotation(Subtype.class);
                // TODO:
                return ann != null ? ann.value() : Top.class;
            }

            @Override
            public Class<?> visitNewClass(NewClassTree node, Void aVoid) {
                // element of a current invoked method
                ExecutableElement invokedMethod = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
                List<? extends ExpressionTree> actualParams = node.getArguments();

                printResultInfo(node, checkParamsMatching(actualParams, invokedMethod, aVoid));
                // TODO:
                return Top.class;
            }

            @Override
            public Class<?> visitReturn(ReturnTree node, Void aVoid) {
                Class<?> res = super.visitReturn(node, aVoid);
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
                    // TODO: add correct return statement
                    res = Top.class;
                }

                return res;
            }

            @Override
            public Class<?> visitVariable(VariableTree node, Void aVoid) {
                // inferred type
                Class<?> res = super.visitVariable(node, aVoid);
                // variable declaration
                Element var = mTrees.getElement(mTrees.getPath(cut, node));
                Subtype ann = var.getAnnotation(Subtype.class);

                if (ann != null) {
                    localVarsTypes.put(var, ann.value());
                    res = ann.value();
                } else if (res != null) {
                    localVarsTypes.put(var, res);
                } else {
                    // TODO: add correct return statement
                    res = Top.class;
                }
                return res;
            }

            @Override
            public Class<?> visitLiteral(LiteralTree node, Void aVoid) {
                Class<?> res = Top.class;
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
                return res;
            }

            @Override
            public Class<?> visitBinary(BinaryTree node, Void aVoid) {
                Class<?> l = this.scan(node.getLeftOperand(), aVoid);
                Class<?> r = this.scan(node.getRightOperand(), aVoid);

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

                // TODO: check that l and r are subtype of int/double/...
                if (!isSubtype(r, l) && !isSubtype(l, r)) {
                    mTrees.printMessage(Diagnostic.Kind.ERROR, "Incompatible types",
                            node,
                            cut);
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

                return generalizeTypes(l, r);
            }

            @Override
            public Class<?> visitIdentifier(IdentifierTree node, Void aVoid) {
                // TODO: add correct return statement
                Class<?> res = Top.class;

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
        TYPE_MISMATCH_PARAMS,
        TYPE_MISMATCH_OPERAND,
        OK
    }
}
