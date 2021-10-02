package br.ufscar.dc.compiladores.la.semantic.analyzer;

import java.util.List;
import java.util.ArrayList;
import org.antlr.v4.runtime.Token;

public class LASemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<>();
    
    public static void adicionarErroSemantico(Token t, String mensagem) {
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }
    
    // ARITMÉRICA
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Exp_aritmeticaContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.TermoContext ta: ctx.termo()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, ta);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                if (aux == TabelaDeSimbolos.TipoLA.NUM_REAL || aux == TabelaDeSimbolos.TipoLA.NUM_INT) {
                    ret = TabelaDeSimbolos.TipoLA.NUM_REAL;
                } else {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }

        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.TermoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.FatorContext fa : ctx.fator()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, fa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                if (aux == TabelaDeSimbolos.TipoLA.NUM_REAL || aux == TabelaDeSimbolos.TipoLA.NUM_INT) {
                    ret = TabelaDeSimbolos.TipoLA.NUM_REAL;
                } else {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.FatorContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.ParcelaContext pa : ctx.parcela()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, pa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                if (aux == TabelaDeSimbolos.TipoLA.NUM_REAL || aux == TabelaDeSimbolos.TipoLA.NUM_INT) {
                    ret = TabelaDeSimbolos.TipoLA.NUM_REAL;
                } else {
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.ParcelaContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        if (ctx.parcela_unario() != null) {
            ret = verificarTipo(tabela, ctx.parcela_unario());
        } else if (ctx.parcela_nao_unario() != null) {
            ret = verificarTipo(tabela, ctx.parcela_nao_unario());
        }
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_unarioContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        if (ctx.identificador() != null) {
            String nomeVar;
            if (ctx.identificador().dimensao().exp_aritmetica().size() > 0) {
                nomeVar = ctx.identificador().IDENT(0).getText();
            } else {
                nomeVar = ctx.identificador().getText();
            }
            if (tabela.existe(nomeVar)) {
                ret = tabela.verificar(nomeVar);
            } else {
                TabelaDeSimbolos tabelaEscopoGlobal = LASemantico.escoposAninhados.percorrerEscoposAninhados().get(LASemantico.escoposAninhados.percorrerEscoposAninhados().size()-1);
                if (tabelaEscopoGlobal.existe(nomeVar)) {
                    ret = tabelaEscopoGlobal.verificar(nomeVar);
                } else {
                    adicionarErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
                    ret = TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        }
        else if (ctx.IDENT() != null) {
            if (LASemantico.funcoes_e_procedimentos.containsKey(ctx.IDENT().getText())) {
                List<TabelaDeSimbolos.TipoLA> aux = LASemantico.funcoes_e_procedimentos.get(ctx.IDENT().getText());
                if (aux.size()-1 == ctx.expressao().size()) {
                    for (int i = 0; i < ctx.expressao().size(); i++) {
                        if (aux.get(i) != verificarTipo(tabela, ctx.expressao().get(i))) {
                            adicionarErroSemantico(ctx.expressao().get(i).getStart(), "incompatibilidade de parametros na chamada de " + ctx.IDENT().getText());
                        }
                    }
                    ret = aux.get(aux.size()-1);
                } else {
                    adicionarErroSemantico(ctx.IDENT().getSymbol(), "incompatibilidade de parametros na chamada de " + ctx.IDENT().getText());
                }
            } else {
                ret = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }
        else if (ctx.NUM_INT() != null) {
            ret = TabelaDeSimbolos.TipoLA.NUM_INT;
        }
        else if (ctx.NUM_REAL() != null) {
            ret = TabelaDeSimbolos.TipoLA.NUM_REAL;
        } else {
            ret = verificarTipo(tabela, ctx.expressao().get(0));
        }
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return TabelaDeSimbolos.TipoLA.LITERAL;
        } else {
            String nomeVar = ctx.identificador().getText();
            if (!tabela.existe(nomeVar)) {
                adicionarErroSemantico(ctx.identificador().getStart(), "identificador " + nomeVar + " nao declarado");
                return TabelaDeSimbolos.TipoLA.INVALIDO;
            } else {
                return tabela.verificar(nomeVar);
            }
        }
    }
    
    // LóGICO
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.ExpressaoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;
        for (LAParser.Termo_logicoContext termo_logico : ctx.termo_logico()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, termo_logico);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                ret = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }
        return ret;
    }

    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Termo_logicoContext ctx) {
        TabelaDeSimbolos.TipoLA ret = null;

        for (LAParser.Fator_logicoContext fator_logico : ctx.fator_logico()) {
            TabelaDeSimbolos.TipoLA aux = verificarTipo(tabela, fator_logico);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != TabelaDeSimbolos.TipoLA.INVALIDO) {
                ret = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }
        return ret;
    }
    
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Fator_logicoContext ctx) {
        if (ctx.parcela_logica().exp_relacional() != null) {
            return verificarTipo(tabela, ctx.parcela_logica().exp_relacional());
        } else {
            return TabelaDeSimbolos.TipoLA.OP_LOGICO;
        }
    }
    
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, LAParser.Exp_relacionalContext ctx) {
        TabelaDeSimbolos.TipoLA retA = null;
        TabelaDeSimbolos.TipoLA retB = null;
        retA = verificarTipo(tabela, ctx.arit1);
        if (ctx.arit2 != null) {
            retB = verificarTipo(tabela, ctx.arit2);
            if (retA == retB) {
                return TabelaDeSimbolos.TipoLA.OP_LOGICO;
            } else {
                if ( (retA == TabelaDeSimbolos.TipoLA.NUM_REAL && retB == TabelaDeSimbolos.TipoLA.NUM_INT) || (retB == TabelaDeSimbolos.TipoLA.NUM_REAL && retA == TabelaDeSimbolos.TipoLA.NUM_INT) ) {
                    return TabelaDeSimbolos.TipoLA.OP_LOGICO;
                } else {
                    return TabelaDeSimbolos.TipoLA.INVALIDO;
                }
            }
        } else {
            return retA;
        }
    }
    
    public static TabelaDeSimbolos.TipoLA verificarTipo(TabelaDeSimbolos tabela, String nome) {
        if (tabela.existe(nome)) {
            return tabela.verificar(nome);
        } else {
            return TabelaDeSimbolos.TipoLA.INVALIDO;
        }
    }
}
