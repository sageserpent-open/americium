import com.jsuereth.sbtpgp.GetSignaturesModule
import com.jsuereth.sbtpgp.PgpKeys.{signaturesModule, updatePgpSignatures}
lazy val settings = Seq((updatePgpSignatures / signaturesModule) :=
  GetSignaturesModule(projectID.value, libraryDependencies.value, Configurations.Default :: Configurations.Pom :: Configurations.Compile :: Nil))

lazy val troublesome = (project in file("."))
  .settings(settings: _*)
