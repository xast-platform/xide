# Xide - an IDE for Xast

For now I am directing all the efforts to making a 3D editor for XastGE, because I don't want to use RSyntaxTextArea and LSP for a text editor, so I am planning to rewrite its plugins to Kotlin and use [kotlin-tree-sitter](https://github.com/tree-sitter/kotlin-tree-sitter) for this; so code editor refactorings TBD are:

```java
interface CodeEditor {
    void setText(String text);
    void applyHighlight(List<HighlightSpan> spans);
    void onTextChanged(TextChangeListener listener);
}
```

```java
class HighlightSpan {
    int start;
    int end;
    TokenType type; // "keyword", "string", etc.
}
```

```java
interface LanguageEngine {
    void onTextChanged(String text);
    List<HighlightSpan> getHighlights();
}
```

```
User types
   |
Editor (UI)
   | event
Document
   |
LanguageEngine (Tree-sitter)
   |
Highlight spans
   |
Editor.applyHighlight()
```