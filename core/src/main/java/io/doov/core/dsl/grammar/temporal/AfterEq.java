/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package io.doov.core.dsl.grammar.temporal;

import java.time.temporal.Temporal;

import io.doov.core.dsl.grammar.Apply2;
import io.doov.core.dsl.grammar.ASTNode;

public class AfterEq<N extends Temporal> extends Apply2<N,N,Boolean> {

    public AfterEq(ASTNode<N> lhs, ASTNode<N> rhs) {
        super(Boolean.class, lhs, rhs);
    }
}
