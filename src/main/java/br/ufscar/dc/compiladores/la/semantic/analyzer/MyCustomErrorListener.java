package br.ufscar.dc.compiladores.la.semantic.analyzer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.System.exit;
import java.util.BitSet;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token; 
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;


public class MyCustomErrorListener implements ANTLRErrorListener {
    PrintWriter outputFile;
    
    public MyCustomErrorListener(PrintWriter pw) throws IOException{
        this.outputFile = pw;
    }
    @Override
    public void	reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
    }
    
    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
    }

    @Override
    public void	syntaxError(Recognizer<?,?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        Token t = (Token) offendingSymbol;
        if(t.getType() != Token.EOF){
            outputFile.println("Linha " + t.getLine() + ": erro sintatico proximo a " + t.getText());
        } else {
            outputFile.println("Linha " + t.getLine() + ": erro sintatico proximo a EOF");
        }
        outputFile.println("Fim da compilacao");
        //Caso um erro seja encontrado, o arquivo é fechado e a execução é finalizada
        outputFile.close();
        exit(0);
    }
}
