package scanner;

import annotation.Type;
import annotation.UnsafeCast;
import scanner.type.*;
import scanner.util.ErrorAndWarningKind;

import static scanner.util.SubtypingChecker.*;
import static scanner.util.Messager.printErrorAndWarning;
import static scanner.util.OperationPermissionChecker.isOperationAllow;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import scanner.util.Messager;

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
                printErrorAndWarning(node, "", "", ErrorAndWarningKind.ANNOTATION_ON_VOID, mTrees, cut);
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
                        printErrorAndWarning(node,
                                "'" + type + "'",
                                "'" + value.toString() + "'",
                                ErrorAndWarningKind.INCORRECT_RETURN_TYPE,
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
        String declType = RawType.class.getName();

        if (ann != null) {
            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                if (mte.getTypeMirror() != null) {
                    declType = mte.getTypeMirror().toString();
                    localVarsTypes.put(var, declType);
                }
            }
        } else {
            localVarsTypes.put(var, actualType.contentEquals(UnknownType.class.getName()) ?
                    RawType.class.getName() : actualType);
        }

        if (!declType.contentEquals(RawType.class.getName()) && !declType.contentEquals(UnknownType.class.getName())) {
            if (!actualType.contentEquals(RawType.class.getName()) && !isSubtype(actualType, declType, processingEnv)) {
                UnsafeCast unsafeCastAnn = var.getAnnotation(UnsafeCast.class);
                if (unsafeCastAnn != null) {
                    if (unsafeCastAnn.printWarning()) {
                        printErrorAndWarning(node,
                                "'" + actualType + "'",
                                "'" + declType + "'",
                                ErrorAndWarningKind.UNSAFE_TYPE_CAST,
                                mTrees, cut);
                    }
                } else {
                    if (actualType.contentEquals(UnknownType.class.getName())) {
                        printErrorAndWarning(node,
                                "'" + actualType + "'",
                                "'" + declType + "'",
                                ErrorAndWarningKind.UNKNOWN_TYPE_ASSIGNMENT,
                                mTrees, cut);
                    } else {
                        printErrorAndWarning(node,
                                "'" + actualType + "'",
                                "'" + declType + "'",
                                ErrorAndWarningKind.TYPE_MISMATCH_OPERAND,
                                mTrees, cut);
                    }
                }
            }
        }

        return localVarsTypes.get(var);
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
                printErrorAndWarning(node,
                        "'" + type + "'",
                        "'" + annotatedRetType + "'",
                        ErrorAndWarningKind.INCORRECT_RETURN_TYPE,
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

                String actualType = scan(var1, p);
                String declType = RawType.class.getName();
                Type ann2 = var2.getAnnotation(Type.class);

                if (ann2 != null) {
                    try {
                        ann2.value();
                    } catch (MirroredTypeException mte) {
                        if (mte.getTypeMirror() != null) {
                            declType = mte.getTypeMirror().toString();
                        }
                    }
                }

                if (!declType.contentEquals(RawType.class.getName()) &&
                        !declType.contentEquals(UnknownType.class.getName())) {
                    if (!actualType.contentEquals(RawType.class.getName()) &&
                            !isSubtype(actualType, declType, processingEnv)) {
                        if (actualType.contentEquals(UnknownType.class.getName())) {
                            printErrorAndWarning(var1,
                                    "'" + actualType + "'",
                                    "'" + declType + "'",
                                    ErrorAndWarningKind.UNKNOWN_TYPE_PARAM,
                                    mTrees, cut);
                        } else {
                            printErrorAndWarning(var1,
                                    "'" + actualType + "'",
                                    "'" + declType + "'",
                                    ErrorAndWarningKind.ARGUMENT_MISMATCH,
                                    mTrees, cut);
                        }
                    }
                }
            }
        }
    }

    @Override
    public String visitMethodInvocation(MethodInvocationTree node, String aVoid) {
        ExecutableElement method = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        checkParamsMatching(node.getArguments(), method.getParameters(), aVoid);

        String type = RawType.class.getName();
        Type ann = method.getAnnotation(Type.class);

        if (ann != null) {
            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                if (mte.getTypeMirror() != null) {
                    type = mte.getTypeMirror().toString();
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

    private void assignmentExpressionCheck(ExpressionTree node, String variable, String expression) {
        if (!variable.contentEquals(RawType.class.getName()) &&
                !variable.contentEquals(UnknownType.class.getName()) &&
                !expression.contentEquals(RawType.class.getName()) &&
                !isSubtype(expression, variable, processingEnv)) {
            if (expression.contentEquals(UnknownType.class.getName())) {
                printErrorAndWarning(node,
                        "'" + expression + "'",
                        "'" + variable + "'",
                        ErrorAndWarningKind.UNKNOWN_TYPE_ASSIGNMENT,
                        mTrees, cut);
            } else {
                printErrorAndWarning(node,
                        "'" + expression + "'",
                        "'" + variable + "'",
                        ErrorAndWarningKind.TYPE_MISMATCH_OPERAND,
                        mTrees, cut);
            }
        }
    }

    @Override
    public String visitAssignment(AssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);
        assignmentExpressionCheck(node, var, expr);
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
        boolean isAllow = isOperationAllow(node.getKind(), type, processingEnv);
        if (!isAllow) {
            printErrorAndWarning(node, operatorKindToSymbol(node.getKind()),
                    "'" + type + "'",
                    ErrorAndWarningKind.WRONG_APPLY_OPERATOR,
                    mTrees, cut);
        }
    }

    @Override
    public String visitCompoundAssignment(CompoundAssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);

        assignmentExpressionCheck(node, var, expr);

        if (isSubtype(expr, var, processingEnv)) {
            operatorApplyCheck(node, var);
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
            printErrorAndWarning(node,
                    "'" + l + "'",
                    "'" + r + "'",
                    ErrorAndWarningKind.TYPE_MISMATCH_OPERAND,
                    mTrees, cut);
        }

        return UnknownType.class.getName();
    }

    @Override
    public String visitMemberSelect(MemberSelectTree node, String aVoid) {
        Element field = mTrees.getElement(mTrees.getPath(cut, node));
        Type ann = field.getAnnotation(Type.class);
        String type = RawType.class.getName();

        if (ann != null) {
            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                if (mte.getTypeMirror() != null) {
                    type = mte.getTypeMirror().toString();
                }
            }
        }

        return type;
    }

    @Override
    public String visitIdentifier(IdentifierTree node, String aVoid) {
        Element var = mTrees.getElement(mTrees.getPath(cut, node));
        Type ann = var.getAnnotation(Type.class);
        String type = RawType.class.getName();

        if (ann != null) {
            try {
                ann.value();
            } catch (MirroredTypeException mte) {
                if (mte.getTypeMirror() != null) {
                    type = mte.getTypeMirror().toString();
                }
            }
        } else if (localVarsTypes.containsKey(var)) {
            type = localVarsTypes.get(var);
        }

        return type;
    }

    @Override
    public String visitLiteral(LiteralTree node, String aVoid) {
        return RawType.class.getName();
    }
}
