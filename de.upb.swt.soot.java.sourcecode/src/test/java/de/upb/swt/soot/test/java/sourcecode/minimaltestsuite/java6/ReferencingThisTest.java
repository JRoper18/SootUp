package de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.java6;

import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.test.java.sourcecode.minimaltestsuite.MinimalTestSuiteBase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReferencingThisTest extends MinimalTestSuiteBase {

  public MethodSignature getMethodSignature() {
    return identifierFactory.getMethodSignature(
        "thisMethod", getDeclaredClassSignature(), "void", Collections.emptyList());
  }

  @Override
  public List<String> expectedBodyStmts() {
    return Stream.of(
            "r0 := @this: ReferencingThis",
            "$r1 = <java.lang.System: java.io.PrintStream out>",
            "virtualinvoke $r1.<java.io.PrintStream: void println(java.lang.String)>(\" this keyword as an argument in the constructor call\")",
            "r0 = new ReferencingThis",
            "$i0 = r0.<ReferencingThis: int a>",
            "$i1 = r0.<ReferencingThis: int b>",
            "specialinvoke $u0.<ReferencingThis: void <init>(int,int)>($i0, $i1)",
            "virtualinvoke $u0.<ReferencingThis: void show()>()",
            "return")
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
