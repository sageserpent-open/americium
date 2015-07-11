package com.sageserpent.infrastructure

import org.junit.extensions.cpsuite.ClasspathSuite
import org.junit.runner.RunWith
import org.junit.Test
import org.junit.extensions.cpsuite.ClasspathSuite.SuiteTypes
import org.junit.extensions.cpsuite.SuiteType.JUNIT38_TEST_CLASSES

import junit.framework.TestCase


@RunWith(classOf[ClasspathSuite])
@SuiteTypes(Array(JUNIT38_TEST_CLASSES))
class AllTests {
}


