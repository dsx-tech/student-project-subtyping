package annvisitor.util;

import ann.type.Top;
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
            default:
                return op.toString();
        }
    }

    public static PermissionPolicy isOperationAllow(Tree.Kind op, String type, ProcessingEnvironment processingEnv) {
        LinkedList<String> path = new LinkedList<>();
        PermissionPolicy result = PermissionPolicy.ALLOW;

        if (findPath(type, Top.class.getName(), path, processingEnv)) {

            String fieldName = treeKindToFieldName(op);
            Trees tree = Trees.instance(processingEnv);

            for (String type1 : path) {
                if (!type1.contentEquals(Top.class.getName())) {
                    TypeElement clazz = processingEnv.getElementUtils().getTypeElement(type1);
                    List<VariableElement> fields = ElementFilter
                            .fieldsIn(clazz.getEnclosedElements())
                            .stream()
                            .filter(ve -> ve.getSimpleName().contentEquals(fieldName))
                            .collect(Collectors.toList());

                    VariableTree field = fields.isEmpty() ? null : (VariableTree) tree.getTree(fields.get(0));

                    if (field != null) {
                        ExpressionTree init = field.getInitializer();
                        if (init != null && init.getKind() == Tree.Kind.MEMBER_SELECT) {
                            String value = init.toString();
                            switch (value) {
                                case "annvisitor.util.PermissionPolicy.ALLOW":
                                case "PermissionPolicy.ALLOW":
                                    result = PermissionPolicy.ALLOW;
                                    break;
                                case "annvisitor.util.PermissionPolicy.ALLOW_WITH_WARNING":
                                case "PermissionPolicy.ALLOW_WITH_WARNING":
                                    result = PermissionPolicy.ALLOW_WITH_WARNING;
                                    break;
                                case "annvisitor.util.PermissionPolicy.FORBID":
                                case "PermissionPolicy.FORBID":
                                    result = PermissionPolicy.FORBID;
                                    break;
                            }
                        } else {
                            CompilationUnitTree cut = tree.getPath(fields.get(0)).getCompilationUnit();
                            printResultInfo(field, ResultKind.WRONG_PERMISSION_VALUE, tree, cut);
                        }

                        break;
                    }
                }
            }
        }

        return result;
    }
}
