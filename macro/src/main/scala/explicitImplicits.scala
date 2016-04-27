
import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

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

    val innerType = c.weakTypeOf[I].typeSymbol.asClass
    val wrapperType = c.weakTypeOf[M[I]].typeSymbol.asClass
    val typedWrapper = tq"$wrapperType[$innerType]"

    val allSealed = childrenFor[I]
    val methodsToImplement = mtt.tpe.members.filter(m => m.isMethod && m.isAbstract).map(_.asMethod)

    val newMethods = methodsToImplement.map { methodToAdd =>
      val appliedMethod = methodToAdd.typeSignatureIn(computeType(typedWrapper))
      val parametersOfGenericType = findParametersOfType(appliedMethod.paramLists, itt.tpe)

      if (parametersOfGenericType.size != 1) {
        c.abort(c.enclosingPosition, s"Can generate call forwarders for methods with single parameter of type ${itt.tpe}. Wrong method: ${show(methodToAdd)}")
      } else {

        val discriminator = parametersOfGenericType.head

        val vparamss = appliedMethod.paramLists.map(_.map { paramSymbol =>
          q"val ${paramSymbol.name.toTermName}:${paramSymbol.typeSignature}"
        })

        val cases: Seq[c.Tree] = allSealed.map {
          case clazz: ClassSymbol =>
            val t = clazz.selfType
            val mt = tq"$wrapperType[$t]"
            val callParameters = appliedMethod.paramLists.map(_.map { p =>
              if (p.name == discriminator.name) q"a.asInstanceOf[$t]" else q"${p.name.toTermName}"
            })
            cq"a : $t => implicitly[$mt].${methodToAdd.name}(...$callParameters)"
        }

        q"""
          def ${methodToAdd.name}(...$vparamss) = ${discriminator.name.toTermName} match {
            case ..$cases
          }
        """
      }
    }

    val tree =
      q"""
      new $typedWrapper {
        ..$newMethods
      }
      """

    c.Expr[M[I]](tree)

  }
}
