package magnolia1.examples

import magnolia1._

/** typeclass for providing a default value for a particular type */
trait HasDefault[T]:
  def defaultValue: Either[String, T]
  def getDynamicDefaultValueForParam(paramLabel: String): Option[Any] = None

/** companion object and derivation object for [[HasDefault]] */
object HasDefault extends AutoDerivation[HasDefault]:

  /** constructs a default for each parameter, using the constructor default (if provided), otherwise using a typeclass-provided default
    */
  def join[T](ctx: CaseClass[HasDefault, T]): HasDefault[T] =
    new HasDefault[T] {
      def defaultValue = ctx.constructMonadic { param =>
        param.default match {
          case Some(arg) => Right(arg)
          case None      => param.typeclass.defaultValue
        }
      }

      override def getDynamicDefaultValueForParam(paramLabel: String): Option[Any] =
        val arr = IArray.genericWrapArray {
          ctx.params
            .filter(_.label == paramLabel)
        }.toArray

        val res = arr.headOption
          .flatMap(_.evaluateDefault.map(res => res()))
        println("Printing res:")
        println(res.mkString("Array(", ", ", ")"))
        res
    }

  /** chooses which subtype to delegate to */
  override def split[T](ctx: SealedTrait[HasDefault, T]): HasDefault[T] =
    new HasDefault[T]:
      def defaultValue = ctx.subtypes.headOption match
        case Some(sub) => sub.typeclass.defaultValue
        case None      => Left("no subtypes")

      override def getDynamicDefaultValueForParam(paramLabel: String): Option[Any] =
        ctx.subtypes.headOption match {
          case Some(sub) => sub.typeclass.getDynamicDefaultValueForParam(paramLabel)
          case _         => None
        }

  /** default value for a string; the empty string */
  given string: HasDefault[String] with
    def defaultValue = Right("")

  /** default value for ints; 0 */
  given int: HasDefault[Int] with { def defaultValue = Right(0) }

  /** oh, no, there is no default Boolean... whatever will we do? */
  given boolean: HasDefault[Boolean] with
    def defaultValue = Left("truth is a lie")

  given double: HasDefault[Double] with
    def defaultValue = Right(0)

  /** default value for sequences; the empty sequence */
  given seq[A]: HasDefault[Seq[A]] with
    def defaultValue = Right(Seq.empty)
