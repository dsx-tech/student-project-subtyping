package ann.type;

import annvisitor.util.PermissionPolicy;

public class Top {
    public static final PermissionPolicy PLUS = PermissionPolicy.FORBID;
    public static final PermissionPolicy MINUS = PermissionPolicy.FORBID;
    public static final PermissionPolicy MULTIPLY = PermissionPolicy.FORBID;
    public static final PermissionPolicy DIVIDE = PermissionPolicy.FORBID;
    public static final PermissionPolicy REMAINDER = PermissionPolicy.FORBID;

    public static final PermissionPolicy AND = PermissionPolicy.FORBID;
    public static final PermissionPolicy OR = PermissionPolicy.FORBID;
    public static final PermissionPolicy XOR = PermissionPolicy.FORBID;

    public static final PermissionPolicy CONDITIONAL_AND = PermissionPolicy.FORBID;
    public static final PermissionPolicy CONDITIONAL_OR = PermissionPolicy.FORBID;

    public static final PermissionPolicy LEFT_SHIFT = PermissionPolicy.FORBID;
    public static final PermissionPolicy RIGHT_SHIFT = PermissionPolicy.FORBID;
    public static final PermissionPolicy UNSIGNED_RIGHT_SHIFT = PermissionPolicy.FORBID;

    public static final PermissionPolicy EQUALS = PermissionPolicy.FORBID;

    public static final PermissionPolicy UNARY_PLUS = PermissionPolicy.FORBID;
    public static final PermissionPolicy UNARY_MINUS = PermissionPolicy.FORBID;

    public static final PermissionPolicy INCREMENT = PermissionPolicy.FORBID;
    public static final PermissionPolicy DECREMENT = PermissionPolicy.FORBID;

    public static final PermissionPolicy LOGICAL_COMPLEMENT = PermissionPolicy.FORBID;
    public static final PermissionPolicy BITWISE_COMPLEMENT = PermissionPolicy.FORBID;
}
