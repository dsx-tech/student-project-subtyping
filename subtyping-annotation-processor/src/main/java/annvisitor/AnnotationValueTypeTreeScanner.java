package annvisitor;

import ann.Type;
import ann.UnsafeCast;
import ann.type.RawType;
import ann.type.UnknownType;
import annvisitor.util.PermissionStatus;
import annvisitor.util.ResultKind;

import static annvisitor.util.AnnotationValueSubtypes.*;
import static annvisitor.util.Messager.printResultInfo;
import static annvisitor.util.TypeOperatorPermissionChecker.getPermissionStatus;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.*;

// first String for inferred type, second String for type checks of return expression
public class AnnotationValueTypeTreeScanner extends TreeScanner<String, String> {

    private static class AnnTreeScanHolder {
        private static final AnnotationValueTypeTreeScanner HOLDER_INSTANCE = new AnnotationValueTypeTreeScanner();
    }

    private ProcessingEnvironment processingEnv;
    private Trees mTrees;
    private CompilationUnitTree cut;
    private Map<Element, String> localVarsTypes;

    private AnnotationValueTypeTreeScanner() {

    }

    public static AnnotationValueTypeTreeScanner getInstance(ProcessingEnvironment processingEnv,
                                                             CompilationUnitTree cut) {
        AnnTreeScanHolder.HOLDER_INSTANCE.processingEnv = processingEnv;
        AnnTreeScanHolder.HOLDER_INSTANCE.mTrees = Trees.instance(processingEnv);
        AnnTreeScanHolder.HOLDER_INSTANCE.cut = cut;
        AnnTreeScanHolder.HOLDER_INSTANCE.localVarsTypes = new HashMap<>();
        return AnnTreeScanHolder.HOLDER_INSTANCE;
    }

    @Override
    public String visitMethod(MethodTree node, String aVoid) {
        // node is a constructor
        if (node.getReturnType() == null) {
            scan(node.getBody(), aVoid);
            return RawType.class.getName();
        }

        ExecutableElement method = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        Type ann = method.getAnnotation(Type.class);
        TypeMirror value = null;

        if (ann != null) {
            if (method.getReturnType().getKind() == TypeKind.VOID) {
                printResultInfo(node, "", "", ResultKind.ANNOTATION_ON_VOID, mTrees, cut);
                scan(node.getBody(), aVoid);
                return null;
            }

            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                value = mte.getTypeMirror();
            }

            if (value != null) {
                // all return expressions of body block must be subtype of value
                scan(node.getBody(), value.toString());
                if (node.getDefaultValue() != null) {
                    String type = scan(node.getDefaultValue(), aVoid);
                    if (!isSubtype(type, value.toString(), processingEnv)) {
                        printResultInfo(node,
                                "'" + type + "'",
                                "'" + value.toString() + "'",
                                ResultKind.INCORRECT_RETURN_TYPE,
                                mTrees, cut);
                    }
                }
                return value.toString();
            }
        }

        scan(node.getBody(), aVoid);
        scan(node.getDefaultValue(), aVoid);

        return method.getReturnType().getKind() == TypeKind.VOID ? null : RawType.class.getName();
    }

    @Override
    public String visitVariable(VariableTree node, String aVoid) {
        String actualType = RawType.class.getName();
        if (node.getInitializer() != null) {
            actualType = scan(node.getInitializer(), aVoid);
        }

        Element var = mTrees.getElement(mTrees.getPath(cut, node));
        Type ann = var.getAnnotation(Type.class);
        TypeMirror value = null;

        if (ann != null) {
            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                value = mte.getTypeMirror();
            }
            if (value != null) {
                if (!actualType.contentEquals(RawType.class.getName()) &&
                        !isSubtype(actualType, value.toString(), processingEnv)) {

                    UnsafeCast unsafeCastAnn = var.getAnnotation(UnsafeCast.class);
                    if (unsafeCastAnn != null) {
                        if (unsafeCastAnn.printWarning()) {
                            printResultInfo(node,
                                    "'" + actualType + "'",
                                    "'" + value.toString() + "'",
                                    ResultKind.UNSAFE_TYPE_CAST,
                                    mTrees, cut);
                        }
                    } else {
                        printResultInfo(node,
                                "'" + actualType + "'",
                                "'" + value.toString() + "'",
                                ResultKind.TYPE_MISMATCH_OPERAND,
                                mTrees, cut);
                    }

                }

                actualType = value.toString();
            }
        }
        localVarsTypes.put(var, actualType);

        return actualType;
    }

    @Override
    public String visitConditionalExpression(ConditionalExpressionTree node, String aVoid) {
        scan(node.getCondition(), aVoid);
        String l = scan(node.getTrueExpression(), aVoid);
        String r = scan(node.getFalseExpression(), aVoid);
        return generalizeTypes(l, r, processingEnv);
    }

    @Override
    public String visitReturn(ReturnTree node, String annotatedRetType) {
        String type = super.visitReturn(node, annotatedRetType);
        if (type != null && annotatedRetType != null) {
            if (!isSubtype(type, annotatedRetType, processingEnv)) {
                printResultInfo(node,
                        "'" + type + "'",
                        "'" + annotatedRetType + "'",
                        ResultKind.INCORRECT_RETURN_TYPE,
                        mTrees, cut);
            }
        }
        return type;
    }

    private void checkParamsMatching(List<? extends ExpressionTree> actParams,
                                     List<? extends VariableElement> formParams,
                                     String p) {
        if (actParams != null && formParams != null && actParams.size() == formParams.size()) {
            Iterator<? extends ExpressionTree> it1 = actParams.iterator();
            Iterator<? extends VariableElement> it2 = formParams.iterator();

            while (it1.hasNext() && it2.hasNext()) {
                ExpressionTree var1 = it1.next();
                VariableElement var2 = it2.next();
                String type1 = scan(var1, p);
                String type2 = UnknownType.class.getName();

                Type ann2 = var2.getAnnotation(Type.class);
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

                if (!type1.contentEquals(RawType.class.getName()) && !isSubtype(type1, type2, processingEnv)) {
                    printResultInfo(var1,
                            "'" + type1 + "'",
                            "'" + type2 + "'",
                            ResultKind.ARGUMENT_MISMATCH,
                            mTrees, cut);

                }
            }
        }
    }

    @Override
    public String visitMethodInvocation(MethodInvocationTree node, String aVoid) {
        ExecutableElement method = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        checkParamsMatching(node.getArguments(), method.getParameters(), aVoid);

        String type = null;
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            type = RawType.class.getName();

            Type ann = method.getAnnotation(Type.class);
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
        checkParamsMatching(node.getArguments(), constructor.getParameters(), aVoid);
        return RawType.class.getName();
    }

    @Override
    public String visitAssignment(AssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);
        if (expr.equals(RawType.class.getName())) {
            var = RawType.class.getName();
        } else if (!isSubtype(expr, var, processingEnv)) {
            printResultInfo(node,
                    "'" + expr + "'",
                    "'" + var + "'",
                    ResultKind.TYPE_MISMATCH_OPERAND,
                    mTrees, cut);
        }
        return var;
    }

    private String operatorKindToSymbol(Tree.Kind operator) {
        String result = "";
        switch (operator) {
            case PLUS:
            case UNARY_PLUS:
            case PLUS_ASSIGNMENT:
                result = "'+'";
                break;
            case MINUS:
            case UNARY_MINUS:
            case MINUS_ASSIGNMENT:
                result = "'-'";
                break;
            case MULTIPLY:
            case MULTIPLY_ASSIGNMENT:
                result = "'*'";
                break;
            case DIVIDE:
            case DIVIDE_ASSIGNMENT:
                result = "'/'";
                break;
            case REMAINDER:
            case REMAINDER_ASSIGNMENT:
                result = "'%'";
                break;
            case POSTFIX_INCREMENT:
            case PREFIX_INCREMENT:
                result = "'++'";
                break;
            case POSTFIX_DECREMENT:
            case PREFIX_DECREMENT:
                result = "'--'";
                break;
            case BITWISE_COMPLEMENT:
                result = "'~'";
                break;
            case LOGICAL_COMPLEMENT:
                result = "'!'";
                break;
            case AND:
            case AND_ASSIGNMENT:
                result = "'&'";
                break;
            case OR:
            case OR_ASSIGNMENT:
                result = "'|'";
                break;
            case XOR:
            case XOR_ASSIGNMENT:
                result = "'^'";
                break;
            case CONDITIONAL_AND:
                result = "'&&'";
                break;
            case CONDITIONAL_OR:
                result = "'||'";
                break;
            case LEFT_SHIFT:
            case LEFT_SHIFT_ASSIGNMENT:
                result = "'<<'";
                break;
            case RIGHT_SHIFT:
            case RIGHT_SHIFT_ASSIGNMENT:
                result = "'>>'";
                break;
            case UNSIGNED_RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                result = "'>>>'";
                break;
            case GREATER_THAN:
                result = "'>'";
                break;
            case LESS_THAN:
                result = "'<'";
                break;
            case GREATER_THAN_EQUAL:
                result = "'>='";
                break;
            case LESS_THAN_EQUAL:
                result = "'<='";
                break;
            case EQUAL_TO:
                result = "'=='";
                break;
            case NOT_EQUAL_TO:
                result = "'!='";
                break;
            default:
        }
        return result;
    }

    private void operatorApplyCheck(ExpressionTree node, String type) {
        PermissionStatus status = getPermissionStatus(node.getKind(), type, processingEnv);
        switch (status) {
            case FORBID:
                printResultInfo(node,
                        operatorKindToSymbol(node.getKind()),
                        "'" + type + "'",
                        ResultKind.WRONG_APPLY_OPERATOR,
                        mTrees, cut);
                break;
            case ALLOW:
            default:
        }
    }

    @Override
    public String visitCompoundAssignment(CompoundAssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);
        if (isSubtype(expr, var, processingEnv)) {
            operatorApplyCheck(node, var);
            return var;
        }
        if (!var.contentEquals(RawType.class.getName()) && !expr.contentEquals(RawType.class.getName())) {
            printResultInfo(node,
                    "'" + expr + "'",
                    "'" + var + "'",
                    ResultKind.TYPE_MISMATCH_OPERAND,
                    mTrees, cut);
        }
        return var;
    }

    @Override
    public String visitUnary(UnaryTree node, String aVoid) {
        String type = this.scan(node.getExpression(), aVoid);
        operatorApplyCheck(node, type);
        return type;
    }

    @Override
    public String visitBinary(BinaryTree node, String aVoid) {
        String l = this.scan(node.getLeftOperand(), aVoid);
        String r = this.scan(node.getRightOperand(), aVoid);

        if (isSubtype(l, r, processingEnv)) {
            operatorApplyCheck(node, r);
            return r;
        }
        if (isSubtype(r, l, processingEnv)) {
            operatorApplyCheck(node, l);
            return l;
        }

        if (!l.contentEquals(RawType.class.getName()) && !r.contentEquals(RawType.class.getName())) {
            printResultInfo(node,
                    "'" + l + "'",
                    "'" + r + "'",
                    ResultKind.TYPE_MISMATCH_OPERAND,
                    mTrees, cut);
        }

        return UnknownType.class.getName();
    }

    @Override
    public String visitMemberSelect(MemberSelectTree node, String aVoid) {
        String type = RawType.class.getName();

        Element field = mTrees.getElement(mTrees.getPath(cut, node));
        Type ann = field.getAnnotation(Type.class);
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
    public String visitLiteral(LiteralTree node, String aVoid) {
        return RawType.class.getName();
    }

    @Override
    public String visitIdentifier(IdentifierTree node, String aVoid) {
        String type = RawType.class.getName();

        Element var = mTrees.getElement(mTrees.getPath(cut, node));
        Type ann = var.getAnnotation(Type.class);
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
}
