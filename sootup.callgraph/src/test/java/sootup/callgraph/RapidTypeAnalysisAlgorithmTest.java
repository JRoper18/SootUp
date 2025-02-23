package sootup.callgraph;

import static junit.framework.TestCase.*;

import categories.Java8Test;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import sootup.core.signatures.MethodSignature;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.java.core.views.JavaView;

/** @author Kadiray Karakaya, Jonas Klauke */
@Category(Java8Test.class)
public class RapidTypeAnalysisAlgorithmTest extends CallGraphTestBase<RapidTypeAnalysisAlgorithm> {

  @Override
  protected RapidTypeAnalysisAlgorithm createAlgorithm(JavaView view, TypeHierarchy typeHierarchy) {
    return new RapidTypeAnalysisAlgorithm(view, typeHierarchy);
  }

  @Test
  public void testMiscExample1() {
    /** We expect constructors for B and C We expect A.print(), B.print(), C.print() */
    CallGraph cg = loadCallGraph("Misc", "example1.Example");

    MethodSignature constructorB =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.B"),
            "<init>",
            "void",
            Collections.emptyList());

    MethodSignature constructorC =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.C"),
            "<init>",
            "void",
            Collections.emptyList());

    MethodSignature methodA =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.A"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    MethodSignature methodB =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.B"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    MethodSignature methodC =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.C"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    MethodSignature methodD =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("example1.D"),
            "print",
            "void",
            Collections.singletonList("java.lang.Object"));

    assertTrue(cg.containsCall(mainMethodSignature, constructorB));
    assertTrue(cg.containsCall(mainMethodSignature, constructorC));

    assertTrue(cg.containsCall(mainMethodSignature, methodA));
    assertTrue(cg.containsCall(mainMethodSignature, methodB));
    assertTrue(cg.containsCall(mainMethodSignature, methodC));
    assertFalse(cg.containsMethod(methodD));

    assertEquals(5, cg.callsFrom(mainMethodSignature).size());

    assertEquals(2, cg.callsTo(constructorB).size());
    assertEquals(1, cg.callsTo(constructorC).size());
    assertEquals(1, cg.callsTo(methodA).size());
    assertEquals(1, cg.callsTo(methodB).size());
    assertEquals(1, cg.callsTo(methodC).size());

    assertEquals(0, cg.callsFrom(methodA).size());
    assertEquals(0, cg.callsFrom(methodB).size());
    assertEquals(0, cg.callsFrom(methodC).size());
  }

  @Test
  public void testRevisitMethod() {
    /* We expect a call edge from RevisitedMethod.alreadyVisitedMethod to A.newTarget, B.newTarget and C.newTarget*/
    CallGraph cg = loadCallGraph("Misc", "revisit.RevisitedMethod");

    MethodSignature alreadyVisitedMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisit.RevisitedMethod"),
            "alreadyVisitedMethod",
            "void",
            Collections.singletonList("revisit.A"));

    MethodSignature newTargetA =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisit.A"),
            "newTarget",
            "int",
            Collections.emptyList());
    MethodSignature newTargetB =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisit.B"),
            "newTarget",
            "int",
            Collections.emptyList());
    MethodSignature newTargetC =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisit.C"),
            "newTarget",
            "int",
            Collections.emptyList());

    assertTrue(cg.containsCall(alreadyVisitedMethod, newTargetA));
    assertTrue(cg.containsCall(alreadyVisitedMethod, newTargetB));
    assertTrue(cg.containsCall(alreadyVisitedMethod, newTargetC));
  }

  @Test
  public void testRecursiveRevisitMethod() {
    /* We expect a call edge from RecursiveRevisitedMethod.alreadyVisitedMethod to A.newTarget, B.newTarget and C.newTarget*/
    CallGraph cg = loadCallGraph("Misc", "revisitrecur.RecursiveRevisitedMethod");

    MethodSignature alreadyVisitedMethod =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisitrecur.RecursiveRevisitedMethod"),
            "recursiveAlreadyVisitedMethod",
            "void",
            Collections.singletonList("revisitrecur.A"));

    MethodSignature newTargetA =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisitrecur.A"),
            "newTarget",
            "int",
            Collections.emptyList());
    MethodSignature newTargetB =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisitrecur.B"),
            "newTarget",
            "int",
            Collections.emptyList());
    MethodSignature newTargetC =
        identifierFactory.getMethodSignature(
            identifierFactory.getClassType("revisitrecur.C"),
            "newTarget",
            "int",
            Collections.emptyList());

    assertTrue(cg.containsCall(alreadyVisitedMethod, newTargetA));
    assertTrue(cg.containsCall(alreadyVisitedMethod, newTargetB));
    assertTrue(cg.containsCall(alreadyVisitedMethod, newTargetC));
  }
}
