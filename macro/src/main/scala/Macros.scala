
import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.macros.whitebox._

package object explicitImplicits {
  def deriveImplicits[I, M[_]]: M[I] = macro deriveImplicits_impl[I, M]

  @compileTimeOnly("This could be used only as a macro")
  def deriveImplicits_impl[I, M[I]](c: Context)(implicit itt: c.WeakTypeTag[I], mtt: c.WeakTypeTag[M[I]]): c.Expr[M[I]] = {
    import c.universe._

    // From MacWire ...
    def typeCheckExpressionOfType(typeTree: c.Tree): Type = {
      val someValueOfTypeString = reify {
        def x[T](): T = throw new Exception
        x[String]()
      }

      val Expr(Block(stats, Apply(TypeApply(someValueFun, _), someTypeArgs))) = someValueOfTypeString

      val someValueOfGivenType = Block(stats, Apply(TypeApply(someValueFun, List(typeTree)), someTypeArgs))
      val someValueOfGivenTypeChecked = c.typecheck(someValueOfGivenType)

      someValueOfGivenTypeChecked.tpe
    }

    def computeType(tpt: c.Tree): c.Type = {
      if (tpt.tpe != null) {
        tpt.tpe
      } else {
        val calculatedType = c.typecheck(tpt.duplicate.asInstanceOf[c.Tree], silent = true, withMacrosDisabled = true).tpe
        val result = if (tpt.tpe == null) calculatedType else tpt.tpe

        if (result == NoType) {
          typeCheckExpressionOfType(tpt)
        } else {
          result
        }
      }
    }
    // ... until here

    def childrenFor[A: c.WeakTypeTag]: List[c.universe.Symbol] = {

      val symbol = weakTypeOf[A].typeSymbol

      if ((!symbol.isClass) || (!symbol.asClass.isSealed)) c.abort(
        c.enclosingPosition,
        "Can only enumerate values of a sealed trait or class."
      )

      val siblingSubclasses: List[Symbol] = scala.util.Try {
        val enclosingModule = c.enclosingClass.asInstanceOf[ModuleDef]
        enclosingModule.impl.body.filter { x =>
          scala.util.Try(x.symbol.asModule.moduleClass.asClass.baseClasses.contains(symbol)).getOrElse(false)
        }.map(_.symbol)
      } getOrElse {
        List()
      }

      symbol.asClass.knownDirectSubclasses.toList ::: siblingSubclasses
    }

    def findParametersOfType(paramLists: List[List[c.universe.Symbol]], expectedType: c.Type) = {
      paramLists.flatMap { paramList =>
        paramList.filter { param =>
          param.typeSignature =:= expectedType
        }
      }
    }

    val itemType = c.weakTypeOf[I].typeSymbol.asClass
    val wrapperType = c.weakTypeOf[M[I]].typeSymbol.asClass
    val resultType = tq"$wrapperType[$itemType]"

    val allSealed = childrenFor[I]
    val methodsToImplement = mtt.tpe.members.filter(m => m.isMethod && m.isAbstract).map { method =>
      method.asMethod
    }

    val invalidMethods = methodsToImplement.filterNot { m =>
      val applied = m.typeSignatureIn(computeType(resultType))
      val genericParameters = findParametersOfType(applied.paramLists, itt.tpe)
      genericParameters.size == 1
    }

    if (invalidMethods.size > 0) {
      val wrongMethods = invalidMethods.map { m =>
        showDecl(m)
      }.mkString(", ")
      c.abort(c.enclosingPosition, s"Can generate call forwarders for methods with single parameter of type ${itt.tpe}. Wrong methods: ${wrongMethods}")
    } else {

      val newMethods = for {
        methodToAdd <- methodsToImplement
      } yield {
        val appliedMethod = methodToAdd.typeSignatureIn(computeType(resultType))
        val discriminator = findParametersOfType(appliedMethod.paramLists, itt.tpe).head

        val vparamss: List[List[ValDef]] = appliedMethod.paramLists.map(_.map {
          paramSymbol =>
            ValDef(
              Modifiers(Flag.PARAM, typeNames.EMPTY, List()),
              paramSymbol.name.toTermName,
              TypeTree(paramSymbol.typeSignature),
              EmptyTree
            )
        })

        val paramName = discriminator.name.toTermName

        val cases: Seq[c.Tree] = allSealed.map {
          case clazz: ClassSymbol =>
            val t = clazz.selfType
            val mt = tq"$wrapperType[$t]"

            val callParameters = vparamss.map(_.map {
              p =>
                if (p.name == discriminator.name)
                  q"a.asInstanceOf[$t]"
                else q"${p.name}"

            })
            cq"a : $t => implicitly[$mt].${methodToAdd.name}(...$callParameters)"
        }

        val delegateInvocation =
          q""" $paramName match {
               case ..$cases
             }
          """

        DefDef(
          Modifiers(),
          methodToAdd.name,
          List(), // TODO - type parameters
          vparamss,
          TypeTree(methodToAdd.returnType),
          delegateInvocation
        )
      }

      val tree =
        q"""
      new $resultType {
        ..$newMethods
      }
      """

      c.Expr[M[I]](tree)
    }
  }
}
