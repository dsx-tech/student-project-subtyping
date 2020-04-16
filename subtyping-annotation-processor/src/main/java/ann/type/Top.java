package ann.type;

import annvisitor.util.PermissionPolicy;

public class Top {
    public static final PermissionPolicy PLUS = PermissionPolicy.ALLOW;
    public static final PermissionPolicy MINUS = PermissionPolicy.ALLOW;
    public static final PermissionPolicy MULTIPLY = PermissionPolicy.ALLOW;
    public static final PermissionPolicy DIVIDE = PermissionPolicy.ALLOW;
    public static final PermissionPolicy REMAINDER = PermissionPolicy.ALLOW;

    public static final PermissionPolicy AND = PermissionPolicy.ALLOW;
    public static final PermissionPolicy OR = PermissionPolicy.ALLOW;
    public static final PermissionPolicy XOR = PermissionPolicy.ALLOW;

    public static final PermissionPolicy CONDITIONAL_AND = PermissionPolicy.ALLOW;
    public static final PermissionPolicy CONDITIONAL_OR = PermissionPolicy.ALLOW;

    public static final PermissionPolicy LEFT_SHIFT = PermissionPolicy.ALLOW;
    public static final PermissionPolicy RIGHT_SHIFT = PermissionPolicy.ALLOW;
    public static final PermissionPolicy UNSIGNED_RIGHT_SHIFT = PermissionPolicy.ALLOW;

    public static final PermissionPolicy EQUALS = PermissionPolicy.ALLOW;

    public static final PermissionPolicy UNARY_PLUS = PermissionPolicy.ALLOW;
    public static final PermissionPolicy UNARY_MINUS = PermissionPolicy.ALLOW;

    public static final PermissionPolicy INCREMENT = PermissionPolicy.ALLOW;
    public static final PermissionPolicy DECREMENT = PermissionPolicy.ALLOW;

    public static final PermissionPolicy LOGICAL_COMPLEMENT = PermissionPolicy.ALLOW;
    public static final PermissionPolicy BITWISE_COMPLEMENT = PermissionPolicy.ALLOW;
}
