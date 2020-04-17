package annvisitor.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;

import javax.tools.Diagnostic;

public class Messager {
    private Messager() {

    }

    public static void printResultInfo(Tree node, ResultKind res, Trees tree, CompilationUnitTree cut) {
        switch (res) {
            case NULLITY:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Reference to actual or formal parameters list is null",
                        node,
                        cut);
                break;
            case DIFFERENT_SIZE:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Count of actual parameters does not correspond to formal's",
                        node,
                        cut);
                break;
            case TYPE_MISMATCH_PARAMS:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Types on actual parameter does not corresponds to formal's",
                        node,
                        cut);
                break;
            case TYPE_MISMATCH_OPERAND:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Incompatible operands types",
                        node,
                        cut);
                break;
            case WRONG_APPLY_OPERATOR:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Operator cannot be apply to the operand",
                        node,
                        cut);
                break;
            case WRONG_PERMISSION_VALUE:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Incorrect value of permission variable",
                        node,
                        cut);
            case INCORRECT_RETURN_TYPE:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Incorrect return type",
                        node,
                        cut);
                break;
            case INCORRECT_VARIABLE_TYPE:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Incorrect variable type",
                        node,
                        cut);
                break;
            case ANNOTATION_ON_VOID:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Annotation on method which returns a void value",
                        node,
                        cut);
                break;
            case NON_ANNOTATED_PARAM_WARNING:
                tree.printMessage(Diagnostic.Kind.WARNING,
                        "Annotated actual parameter use as non annotated",
                        node,
                        cut);
                break;
            case MISSING_VALUE:
                tree.printMessage(Diagnostic.Kind.ERROR,
                        "Missing annotation value",
                        node,
                        cut);
                break;
            case APPLY_OPERATOR_WITH_WARNING:
                tree.printMessage(Diagnostic.Kind.WARNING,
                        "Apply operator with warning",
                        node,
                        cut);
            case OK:
            default:
        }
    }
}
