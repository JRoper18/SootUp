/* Soot - a J*va Optimization Framework
 * Copyright (C) 1999 Patrick Lam
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

package de.upb.soot.jimple.common.expr;

import de.upb.soot.jimple.basic.Value;
import de.upb.soot.jimple.visitor.ExprVisitor;
import de.upb.soot.jimple.visitor.Visitor;
import de.upb.soot.util.Copyable;
import javax.annotation.Nonnull;

public final class JDivExpr extends AbstractFloatBinopExpr implements Copyable {

  public JDivExpr(Value op1, Value op2) {
    super(op1, op2);
  }

  @Override
  public final String getSymbol() {
    return " / ";
  }

  @Override
  public void accept(Visitor sw) {
    ((ExprVisitor) sw).caseDivExpr(this);
  }

  @Nonnull
  public JDivExpr withOp1(Value op1) {
    return new JDivExpr(op1, getOp2());
  }

  @Nonnull
  public JDivExpr withOp2(Value op2) {
    return new JDivExpr(getOp1(), op2);
  }
}
