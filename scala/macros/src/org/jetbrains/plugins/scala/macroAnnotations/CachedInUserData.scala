package org.jetbrains.plugins.scala.macroAnnotations

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
  * This annotation makes the compiler generate code that caches values in the user data.
  *
  * UserDataHolder type should have instance of a `org.jetbrains.plugins.scala.caches.ProjectUserDataHolder` type class
  *
  * Caches are invalidated on change of `dependencyItem`.
  *
  * Author: Svyatoslav Ilinskiy, Nikolay.Tropin
  * Date: 9/25/15.
  */
class CachedInUserData(userDataHolder: Any, dependencyItem: Object) extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro CachedInUserData.cachedInsideUserDataImpl
}

object CachedInUserData {
  def cachedInsideUserDataImpl(c: whitebox.Context)(annottees: c.Tree*): c.Expr[Any] = {
    import CachedMacroUtil._
    import c.universe._
    implicit val x: c.type = c
    def parameters: (Tree, Tree) = {
      c.prefix.tree match {
        case q"new CachedInUserData(..$params)" if params.length == 2 =>
          (params(0), params(1))
        case _ => abort("Wrong annotation parameters!")
      }
    }

    //annotation parameters
    val (elem, modTracker) = parameters

    annottees.toList match {
      case DefDef(mods, name, tpParams, paramss, retTp, rhs) :: Nil =>
        if (retTp.isEmpty) {
          abort("You must specify return type")
        }
        //function parameters
        val flatParams = paramss.flatten
        val parameterTypes = flatParams.map(_.tpt)
        val parameterNames: List[c.universe.TermName] = flatParams.map(_.name)
        val hasParams = flatParams.nonEmpty

        //generated types
        val dataType = if (hasParams) tq"(..$parameterTypes)" else tq"Unit"
        val resultType = box(c)(retTp)

        //generated names
        val keyId = c.freshName(name.toString + "cacheKey")
        val elemName = generateTermName("element")
        val dataName = generateTermName("data")
        val keyVarName = generateTermName("key")
        val holderName = generateTermName("holder")
        val resultName = generateTermName("result")
        val cachedFunName = generateTermName(name.toString + "cachedFun")

        val dataValue = if (hasParams) q"(..$parameterNames)" else q"()"
        val getOrCreateCachedHolder =
          if (hasParams)
            q"$cachesUtilFQN.getOrCreateCachedMap[$elemName.type, $dataType, $resultType]($elemName, $keyVarName, () => $modTracker)"
          else
            q"$cachesUtilFQN.getOrCreateCachedRef[$elemName.type, $resultType]($elemName, $keyVarName, () => $modTracker)"

        val getFromHolder =
          if (hasParams) q"$holderName.get($dataName)"
          else q"$holderName.get()"

        val updateHolder =
          if (hasParams) q"$holderName.putIfAbsent($dataName, $resultName)"
          else q"$holderName.compareAndSet(null, $resultName)"


        val hasReturnStmts = hasReturnStatements(c)(rhs)
        val withUIGuard = withUIFreezingGuard(c)(rhs)

        val cachedFun =
          if (hasReturnStmts) q"def $cachedFunName(): $retTp = $withUIGuard" else EmptyTree

        val computation =
          if (hasReturnStmts) q"$cachedFunName()" else q"$withUIGuard"

        val updatedRhs = q"""
          ..$cachedFun

          val $dataName = $dataValue
          val $keyVarName = ${getOrCreateKey(c, hasParams)(q"$keyId", dataType, resultType)}
          val $elemName = $elem

          val $holderName = $getOrCreateCachedHolder
          val fromCachedHolder = $getFromHolder
          if (fromCachedHolder != null) return fromCachedHolder

          val stackStamp = $recursionManagerFQN.markStack()

          val $resultName: $retTp = $computation

          if (stackStamp.mayCacheNow()) {
            $updateHolder
          }

          $resultName
          """
        val updatedDef = DefDef(mods, name, tpParams, paramss, retTp, updatedRhs)

        c.Expr(q"..$updatedDef")
      case _ => abort("You can only annotate one function!")
    }
  }
}
