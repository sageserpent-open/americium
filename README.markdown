Port of NTestCaseBuilder to Scala
=================================

A very slow port of NTestCaseBuilder (written in F#) to Scala.

Currently a sandbox for evaluating Scala as an alternative to F#.

There is a chunk of new code written for the Scala implementation that substitutes for an algorithm taken from an off-shelf third-party .NET library (PowerCollections).

I've been backporting this Scala implementation to F# to see how it performs, both via manual porting and also as a contrast, a binary-port via IKVM.

There are several tags in the NTestCaseBuilder repository that have this ported code in a reduced solution. Feel free to play with these to do benchmarking.