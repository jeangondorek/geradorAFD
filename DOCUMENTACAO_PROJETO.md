# Reconhecedor Sintatico SLR para uma Linguagem Hipotetica

**Autor:** Jean Carlos Canova Gondorek  
**Instituicao:** UFFS – Universidade Federal da Fronteira Sul, Chapeco  
**Curso:** Ciencia da Computacao  
**Disciplina:** Compiladores / Linguagens Formais e Automatos

## Resumo

Este trabalho apresenta a implementacao de um reconhecedor sintatico para uma linguagem hipotetica baseada nos tokens gerados pelo analisador lexico do Projeto 1. A solucao constroi uma gramatica livre de contexto, normaliza a gramatica por remocao de producoes inuteis e fatoracao simples, calcula FIRST/FOLLOW, gera o conjunto canonico de itens LR(0), monta uma tabela SLR e executa o reconhecimento sintatico a partir da fita de saida do lexico. Como extensao, o projeto tambem adiciona informacoes semanticas na tabela de simbolos, executa uma analise semantica demonstrativa, gera codigo intermediario durante reducoes sintaticas e aplica uma estrategia de otimizacao sobre esse codigo.

## Introducao

Reconhecedores sintaticos sao componentes centrais em compiladores e interpretadores. Eles recebem como entrada uma sequencia de tokens produzida pelo analisador lexico e verificam se essa sequencia respeita as regras estruturais de uma linguagem. Enquanto o analisador lexico identifica unidades basicas, como identificadores, palavras reservadas, constantes e simbolos, o analisador sintatico organiza essas unidades segundo uma gramatica formal.

O problema abordado neste projeto e construir um analisador sintatico capaz de reconhecer uma linguagem hipotetica definida a partir dos tokens do projeto lexico anterior. O objetivo pratico e demonstrar o fluxo classico de uma etapa de compilacao: leitura da fita lexica, construcao da gramatica, geracao da tabela SLR, reconhecimento sintatico, anotacao da tabela de simbolos, analise semantica, geracao de codigo intermediario e otimizacao.

## Referencial Teorico

Uma gramatica livre de contexto, ou GLC, e definida por um conjunto de terminais, nao terminais, producoes e um simbolo inicial. Em compiladores, GLCs sao usadas para representar a sintaxe de linguagens de programacao. Antes da construcao do parser, e comum normalizar a gramatica, removendo simbolos inuteis e aplicando fatoracao quando ha alternativas com prefixos comuns.

O conjunto FIRST indica quais terminais podem iniciar sentencas derivadas de um simbolo ou sequencia de simbolos. O conjunto FOLLOW indica quais terminais podem aparecer imediatamente apos um nao terminal em alguma forma sentencial. Esses conjuntos sao fundamentais para a construcao de tabelas preditivas e tambem para parsers SLR.

O metodo LR reconhece linguagens por deslocamentos e reducoes. No SLR, constroi-se o conjunto canonico de itens LR(0), calcula-se as transicoes entre estados e usa-se FOLLOW para decidir em quais colunas da tabela devem ocorrer reducoes. A tabela resultante possui acoes do tipo shift, reduce e accept, alem das transicoes GOTO para nao terminais.

A tabela de simbolos armazena informacoes relevantes sobre os tokens reconhecidos. No projeto, ela e usada nao apenas pelo lexico, mas tambem pelas etapas sintatica e semantica. O esquema de traducao ocorre quando acoes sao executadas durante o reconhecimento, por exemplo ao deslocar um token ou reduzir uma producao.

Codigo intermediario e uma representacao entre o codigo fonte e o codigo final. Ele facilita analises e otimizacoes. Neste projeto, a geracao e demonstrativa e ocorre durante reducoes da producao `TOKEN -> terminal`. A otimizacao aplicada e a propagacao simples de copia, removendo temporarios usados apenas para repassar o valor para outro rotulo.

## Implementacao e Resultados

O projeto esta organizado principalmente no pacote `org.acme.afd.sintatico`. O arquivo `GrammarLoader.java` le as producoes da gramatica, trata simbolos entre `< >`, reconhece producoes vazias e acrescenta o simbolo de fim de entrada `$`. Um cuidado importante foi representar `epsilon` como lado direito vazio, e nao como terminal consumivel. Isso evita erro no fim da fita, pois ao encontrar `$` o parser deve reduzir a producao vazia quando a tabela indicar essa reducao.

O arquivo `GrammarNormalizer.java` aplica duas transformacoes da etapa 1. Primeiro remove producoes inuteis, mantendo apenas nao terminais produtivos e alcancaveis. Depois aplica fatoracao simples em alternativas com prefixo comum. O resultado normalizado e usado como entrada para o calculo de FIRST/FOLLOW e para a construcao da tabela SLR.

O arquivo `FirstFollowCalculator.java` calcula os conjuntos FIRST e FOLLOW. O arquivo `SLRTableBuilder.java` constroi os itens LR(0), as transicoes entre estados e a tabela SLR. Os artefatos sao gravados em CSV:

- `producoes.csv`: lista das producoes usadas pelo parser.
- `first_follow.csv`: conjuntos FIRST e FOLLOW.
- `itens_lr0.csv`: itens LR(0) de cada estado.
- `transicoes_lr0.csv`: transicoes entre estados.
- `tabela_slr.csv`: tabela ACTION/GOTO.
- `conflitos_slr.csv`: conflitos encontrados, ou indicacao de ausencia de conflitos.

O reconhecimento sintatico esta em `SyntacticAnalyzer.java`. Ele usa duas pilhas: uma pilha de estados e uma pilha de simbolos. Para cada token da fita, consulta a tabela SLR. Em `shift`, desloca o token, atualiza a tabela de simbolos e avanca a entrada. Em `reduce`, desempilha o lado direito da producao, consulta GOTO para o lado esquerdo e registra a reducao. Em `accept`, retorna aceite sintatico. Em caso de erro, informa o estado atual, o token inesperado e os tokens esperados.

A tabela de simbolos foi estendida com campos especificos para as etapas seguintes:

- `CategoriaSintatica`
- `ObservacaoSintatica`
- `NomeSemantico`
- `TipoSemantico`
- `StatusSemantico`

Na etapa 2, a caracteristica semantica escolhida foi: cada simbolo reconhecido deve possuir nome e tipo semantico, e o mesmo lexema nao deve aparecer duplicado dentro da mesma categoria sintatica. As informacoes sao adicionadas durante o reconhecimento sintatico, por meio das acoes `annotateShift` e `annotateReduce`. A validacao ocorre em `SemanticAnalyzer.java`, e a saida e gravada em `resultado_semantico.txt`.

Na etapa 3, a geracao de codigo intermediario ocorre durante reducoes da regra `TOKEN -> terminal`. Para cada token reconhecido, o parser gera um temporario e associa esse temporario ao rotulo do token. Um exemplo de saida em `codigo_intermediario.txt` e:

```text
t1 = TOKEN(F, banana)
F = t1
```

Na etapa 4, o arquivo `IntermediateCodeOptimizer.java` aplica propagacao simples de copia. Quando encontra uma sequencia do tipo:

```text
t1 = TOKEN(F, banana)
F = t1
```

ela e transformada em:

```text
F = TOKEN(F, banana)
```

O resultado otimizado e gravado em `codigo_intermediario_otimizado.txt`.

Como estudo de caso, a fita gerada pelo lexico foi:

```text
F,N,T,U,U,U,$
```

O parser aceitou a entrada, gerou a tabela SLR sem conflitos, anotou a tabela de simbolos, aceitou a analise semantica, gerou codigo intermediario e produziu codigo otimizado.

## Conclusoes

O projeto implementou um fluxo completo de reconhecimento sintatico SLR integrado ao analisador lexico anterior. Foram construidas as regras sintaticas, a normalizacao da GLC, os conjuntos FIRST/FOLLOW, os itens LR(0), as transicoes, a tabela SLR e o algoritmo de reconhecimento por shift/reduce. Alem disso, a tabela de simbolos foi ampliada para dar suporte a analise semantica, geracao de codigo intermediario e otimizacao.

Uma dificuldade relevante foi o tratamento correto de producoes vazias. Inicialmente, `epsilon` podia ser interpretado como simbolo da gramatica, o que fazia o parser rejeitar a entrada ao chegar em `$`. A correcao foi representar producoes vazias como lado direito vazio, permitindo reducoes corretas no fim da entrada.

Como continuidade, a implementacao poderia evoluir para uma linguagem hipotetica mais rica, com regras para declaracoes, atribuicoes e expressoes aritmeticas. Isso permitiria uma analise semantica mais proxima de compiladores reais, por exemplo verificacao de tipos, escopo de identificadores e inicializacao de variaveis. Tambem seria possivel ampliar a geracao de codigo intermediario para expressoes completas e aplicar otimizacoes adicionais, como eliminacao de codigo morto e dobramento de constantes.

## Pontos Para Arguicao

- A fita de entrada do sintatico vem do lexico e termina com `$`.
- `epsilon` nao e token da fita; ele representa uma producao vazia.
- FIRST/FOLLOW sao usados para decidir reducoes na tabela SLR.
- `shift` consome token; `reduce` reconhece uma estrutura da gramatica.
- A tabela de simbolos recebe informacoes sintaticas e semanticas durante o parsing.
- A etapa semantica valida nome, tipo e duplicidade por categoria.
- O codigo intermediario e gerado durante reducoes.
- A otimizacao remove temporarios redundantes por propagacao de copia.
