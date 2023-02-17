package sootup.core.jimple.visitor;

/*-
 * #%L
 * Soot - a J*va Optimization Framework
 * %%
 * Copyright (C) 2020 Zun Wang
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

import java.util.function.Function;
import javax.annotation.Nonnull;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.AbstractInvokeExpr;
import sootup.core.jimple.common.expr.Expr;
import sootup.core.jimple.common.ref.Ref;
import sootup.core.jimple.common.stmt.*;
import sootup.core.jimple.javabytecode.stmt.*;

/**
 * Replace old use(Value) of a Stmt with a new use(Value)
 *
 * @author Zun Wang
 */
public class ReplaceUseStmtVisitor extends AbstractStmtVisitor<Stmt> {

  @Nonnull private final Value oldUse;
  @Nonnull private final Value newUse;

  final ReplaceUseExprVisitor exprVisitor = new ReplaceUseExprVisitor();
  final ReplaceUseRefVisitor refVisitor = new ReplaceUseRefVisitor();

  public ReplaceUseStmtVisitor(@Nonnull Value oldUse, @Nonnull Value newUse) {
    this.oldUse = oldUse;
    this.newUse = newUse;
  }

  @Override
  public void caseBreakpointStmt(@Nonnull JBreakpointStmt stmt) {
    defaultCaseStmt(stmt);
  }

  @Override
  public void caseInvokeStmt(@Nonnull JInvokeStmt stmt) {
    Expr invokeExpr = stmt.getInvokeExpr();
    exprVisitor.init(oldUse, newUse);
    invokeExpr.accept(exprVisitor);

    if (exprVisitor.getResult() != invokeExpr) {
      setResult(stmt.withInvokeExpr((AbstractInvokeExpr) exprVisitor.getResult()));
    } else {
      defaultCaseStmt(stmt);
    }
  }

  private JAssignStmt<?, ?> caseAssignRVStmtHelper(
      @Nonnull JAssignStmt<?, ?> stmt, Value value, Function<Value, JAssignStmt<?, ?>> setV) {
    if (value instanceof Immediate) {
      if (value == oldUse) {
        return (setV.apply(newUse));
      }

    } else if (value instanceof Ref) {
      if (value == oldUse) {
        return (setV.apply(newUse));
      } else {
        refVisitor.init(oldUse, newUse);
        ((Ref) value).accept(refVisitor);
        if (refVisitor.getResult() != value) {
          return (setV.apply(refVisitor.getResult()));
        }
      }

    } else if (value instanceof Expr) {

      exprVisitor.init(oldUse, newUse);
      ((Expr) value).accept(exprVisitor);
      if (exprVisitor.getResult() != value) {
        return (setV.apply(exprVisitor.getResult()));
      }
    } else {
      errorHandler(stmt);
    }
    return stmt;
  }

  @Override
  public void caseAssignStmt(@Nonnull JAssignStmt<?, ?> stmt) {
    // Note that the "uses" of a stmt are defined as BOTH left AND right uses.
    int exCount = 0;
    // We count the number of exceptions because the substitutions fail (intentionally) when there's
    // nothing to substitute.
    // However, it may be the case that something needs to get replaced on the LHS but not on the
    // RHS, or vice-versa.
    // We still want to mantain the invariant "if nothing is replaced, that's bad"
    //   but it's fine if there was only a replace on the left or the right.
    JAssignStmt<?, ?> newStmt = stmt;
    try {
      newStmt = caseAssignRVStmtHelper(newStmt, newStmt.getRightOp(), newStmt::withRValue);
    } catch (IllegalArgumentException ex) {
      exCount += 1;
    }
    try {
      newStmt = caseAssignRVStmtHelper(newStmt, newStmt.getLeftOp(), newStmt::withVariable);
    } catch (IllegalArgumentException ex) {
      exCount += 1;
      if (exCount == 2) {
        throw ex;
      }
    }
    setResult(newStmt);
  }

  @Override
  public void caseIdentityStmt(@Nonnull JIdentityStmt stmt) {
    defaultCaseStmt(stmt);
  }

  @Override
  public void caseEnterMonitorStmt(@Nonnull JEnterMonitorStmt stmt) {
    setResult(stmt.withOp((Immediate) newUse));
  }

  @Override
  public void caseExitMonitorStmt(@Nonnull JExitMonitorStmt stmt) {
    setResult(stmt.withOp((Immediate) newUse));
  }

  @Override
  public void caseGotoStmt(@Nonnull JGotoStmt stmt) {
    defaultCaseStmt(stmt);
  }

  @Override
  public void caseIfStmt(@Nonnull JIfStmt stmt) {
    Expr conditionExpr = stmt.getCondition();
    exprVisitor.init((Immediate) oldUse, (Immediate) newUse);
    conditionExpr.accept(exprVisitor);
    if (exprVisitor.getResult() != conditionExpr) {
      setResult(stmt.withCondition((AbstractConditionExpr) exprVisitor.getResult()));
    } else {
      errorHandler(stmt);
    }
  }

  @Override
  public void caseNopStmt(@Nonnull JNopStmt stmt) {
    defaultCaseStmt(stmt);
  }

  @Override
  public void caseRetStmt(@Nonnull JRetStmt stmt) {
    setResult(stmt.withStmtAddress(newUse));
  }

  @Override
  public void caseReturnStmt(@Nonnull JReturnStmt stmt) {
    setResult(stmt.withReturnValue((Immediate) newUse));
  }

  @Override
  public void caseReturnVoidStmt(@Nonnull JReturnVoidStmt stmt) {
    defaultCaseStmt(stmt);
  }

  @Override
  public void caseSwitchStmt(@Nonnull JSwitchStmt stmt) {
    setResult(stmt.withKey((Immediate) newUse));
  }

  @Override
  public void caseThrowStmt(@Nonnull JThrowStmt stmt) {
    setResult(stmt.withOp((Immediate) newUse));
  }

  public void defaultCaseStmt(@Nonnull Stmt stmt) {
    setResult(stmt);
  }

  public void errorHandler(@Nonnull Stmt stmt) {
    defaultCaseStmt(stmt);
    /*
    throw new IllegalArgumentException(
            "The given oldUse '"+ oldUse +"' which should be replaced is not a current use of " + stmt + "!");
            */
  }
}
