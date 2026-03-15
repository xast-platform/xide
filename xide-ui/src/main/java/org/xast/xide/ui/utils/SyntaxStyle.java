package org.xast.xide.ui.utils;

public enum SyntaxStyle {
    Plain("text/plain"),
    ActionScript("text/actionscript"),
    AssemblerX86("text/asm"),
    Assembler6502("text/asm6502"),
    BBCode("text/bbcode"),
    C("text/c"),
    Clojure("text/clojure"),
    CPlusPlus("text/cpp"),
    CSharp("text/cs"),
    CSS("text/css"),
    CSV("text/csv"),
    D("text/d"),
    Dockerfile("text/dockerfile"),
    Dart("text/dart"),
    Delphi("text/delphi"),
    DTD("text/dtd"),
    Fortran("text/fortran"),
    Go("text/golang"),
    Groovy("text/groovy"),
    Handlebars("text/handlebars"),
    Hosts("text/hosts"),
    Htaccess("text/htaccess"),
    HTML("text/html"),
    INI("text/ini"),
    Java("text/java"),
    JavaScript("text/javascript"),
    JSON("text/json"),
    JSONWithComments("text/jshintrc"),
    JSP("text/jsp"),
    Kotlin("text/kotlin"),
    LaTeX("text/latex"),
    Less("text/less"),
    Lisp("text/lisp"),
    Lua("text/lua"),
    Makefile("text/makefile"),
    Markdown("text/markdown"),
    MXML("text/mxml"),
    NSIS("text/nsis"),
    Perl("text/perl"),
    PHP("text/php"),
    Powershell("text/powershell"),
    Proto("text/proto"),
    PropertiesFile("text/properties"),
    Python("text/python"),
    Ruby("text/ruby"),
    Rust("text/rust"),
    SAS("text/sas"),
    Scala("text/scala"),
    SQL("text/sql"),
    Tcl("text/tcl"),
    TypeScript("text/typescript"),
    UnixShell("text/unix"),
    VisualBasic("text/vb"),
    VHDL("text/vhdl"),
    WindowsBatch("text/bat"),
    XML("text/xml"),
    YAML("text/yaml");

    private final String mimeType;

    SyntaxStyle(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}
