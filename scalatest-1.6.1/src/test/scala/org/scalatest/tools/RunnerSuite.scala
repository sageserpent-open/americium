/*
 * Copyright 2001-2008 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatest.tools

import org.scalatest._

class RunnerSuite() extends Suite with PrivateMethodTester {

  def testParseArgsIntoLists() {

    // this is how i solved the problem of wanting to reuse these val names, runpathList, reportersList, etc.
    // by putting them in a little verify method, it gets reused each time i call that method
    def verify(
      args: Array[String],
      expectedRunpathList: List[String],
      expectedReporterList: List[String],
      expectedSuitesList: List[String],
      expectedJunitsList: List[String],
      expectedPropsList: List[String],
      expectedIncludesList: List[String],
      expectedExcludesList: List[String],
      expectedConcurrentList: List[String],
      expectedMemberOfList: List[String],
      expectedBeginsWithList: List[String],
      expectedTestNGList: List[String]
    ) = {

      val (
        runpathList,
        reportersList,
        suitesList,
        junitsList,
        propsList,
        includesList,
        excludesList,
        concurrentList,
        memberOfList,
        beginsWithList,
        testNGList
      ) = Runner.parseArgs(args)

      assert(runpathList === expectedRunpathList)
      assert(reportersList === expectedReporterList)
      assert(suitesList === expectedSuitesList)
      assert(junitsList === expectedJunitsList)
      assert(propsList === expectedPropsList)
      assert(includesList === expectedIncludesList)
      assert(excludesList === expectedExcludesList)
      assert(concurrentList === expectedConcurrentList)
      assert(memberOfList === expectedMemberOfList)
      assert(beginsWithList === expectedBeginsWithList)
      assert(testNGList === expectedTestNGList)
    }

    verify(
      Array("-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
            "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out", "-p"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-p"),
      List("-g", "-g", "-f", "file.out"),
      Nil,
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil
    )

    verify(
      Array("-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
            "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out",
            "-s", "SuiteOne", "-s", "SuiteTwo"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-f", "file.out"),
      List("-s", "SuiteOne", "-s", "SuiteTwo"),
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil
    )

    verify(
      Array(),
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil
    )

    verify(
      Array("-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
            "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out",
            "-n", "JustOne", "-s", "SuiteOne", "-s", "SuiteTwo"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-f", "file.out"),
      List("-s", "SuiteOne", "-s", "SuiteTwo"),
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      List("-n", "JustOne"),
      Nil,
      Nil,
      Nil,
      Nil,
      Nil
    )

    verify(
      Array("-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
            "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out",
            "-n", "One Two Three", "-l", "SlowTests", "-s", "SuiteOne", "-s", "SuiteTwo"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-f", "file.out"),
      List("-s", "SuiteOne", "-s", "SuiteTwo"),
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      List("-n", "One Two Three"),
      List("-l", "SlowTests"),
      Nil,
      Nil,
      Nil,
      Nil
    )

    verify(
      Array("-c", "-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
            "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out",
            "-n", "One Two Three", "-l", "SlowTests", "-s", "SuiteOne", "-s", "SuiteTwo"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-f", "file.out"),
      List("-s", "SuiteOne", "-s", "SuiteTwo"),
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      List("-n", "One Two Three"),
      List("-l", "SlowTests"),
      List("-c"),
      Nil,
      Nil,
      Nil
    )

    verify(
      Array("-c", "-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
          "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out",
          "-n", "One Two Three", "-l", "SlowTests", "-s", "SuiteOne", "-s", "SuiteTwo", "-m", "com.example.webapp",
          "-w", "com.example.root"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-f", "file.out"),
      List("-s", "SuiteOne", "-s", "SuiteTwo"),
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      List("-n", "One Two Three"),
      List("-l", "SlowTests"),
      List("-c"),
      List("-m", "com.example.webapp"),
      List("-w", "com.example.root"),
      Nil
    )
    // Try a TestNGSuite
    verify(
      Array("-c", "-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
          "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out",
          "-n", "One Two Three", "-l", "SlowTests", "-s", "SuiteOne", "-s", "SuiteTwo", "-m", "com.example.webapp",
          "-w", "com.example.root", "-t", "some/path/file.xml"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-f", "file.out"),
      List("-s", "SuiteOne", "-s", "SuiteTwo"),
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      List("-n", "One Two Three"),
      List("-l", "SlowTests"),
      List("-c"),
      List("-m", "com.example.webapp"),
      List("-w", "com.example.root"),
      List("-t", "some/path/file.xml")
    )
    // Try a junit Suite
    verify(
      Array("-c", "-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
          "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-f", "file.out",
          "-n", "One Two Three", "-l", "SlowTests", "-s", "SuiteOne", "-j", "junitTest", "-j", "junitTest2",
          "-m", "com.example.webapp", "-w", "com.example.root", "-t", "some/path/file.xml"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-f", "file.out"),
      List("-s", "SuiteOne"),
      List("-j", "junitTest", "-j", "junitTest2"),
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      List("-n", "One Two Three"),
      List("-l", "SlowTests"),
      List("-c"),
      List("-m", "com.example.webapp"),
      List("-w", "com.example.root"),
      List("-t", "some/path/file.xml")
    )
    // Test -u option
    verify(
      Array("-c", "-g", "-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188", "-p",
          "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\"", "-g", "-u", "directory/",
          "-n", "One Two Three", "-l", "SlowTests", "-s", "SuiteOne",
          "-m", "com.example.webapp", "-w", "com.example.root", "-t", "some/path/file.xml"),
      List("-p", "\"serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar\""),
      List("-g", "-g", "-u", "directory/"),
      List("-s", "SuiteOne"),
      Nil,
      List("-Dincredible=whatshername", "-Ddbname=testdb", "-Dserver=192.168.1.188"),
      List("-n", "One Two Three"),
      List("-l", "SlowTests"),
      List("-c"),
      List("-m", "com.example.webapp"),
      List("-w", "com.example.root"),
      List("-t", "some/path/file.xml")
    )
  }

  def testParseCompoundArgIntoSet() {
    expect(Set("Cat", "Dog")) {
      Runner.parseCompoundArgIntoSet(List("-n", "Cat Dog"), "-n")
    }
  }

  def testParseConfigSet() {

    val parseConfigSet = PrivateMethod[Set[ReporterConfigParam]]('parseConfigSet)

    intercept[NullPointerException] {
      Runner invokePrivate parseConfigSet(null)
    }
    intercept[IllegalArgumentException] {
      Runner invokePrivate parseConfigSet("-fJ")
    }
    intercept[IllegalArgumentException] {
      Runner invokePrivate parseConfigSet("-uJ")
    }
    intercept[IllegalArgumentException] {
      Runner invokePrivate parseConfigSet("-oYZTFUPBISARG-")
    }
    intercept[IllegalArgumentException] {
      Runner invokePrivate parseConfigSet("-")
    }
    intercept[IllegalArgumentException] {
      Runner invokePrivate parseConfigSet("")
    }

    expect(Set(FilterTestStarting)) {
      Runner invokePrivate parseConfigSet("-oN")
    }
    expect(Set(FilterTestSucceeded)) {
      Runner invokePrivate parseConfigSet("-oC")
    }
    expect(Set(FilterTestIgnored)) {
      Runner invokePrivate parseConfigSet("-oX")
    }
    expect(Set(FilterTestPending)) {
      Runner invokePrivate parseConfigSet("-oE")
    }
    expect(Set(FilterSuiteStarting)) {
      Runner invokePrivate parseConfigSet("-oH")
    }
    expect(Set(FilterSuiteCompleted)) {
      Runner invokePrivate parseConfigSet("-oL")
    }
    expect(Set(FilterInfoProvided)) {
      Runner invokePrivate parseConfigSet("-oO")
    }
    expect(Set(PresentWithoutColor)) {
      Runner invokePrivate parseConfigSet("-oW")
    }
    expect(Set(PresentAllDurations)) {
      Runner invokePrivate parseConfigSet("-oD")
    }
    expect(Set(PresentFullStackTraces)) {
      Runner invokePrivate parseConfigSet("-oF")
    }
    expect(Set[ReporterConfigParam]()) {
      Runner invokePrivate parseConfigSet("-f")
    }
    expect(Set[ReporterConfigParam]()) {
      Runner invokePrivate parseConfigSet("-u")
    }

    expect(Set(FilterInfoProvided, PresentWithoutColor)) {
      Runner invokePrivate parseConfigSet("-oOW")
    }
    expect(Set(FilterInfoProvided, PresentWithoutColor)) {
      Runner invokePrivate parseConfigSet("-oWO") // Just reverse the order of the params
    }
    val allOpts = Set(
      FilterInfoProvided,
      FilterSuiteCompleted,
      FilterSuiteStarting,
      FilterTestIgnored,
      FilterTestPending,
      FilterTestStarting,
      FilterTestSucceeded,
      PresentAllDurations,
      PresentWithoutColor,
      PresentFullStackTraces
    )
    expect(allOpts) {
      Runner invokePrivate parseConfigSet("-oNCXEHLOWDF")
    }
  }
                                         
  def testParseReporterArgsIntoSpecs() {
    intercept[NullPointerException] {
      Runner.parseReporterArgsIntoConfigurations(null)
    }
    intercept[NullPointerException] {
      Runner.parseReporterArgsIntoConfigurations(List("Hello", null, "World"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("Hello", "-", "World"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("Hello", "", "World"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("-g", "-l", "-o"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("Hello", " there", " world!"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("-g", "-o", "-g", "-e"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("-o", "-o", "-g", "-e"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("-e", "-o", "-g", "-e"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("-f")) // Can't have -f last, because need a file name
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("-u")) // Can't have -u last, because need a directory name
    }
    intercept[IllegalArgumentException] {
      Runner.parseReporterArgsIntoConfigurations(List("-r")) // Can't have -r last, because need a reporter class
    }
    expect(new ReporterConfigurations(None, Nil, Nil, None, None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(Nil)
    }
    expect(new ReporterConfigurations(Some(new GraphicReporterConfiguration(Set())), Nil, Nil, None, None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-g"))
    }
    expect(new ReporterConfigurations(Some(new GraphicReporterConfiguration(Set(FilterSuiteCompleted))), Nil, Nil, None, None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-gL"))
    }
    expect(new ReporterConfigurations(None, Nil, Nil, Some(new StandardOutReporterConfiguration(Set())), None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-o"))
    }
    expect(new ReporterConfigurations(None, Nil, Nil, Some(new StandardOutReporterConfiguration(Set(FilterTestSucceeded,FilterTestIgnored))), None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-oCX"))
    }
    expect(new ReporterConfigurations(None, Nil, Nil, None, Some(new StandardErrReporterConfiguration(Set())), Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-e"))
    }
    expect(new ReporterConfigurations(None, Nil, Nil, None, Some(new StandardErrReporterConfiguration(Set(PresentFullStackTraces))), Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-eF"))
    }
    expect(new ReporterConfigurations(None, List(new FileReporterConfiguration(Set(), "theFilename")), Nil, None, None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-f", "theFilename"))
    }
    expect(new ReporterConfigurations(None, Nil, List(new XmlReporterConfiguration(Set(), "target")), None, None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-u", "target"))
    }
    expect(new ReporterConfigurations(None, Nil, List(new XmlReporterConfiguration(Set(), "target")), None, None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-uN", "target"))
    }
    expect(new ReporterConfigurations(None, List(new FileReporterConfiguration(Set(FilterTestStarting), "theFilename")), Nil, None, None, Nil, Nil)) {
      Runner.parseReporterArgsIntoConfigurations(List("-fN", "theFilename"))
    }
    expect(new ReporterConfigurations(None, Nil, Nil, None, None, Nil, List(new CustomReporterConfiguration(Set(), "the.reporter.Class")))) {
      Runner.parseReporterArgsIntoConfigurations(List("-r", "the.reporter.Class"))
    }
    expect(new ReporterConfigurations(None, Nil, Nil, None, None, Nil, List(new CustomReporterConfiguration(Set(FilterTestPending), "the.reporter.Class")))) {
      Runner.parseReporterArgsIntoConfigurations(List("-rE", "the.reporter.Class"))
    }
  }

  def testParseSuiteArgsIntoClassNameStrings() {
    intercept[NullPointerException] {
      Runner.parseSuiteArgsIntoNameStrings(null, "-s")
    }
    intercept[NullPointerException] {
      Runner.parseSuiteArgsIntoNameStrings(List("-s", null, "-s"), "-s")
    }
    intercept[IllegalArgumentException] {
      Runner.parseSuiteArgsIntoNameStrings(List("-s", "SweetSuite", "-s"), "-s")
    }
    intercept[IllegalArgumentException] {
      Runner.parseSuiteArgsIntoNameStrings(List("-s", "SweetSuite", "-s", "-s"), "-s")
    }
    expect(List("SweetSuite", "OKSuite")) {
      Runner.parseSuiteArgsIntoNameStrings(List("-s", "SweetSuite", "-s", "OKSuite"), "-s")
    }
    expect(List("SweetSuite", "OKSuite", "SomeSuite")) {
      Runner.parseSuiteArgsIntoNameStrings(List("-s", "SweetSuite", "-s", "OKSuite", "-s", "SomeSuite"), "-s")
    }
  }

  def testParseRunpathArgIntoList() {
    intercept[NullPointerException] {
      Runner.parseRunpathArgIntoList(null)
    }
    intercept[NullPointerException] {
      Runner.parseRunpathArgIntoList(List("-p", null))
    }
    intercept[NullPointerException] {
      Runner.parseRunpathArgIntoList(List(null, "serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseRunpathArgIntoList(List("-p"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseRunpathArgIntoList(List("-p", "bla", "bla"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseRunpathArgIntoList(List("-pX", "bla"))
    }
    intercept[IllegalArgumentException] {
      Runner.parseRunpathArgIntoList(List("-p", "  "))
    }
    intercept[IllegalArgumentException] {
      Runner.parseRunpathArgIntoList(List("-p", "\t"))
    }
    expect(List("bla")) {
      Runner.parseRunpathArgIntoList(List("-p", "bla"))
    }
    expect(List("bla", "bla", "bla")) {
      Runner.parseRunpathArgIntoList(List("-p", "bla bla bla"))
    }
    expect(List("serviceuitest-1.1beta4.jar", "myjini", "http://myhost:9998/myfile.jar")) {
      Runner.parseRunpathArgIntoList(List("-p", "serviceuitest-1.1beta4.jar myjini http://myhost:9998/myfile.jar"))
    }
    expect(List("\\", "c:\\", "c:\\Program Files", "c:\\Documents and Settings", "\\", "myjini")) {
      Runner.parseRunpathArgIntoList(List("-p", """\ c:\ c:\Program\ Files c:\Documents\ and\ Settings \ myjini"""))
    }
  }

  def testParsePropertiesArgsIntoMap() {
    intercept[NullPointerException] {
      Runner.parsePropertiesArgsIntoMap(null)
    }
    intercept[NullPointerException] {
      Runner.parsePropertiesArgsIntoMap(List("-Da=b", null))
    }
    intercept[IllegalArgumentException] {
      Runner.parsePropertiesArgsIntoMap(List("-Dab")) // = sign missing
    }
    intercept[IllegalArgumentException] {
      Runner.parsePropertiesArgsIntoMap(List("ab")) // needs to start with -D
    }
    intercept[IllegalArgumentException] {
      Runner.parsePropertiesArgsIntoMap(List("-D=ab")) // no key
    }
    intercept[IllegalArgumentException] {
      Runner.parsePropertiesArgsIntoMap(List("-Dab=")) // no value
    }
    expect(Map("a" -> "b", "cat" -> "dog", "Glorp" -> "Glib")) {
      Runner.parsePropertiesArgsIntoMap(List("-Da=b", "-Dcat=dog", "-DGlorp=Glib"))
    }
  }

  def testCheckArgsForValidity() {
    intercept[NullPointerException] {
      Runner.checkArgsForValidity(null)
    }
    expect(None) {
      Runner.checkArgsForValidity(Array("-Ddbname=testdb", "-Dserver=192.168.1.188", "-p", "serviceuitest-1.1beta4.jar", "-g", "-eFBA", "-s", "MySuite"))
    }
    assert(Runner.checkArgsForValidity(Array("-Ddbname=testdb", "-Dserver=192.168.1.188", "-z", "serviceuitest-1.1beta4.jar", "-g", "-eFBA", "-s", "MySuite")) != None)
    expect(None) {
      Runner.checkArgsForValidity(Array("-Ddbname=testdb", "-Dserver=192.168.1.188", "-p", "serviceuitest-1.1beta4.jar", "-g", "-eFBA", "-s", "MySuite", "-c"))
    }
  }

/*
  def testRunpathPropertyAddedToPropertiesMap() {
    val a = new Suite {
      var theProperties: Map[String, Any] = Map()
      override def execute(testName: Option[String], reporter: Reporter, stopper: Stopper, includes: Set[String], excludes: Set[String],
          properties: Map[String, Any], distributor: Option[Distributor]) {
        theProperties = properties
      }
    }

    val dispatchReporter = new DispatchReporter(Nil, System.out)
    val suitesList = List("org.scalatest.usefulstuff.RunpathPropCheckerSuite")

    // Runner.doRunRunRunADoRunRun(new DispatchReporter)
    // Runner.doRunRunRunADoRunRun(dispatchReporter, suitesList, new Stopper {}, Filter(), Map(), false,
         List(), List(), runpath: "build_tests", loader: ClassLoader,
      doneListener: RunDoneListener) = {

    ()
  }
}

package org.scalatest.usefulstuff {

  class RunpathPropCheckerSuite extends Suite {
    var theProperties: Map[String, Any] = Map()
    override def execute(testName: Option[String], reporter: Reporter, stopper: Stopper, includes: Set[String], excludes: Set[String],
        properties: Map[String, Any], distributor: Option[Distributor]) {
      theProperties = properties
    }
  }
*/
}
