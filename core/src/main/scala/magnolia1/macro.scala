package magnolia1

import scala.quoted.*

object Macro:
  inline def isObject[T]: Boolean = ${ isObject[T] }
  inline def isEnum[T]: Boolean = ${ isEnum[T] }
  inline def anns[T]: List[Any] = ${ anns[T] }
  inline def inheritedAnns[T]: List[Any] = ${ inheritedAnns[T] }
  inline def typeAnns[T]: List[Any] = ${ typeAnns[T] }
  inline def paramAnns[T]: List[(String, List[Any])] = ${ paramAnns[T] }
  inline def inheritedParamAnns[T]: List[(String, List[Any])] = ${
    inheritedParamAnns[T]
  }
  inline def isValueClass[T]: Boolean = ${ isValueClass[T] }
  inline def defaultValue[T]: List[(String, Option[() => Any])] = ${
    defaultValue[T]
  }
  inline def paramTypeAnns[T]: List[(String, List[Any])] = ${ paramTypeAnns[T] }
  inline def repeated[T]: List[(String, Boolean)] = ${ repeated[T] }
  inline def typeInfo[T]: TypeInfo = ${ typeInfo[T] }

  def isObject[T: Type](using Quotes): Expr[Boolean] =
    import quotes.reflect.*

    Expr(TypeRepr.of[T].typeSymbol.flags.is(Flags.Module))

  def isEnum[T: Type](using Quotes): Expr[Boolean] =
    import quotes.reflect.*

    Expr(TypeRepr.of[T].typeSymbol.flags.is(Flags.Enum))

  def anns[T: Type](using Quotes): Expr[List[Any]] =
    new CollectAnnotations[T].anns

  def inheritedAnns[T: Type](using Quotes): Expr[List[Any]] =
    new CollectAnnotations[T].inheritedAnns

  def typeAnns[T: Type](using Quotes): Expr[List[Any]] =
    new CollectAnnotations[T].typeAnns

  def paramAnns[T: Type](using Quotes): Expr[List[(String, List[Any])]] =
    new CollectAnnotations[T].paramAnns

  def inheritedParamAnns[T: Type](using
      Quotes
  ): Expr[List[(String, List[Any])]] =
    new CollectAnnotations[T].inheritedParamAnns

  def isValueClass[T: Type](using Quotes): Expr[Boolean] =
    import quotes.reflect.*

    Expr(
      TypeRepr.of[T].baseClasses.contains(Symbol.classSymbol("scala.AnyVal"))
    )

  def defaultValue[T: Type](using
      Quotes
  ): Expr[List[(String, Option[() => Any])]] =
    import quotes.reflect._
    def exprOfOption(
        oet: (Expr[String], Option[Expr[Any]])
    ): Expr[(String, Option[() => Any])] = oet match {
      case (label, None)     => Expr(label.valueOrAbort -> None)
      case (label, Some(et)) => '{ $label -> Some(() => $et) }
    }
    val tpe = TypeRepr.of[T].typeSymbol
    val terms = tpe.primaryConstructor.paramSymss.flatten
      .filter(_.isValDef)
      .zipWithIndex
      .map { case (field, i) =>
        exprOfOption {
          val defaultMethodName = s"$$lessinit$$greater$$default$$${i + 1}"
          Expr(field.name) -> tpe.companionClass
            .declaredMethod(defaultMethodName)
            .headOption
            .map { defaultMethod =>
              val callDefault = {
                val base = Ident(tpe.companionModule.termRef).select(defaultMethod)
                val tParams = defaultMethod.paramSymss.headOption.filter(_.forall(_.isType))
                tParams match
                  case Some(tParams) => TypeApply(base, tParams.map(TypeTree.ref))
                  case _             => base
              }

              defaultMethod.tree match {
                case tree: DefDef => tree.rhs.getOrElse(callDefault)
                case _            => callDefault
              }
            }
            .map(_.asExprOf[Any])
        }
      }
    Expr.ofList(terms)

  def paramTypeAnns[T: Type](using Quotes): Expr[List[(String, List[Any])]] =
    import quotes.reflect._

    def getAnnotations(t: TypeRepr): List[Term] = t match
      case AnnotatedType(inner, ann) => ann :: getAnnotations(inner)
      case _                         => Nil

    Expr.ofList {
      val typeRepr = TypeRepr.of[T]
      typeRepr.typeSymbol.caseFields
        .map { field =>
          val tpeRepr = typeRepr.memberType(field)

          Expr(field.name) -> getAnnotations(tpeRepr)
            .filter { a =>
              a.tpe.typeSymbol.maybeOwner.isNoSymbol ||
              a.tpe.typeSymbol.owner.fullName != "scala.annotation.internal"
            }
            .map(_.asExpr.asInstanceOf[Expr[Any]])
        }
        .filter(_._2.nonEmpty)
        .map { (name, annots) => Expr.ofTuple(name, Expr.ofList(annots)) }
    }

  def repeated[T: Type](using Quotes): Expr[List[(String, Boolean)]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[T]
    val areRepeated =
      if tpe.typeSymbol.isNoSymbol then Nil
      else {
        val symbol = tpe.typeSymbol
        val ctor = symbol.primaryConstructor
        for param <- ctor.paramSymss.flatten
        yield
          val isRepeated = tpe.memberType(param) match {
            case AnnotatedType(_, annot) => annot.tpe.typeSymbol == defn.RepeatedAnnot
            case _                       => false
          }
          param.name -> isRepeated
      }

    Expr(areRepeated)

  def typeInfo[T: Type](using Quotes): Expr[TypeInfo] =
    import quotes.reflect._

    def normalizedName(s: Symbol): String =
      if s.flags.is(Flags.Module) then s.name.stripSuffix("$") else s.name
    def name(tpe: TypeRepr): Expr[String] = tpe.dealias match
      case matchedTpe @ TermRef(typeRepr, name) if matchedTpe.typeSymbol.flags.is(Flags.Module) =>
        Expr(name.stripSuffix("$"))
      case TermRef(typeRepr, name) => Expr(name)
      case matchedTpe              => Expr(normalizedName(matchedTpe.typeSymbol))

    def ownerNameChain(sym: Symbol): List[String] =
      if sym.isNoSymbol then List.empty
      else if sym == defn.EmptyPackageClass then List.empty
      else if sym == defn.RootPackage then List.empty
      else if sym == defn.RootClass then List.empty
      else ownerNameChain(sym.owner) :+ normalizedName(sym)

    def owner(tpe: TypeRepr): Expr[String] = Expr(
      ownerNameChain(tpe.dealias.typeSymbol.maybeOwner).mkString(".")
    )

    def typeInfo(tpe: TypeRepr): Expr[TypeInfo] = tpe match
      case AppliedType(tpe, args) =>
        '{
          TypeInfo(
            ${ owner(tpe) },
            ${ name(tpe) },
            ${ Expr.ofList(args.map(typeInfo)) }
          )
        }
      case _ =>
        '{ TypeInfo(${ owner(tpe) }, ${ name(tpe) }, Nil) }

    typeInfo(TypeRepr.of[T])

  private class CollectAnnotations[T: Type](using val quotes: Quotes) {
    import quotes.reflect.*

    private val tpe: TypeRepr = TypeRepr.of[T]

    def anns: Expr[List[scala.annotation.Annotation]] =
      Expr.ofList {
        tpe.typeSymbol.annotations
          .filter(filterAnnotation)
          .map(_.asExpr.asInstanceOf[Expr[scala.annotation.Annotation]])
      }

    def inheritedAnns: Expr[List[scala.annotation.Annotation]] =
      Expr.ofList {
        tpe.baseClasses
          .filterNot(isObjectOrScala)
          .collect {
            case s if s != tpe.typeSymbol => s.annotations
          } // skip self
          .flatten
          .filter(filterAnnotation)
          .map(_.asExpr.asInstanceOf[Expr[scala.annotation.Annotation]])
      }

    def typeAnns: Expr[List[scala.annotation.Annotation]] = {

      def getAnnotations(t: TypeRepr): List[Term] = t match
        case AnnotatedType(inner, ann) => ann :: getAnnotations(inner)
        case _                         => Nil

      val symbol: Option[Symbol] =
        if tpe.typeSymbol.isNoSymbol then None else Some(tpe.typeSymbol)
      Expr.ofList {
        symbol.toList.map(_.tree).flatMap {
          case ClassDef(_, _, parents, _, _) =>
            parents
              .collect { case t: TypeTree => t.tpe }
              .flatMap(getAnnotations)
              .filter(filterAnnotation)
              .map(_.asExpr.asInstanceOf[Expr[scala.annotation.Annotation]])
          case _ =>
            // Best effort in case whe -Yretain-trees is not used
            // Does not support class parent annotations (in the extends clouse)
            tpe.baseClasses
              .map(tpe.baseType(_))
              .flatMap(getAnnotations(_))
              .filter(filterAnnotation)
              .map(_.asExprOf[scala.annotation.Annotation])
        }
      }
    }

    def paramAnns: Expr[List[(String, List[scala.annotation.Annotation])]] =
      Expr.ofList {
        groupByParamName {
          (fromConstructor(tpe.typeSymbol) ++ fromDeclarations(tpe.typeSymbol))
            .filter { case (_, anns) => anns.nonEmpty }
        }
      }

    def inheritedParamAnns: Expr[List[(String, List[scala.annotation.Annotation])]] =
      Expr.ofList {
        groupByParamName {
          tpe.baseClasses
            .filterNot(isObjectOrScala)
            .collect {
              case s if s != tpe.typeSymbol =>
                (fromConstructor(s)).filter { case (_, anns) =>
                  anns.nonEmpty
                }
            }
            .flatten ++ fromDeclarations(tpe.typeSymbol, inherited = true)
        }
      }

    private def fromConstructor(from: Symbol): List[(String, List[Expr[scala.annotation.Annotation]])] =
      from.primaryConstructor.paramSymss.flatten.map { field =>
        field.name -> field.annotations
          .filter(filterAnnotation)
          .map(_.asExpr.asInstanceOf[Expr[scala.annotation.Annotation]])
      }

    private def fromDeclarations(
        from: Symbol,
        inherited: Boolean = false
    ): List[(String, List[Expr[scala.annotation.Annotation]])] =
      from.fieldMembers.collect { case field: Symbol =>
        val annotations = if (!inherited) field.annotations else field.allOverriddenSymbols.flatMap(_.annotations).toList
        field.name -> annotations
          .filter(filterAnnotation)
          .map(_.asExpr.asInstanceOf[Expr[scala.annotation.Annotation]])
      }

    private def groupByParamName(anns: List[(String, List[Expr[scala.annotation.Annotation]])]) =
      anns
        .groupBy { case (name, _) => name }
        .toList
        .map { case (name, l) => name -> l.flatMap(_._2) }
        .map { (name, anns) => Expr.ofTuple(Expr(name), Expr.ofList(anns)) }

    private def isObjectOrScala(bc: Symbol) =
      bc.name.contains("java.lang.Object") || bc.fullName.startsWith("scala.")

    private def filterAnnotation(a: Term): Boolean =
      scala.util.Try(a.tpe <:< TypeRepr.of[scala.annotation.Annotation]).toOption.contains(true) &&
      (a.tpe.typeSymbol.maybeOwner.isNoSymbol ||
        (a.tpe.typeSymbol.owner.fullName != "scala.annotation.internal" &&
          a.tpe.typeSymbol.owner.fullName != "jdk.internal"))
  }
