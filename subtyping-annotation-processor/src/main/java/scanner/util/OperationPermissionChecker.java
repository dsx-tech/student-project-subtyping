package scanner.util;

import com.sun.source.tree.Tree;

public interface OperationPermissionChecker {
    boolean isOperationAllow(Tree.Kind operator, String type);
}
