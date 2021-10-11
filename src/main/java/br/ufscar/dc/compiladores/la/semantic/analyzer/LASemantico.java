package br.ufscar.dc.compiladores.la.semantic.analyzer;

import java.util.HashMap;
import java.util.ArrayList;
import org.antlr.v4.runtime.Token;
import br.ufscar.dc.compiladores.la.semantic.analyzer.Escopos;
import br.ufscar.dc.compiladores.la.semantic.analyzer.TabelaDeSimbolos.TipoLA;

public class LASemantico extends LABaseVisitor<Void> {

    static Escopos escoposAninhados = new Escopos();
    HashMap<String, ArrayList<String>> registros = new HashMap<>();
    HashMap<String, String> novoTipoUnario = new HashMap<>();
    static HashMap<String, ArrayList<TipoLA>> funcoes_e_procedimentos = new HashMap<>();
    
    @Override
    public Void visitCorpo(LAParser.CorpoContext ctx) {
        escoposAninhados.criarNovoEscopo();
        for (LAParser.CmdContext cmd : ctx.cmd())
        if (cmd.cmdRetorne() != null) {
            LASemanticoUtils.adicionarErroSemantico(cmd.getStart(), "comando retorne nao permitido nesse escopo");
        }
        return super.visitCorpo(ctx);
    }
    
    public TipoLA getTipoVariavel(String tipo) {
        if (registros.containsKey(tipo)) {
            return TipoLA.REGISTRO;
        }

        switch (tipo) {
            case "literal":
                return TipoLA.LITERAL;
            case "^literal":
                return TipoLA.LITERAL;
            case "inteiro":
                return TipoLA.NUM_INT;
            case "^inteiro":
                return TipoLA.NUM_INT;
            case "real":
                return TipoLA.NUM_REAL;
            case "^real":
                return TipoLA.NUM_REAL;
            case "logico":
                return TipoLA.OP_LOGICO;
            case "^logico":
                return TipoLA.OP_LOGICO;
            case "registro":
                return TipoLA.REGISTRO;
            default:
                return TipoLA.INVALIDO;
        }
    }
    
    public void adicionaVariavelTabela(String nome, String strTipo, Token tokenNome, Token tokenTipo) {
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        TabelaDeSimbolos tabelaEscopoGlobal = escoposAninhados.percorrerEscoposAninhados().get(escoposAninhados.percorrerEscoposAninhados().size()-1);
        TipoLA tipo = getTipoVariavel(strTipo);
        if (tipo == TipoLA.INVALIDO) {
            LASemanticoUtils.adicionarErroSemantico(tokenTipo, "tipo " + strTipo + " nao declarado");
        }
        if ( !tabela.existe(nome) ) {
            if (!tabelaEscopoGlobal.existe(nome)) {
                tabela.adicionar(nome, tipo);
            } else {
                LASemanticoUtils.adicionarErroSemantico(tokenNome, "identificador " + nome + " ja declarado anteriormente");
            }
        } else {
            LASemanticoUtils.adicionarErroSemantico(tokenNome, "identificador " + nome + " ja declarado anteriormente");
        }
    }
    
    public String verificaDimensao(LAParser.IdentificadorContext ident) {
        String nomeVar;
        if (ident.dimensao().exp_aritmetica().size() > 0) {
            nomeVar = ident.IDENT(0).getText();
        } else {
            nomeVar = ident.getText();
        }
        return nomeVar;
    }
    
    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        String strTipoVar = null;
        
        if (ctx.variavel() != null) {
            if (ctx.variavel().tipo().registro() != null) {
                for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                    adicionaVariavelTabela(ident.getText(), "registro", ident.getStart(), null);
                    for (LAParser.VariavelContext vars : ctx.variavel().tipo().registro().variavel()) {
                        strTipoVar = vars.tipo().getText();
                        for (LAParser.IdentificadorContext ident_reg : vars.identificador()) {
                            adicionaVariavelTabela(ident.getText() + '.' + ident_reg.getText(), strTipoVar, ident_reg.getStart(), vars.tipo().getStart());
                        }
                    }
                }
            } else {
                strTipoVar = ctx.variavel().tipo().getText();
                String nome;
                if (registros.containsKey(strTipoVar)) {
                    ArrayList<String> variaveis_registro = registros.get(strTipoVar);
                    for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                        nome = verificaDimensao(ident);
                        if (tabela.existe(nome) || registros.containsKey(nome)) {
                            LASemanticoUtils.adicionarErroSemantico(ident.getStart(), "identificador " + nome + " ja declarado anteriormente");
                        } else {
                            adicionaVariavelTabela(nome, "registro", ident.getStart(), ctx.variavel().tipo().getStart());
                            for (int i = 0; i < variaveis_registro.size(); i = i + 2) {
                                adicionaVariavelTabela(nome + '.' + variaveis_registro.get(i), variaveis_registro.get(i+1), ident.getStart(), ctx.variavel().tipo().getStart());
                            }
                        }
                    }
                } else if (novoTipoUnario.containsKey(strTipoVar)) {
                    strTipoVar = novoTipoUnario.get(strTipoVar);
                    for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                        adicionaVariavelTabela(ident.getText(), strTipoVar, ident.getStart(), ctx.variavel().tipo().getStart());
                    }
                } else {
                    for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                        String nomeVar = verificaDimensao(ident);
                        if (funcoes_e_procedimentos.containsKey(nomeVar)) {
                            LASemanticoUtils.adicionarErroSemantico(ident.getStart(), "identificador " + nomeVar + " ja declarado anteriormente");
                        } else {
                            adicionaVariavelTabela(nomeVar, strTipoVar, ident.getStart(), ctx.variavel().tipo().getStart());
                        }
                    }
                }
            }
        } else if (ctx.tipo_basico() != null) {
            strTipoVar = ctx.tipo_basico().getText();
            adicionaVariavelTabela(ctx.IDENT().getText(), strTipoVar, ctx.IDENT().getSymbol(), ctx.IDENT().getSymbol());
        } else {
            if (ctx.tipo().registro() != null) {
                ArrayList<String> variaveis_registro = new ArrayList<String>();
                for (LAParser.VariavelContext vars : ctx.tipo().registro().variavel()) {
                    strTipoVar = vars.tipo().getText();
                    for (LAParser.IdentificadorContext ident_registro : vars.identificador()) {
                        variaveis_registro.add(ident_registro.getText());
                        variaveis_registro.add(vars.tipo().getText());
                    }
                }
                registros.put(ctx.IDENT().getText(), variaveis_registro);
            } else {
                strTipoVar = ctx.tipo().getText();
                novoTipoUnario.put(ctx.IDENT().getText(), strTipoVar);
            }
        }

        return super.visitDeclaracao_local(ctx);
    }
    
    @Override 
    public Void visitCmd(LAParser.CmdContext ctx) {
        if (ctx.cmdAtribuicao() != null) {
            return visitCmdAtribuicao(ctx.cmdAtribuicao());
        } else if (ctx.cmdLeia() != null) {
            return visitCmdLeia(ctx.cmdLeia());
        } else if (ctx.cmdEscreva() != null) {
            return visitCmdEscreva(ctx.cmdEscreva());
        } else if (ctx.cmdSe() != null) {
            return visitCmdSe(ctx.cmdSe());
        } else if (ctx.cmdCaso() != null) {
            return visitCmdCaso(ctx.cmdCaso());
        } else if (ctx.cmdEnquanto() != null) {
            return visitCmdEnquanto(ctx.cmdEnquanto());
        } else if (ctx.cmdChamada() != null) {
            return visitCmdChamada(ctx.cmdChamada());
        } else if (ctx.cmdPara() != null) {
            return visitCmdPara(ctx.cmdPara());
        } else if (ctx.cmdFaca() != null) {
            return visitCmdFaca(ctx.cmdFaca());
        } else if (ctx.cmdRetorne() != null) {
            return visitCmdRetorne(ctx.cmdRetorne());
        }
        return null;
    }
    
    @Override
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        TipoLA tipoExpressao = LASemanticoUtils.verificarTipo(tabela, ctx.expressao());
        String varAtribuicao = verificaDimensao(ctx.identificador());

        if (tipoExpressao != TipoLA.INVALIDO) {
            if (!tabela.existe(varAtribuicao)) {
                LASemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "identificador " + ctx.identificador().getText() + " nao declarado");
            } else {
                TipoLA tipoVarAtribuicao = LASemanticoUtils.verificarTipo(tabela, varAtribuicao);
                if (tipoVarAtribuicao != tipoExpressao) {
                    if ( !(((tipoVarAtribuicao == TipoLA.NUM_REAL) || (tipoVarAtribuicao == TipoLA.NUM_INT)) & ((tipoExpressao == TipoLA.NUM_INT) || (tipoExpressao == TipoLA.NUM_REAL))) ) {
                        if (ctx.ponteiro != null) {
                            LASemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para ^" + ctx.identificador().getText());
                        } else {
                            LASemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para " + ctx.identificador().getText());
                        }
                    }
                }
            }
        } else {
            LASemanticoUtils.adicionarErroSemantico(ctx.identificador().getStart(), "atribuicao nao compativel para " + ctx.identificador().getText());
        }
        return super.visitCmdAtribuicao(ctx);
    }
    
    @Override
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        TipoLA tipoExpressao;
        String nomeVar;        
        for (LAParser.IdentificadorContext ident : ctx.identificador()) {
            nomeVar = verificaDimensao(ident);
            if (!tabela.existe(nomeVar)) {
                LASemanticoUtils.adicionarErroSemantico(ident.getStart(), "identificador " + ident.getText() + " nao declarado");
            }
        }
        return super.visitCmdLeia(ctx);
    }

    @Override
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        TipoLA tipoExpressao;
        for (LAParser.ExpressaoContext expressao : ctx.expressao()) {
            tipoExpressao = LASemanticoUtils.verificarTipo(tabela, expressao);
        }
        return super.visitCmdEscreva(ctx);
    }

    @Override
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        TipoLA tipoExpressao = LASemanticoUtils.verificarTipo(tabela, ctx.expressao());
        return super.visitCmdSe(ctx);
    }

    @Override
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) { 
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        TipoLA tipoExpressao = LASemanticoUtils.verificarTipo(tabela, ctx.expressao());
        return super.visitCmdEnquanto(ctx);
    }

    @Override 
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        TipoLA tipoExpressao = LASemanticoUtils.verificarTipo(tabela, ctx.exp_aritmetica());
        ctx.cmd().forEach(cmd -> visitCmd(cmd));
        return super.visitCmdCaso(ctx);
    }

    @Override 
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        if (!tabela.existe(ctx.IDENT().getText())) {
            LASemanticoUtils.adicionarErroSemantico(ctx.IDENT().getSymbol(), "identificador " + ctx.IDENT().getText() + " nao declarado");
        }
        ctx.exp_aritmetica().forEach(expr_arit -> LASemanticoUtils.verificarTipo(tabela, expr_arit));
        ctx.cmd().forEach(cmd -> visitCmd(cmd));
        return super.visitCmdPara(ctx);
    }

    @Override 
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) { 
        escoposAninhados.criarNovoEscopo();
        TabelaDeSimbolos tabela = escoposAninhados.obterEscopoAtual();
        ArrayList<TipoLA> variaveis_do_parametro = new ArrayList<TipoLA>();

        if (ctx.funcao != null) {
            for (LAParser.ParametroContext parametro : ctx.parametros().parametro()) {
                if (parametro.tipo_estendido().tipo_basico_ident().tipo_basico() != null) {
                    for (LAParser.IdentificadorContext ident : parametro.identificador()) {
                        adicionaVariavelTabela(ident.getText(), parametro.tipo_estendido().getText(), ident.getStart(), parametro.tipo_estendido().getStart());
                        variaveis_do_parametro.add(getTipoVariavel(parametro.tipo_estendido().getText()));
                    }
                } else {
                    if (registros.containsKey(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText())) {
                        ArrayList<String> variaveis_registro = registros.get(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText());
                        for (LAParser.IdentificadorContext ident : parametro.identificador()) {
                            variaveis_do_parametro.add(getTipoVariavel(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText()));
                            for (int i = 0; i < variaveis_registro.size(); i = i + 2) {
                                adicionaVariavelTabela(ident.getText() + '.' + variaveis_registro.get(i), variaveis_registro.get(i+1), ident.getStart(), ident.getStart());
                            }
                        }
                    } else {
                        LASemanticoUtils.adicionarErroSemantico(parametro.getStart(), "tipo não declarado");
                    }
                }
            }
            variaveis_do_parametro.add(getTipoVariavel(ctx.tipo_estendido().getText()));
            funcoes_e_procedimentos.put(ctx.IDENT().getText(), variaveis_do_parametro);

        } else {
            for (LAParser.ParametroContext parametro : ctx.parametros().parametro()) {
                if (parametro.tipo_estendido().tipo_basico_ident().tipo_basico() != null) {
                    for (LAParser.IdentificadorContext ident : parametro.identificador()) {
                        adicionaVariavelTabela(ident.getText(), parametro.tipo_estendido().getText(), ident.getStart(), parametro.tipo_estendido().getStart());
                        variaveis_do_parametro.add(getTipoVariavel(parametro.tipo_estendido().getText()));
                    }
                } else {
                    if (registros.containsKey(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText())) {
                        ArrayList<String> variaveis_registro = registros.get(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText());
                        for (LAParser.IdentificadorContext ident : parametro.identificador()) {
                            variaveis_do_parametro.add(getTipoVariavel(parametro.tipo_estendido().tipo_basico_ident().IDENT().getText()));
                            for (int i = 0; i < variaveis_registro.size(); i = i + 2) {
                                adicionaVariavelTabela(ident.getText() + '.' + variaveis_registro.get(i), variaveis_registro.get(i+1), ident.getStart(), ident.getStart());
                            }
                        }
                    } else {
                        LASemanticoUtils.adicionarErroSemantico(parametro.getStart(), "tipo não declarado");
                    }
                }
            }
            for (LAParser.CmdContext comando : ctx.cmd()) {
                if (comando.cmdRetorne() != null) {
                    LASemanticoUtils.adicionarErroSemantico(comando.getStart(), "comando retorne nao permitido nesse escopo");
                }
            }
            funcoes_e_procedimentos.put(ctx.IDENT().getText(), variaveis_do_parametro);
        }
        return super.visitDeclaracao_global(ctx);
    }
}