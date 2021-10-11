package br.ufscar.dc.compiladores.la.semantic.analyzer;

import java.util.HashMap;
import java.util.ArrayList;
import br.ufscar.dc.compiladores.la.semantic.analyzer.TabelaDeSimbolos.TipoLA;

public class LAGeradorC extends LABaseVisitor<Void> {
    
    StringBuilder saida;
    TabelaDeSimbolos tabela;
    HashMap<String, ArrayList<String>> registro = new HashMap<>();
    
    //Construtor
    public LAGeradorC(){
        saida = new StringBuilder();
        this.tabela = new TabelaDeSimbolos();
    }
    
    //Tipos das variáveis em C
    //Função auxiliar para determinar o que escrever no código em C gerado
    public static String getTipoVariavelTexto(String tipo) {
        String tipoVar, strTipoVar;
        switch (tipo) {
            case "inteiro":
                return "int";
            case "^inteiro":
                return "int *";
            case "real":
                return "float";
            case "^real":
                return "float *";
            case "literal":
                return "char";
            case "^literal":
                return "char *";
            case "logico":
                return "boolean";
            case "^logico":
                return "boolean *";
            case "registro":
                return "struct";
            default:
                return null;
        }
    }

    //Aqui é uma conversão sobre a string recebida para uma informação do tipo TipoLA
    public static TipoLA getTipoVariavelLA(String tipo) {
        switch (tipo) {
            case "inteiro":
                return TipoLA.NUM_INT;
            case "^inteiro":
                return TipoLA.NUM_INT;
            case "real":
                return TipoLA.NUM_REAL;
            case "^real":
                return TipoLA.NUM_REAL;
            case "literal":
                return TipoLA.LITERAL;
            case "^literal":
                return TipoLA.LITERAL;
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
    
    //Toda entrada e saída do C usa um receptor diferente. Aqui, com base no TipoLA,
    //uma dessas opções é selecionada
    public static String getTipoInOut(TipoLA tipo) {
        switch (tipo) {
            case NUM_INT:
                return "%d";
            case NUM_REAL:
                return "%f";
            case LITERAL:
                return "%s";
            case OP_LOGICO:
                return "%d";
            default:
                return null;
        }
    }
    
    //Começamos com o programa
    //Fazemos as visitações sempre que um novo é necessário
    //Adicionado mais includes que o necessário... Nunca se sabe
    @Override
    public Void visitPrograma(LAParser.ProgramaContext ctx){
        saida.append("#include <math.h>\n");
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("#include <string.h>\n");
        saida.append("#include <stdbool.h>\n\n");
        
        ctx.declaracoes().decl_local_global().forEach(dec -> visitDecl_local_global(dec));
        
        saida.append("\n");
        saida.append("int main() {\n");
        
        ctx.corpo().declaracao_local().forEach(x -> visitDeclaracao_local(x));
        ctx.corpo().cmd().forEach(x -> visitCmd(x));
        
        saida.append("return 0;\n}\n");
        return null;
    }
    
    //As declarações globais e locais são separadas aqui, antes de entrar na main principal
    //do programa
    @Override
    public Void visitDecl_local_global(LAParser.Decl_local_globalContext ctx){
        if(ctx.declaracao_global() == null)
            visitDeclaracao_local(ctx.declaracao_local());
        else
            visitDeclaracao_global(ctx.declaracao_global());
        
        return null;
    }
    
    //As declarações globais de funções tem o retorno especificado ou VOID, caso precise e
    //o tipo venha vazio
    @Override
    public Void visitDeclaracao_global(LAParser.Declaracao_globalContext ctx){
        if (ctx.funcao != null) {
            saida.append(getTipoVariavelTexto(ctx.tipo_estendido().getText()) + " " + ctx.IDENT().getText() + " (");
            visitParametros(ctx.parametros());
            saida.append("){\n");
            ctx.declaracao_local().forEach(x -> visitDeclaracao_local(x));
            ctx.cmd().forEach(x -> visitCmd(x));
            saida.append("}\n\n");
        } else {
            saida.append("void " + ctx.IDENT().getText() + "( ");
            visitParametros(ctx.parametros());
            saida.append("){\n");
            ctx.declaracao_local().forEach(dec -> visitDeclaracao_local(dec));
            ctx.cmd().forEach(cmd -> visitCmd(cmd));
            saida.append("}\n\n");
        }
        
        return null;
    }
    
    //Defines e structs, antes da main iniciar. Tamanho padrão dos arrays foi definido como 99. 
    //Como não houve problema nenhum, foi deixado assim
    @Override
    public Void visitDeclaracao_local(LAParser.Declaracao_localContext ctx){
        if (ctx.variavel() != null) {
            String strTipoVar = getTipoVariavelTexto(ctx.variavel().tipo().getText());
            TipoLA tipoVar = getTipoVariavelLA(ctx.variavel().tipo().getText());
            if (ctx.variavel().tipo().registro() != null) {
                for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                    tabela.adicionar(ident.getText(), TipoLA.REGISTRO);
                    saida.append("struct " + " {\n");
                    for (LAParser.VariavelContext vars : ctx.variavel().tipo().registro().variavel()) {
                        strTipoVar = getTipoVariavelTexto(vars.tipo().getText());
                        for (LAParser.IdentificadorContext ident_reg : vars.identificador()) {
                            tabela.adicionar(ident.getText() + '.' + ident_reg.getText(), getTipoVariavelLA(vars.tipo().getText()));
                            if (strTipoVar.equals("char")) {
                                saida.append("\t" + strTipoVar + " " + ident_reg.getText() + "[99];\n");
                            } else {
                                saida.append("\t" + strTipoVar + " " + ident_reg.getText() + ";\n");
                            }
                        }
                    }
                    saida.append("}" + ident.getText() + ";\n");
                }
            } else if (registro.containsKey(ctx.variavel().tipo().getText())) {
                ArrayList<String> variaveis_registro = registro.get(ctx.variavel().tipo().getText());
                for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                    saida.append(ctx.variavel().tipo().getText() + " " + ident.getText() + ";\n");
                    for (int i = 0; i < variaveis_registro.size(); i = i + 2) {
                        tabela.adicionar(ident.getText() + '.' + variaveis_registro.get(i), getTipoVariavelLA(variaveis_registro.get(i+1)));
                    }
                } 
            } else {
                saida.append(strTipoVar + " ");
                for (LAParser.IdentificadorContext ident : ctx.variavel().identificador()) {
                    if (ident.dimensao() != null) {
                        tabela.adicionar(ident.IDENT(0).getText(), tipoVar);
                    }
                    if (strTipoVar.equals("char")) {
                        saida.append(ident.getText() + "[99]");
                    } else {
                        saida.append(ident.getText());
                    }
                    tabela.adicionar(ident.getText(), tipoVar);
                    saida.append(", ");
                }
                saida.delete(saida.length()-2, saida.length());
                saida.append(";\n");
            }
        } else if (ctx.tipo_basico() != null) {
            saida.append("#define " + ctx.IDENT().getText() + " " + ctx.valor_constante().getText());
            tabela.adicionar(ctx.IDENT().getText(), getTipoVariavelLA(ctx.tipo_basico().getText()));
        } else {
            ArrayList<String> variaveis_registro = new ArrayList<String>();
            saida.append("typedef struct {\n");
            for (LAParser.VariavelContext vars : ctx.tipo().registro().variavel()) {
                String strTipoVar = vars.tipo().getText();
                for (LAParser.IdentificadorContext ident_registro : vars.identificador()) {
                    variaveis_registro.add(ident_registro.getText());
                    variaveis_registro.add(vars.tipo().getText());
                    if (getTipoVariavelTexto(vars.tipo().getText()).equals("char")) {
                        saida.append("\t" + getTipoVariavelTexto(strTipoVar) + " " + ident_registro.getText() + "[99];\n");
                    } else {
                        saida.append("\t" + getTipoVariavelTexto(strTipoVar) + " " + ident_registro.getText() + ";\n");
                    }
                }
            }
            saida.append("}" + ctx.IDENT().getText() + ";\n");
            registro.put(ctx.IDENT().getText(), variaveis_registro);
        }
        return null;
    }
    
    @Override
    public Void visitParametros(LAParser.ParametrosContext ctx){
        for (LAParser.ParametroContext parametro : ctx.parametro()) {
            String strTipoVar = parametro.tipo_estendido().getText();
            for (LAParser.IdentificadorContext ident : parametro.identificador()) {
                tabela.adicionar(ident.getText(), getTipoVariavelLA(parametro.tipo_estendido().getText()));
                if (getTipoVariavelTexto(strTipoVar).equals("char")) {
                    saida.append(getTipoVariavelTexto(strTipoVar) + "* " + ident.getText() + ",");
                } else {
                    saida.append(getTipoVariavelTexto(strTipoVar) + " " + ident.getText() + ",");
                }
            }
            saida.deleteCharAt(saida.length()-1);
        }
        return null;
    }
    
    //Quando o comando Leia for encontrado, deve ser gerado o SCANF com o respectivo receptor
    //Assim, a função getTipoInOut é chamada para determinar
    @Override 
    public Void visitCmdLeia(LAParser.CmdLeiaContext ctx) {
        saida.append("scanf(\"");
        for (LAParser.IdentificadorContext ident : ctx.identificador()) { 
            if (ident.dimensao() != null) {
                saida.append(getTipoInOut(tabela.verificar(ident.IDENT(0).getText())) + " ");
            } else {
                saida.append(getTipoInOut(tabela.verificar(ident.getText())) + " ");
            }
        }
        saida.deleteCharAt(saida.length()-1);
        saida.append("\"");
        for (LAParser.IdentificadorContext ident : ctx.identificador()) { 
            saida.append(", &" + ident.getText());
        }
        saida.append(");\n");

        return null;
    }

    //Assim como o caso assima, o PRINTF deve ser gerado corretamente
    @Override 
    public Void visitCmdEscreva(LAParser.CmdEscrevaContext ctx) {
        saida.append("printf(\"");
        for (LAParser.ExpressaoContext expressao : ctx.expressao()) {
            saida.append(getTipoInOut(LASemanticoUtils.verificarTipo(tabela, expressao)));
        }
        saida.append("\",");
        for (LAParser.ExpressaoContext expressao : ctx.expressao()) { 
            saida.append(expressao.getText() + ", ");
        }
        saida.delete(saida.length()-2, saida.length());
        saida.append(");\n");
        return null;
    }
    
    //O IF depende da quantidade de comandos . Se for 0, apensar o IF é criado, sem o ELSE
    //Se o número de comandos for maior, ELSE's aninhados são criados
    @Override 
    public Void visitCmdSe(LAParser.CmdSeContext ctx) {
        saida.append("if( ");
        visitExpressao(ctx.expressao());
        saida.append(" ){\n");
        int comandos = 0;
        for (LAParser.CmdContext comando : ctx.cmd()) {
            if (comandos > 0) {
                saida.append("}else{\n");
            }
            visitCmd(comando);
            comandos++;
        }
        saida.append("}\n");
        return null;
    }
    
    //No caso do SWITCH a lógica é a mesma. Onde a quantidade de casos depende do tamanho da lista de NUM_INT
    //Assim que os comandos terminarem, o DEFAULT é chamado, com seu respectivo BREAK
    @Override 
    public Void visitCmdCaso(LAParser.CmdCasoContext ctx) {
        saida.append("switch(" + ctx.exp_aritmetica().getText() + "){\n");
        for (LAParser.Item_selecaoContext selecao : ctx.selecao().item_selecao()) {
            for (LAParser.Numero_intervaloContext intervalo : selecao.constantes().numero_intervalo()) {
                if (intervalo.NUM_INT().size() > 1) {
                    for (int i = Integer.parseInt(intervalo.NUM_INT(0).getText()); i <= Integer.parseInt(intervalo.NUM_INT(1).getText()); i++ ) {
                        saida.append("\tcase " + i + ":\n");
                    }
                } else {
                    saida.append("\tcase " + intervalo.NUM_INT(0).getText() + ":\n");
                }
            }
            for (LAParser.CmdContext cmd : selecao.cmd()) {
                saida.append("\t\t");
                visitCmd(cmd);
                saida.append("\t\tbreak;\n");
            }
        }
        if (ctx.cmd() != null) {
            saida.append("\tdefault:\n\t\t");
            ctx.cmd().forEach(cmd -> visitCmd(cmd));
            saida.append("\t\tbreak;\n");
        }
        saida.append("};\n");
        return null;
    }
    
    //Todos os FOR se baseiam na lógica do menor-ou-igual, diminuindo o código
    @Override 
    public Void visitCmdPara(LAParser.CmdParaContext ctx) {
        saida.append("for(" + ctx.IDENT().getText() + " = " + ctx.exp_aritmetica(0).getText() + "; " + ctx.IDENT().getText() + " <= " + 
            ctx.exp_aritmetica(1).getText() + "; " + ctx.IDENT().getText() + "++){\n");
        ctx.cmd().forEach(cmd -> visitCmd(cmd));
        saida.append("}\n");

        return null;
    }
    
    //While depende da expressão, apenas a sintaxe básica é colocada aqui, o restante
    //depende da visita na lista de expressões e na lista de comandos
    @Override 
    public Void visitCmdEnquanto(LAParser.CmdEnquantoContext ctx) {
        saida.append("while(");
        visitExpressao(ctx.expressao());
        saida.append("){\n");
        ctx.cmd().forEach(cmd -> visitCmd(cmd));
        saida.append("}\n");
        return null;
    }
    
    //A mesma lógica de cima, mas em outra ordem, assim como o WHILE é em C
    @Override 
    public Void visitCmdFaca(LAParser.CmdFacaContext ctx) {
        saida.append("do{\n");
        ctx.cmd().forEach(x -> visitCmd(x));
        saida.append("}while( "); 
        visitExpressao(ctx.expressao());
        saida.append(");\n");
        return null;
    }
    
    //Aqui entra a biblioteca string. A atribuição em C de uma string já declarada
    //depende do strcpy.
    @Override 
    public Void visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        if (ctx.ponteiro != null) {
            saida.append("*");
        }
        if (ctx.identificador().IDENT().size() > 1) {
            if (tabela.verificar(ctx.identificador().getText()) == TipoLA.LITERAL) {
                saida.append("strcpy(" + ctx.identificador().getText() + "," + ctx.expressao().getText() + ");\n");
            } else {
                saida.append(ctx.identificador().getText() + " = ");
                saida.append(ctx.expressao().getText() + ";\n");
            }
        } else {
            saida.append(ctx.identificador().getText() + " = ");
            saida.append(ctx.expressao().getText() + ";\n");
        }
        return null;
    }
    

    @Override 
    public Void visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        saida.append(ctx.IDENT().getText() + "(");
        for (LAParser.ExpressaoContext exp : ctx.expressao()) {
            saida.append(exp.getText() + ", ");
            saida.delete(saida.length()-2, saida.length());
        }
        saida.append(");\n");
        return null;
    }

    @Override 
    public Void visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        saida.append("return ");
        visitExpressao(ctx.expressao());
        saida.append(";\n");
        return null;
    }
    
    //Conversão da expressão lógica OU - OR
    @Override 
    public Void visitExpressao(LAParser.ExpressaoContext ctx) {
        visitTermo_logico(ctx.termo_logico(0));
        for (int i = 0; i < ctx.op_logico_1().size(); i++) {
            saida.append(" || ");
            visitTermo_logico(ctx.termo_logico(i + 1));
        }
        return null;
    }
    
    //Conversão da expressão lógica E - AND
    @Override 
    public Void visitTermo_logico(LAParser.Termo_logicoContext ctx) {
        visitFator_logico(ctx.fator_logico(0));
        for (int i = 0; i < ctx.op_logico_2().size(); i++) {
            saida.append(" && ");
            visitFator_logico(ctx.fator_logico(i + 1));
        }
        return null;
    }

    //Conversão da expressão lógica NÃO - NOT
    @Override 
    public Void visitFator_logico(LAParser.Fator_logicoContext ctx) {
        if (ctx.not != null) {
            saida.append("!");
        }
        visitParcela_logica(ctx.parcela_logica());
        return null;
    }

    //Conversão das expressões lógicas Verdadeiro - TRUE e Falso - FALSE
    @Override 
    public Void visitParcela_logica(LAParser.Parcela_logicaContext ctx) {
        if (ctx.verdadeiro != null) {
            saida.append("true");
        } else if (ctx.falso != null) {
            saida.append("false");
        }
        visitExp_relacional(ctx.exp_relacional());
        return null;
    }

    //Conversão da expressão relacional IGUAL e DIFERENTE
    @Override 
    public Void visitExp_relacional(LAParser.Exp_relacionalContext ctx) {
        String aux;
        visitExp_aritmetica(ctx.arit1);
        if (ctx.op_relacional() != null) {
            if (ctx.op_relacional().getText().equals("=")) {
                aux = "==";
            } else if (ctx.op_relacional().getText().equals("<>")) {
                aux = "!=";
            } else {
                aux = ctx.op_relacional().getText();
            }
            saida.append(" " + aux + " ");
            visitExp_aritmetica(ctx.arit2);
        }
        return null;
    }

    @Override 
    public Void visitExp_aritmetica(LAParser.Exp_aritmeticaContext ctx) {
        if (ctx.termo(0).fator(0).parcela(0).parcela_unario().expressao(0) != null) {
            saida.append('(');
            visitExpressao(ctx.termo(0).fator(0).parcela(0).parcela_unario().expressao(0));
            saida.append(')');
        } else {
            saida.append(ctx.getText());
        }
        return null;
    }
    
}
