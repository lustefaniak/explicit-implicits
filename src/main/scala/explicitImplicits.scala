
import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

package object explicitImplicits {
  def deriveFromImplicits[I, M[_]]: M[I] = macro deriveFromImplicits_impl[I, M]

  @compileTimeOnly("This could be used only as a macro")
  def deriveFromImplicits_impl[I, M[I]](c: Context)(implicit itt: c.WeakTypeTag[I], mtt: c.WeakTypeTag[M[I]]): c.Expr[M[I]] = {
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

    // From https://github.com/ochrons/boopickle
    def findConcreteTypes(tpe: c.universe.Type): Seq[c.universe.ClassSymbol] = {

      val sym = tpe.typeSymbol.asClass
      // must be a sealed trait
      if (!sym.isSealed) {
        val msg = s"The referenced trait ${sym.name} must be sealed"
        c.abort(c.enclosingPosition, msg)
      }

      if (sym.knownDirectSubclasses.isEmpty) {
        val msg = s"The referenced trait ${sym.name} does not have any sub-classes. This may " +
          "happen due to a limitation of scalac (SI-7046) given that the trait is " +
          "not in the same package."
        c.abort(c.enclosingPosition, msg)
      }

      // find all implementation classes in the trait hierarchy
      def findSubClasses(p: c.universe.ClassSymbol): Set[c.universe.ClassSymbol] = {
        p.knownDirectSubclasses.flatMap { sub =>
          val subClass = sub.asClass
          if (subClass.isTrait)
            findSubClasses(subClass)
          else
            Set(subClass) ++ findSubClasses(subClass)
        }
      }
      // sort class names to make sure they are always in the same order
      findSubClasses(sym).toSeq.sortBy(_.name.toString)
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

    val allSealed = findConcreteTypes(itt.tpe)
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
