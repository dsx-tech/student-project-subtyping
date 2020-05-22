package scanner.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import scanner.type.RawType;
import scanner.type.UnknownType;

import javax.tools.Diagnostic;

public class ErrorsWithTypesNamesMessager implements Messager {
    private final Trees tree;
    private CompilationUnitTree cut;

    public ErrorsWithTypesNamesMessager(Trees tree) {
        this.tree = tree;
    }

    private String convertTypeNameForRawAndUnknownTypes(String name) {
        if (name.contentEquals("'" + RawType.class.getName() + "'")) {
            return "'raw type'";
        }
        if (name.contentEquals("'" + UnknownType.class.getName() + "'")) {
            return "'unknown type'";
        }
        return name;
    }

    @Override
    public void init(CompilationUnitTree cut) {
        this.cut = cut;
    }

    @Override
    public void printErrorAndWarning(Tree node, String name1, String name2, ErrorAndWarningKind res) {
        StringBuilder errorMessage = new StringBuilder();
        name1 = convertTypeNameForRawAndUnknownTypes(name1);
        name2 = convertTypeNameForRawAndUnknownTypes(name2);

        switch (res) {
            case ARGUMENT_MISMATCH:
                errorMessage
                        .append("argument mismatch: ")
                        .append(name1)
                        .append(" cannot be converted to ")
                        .append(name2);
                this.tree.printMessage(Diagnostic.Kind.ERROR,
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
                this.tree.printMessage(Diagnostic.Kind.ERROR,
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
                this.tree.printMessage(Diagnostic.Kind.ERROR,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case UNKNOWN_TYPE_ASSIGNMENT:
                errorMessage
                        .append(name1)
                        .append(" expression is assigned to ")
                        .append(name2)
                        .append(" variable");
                this.tree.printMessage(Diagnostic.Kind.WARNING,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case UNKNOWN_TYPE_PARAM:
                errorMessage
                        .append(name1)
                        .append(" argument is used as ")
                        .append(name2);
                this.tree.printMessage(Diagnostic.Kind.WARNING,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case MISSING_ANNOTATION_ON_FIELD:
                errorMessage
                        .append("missing ")
                        .append(name1)
                        .append(" annotation on field");
                this.tree.printMessage(Diagnostic.Kind.WARNING,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case INCORRECT_RETURN_TYPE:
                errorMessage
                        .append("incompatible types: cannot return ")
                        .append(name1)
                        .append(" value from a method with ")
                        .append(name2)
                        .append(" result type");
                this.tree.printMessage(Diagnostic.Kind.ERROR,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            case ANNOTATION_ON_VOID:
                this.tree.printMessage(Diagnostic.Kind.ERROR,
                        "cannot return a value from a method with a void return type",
                        node,
                        cut);
                break;
            case UNSAFE_TYPE_CAST:
                errorMessage
                        .append("unsafe type cast: ")
                        .append(name1)
                        .append(" to ")
                        .append(name2);
                this.tree.printMessage(Diagnostic.Kind.WARNING,
                        errorMessage.toString(),
                        node,
                        cut);
                break;
            default:
        }
    }
}
