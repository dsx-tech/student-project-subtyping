package scanner.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class AnnotationValueSubtypesTest {
    List<String> path1 = new LinkedList<>();
    List<String> path2 = new LinkedList<>();
    List<String> path3 = new LinkedList<>();
    List<String> path4 = new LinkedList<>();

    @Before
    public void setUpPaths() {
        path1.add("Top");
        path1.add("Numbers");
        path1.add("Id");
        path1.add("CarId");
        path1.add("LightWeightCarId");

        path2.add("Top");
        path2.add("Numbers");
        path2.add("Id");
        path2.add("BicycleId");

        path3.add("Top");
        path3.add("Numbers");
        path3.add("Id");
        path3.add("CarId");
        path3.add("HeavyWeightCarId");

        path4.add("Top");
        path4.add("Numbers");
        path4.add("RegionCode");
    }

    @After
    public void tearDownPaths() {
        path1.clear();
        path2.clear();
        path3.clear();
        path4.clear();
    }

    @Test
    public void findMostCommonType() {
        assertEquals(SubtypingChecker.findMostCommonType(path1, path2), "Id");
        assertEquals(SubtypingChecker.findMostCommonType(path1, path3), "CarId");
        assertEquals(SubtypingChecker.findMostCommonType(path1, path4), "Numbers");
        assertEquals(SubtypingChecker.findMostCommonType(path2, path1), "Id");
        assertEquals(SubtypingChecker.findMostCommonType(path2, path3), "Id");
        assertEquals(SubtypingChecker.findMostCommonType(path2, path4), "Numbers");
        assertEquals(SubtypingChecker.findMostCommonType(path3, path1), "CarId");
        assertEquals(SubtypingChecker.findMostCommonType(path3, path2), "Id");
        assertEquals(SubtypingChecker.findMostCommonType(path3, path4), "Numbers");
        assertEquals(SubtypingChecker.findMostCommonType(path4, path1), "Numbers");
        assertEquals(SubtypingChecker.findMostCommonType(path4, path2), "Numbers");
        assertEquals(SubtypingChecker.findMostCommonType(path4, path3), "Numbers");
    }
}