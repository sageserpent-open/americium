package com.sageserpent.americium.java;

@FunctionalInterface
public interface ConstructorInvoker {
    Object invoke(Object[] args) throws Exception;
}
