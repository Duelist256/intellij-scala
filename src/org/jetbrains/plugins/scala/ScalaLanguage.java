package org.jetbrains.plugins.scala;

import com.intellij.lang.Language;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.lang.Commenter;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.codeInsight.generation.surroundWith.JavaExpressionSurroundDescriptor;
import com.intellij.codeInsight.generation.surroundWith.JavaStatementsSurroundDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scala.highlighter.ScalaSyntaxHighlighter;
import org.jetbrains.plugins.scala.highlighter.ScalaBraceMatcher;
import org.jetbrains.plugins.scala.highlighter.ScalaCommenter;
import org.jetbrains.plugins.scala.util.ScalaToolsFactory;


//import org.jetbrains.plugins.scala.structure.ScalaStructureViewBuilder;
//import org.jetbrains.plugins.scala.lang.parser.ScalaParserDefinition;

/**
 * Author: Ilya Sergey
 * Date: 20.09.2006
 * Time: 15:01:34
 */
public class ScalaLanguage extends Language {

  protected ScalaLanguage(String s) {
    super(s);
  }

  protected ScalaLanguage(String s, String... strings) {
    super(s, strings);
  }

  public ScalaLanguage() {
    super("Scala");
  }

  public FoldingBuilder getFoldingBuilder() {
    //return null;
    return ScalaToolsFactory.getInstance().createScalaFoldingBuilder();
  }

  public ParserDefinition getParserDefinition() {
    System.out.println("getScalaParserDefinition in ScalaLanguage");
    return ScalaToolsFactory.getInstance().createScalaParserDefinition();
  }

  @NotNull
  public SyntaxHighlighter getSyntaxHighlighter(Project project, final VirtualFile virtualFile) {
    return new ScalaSyntaxHighlighter();
  }

  @Nullable
  public PairedBraceMatcher getPairedBraceMatcher() {
    return new ScalaBraceMatcher();
  }

 /* @NotNull
  public StructureViewBuilder getStructureViewBuilder(@NotNull final PsiFile psiFile) {
      return new ScalaStructureViewBuilder(psiFile);
  }*/

  @Nullable
  public Commenter getCommenter() {
    System.out.println("getCommenter in ScalaLanguage");
    return new ScalaCommenter();
  }

 /* @NotNull
  public SurroundDescriptor[] getSurroundDescriptors() {
    System.out.println("getSurroundDescriptors in ScalaLanguage");
    return ScalaToolsFactory.getInstance().createSurroundDescriptors().getSurroundDescriptors();
  }*/
}
