package annvisitor.util;

import ann.type.UnknownType;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static annvisitor.util.AnnotationValueSubtypes.findPath;
import static annvisitor.util.Messager.printResultInfo;

public class TypeOperatorPermissionChecker {
    private TypeOperatorPermissionChecker() {

    }

    public static String treeKindToFieldName(Tree.Kind op) {
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
            case PLUS_ASSIGNMENT:
                return Tree.Kind.PLUS.toString();
            case MINUS_ASSIGNMENT:
                return Tree.Kind.MINUS.toString();
            case MULTIPLY_ASSIGNMENT:
                return Tree.Kind.MULTIPLY.toString();
            case DIVIDE_ASSIGNMENT:
                return Tree.Kind.DIVIDE.toString();
            case REMAINDER_ASSIGNMENT:
                return Tree.Kind.REMAINDER.toString();
            case AND_ASSIGNMENT:
                return Tree.Kind.AND.toString();
            case OR_ASSIGNMENT:
                return Tree.Kind.OR.toString();
            case XOR_ASSIGNMENT:
                return Tree.Kind.XOR.toString();
            case LEFT_SHIFT_ASSIGNMENT:
                return Tree.Kind.LEFT_SHIFT.toString();
            case RIGHT_SHIFT_ASSIGNMENT:
                return Tree.Kind.RIGHT_SHIFT.toString();
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                return Tree.Kind.UNSIGNED_RIGHT_SHIFT.toString();
            default:
                return op.toString();
        }
    }

    public static PermissionStatus getPermissionStatus(Tree.Kind operator,
                                                       String type,
                                                       ProcessingEnvironment processingEnv) {
        LinkedList<String> path = new LinkedList<>();
        PermissionStatus status = PermissionStatus.FORBID;

        if (findPath(type, UnknownType.class.getName(), path, processingEnv)) {

            String fieldName = treeKindToFieldName(operator);
            Trees tree = Trees.instance(processingEnv);

            for (String type1 : path) {
                if (!type1.contentEquals(UnknownType.class.getName())) {
                    TypeElement clazz = processingEnv.getElementUtils().getTypeElement(type1);
                    List<VariableElement> fields = ElementFilter
                            .fieldsIn(clazz.getEnclosedElements())
                            .stream()
                            .filter(ve -> ve.getSimpleName().contentEquals(fieldName))
                            .collect(Collectors.toList());

                    VariableTree field = fields.isEmpty() ? null : (VariableTree) tree.getTree(fields.get(0));

                    if (field != null) {
                        CompilationUnitTree cut = tree.getPath(fields.get(0)).getCompilationUnit();
                        ExpressionTree init = field.getInitializer();
                        if (init != null && init.getKind() == Tree.Kind.MEMBER_SELECT) {
                            String value = init.toString();
                            switch (value) {
                                case "annvisitor.util.PermissionPolicy.ALLOW":
                                case "PermissionPolicy.ALLOW":
                                    status = PermissionStatus.ALLOW;
                                    break;
                                case "annvisitor.util.PermissionPolicy.ALLOW_WITH_WARNING":
                                case "PermissionPolicy.ALLOW_WITH_WARNING":
                                    status = PermissionStatus.ALLOW_WITH_WARNING;
                                    break;
                                default:
                                    printResultInfo(field, "", "",
                                            ResultKind.WRONG_PERMISSION_VALUE,
                                            tree, cut);
                            }
                        } else {
                            printResultInfo(field, "", "",
                                    ResultKind.WRONG_PERMISSION_VALUE,
                                    tree, cut);
                        }

                        break;
                    }
                }
            }
        }

        return status;
    }
}
