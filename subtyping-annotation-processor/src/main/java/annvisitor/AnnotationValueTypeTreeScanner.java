package annvisitor;

import ann.Type;
import ann.type.Top;
import annvisitor.util.PermissionPolicy;
import annvisitor.util.ResultKind;

import static annvisitor.util.AnnotationValueSubtypes.*;
import static annvisitor.util.Messager.printResultInfo;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
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
import javax.lang.model.util.ElementFilter;
import java.util.*;
import java.util.stream.Collectors;

// first String for inferred type, second  String for type checks of return expression
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
                    if (type1 != null && !type1.equals(Top.class.getName()) && type2.equals(Top.class.getName())) {
                        return ResultKind.NON_ANNOTATED_PARAM;
                    }
                    if (!isSubtype(type1, type2, processingEnv)) {
                        return ResultKind.TYPE_MISMATCH_PARAMS;
                    }
                }
            }
        }

        return ResultKind.OK;
    }

    private String treeKindToFieldName(Tree.Kind op) {
        switch (op) {
            case POSTFIX_DECREMENT:
            case PREFIX_DECREMENT:
                return "DECREMENT";
            case POSTFIX_INCREMENT:
            case PREFIX_INCREMENT:
                return "INCREMENT";
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case GREATER_THAN_EQUAL:
            case LESS_THAN_EQUAL:
            case LESS_THAN:
            case GREATER_THAN:
                return "EQUALS";
            default:
                return op.toString();
        }
    }

    private PermissionPolicy isOperationAllow(Tree.Kind op, String type) {
        LinkedList<String> path = new LinkedList<>();
        PermissionPolicy result = PermissionPolicy.ALLOW;

        if (findPath(type, Top.class.getName(), path, processingEnv)) {
            String fieldName = treeKindToFieldName(op);
            for (String type1 : path) {
                if (!type1.contentEquals(Top.class.getName())) {
                    TypeElement clazz = processingEnv.getElementUtils().getTypeElement(type1);
                    List<VariableElement> fields = ElementFilter
                            .fieldsIn(clazz.getEnclosedElements())
                            .stream()
                            .filter(ve -> ve.getSimpleName().contentEquals(fieldName))
                            .collect(Collectors.toList());
                    VariableTree field = fields.isEmpty() ? null : (VariableTree) mTrees.getTree(fields.get(0));
                    if (field != null) {
                        ExpressionTree init = field.getInitializer();
                        if (init != null && init.getKind() == Tree.Kind.MEMBER_SELECT) {
                            // todo: implement correctly
                            String value = ((MemberSelectTree) init).getIdentifier().toString();
                            switch (value) {
                                case "ALLOW":
                                    result = PermissionPolicy.ALLOW;
                                    break;
                                case "ALLOW_WITH_WARNING":
                                    result = PermissionPolicy.ALLOW_WITH_WARNING;
                                    break;
                                case "FORBID":
                                    result = PermissionPolicy.FORBID;
                                    break;
                            }
                        } else {
                            printResultInfo(field, ResultKind.MISSING_PERMISSION_VALUE, mTrees, cut);
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public String visitMethod(MethodTree node, String aVoid) {
        // node is a constructor
        if (node.getReturnType() == null) {
            scan(node.getBody(), aVoid);
            return Top.class.getName();
        }

        ExecutableElement method = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        Type ann = method.getAnnotation(Type.class);
        TypeMirror value = null;

        if (ann != null) {
            if (method.getReturnType().getKind() == TypeKind.VOID) {
                printResultInfo(node, ResultKind.ANNOTATION_ON_VOID, mTrees, cut);
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
                    if (!isSubtype(type, value.toString(), processingEnv)) {
                        printResultInfo(node, ResultKind.INCORRECT_RETURN_TYPE, mTrees, cut);
                    }
                }
                return value.toString();
            } else {
                printResultInfo(node, ResultKind.MISSING_VALUE, mTrees, cut);
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
        Type ann = var.getAnnotation(Type.class);
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
                } else if (!isSubtype(type, value.toString(), processingEnv)) {
                    printResultInfo(node, ResultKind.INCORRECT_VARIABLE_TYPE, mTrees, cut);
                }
                localVarsTypes.put(var, value.toString());
                type = value.toString();
            } else {
                printResultInfo(node, ResultKind.MISSING_VALUE, mTrees, cut);
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
        return generalizeTypes(l, r, processingEnv);
    }

    @Override
    public String visitReturn(ReturnTree node, String annotatedRetType) {
        String type = super.visitReturn(node, annotatedRetType);
        if (type != null && annotatedRetType != null) {
            if (!isSubtype(type, annotatedRetType, processingEnv)) {
                printResultInfo(node, ResultKind.INCORRECT_RETURN_TYPE, mTrees, cut);
            }
        }
        return type;
    }

    @Override
    public String visitMethodInvocation(MethodInvocationTree node, String aVoid) {
        ExecutableElement method = (ExecutableElement) mTrees.getElement(mTrees.getPath(cut, node));
        List<? extends ExpressionTree> actualParams = node.getArguments();
        printResultInfo(node, checkParamsMatching(actualParams, method, aVoid), mTrees, cut);

        String type = null;
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            type = Top.class.getName();

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
        List<? extends ExpressionTree> actualParams = node.getArguments();
        printResultInfo(node, checkParamsMatching(actualParams, constructor, aVoid), mTrees, cut);
        return Top.class.getName();
    }

    @Override
    public String visitAssignment(AssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);
        if (expr.equals(Top.class.getName())) {
            var = Top.class.getName();
        } else if (!isSubtype(expr, var, processingEnv)) {
            printResultInfo(node, ResultKind.INCORRECT_VARIABLE_TYPE, mTrees, cut);
        }
        return var;
    }

    @Override
    public String visitCompoundAssignment(CompoundAssignmentTree node, String aVoid) {
        String var = scan(node.getVariable(), aVoid);
        String expr = scan(node.getExpression(), aVoid);
        Tree.Kind op = node.getKind();
        if (!expr.equals(Top.class.getName()) && !isSubtype(expr, var, processingEnv)) {
            printResultInfo(node, ResultKind.INCORRECT_VARIABLE_TYPE, mTrees, cut);
        }
        PermissionPolicy resOnVar = isOperationAllow(op, var);
        PermissionPolicy resOnExpr = isOperationAllow(op, expr);
        if (resOnVar != PermissionPolicy.ALLOW) {
            printResultInfo(node, ResultKind.WRONG_APPLY_OPERATOR , mTrees, cut);
        }
        if (resOnExpr != PermissionPolicy.ALLOW) {
            printResultInfo(node, ResultKind.WRONG_APPLY_OPERATOR, mTrees, cut);
        }
        return var;
    }

    @Override
    public String visitUnary(UnaryTree node, String aVoid) {
        String type = this.scan(node.getExpression(), aVoid);
        Tree.Kind op = node.getKind();
        PermissionPolicy resOnExpr = isOperationAllow(op, type);
        if (resOnExpr != PermissionPolicy.ALLOW) {
            printResultInfo(node, ResultKind.WRONG_APPLY_OPERATOR, mTrees, cut);
        }
        return type;
    }

    @Override
    public String visitBinary(BinaryTree node, String aVoid) {
        String l = this.scan(node.getLeftOperand(), aVoid);
        String r = this.scan(node.getRightOperand(), aVoid);
        Tree.Kind op = node.getKind();
        if (!isSubtype(l, r, processingEnv) && !isSubtype(r, l, processingEnv)) {
            printResultInfo(node, ResultKind.TYPE_MISMATCH_OPERAND, mTrees, cut);
        }
        PermissionPolicy resOnL = isOperationAllow(op, l);
        PermissionPolicy resOnR = isOperationAllow(op, r);
        if (resOnL != PermissionPolicy.ALLOW) {
            printResultInfo(node, ResultKind.WRONG_APPLY_OPERATOR, mTrees, cut);
        }
        if (resOnR != PermissionPolicy.ALLOW) {
            printResultInfo(node, ResultKind.WRONG_APPLY_OPERATOR, mTrees, cut);
        }
        return generalizeTypes(l, r, processingEnv);
    }

    @Override
    public String visitMemberSelect(MemberSelectTree node, String aVoid) {
        String type = Top.class.getName();

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
            } else {
                printResultInfo(node, ResultKind.MISSING_VALUE, mTrees, cut);
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
