package com.sageserpent.americium.java;

@FunctionalInterface
public interface InlinedCaseFiltration {
    /**
     * Executes a runnable in a filtration context associated with some
     * implied case: calls to {@link Trials#whenever(Boolean, Runnable)} will
     * be handled in this context, so if that construct does not have its
     * guard precondition met, the context considers the case to have been
     * filtered out.
     * <p>
     * It is also possible to note certain exceptions thrown by the runnable
     * as also signifying the case to have been filtered out - they will be
     * re-thrown to propagate out of the call.
     *
     * @param runnable                               Code that is run in
     *                                               context - it is the
     *                                               responsibility of the
     *                                               caller to ensure that
     *                                               this code closes over
     *                                               the implied case.
     * @param additionalExceptionsToNoteAsFiltration Exception classes that
     *                                               also signal the
     *                                               filtering out of the
     *                                               implied case.
     * @return True if the case was *not* filtered out.
     */
    boolean executeInFiltrationContext(Runnable runnable,
                                       Class<? extends Throwable>[] additionalExceptionsToNoteAsFiltration);
}
