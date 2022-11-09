package de.upb.sse.sootup.java.bytecode.interceptors;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 1997-2020 Raja Vallée-Rai, Marcus Nachtigall, Markus Schmidt and others
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
import de.upb.sse.sootup.core.graph.StmtGraph;
import de.upb.sse.sootup.core.jimple.common.stmt.JNopStmt;
import de.upb.sse.sootup.core.jimple.common.stmt.Stmt;
import de.upb.sse.sootup.core.model.Body;
import de.upb.sse.sootup.core.transform.BodyInterceptor;
import java.util.*;
import javax.annotation.Nonnull;

/**
 * This class removes {@link JNopStmt}s from a given {@link Body}.
 *
 * @author Marcus Nachtigall
 * @author Markus Schmidt
 */
public class NopEliminator implements BodyInterceptor {

  /**
   * Removes {@link JNopStmt}s from the given {@link Body}. Complexity is linear with respect to the
   * statements.
   *
   * @param builder The current body before interception.
   */
  @Override
  public void interceptBody(@Nonnull Body.BodyBuilder builder) {
    StmtGraph graph = builder.getStmtGraph();
    Set<Stmt> stmtSet = graph.nodes();

    builder.enableDeferredStmtGraphChanges();
    for (Stmt stmt : stmtSet) {
      if (stmt instanceof JNopStmt) {
        final Stmt nopStmt = stmt;
        final List<Stmt> successors = graph.successors(stmt);
        // relink predecessors to successor of nop
        // [ms] in a valid Body there is always a successor of nop
        final Stmt successorOfNop = successors.iterator().next();
        builder.removeFlow(nopStmt, successorOfNop);
        for (Stmt pred : graph.predecessors(nopStmt)) {
          builder.removeFlow(pred, nopStmt);
          builder.addFlow(pred, successorOfNop);
        }
      }
    }
    builder.commitDeferredStmtGraphChanges();
  }
}