package org.jetbrains.plugins.scala.annotator.element

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.util.TextRange
import org.jetbrains.plugins.scala.annotator.TypeDiff
import org.jetbrains.plugins.scala.annotator.TypeDiff.Mismatch
import org.jetbrains.plugins.scala.annotator.quickfix.ReportHighlightingErrorQuickFix
import org.jetbrains.plugins.scala.lang.psi.api.base.types.ScTypeElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScTypedExpression}
import org.jetbrains.plugins.scala.lang.psi.types.{ScLiteralType, ScType}
import org.jetbrains.plugins.scala.extensions._

object ScTypedExpressionAnnotator extends ElementAnnotator[ScTypedExpression] {
  override def annotate(element: ScTypedExpression, holder: AnnotationHolder, typeAware: Boolean): Unit = {
    if (typeAware) {
      element.typeElement.foreach(checkUpcasting(element.expr, _, holder))
    }
  }

  // SCL-15544
  private def checkUpcasting(expression: ScExpression, typeElement: ScTypeElement, holder: AnnotationHolder): Unit = {
    expression.getTypeAfterImplicitConversion().tr.foreach { actual =>
      val expected = typeElement.calcType

      if (!actual.conforms(expected)) {
        val ranges = mismatchRangesIn(typeElement, actual)
        // TODO add messange to the whole element, but higlight separate parts?
        // TODO fine-grained tooltip
        val wideActual = (expected, actual) match {
          case (_: ScLiteralType, t2: ScLiteralType) => t2
          case (_, t2: ScLiteralType) => t2.wideType
          case (_, t2) => t2
        }
        val message = s"Cannot upcast ${wideActual.presentableText} to ${expected.presentableText}"
        ranges.foreach { range =>
          val annotation = holder.createErrorAnnotation(range, message)
          annotation.registerFix(ReportHighlightingErrorQuickFix)
        }
      }
    }
  }

  // SCL-15481
  def mismatchRangesIn(expected: ScTypeElement, actual: ScType): Seq[TextRange] = {
    val diff = TypeDiff.forExpected(expected.calcType, actual)

    if (diff.text == expected.getText) { // make sure that presentations match
      val (ranges, _) =  diff.flatten.foldLeft((Seq.empty[TextRange], expected.getTextOffset)) { case ((acc, offset), x) =>
        val length = x.text.length
        (if (x.is[Mismatch]) TextRange.create(offset, offset + length) +: acc else acc, offset + length)
      }
      ranges
    } else {
      Seq(expected.getTextRange)
    }
  }
}
