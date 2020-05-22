package scanner.util;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

public interface Messager {
    void init(CompilationUnitTree cut);

    void printErrorAndWarning(Tree node, String name1, String name2, ErrorAndWarningKind res);
}
