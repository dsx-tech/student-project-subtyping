package scanner.util;

import annotation.operation.arithmetic.*;
import annotation.operation.logical.*;
import annotation.operation.bitwise.*;
import annotation.operation.equal.Equal;
import com.sun.source.tree.Tree;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;


public class OperationPermissionChecker {
    private OperationPermissionChecker() {

    }

    public static Class<? extends Annotation> treeKindToAnnotation(Tree.Kind op) {
        switch (op) {
            case POSTFIX_DECREMENT:
            case PREFIX_DECREMENT:
                return Decrement.class;
            case POSTFIX_INCREMENT:
            case PREFIX_INCREMENT:
                return Increment.class;
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case GREATER_THAN_EQUAL:
            case LESS_THAN_EQUAL:
            case LESS_THAN:
            case GREATER_THAN:
                return Equal.class;
            case PLUS:
            case PLUS_ASSIGNMENT:
                return Plus.class;
            case MINUS:
            case MINUS_ASSIGNMENT:
                return Minus.class;
            case MULTIPLY:
            case MULTIPLY_ASSIGNMENT:
                return Multiply.class;
            case DIVIDE:
            case DIVIDE_ASSIGNMENT:
                return Divide.class;
            case REMAINDER:
            case REMAINDER_ASSIGNMENT:
                return Remainder.class;
            case AND:
            case AND_ASSIGNMENT:
                return And.class;
            case OR:
            case OR_ASSIGNMENT:
                return Or.class;
            case XOR:
            case XOR_ASSIGNMENT:
                return Xor.class;
            case CONDITIONAL_AND:
                return ConditionalAnd.class;
            case CONDITIONAL_OR:
                return ConditionalOr.class;
            case LOGICAL_COMPLEMENT:
                return LogicalComplement.class;
            case LEFT_SHIFT:
            case LEFT_SHIFT_ASSIGNMENT:
                return LeftShift.class;
            case RIGHT_SHIFT:
            case RIGHT_SHIFT_ASSIGNMENT:
                return RightShift.class;
            case UNSIGNED_RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
                return UnsignedRightShift.class;
            case BITWISE_COMPLEMENT:
                return BitwiseComplement.class;
            case UNARY_PLUS:
                return UnaryPlus.class;
            case UNARY_MINUS:
                return UnaryMinus.class;
            default:
                return null;
        }
    }

    public static boolean isOperationAllow(Tree.Kind operator,
                                           String type,
                                           ProcessingEnvironment processingEnv) {
        TypeElement clazz = processingEnv.getElementUtils().getTypeElement(type);
        Annotation ann = clazz.getAnnotation(treeKindToAnnotation(operator));
        return ann != null;
    }
}
