/** @author: Hasitha Rajapakse */
package de.upb.sse.sootup.test.java.sourcecode.minimaltestsuite.java6;

import static org.junit.Assert.assertTrue;

import categories.Java8Test;
import de.upb.sse.sootup.core.model.SootClass;
import de.upb.sse.sootup.core.model.SootMethod;
import de.upb.sse.sootup.core.signatures.MethodSignature;
import de.upb.sse.sootup.test.java.sourcecode.minimaltestsuite.MinimalSourceTestSuiteBase;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Java8Test.class)
public class DeclareEnumTest extends MinimalSourceTestSuiteBase {

  @Test
  public void test() {
    SootClass sc =
        loadClass(
            identifierFactory.getClassType(
                getDeclaredClassSignature().getFullyQualifiedName() + "$Type"));
    assertTrue(sc.isEnum());
    SootMethod method = loadMethod(getMethodSignature());
    assertJimpleStmts(method, expectedBodyStmts());
  }

  @Override
  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        getDeclaredClassSignature(), "declareEnum", "void", Collections.emptyList());
  }

  /**  <pre>
   * public void declareEnum(){
   * for(Type type:Type.values()){
   * System.out.println(type);
   * }
   * }
   *
   * <pre>*/
  @Override
  public List<String> expectedBodyStmts() {
    return super.expectedBodyStmts(
        "r0 := @this: DeclareEnum",
        "$r1 = staticinvoke <DeclareEnum$Type: DeclareEnum$Type[] values()>()",
        "$i0 = 0",
        "label1:",
        "$i1 = lengthof $r1",
        "$z0 = $i0 < $i1",
        "if $z0 == 0 goto label2",
        "$r2 = $r1[$i0]",
        "$r3 = <java.lang.System: java.io.PrintStream out>",
        "virtualinvoke $r3.<java.io.PrintStream: void println(java.lang.Object)>($r2)",
        "$i2 = $i0",
        "$i3 = $i0 + 1",
        "$i0 = $i3",
        "goto label1",
        "label2:",
        "return");
  }
}