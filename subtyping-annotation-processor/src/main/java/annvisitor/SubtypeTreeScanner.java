package annvisitor;

import ann.Subtype;
import ann.subtype.Top;
import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.*;
// first String for inferred type, second  String for type checks of return expression
public class SubtypeTreeScanner extends TreeScanner<String, String> {
    private final ProcessingEnvironment processingEnv;
    private final Trees mTrees;
    private CompilationUnitTree cut;

    Map<Element, String> localVarsTypes = new HashMap<>();

    public SubtypeTreeScanner(ProcessingEnvironment processingEnv, CompilationUnitTree cut) {
        this.processingEnv = processingEnv;
        this.mTrees = Trees.instance(processingEnv);
        this.cut = cut;
    }

    // TODO: unit tests
    private boolean findPath(String leaf, String root,
                             LinkedList<String> path) {
        if (leaf == null || root == null) {
            return false;
        }

        TypeElement curNode;
        while (!leaf.equals(root)) {
            if (leaf.equals(Top.class.getName())) {
                return false;
            }

            path.addFirst(leaf);

            curNode = processingEnv.getElementUtils().getTypeElement(leaf);
            List<? extends TypeMirror> allSupInterfaces = curNode.getInterfaces();

            if (allSupInterfaces.isEmpty()) {
                leaf = Top.class.getName();
            } else {
                // multiple inheritance is prohibited
                leaf = allSupInterfaces.get(0).toString();
            }
        }
        path.addFirst(leaf);
        return true;
    }

    // TODO: unit tests
    private String generalizeTypes(String r, String l) {
        if (r.equals(l)) {
            return r;
        }

        LinkedList<String> anc1 = new LinkedList<>();
        LinkedList<String> anc2 = new LinkedList<>();
        String commonAnc = Top.class.getName();

        if (findPath(r, commonAnc, anc1) && findPath(l, commonAnc, anc2)) {
            Iterator<String> it1 = anc1.listIterator();
            Iterator<String> it2 = anc2.listIterator();
            String lastCommon;

            while (it1.hasNext() && it2.hasNext()) {
                lastCommon = it1.next();
                if (lastCommon.equals(it2.next())) {
                    commonAnc = lastCommon;
                } else {
                    break;
                }
            }
        }

        return commonAnc;
    }

    private boolean isSubtype(String subt, String supt) {
        if (subt != null && supt != null &&
                supt.equals(Top.class.getName())) {
            return true;
        }
        LinkedList<String> buf = new LinkedList<>();
        return findPath(subt, supt, buf);
    }


    private ResultKind checkParamsMatching(List<? extends ExpressionTree> actParams,
                                           ExecutableElement exec,
                                           String p) {
        List<? extends VariableElement> formParams = exec.getParameters();

        if (actParams == null ^ formParams == null) {
            return ResultKind.NULLITY;
        }

        if (actParams != null) {
            if (actParams.size() != formParams.size()) {
                return ResultKind.DIFFERENT_SIZE;
            } else {    // lists of actual and formal parameters != null and have the same size
                Iterator<? extends ExpressionTree> it1 = actParams.iterator();
                Iterator<? extends VariableElement> it2 = formParams.iterator();

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
                    if (type1 != null && !type1.equals(Top.class.getName()) && type2.equals(Top.class.getName())) {
                        return ResultKind.NON_ANNOTATED_PARAM;
                    }
                    if (!isSubtype(type1, type2)) {
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
            case WRONG_APPLY_OPERATOR:
                mTrees.printMessage(Diagnostic.Kind.ERROR,
                        "Operator cannot be apply to the operand",
                        node,
                        cut);
                break;
            case INCORRECT_RETURN_TYPE:
                mTrees.printMessage(Diagnostic.Kind.ERROR,
                        "Incorrect return type",
                        node,
                        cut);
                break;
            case INCORRECT_VARIABLE_TYPE:
                mTrees.printMessage(Diagnostic.Kind.ERROR,
                        "Incorrect variable type",
                        node,
                        cut);
                break;
            case ANNOTATION_ON_VOID:
                mTrees.printMessage(Diagnostic.Kind.ERROR,
                        "Annotation on method which returns a void value",
                        node,
                        cut);
                break;
            case NON_ANNOTATED_PARAM:
                mTrees.printMessage(Diagnostic.Kind.WARNING,
                        "Annotated actual parameter use as non annotated",
                        node,
                        cut);
                break;
            case MISSING_VALUE:
                mTrees.printMessage(Diagnostic.Kind.ERROR,
                        "Missing annotation value",
                        node,
                        cut);
                break;
            case OK:
            default:
        }
    }

    @Override
    public String visitMethod(MethodTree node, String aVoid) {
        // node is a constructor
        if (node.getReturnType() == null) {
            scan(node.getBody(), aVoid);
            return Top.class.getName();
        }

        ExecutableElement method = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        Subtype ann = method.getAnnotation(Subtype.class);
        TypeMirror value = null;

        if (ann != null) {
            if (method.getReturnType().getKind() == TypeKind.VOID) {
                printResultInfo(node, ResultKind.ANNOTATION_ON_VOID);
                scan(node.getBody(), aVoid);
                return null;
            }
            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                value = mte.getTypeMirror();
            }
            if (value != null) {
                if (node.getBody() != null) {
                    // all return expressions of body block must be subtype of value
                    scan(node.getBody(), value.toString());
                } else if (node.getDefaultValue() != null) {
                        String type = scan(node.getDefaultValue(), aVoid);
                        if (!isSubtype(type, value.toString())) {
                            printResultInfo(node, ResultKind.INCORRECT_RETURN_TYPE);
                        }
                }
                return value.toString();
            } else {
                printResultInfo(node, ResultKind.MISSING_VALUE);
            }
        }

        scan(node.getBody(), aVoid);
        scan(node.getDefaultValue(), aVoid);

        return method.getReturnType().getKind() == TypeKind.VOID ? null : Top.class.getName();
    }

    @Override
    public String visitVariable(VariableTree node, String aVoid) {
        String type = Top.class.getName();
        if (node.getInitializer() != null) {
            type = scan(node.getInitializer(), aVoid);
        }

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
                if (type.equals(Top.class.getName())) {
                    localVarsTypes.put(var, value.toString());
                } else if (!isSubtype(type, value.toString())) {
                    printResultInfo(node, ResultKind.INCORRECT_VARIABLE_TYPE);
                }
                localVarsTypes.put(var, value.toString());
                type = value.toString();
            } else {
                printResultInfo(node, ResultKind.MISSING_VALUE);
            }
        } else {
            localVarsTypes.put(var, type);
        }

        return type;
    }

    @Override
    public String visitConditionalExpression(ConditionalExpressionTree node, String aVoid) {
        scan(node.getCondition(), aVoid);
        String l = scan(node.getTrueExpression(), aVoid);
        String r = scan(node.getFalseExpression(), aVoid);
        return generalizeTypes(l, r);
    }

    @Override
    public String visitReturn(ReturnTree node, String annotatedRetType) {
        String type = super.visitReturn(node, annotatedRetType);
        if (type != null && annotatedRetType != null) {
            if (!isSubtype(type, annotatedRetType)) {
                printResultInfo(node, ResultKind.INCORRECT_RETURN_TYPE);
            }
        }
        return type;
    }

    @Override
    public String visitMethodInvocation(MethodInvocationTree node, String aVoid) {
        ExecutableElement method = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        List<? extends ExpressionTree> actualParams = node.getArguments();
        printResultInfo(node, checkParamsMatching(actualParams, method, aVoid));

        String type = null;
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            type = Top.class.getName();

            Subtype ann = method.getAnnotation(Subtype.class);
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
        }
        return type;
    }

    @Override
    public String visitNewClass(NewClassTree node, String aVoid) {
        ExecutableElement constructor = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        List<? extends ExpressionTree> actualParams = node.getArguments();
        printResultInfo(node, checkParamsMatching(actualParams, constructor, aVoid));
        return Top.class.getName();
    }

    @Override
    public String visitAssignment(AssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);
        if (expr.equals(Top.class.getName())) {
            var = Top.class.getName();
        } else if (!isSubtype(expr, var)) {
            printResultInfo(node, ResultKind.INCORRECT_VARIABLE_TYPE);
        }
        return var;
    }

    @Override
    public String visitCompoundAssignment(CompoundAssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);
        Tree.Kind op = node.getKind();
        switch (op) {
            case PLUS_ASSIGNMENT:
            case MINUS_ASSIGNMENT:
                if (!expr.equals(Top.class.getName()) && !isSubtype(expr, var)) {
                    printResultInfo(node, ResultKind.INCORRECT_VARIABLE_TYPE);
                }
                break;
            default:
                printResultInfo(node, ResultKind.WRONG_APPLY_OPERATOR);
        }
        return var;
    }

    @Override
    public String visitBinary(BinaryTree node, String aVoid) {
        String l = this.scan(node.getLeftOperand(), aVoid);
        String r = this.scan(node.getRightOperand(), aVoid);
        Tree.Kind op = node.getKind();
        switch (op) {
            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case EQUAL_TO:
            case NOT_EQUAL_TO:
                if (!l.equals(r)) {
                    printResultInfo(node, ResultKind.TYPE_MISMATCH_OPERAND);
                }
                break;
            case PLUS:
            case MINUS:
                if (!isSubtype(l, r) && !isSubtype(r, l)) {
                    printResultInfo(node, ResultKind.TYPE_MISMATCH_OPERAND);
                }
                break;
            default:
                printResultInfo(node, ResultKind.WRONG_APPLY_OPERATOR);

        }
        return generalizeTypes(l, r);
    }

    @Override
    public String visitMemberSelect(MemberSelectTree node, String aVoid) {
        String type = Top.class.getName();

        Element field = mTrees.getElement(mTrees.getPath(cut, node));
        Subtype ann = field.getAnnotation(Subtype.class);
        TypeMirror value = null;

        if (ann != null) {
            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                value = mte.getTypeMirror();
            }
            if (value != null) {
                type = value.toString();
            } else {
                printResultInfo(node, ResultKind.MISSING_VALUE);
            }
        }

        return type;
    }



    @Override
    public String visitLiteral(LiteralTree node, String aVoid) {
        return Top.class.getName();
    }

    @Override
    public String visitIdentifier(IdentifierTree node, String aVoid) {
        String type = Top.class.getName();

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
                type = value.toString();
            }
        } else if (localVarsTypes.containsKey(var)) {
            type = localVarsTypes.get(var);
        }

        return type;
    }

    public enum ResultKind {
        NULLITY,
        DIFFERENT_SIZE,
        TYPE_MISMATCH_PARAMS,
        TYPE_MISMATCH_OPERAND,
        WRONG_APPLY_OPERATOR,
        INCORRECT_RETURN_TYPE,
        INCORRECT_VARIABLE_TYPE,
        ANNOTATION_ON_VOID,
        NON_ANNOTATED_PARAM,
        MISSING_VALUE,
        OK
    }
}
