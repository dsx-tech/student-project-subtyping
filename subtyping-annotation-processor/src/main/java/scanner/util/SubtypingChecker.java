package scanner.util;

import java.util.LinkedList;
import java.util.List;

public interface SubtypingChecker {
    boolean findPath(String leaf, String root, LinkedList<String> path);

    String generalizeTypes(String type1, String type2);

    String findMostCommonType(List<String> path1, List<String> path2);

    boolean isSubtype(String subtype, String supertype);
}
