package annvisitor.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;

import javax.tools.Diagnostic;

public class Messager {
    private Messager() {

    }

    public static void printResultInfo(Tree node, String name1, String name2, ResultKind res,
                                       Trees tree, CompilationUnitTree cut) {
        StringBuilder errorMessage = new StringBuilder();
        switch (res) {
            case ARGUMENT_MISMATCH:
                errorMessage
                        .append("argument mismatch: ")
                        .append(name1)
                        .append(" cannot be converted to ")
                        .append(name2);
                tree.printMessage(Diagnostic.Kind.ERROR,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case TYPE_MISMATCH_OPERAND:
                errorMessage
                        .append("incompatible types: ")
                        .append(name1)
                        .append(" cannot be converted to ")
                        .append(name2);
                tree.printMessage(Diagnostic.Kind.ERROR,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case WRONG_APPLY_OPERATOR:
                errorMessage
                        .append("operator ")
                        .append(name1)
                        .append(" cannot be applied to ")
                        .append(name2);
                tree.printMessage(Diagnostic.Kind.ERROR,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case WRONG_PERMISSION_VALUE:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "incorrect value of permission variable",
                        node,
                        cut);
            case INCORRECT_RETURN_TYPE:
                errorMessage
                        .append("incompatible types: cannot return ")
                        .append(name1)
                        .append(" value from a method with ")
                        .append(name2)
                        .append(" result type");
                tree.printMessage(Diagnostic.Kind.ERROR,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case ANNOTATION_ON_VOID:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "cannot return a value from a method with a void return type",
                        node,
                        cut);
                break;
            case NON_ANNOTATED_PARAM_WARNING:
                errorMessage
                        .append("annotated argument ")
                        .append(name1)
                        .append(" used as non annotated");
                tree.printMessage(Diagnostic.Kind.WARNING,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case MISSING_VALUE:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "missing annotation value",
                        node,
                        cut);
                break;
            case APPLY_OPERATOR_WITH_WARNING:
                errorMessage
                        .append("apply operator ")
                        .append(name1)
                        .append(" to ")
                        .append(name2);
                tree.printMessage(Diagnostic.Kind.WARNING,
                        errorMessage.toString(),
                        node,
                        cut);
            case OK:
            default:
        }
    }
}
