package de.upb.swt.soot.callgraph;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2019-2020 Christian Brüggemann, Ben Hermann, Markus Schmidt and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import de.upb.swt.soot.callgraph.typehierarchy.TypeHierarchy;
import de.upb.swt.soot.core.frontend.AbstractClassSource;
import de.upb.swt.soot.core.frontend.ResolveException;
import de.upb.swt.soot.core.jimple.common.expr.AbstractInvokeExpr;
import de.upb.swt.soot.core.jimple.common.stmt.Stmt;
import de.upb.swt.soot.core.model.AbstractClass;
import de.upb.swt.soot.core.model.Method;
import de.upb.swt.soot.core.model.SootClass;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.signatures.MethodSignature;
import de.upb.swt.soot.core.signatures.MethodSubSignature;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.views.View;
import de.upb.swt.soot.java.core.types.JavaClassType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public abstract class AbstractCallGraphAlgorithm implements CallGraphAlgorithm {

  @Nonnull protected final View view;
  @Nonnull protected final TypeHierarchy typeHierarchy;

  protected AbstractCallGraphAlgorithm(View view, TypeHierarchy typeHierarchy) {
    this.view = view;
    this.typeHierarchy = typeHierarchy;
  }

  @Nonnull
  final CallGraph constructCompleteCallGraph(View view, List<MethodSignature> entryPoints) {
    MutableCallGraph cg = new GraphBasedCallGraph();

    Deque<MethodSignature> workList = new ArrayDeque<>(entryPoints);
    Set<MethodSignature> processed = new HashSet<>();

    processWorkList(view, workList, processed, cg);
    return cg;
  }

  /**
   * Processes all entries in the <code>workList</code>, skipping those present in <code>processed
   * </code>, adding call edges to the graph. Newly discovered methods are added to the <code>
   * workList</code> and processed as well. <code>cg</code> is updated accordingly.
   */
  final void processWorkList(
      View view,
      Deque<MethodSignature> workList,
      Set<MethodSignature> processed,
      MutableCallGraph cg) {
    while (!workList.isEmpty()) {
      MethodSignature currentMethodSignature = workList.pop();
      if (processed.contains(currentMethodSignature)) continue;

      Stream<MethodSignature> invocationTargets =
          resolveAllCallsFromSourceMethod(view, currentMethodSignature);

      invocationTargets.forEach(
          t -> {
            if (!cg.containsMethod(currentMethodSignature)) cg.addMethod(currentMethodSignature);
            if (!cg.containsMethod(t)) cg.addMethod(t);
            if (!cg.containsCall(currentMethodSignature, t)) {
              cg.addCall(currentMethodSignature, t);
              workList.push(t);
            }
          });
      processed.add(currentMethodSignature);
    }
  }

  @Nonnull
  Stream<MethodSignature> resolveAllCallsFromSourceMethod(View view, MethodSignature sourceMethod) {
    Method currentMethodCandidate =
        view.getClass(sourceMethod.getDeclClassType())
            .flatMap(c -> c.getMethod(sourceMethod))
            .orElse(null);
    if (currentMethodCandidate == null) return Stream.empty();

    SootMethod currentMethod = (SootMethod) currentMethodCandidate;

    if (currentMethod.hasBody()) {
      return currentMethod.getBody().getStmtGraph().nodes().stream()
          .filter(Stmt::containsInvokeExpr)
          .flatMap(s -> resolveCall(currentMethod, s.getInvokeExpr()));
    } else {
      return Stream.empty();
    }
  }

  /** finds the given method signature in class's superclasses */
  final <T extends Method> T findMethodInHierarchy(View view, MethodSignature sig) {
    SootClass sc = (SootClass) view.getClass(sig.getDeclClassType()).get();
    Optional<ClassType> optSuperclass = sc.getSuperclass();

    Optional<SootMethod> optMethod;
    while (optSuperclass.isPresent()) {
      ClassType superClassType = optSuperclass.get();
      SootClass superClass = (SootClass) view.getClass(superClassType).get();
      optMethod = superClass.getMethod((MethodSubSignature) sig.getSubSignature());
      if (optMethod.isPresent()) {
        return (T) optMethod.get();
      }
      optSuperclass = superClass.getSuperclass();
    }
    throw new ResolveException(
        "Could not find \""
            + sig.getSubSignature()
            + "\" in "
            + sig.getDeclClassType().getClassName()
            + " and in its superclasses");
  }

  @Nonnull
  @Override
  public CallGraph addClass(@Nonnull CallGraph oldCallGraph, @Nonnull JavaClassType classType) {
    MutableCallGraph updated = oldCallGraph.copy();

    AbstractClass<? extends AbstractClassSource> clazz = view.getClassOrThrow(classType);
    Set<MethodSignature> newMethodSignatures =
        clazz.getMethods().stream().map(Method::getSignature).collect(Collectors.toSet());

    if (newMethodSignatures.stream().anyMatch(oldCallGraph::containsMethod)) {
      throw new IllegalArgumentException("CallGraph already contains methods from " + classType);
    }

    // Step 1: Add edges from the new methods to other methods
    Deque<MethodSignature> workList = new ArrayDeque<>(newMethodSignatures);
    Set<MethodSignature> processed = new HashSet<>(oldCallGraph.getMethodSignatures());
    processWorkList(view, workList, processed, updated);

    // Step 2: Add edges from old methods to methods overridden in the new class
    List<ClassType> superClasses = typeHierarchy.superClassesOf(classType);
    Set<ClassType> implementedInterfaces = typeHierarchy.implementedInterfacesOf(classType);
    Stream<ClassType> superTypes =
        Stream.concat(superClasses.stream(), implementedInterfaces.stream());

    Set<MethodSubSignature> newMethodSubSigs =
        newMethodSignatures.stream()
            .map(methodSignature -> (MethodSubSignature) methodSignature.getSubSignature())
            .collect(Collectors.toSet());

    superTypes
        .map(view::getClassOrThrow)
        .flatMap(superType -> superType.getMethods().stream())
        .map(Method::getSignature)
        .filter(
            superTypeMethodSig -> newMethodSubSigs.contains(superTypeMethodSig.getSubSignature()))
        .forEach(
            overriddenMethodSig -> {
              //noinspection OptionalGetWithoutIsPresent (We know this exists)
              MethodSignature overridingMethodSig =
                  clazz
                      .getMethod((MethodSubSignature) overriddenMethodSig.getSubSignature())
                      .get()
                      .getSignature();

              for (MethodSignature callingMethodSig : updated.callsTo(overriddenMethodSig)) {
                updated.addCall(callingMethodSig, overridingMethodSig);
              }
            });

    return updated;
  }

  @Nonnull
  abstract Stream<MethodSignature> resolveCall(SootMethod method, AbstractInvokeExpr invokeExpr);
}