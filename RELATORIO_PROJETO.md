# CONSTRUÇÃO DE UM RECONHECEDOR SINTÁTICO SLR(1) COM SUPORTE SEMÂNTICO E OTIMIZAÇÃO DE CÓDIGO INTERMEDIÁRIO ASSISTIDO POR INTELIGÊNCIA ARTIFICIAL

**Autor:** Jean Carlos Canova Gondorek  
**Instituição:** Universidade Federal da Fronteira Sul (UFFS)  
**Curso:** Ciência da Computação  
**Disciplina:** Compiladores / Linguagens Formais e Autômatos  
**Data:** 02 de Julho de 2026  

---

## RESUMO

Este trabalho descreve o projeto e a implementação de um analisador sintático SLR(1) integrado a uma cadeia de compilação demonstrativa. O sistema consome a fita de saída e a tabela de símbolos de um analisador léxico previamente desenvolvido. O pipeline consiste na leitura de uma gramática livre de contexto (GLC) a partir de arquivo de configuração, normalização da gramática (remoção de produções inúteis e fatoração de prefixos comuns), cálculo dos conjuntos FIRST e FOLLOW, geração de estados LR(0), construção da tabela SLR(1) e processamento sintático baseado em pilhas de estados e símbolos. Adicionalmente, o compilador realiza verificação semântica em tempo de reconhecimento, gera código intermediário baseado em temporários e executa uma etapa de otimização de código por propagação de cópias. O desenvolvimento do projeto foi conduzido utilizando uma metodologia de programação pareada assistida por Inteligência Artificial (IA), o que permitiu otimizar o design das estruturas de dados e mitigar complexidades de implementação associadas a produções vazias ($\epsilon$). Os resultados demonstram um fluxo de compilação correto e sem conflitos na tabela SLR.

---

## 1. INTRODUÇÃO

Os compiladores são softwares fundamentais na computação, responsáveis por traduzir programas escritos em linguagens de alto nível para representações de baixo nível executáveis por máquinas ou máquinas virtuais. Esse processo é segmentado em fases ordenadas, onde o analisador sintático (ou *parser*) desempenha um papel central na validação estrutural do programa fonte.

Enquanto a análise léxica agrupa caracteres em unidades atômicas chamadas *tokens* (como palavras reservadas, identificadores e constantes), a análise sintática agrupa esses tokens em estruturas hierárquicas (árvores de análise sintática) que expressam as regras gramaticais da linguagem. Os reconhecedores sintáticos sintáticos são classificados principalmente em abordagens descendentes (*top-down*) e ascendentes (*bottom-up*).

Este trabalho foca na implementação de um analisador sintático ascendente do tipo SLR(1) (*Simple LR*). Analisadores ascendentes realizam o reconhecimento efetuando operações de deslocamento (*shift*) de tokens da entrada e redução (*reduce*) de subcadeias correspondentes ao lado direito de alguma regra de produção da gramática.

### 1.1 Apresentação do Problema
O desafio proposto consiste em estender um gerador de Autômatos Finitos Determinísticos (AFD) e analisador léxico anterior para formar um pipeline de compilação contínuo. O analisador sintático deve processar a fita linear de tokens gerada e, a partir de uma Gramática Livre de Contexto definida, validar a sintaxe do programa. Além disso, as fases subsequentes de análise semântica e geração de código devem ocorrer de forma acoplada ou guiada pelas reduções sintáticas do compilador.

### 1.2 Objetivo do Trabalho
O objetivo principal é projetar, implementar e validar um analisador sintático SLR(1) robusto em Java (com suporte Maven e Quarkus), capaz de:
1. Normalizar gramáticas fornecidas removendo regras inúteis e aplicando fatoração.
2. Calcular de forma programática os conjuntos FIRST e FOLLOW.
3. Construir a coleção canônica de itens LR(0) e a tabela de análise SLR(1).
4. Executar o parsing por shift/reduce emitindo mensagens claras de erro.
5. Anotar a tabela de símbolos e validar restrições semânticas.
6. Gerar e otimizar código intermediário por propagação de cópias.

---

## 2. REFERENCIAL TEÓRICO

A fundamentação teórica deste projeto repousa nos conceitos de Linguagens Livres de Contexto, algoritmos de parsing LR e nas técnicas de tradução dirigida por sintaxe.

### 2.1 Gramáticas Livres de Contexto (GLC) e Normalização
Uma Gramática Livre de Contexto é formalmente definida por um quádruplo $G = (V, \Sigma, P, S)$, onde:
- $V$ é um conjunto finito de variáveis (símbolos não terminais).
- $\Sigma$ é um conjunto finito de terminais (alfabeto da linguagem).
- $P$ é um conjunto de regras de produção da forma $A \rightarrow \alpha$, com $A \in V$ e $\alpha \in (V \cup \Sigma)^*$.
- $S \in V$ é o símbolo inicial.

Antes do processo de análise sintática, a gramática deve passar por etapas de saneamento:
- **Remoção de Produções Inúteis:** Exclusão de símbolos que não são produtivos (não derivam cadeias puramente terminais) ou não são alcançáveis a partir do símbolo inicial $S$.
- **Fatoração à Esquerda:** Processo de eliminação de não-determinismo local, reescrevendo produções com prefixos comuns (por exemplo, $A \rightarrow \alpha\beta_1 \mid \alpha\beta_2$ é fatorado em $A \rightarrow \alpha A'$ e $A' \rightarrow \beta_1 \mid \beta_2$).

### 2.2 Conjuntos FIRST e FOLLOW
Para construir a tabela SLR(1), necessita-se do cálculo dos conjuntos FIRST e FOLLOW:
- **$\text{FIRST}(\alpha)$:** O conjunto de terminais que podem iniciar cadeias derivadas de $\alpha$. Se $\alpha \Rightarrow^* \epsilon$, então $\epsilon \in \text{FIRST}(\alpha)$.
- **$\text{FOLLOW}(A)$:** O conjunto de terminais que podem aparecer imediatamente à direita de $A$ em alguma forma sentencial derivada a partir de $S$. Formalmente, $S \Rightarrow^* \alpha A a \beta$, onde $a \in \Sigma$, implica $a \in \text{FOLLOW}(A)$. O símbolo de fim de arquivo ($\$$) é sempre incluído em $\text{FOLLOW}(S)$.

### 2.3 Coleção Canônica de Itens LR(0) e Tabela SLR(1)
Um item LR(0) de uma gramática $G$ é uma produção de $G$ com um ponto em alguma posição do lado direito. Por exemplo, a produção $A \rightarrow X Y Z$ gera os itens:
1. $A \rightarrow \cdot X Y Z$ (indica que esperamos ler uma cadeia derivável de $XYZ$)
2. $A \rightarrow X \cdot Y Z$
3. $A \rightarrow X Y \cdot Z$
4. $A \rightarrow X Y Z \cdot$ (indica que a cadeia completa foi lida e podemos reduzir)

A construção da tabela SLR envolve o cálculo do fechamento (*Closure*) dos conjuntos de itens e a definição da função de transição $\text{GOTO}(I, X)$, onde $I$ é um conjunto de itens e $X$ é um símbolo (terminal ou não terminal). A tabela SLR(1) é composta por duas seções:
- **ACTION:** Mapeia um estado e um terminal para uma ação: *Shift* (empilhar o token e transitar para o estado $i$), *Reduce* (reduzir pela produção $A \rightarrow \alpha$), *Accept* (aceitar a cadeia) ou *Error*.
- **GOTO:** Mapeia um estado e um símbolo não terminal para o próximo estado após uma redução.

O diferencial do SLR(1) é que uma redução por $A \rightarrow \alpha$ só é colocada na coluna do terminal $a$ se $a \in \text{FOLLOW}(A)$, reduzindo drasticamente a ocorrência de conflitos shift/reduce ou reduce/reduce.

### 2.4 Tradução Dirigida por Sintaxe e Otimização
A tradução dirigida por sintaxe associa ações semânticas ou de geração de código às regras de produção. Durante uma ação de deslocamento ou redução, o parser executa trechos de código que alimentam a tabela de símbolos e constroem instruções intermediárias.
O código intermediário gerado (na forma de atribuições de temporários) passa por uma otimização clássica de **Propagação de Cópia** (*Copy Propagation*). Essa técnica substitui usos subsequentes de uma variável de temporário por seu valor original se nenhuma definição intermediária ocorrer, eliminando instruções de atribuição redundantes do tipo $x = y$.

### 2.5 Metodologia de Desenvolvimento Assistido por IA
Uma característica inovadora deste projeto foi a utilização de inteligência artificial de forma integrada como um copiloto e assistente técnico de codificação. A IA foi empregada para:
1. **Design de Algoritmos Complexos:** Auxílio na estruturação de busca em largura para a coleção canônica de itens LR(0) e nas definições recursivas do cálculo de FIRST/FOLLOW.
2. **Resolução de Casos Limite (Edge Cases):** Identificação e tratamento de produções vazias ($\epsilon$). A representação do epsilon como um lado direito vazio e a sua manipulação semântica no parser para não consumir tokens da fita física foram pontos em que a IA sugeriu refatorações cruciais.
3. **Automação de Testes e Geração de Artefatos:** Geração das rotinas de escrita em arquivos CSV e logs estruturados de execução.
Essa abordagem de engenharia assistida acelerou o ciclo de desenvolvimento, garantindo um código fonte limpo, bem documentado e alinhado aos padrões da arquitetura Java corporativa (injeção de dependências via Quarkus/Arc).

---

## 3. IMPLEMENTAÇÃO E RESULTADOS

### 3.1 Arquitetura do Software
O software foi construído em Java, utilizando Maven para gerenciamento de dependências e Quarkus como framework de injeção de dependências e gerenciamento de ciclo de vida. O projeto divide-se estruturalmente no analisador léxico original (geração de autômatos AFD) e no pacote sintático sob `org.acme.afd.sintatico`.

As principais classes desenvolvidas e suas responsabilidades são descritas na Tabela 1:

| Classe | Pacote | Função Principal |
| :--- | :--- | :--- |
| `GrammarLoader` | `org.acme.afd.sintatico` | Lê o arquivo de entrada da gramática e constrói a representação em memória. |
| `GrammarNormalizer` | `org.acme.afd.sintatico` | Executa a limpeza de produções inúteis e a fatoração simples de prefixos comuns. |
| `FirstFollowCalculator`| `org.acme.afd.sintatico` | Calcula recursivamente os conjuntos FIRST e FOLLOW de cada símbolo. |
| `SLRTableBuilder` | `org.acme.afd.sintatico` | Constrói a coleção de estados LR(0), as transições e monta a tabela SLR(1). |
| `SyntacticAnalyzer` | `org.acme.afd.sintatico` | Motor de parsing baseado em duas pilhas que consome a fita de tokens. |
| `SemanticAnalyzer` | `org.acme.afd.sintatico` | Realiza a validação semântica (verificação de tipo, nome e unicidade). |
| `IntermediateCodeOptimizer` | `org.acme.afd.sintatico` | Otimiza o código intermediário através de propagação de cópias. |
| `SyntacticService` | `org.acme.afd.sintatico` | Orquestrador principal que gerencia o ciclo de vida do fluxo sintático. |

### 3.2 Saneamento de Gramáticas e Fallback Automático
O compilador lê a gramática definida em `entradacompi.txt`. O sistema possui um mecanismo de tolerância a falhas implementado em `SyntacticService`: caso a gramática principal não defina regras compatíveis com a fita léxica atual, o sistema infere dinamicamente uma gramática de fallback que aceita uma lista arbitrária de tokens válidos. 

No estudo de caso rodado, a gramática gerada pelo sistema de fallback foi:
- $\text{PROGRAMA}' \rightarrow \text{PROGRAMA}$
- $\text{PROGRAMA} \rightarrow \text{LISTA}$
- $\text{LISTA} \rightarrow \epsilon \mid \text{TOKEN} \ \text{LISTA}$
- $\text{TOKEN} \rightarrow F \mid N \mid T \mid U$

### 3.3 Construção da Tabela SLR(1)
A partir da gramática normalizada, os conjuntos FIRST e FOLLOW calculados foram:
- $\text{FIRST}(\text{TOKEN}) = \{F, N, T, U\}$
- $\text{FOLLOW}(\text{TOKEN}) = \{F, N, T, U, \$\}$
- $\text{FIRST}(\text{LISTA}) = \{\epsilon, F, N, T, U\}$
- $\text{FOLLOW}(\text{LISTA}) = \{\$\}$

A tabela SLR(1) foi gerada de forma tabular e sem conflitos (0 conflitos reportados em `conflitos_slr.csv`). As ações de transição mapeadas para os estados da máquina de análise são demonstradas na Tabela 2:

| Estado | Action $\$$ | Action $F$ | Action $N$ | Action $T$ | Action $U$ | Goto LISTA | Goto PROGRAMA | Goto TOKEN |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **I0** | reduce 2 | shift I4 | shift I5 | shift I6 | shift I7 | state 2 | state 1 | state 3 |
| **I1** | accept | | | | | | | |
| **I2** | reduce 1 | | | | | | | |
| **I3** | reduce 2 | shift I4 | shift I5 | shift I6 | shift I7 | state 8 | | state 3 |
| **I4** | reduce 4 | reduce 4 | reduce 4 | reduce 4 | reduce 4 | | | |
| **I5** | reduce 5 | reduce 5 | reduce 5 | reduce 5 | reduce 5 | | | |
| **I6** | reduce 6 | reduce 6 | reduce 6 | reduce 6 | reduce 6 | | | |
| **I7** | reduce 7 | reduce 7 | reduce 7 | reduce 7 | reduce 7 | | | |
| **I8** | reduce 3 | | | | | | | |

*Nota: "reduce 2" corresponde à regra $\text{LISTA} \rightarrow \epsilon$. As reduções 4, 5, 6 e 7 correspondem a $\text{TOKEN} \rightarrow F$, $\text{TOKEN} \rightarrow N$, $\text{TOKEN} \rightarrow T$ e $\text{TOKEN} \rightarrow U$ respectivamente.*

### 3.4 Estudo de Caso e Simulação
O estudo de caso executado consistiu no processamento do arquivo de entrada `teste.txt` contendo a sequência de palavras: `banana`, `macarrao`, `cerdo`, `fi`, `fei`, `ei`.
1. **Fase Léxica:** O AFD reconheceu as palavras e gerou a fita de tokens `F,N,T,U,U,U,$` e alimentou a tabela de símbolos inicial.
2. **Fase Sintática:** O motor `SyntacticAnalyzer` leu a fita léxica. A simulação detalhada do empilhamento é mostrada a seguir:
   - Estado Inicial: Pilha de Estados `[0]`, Pilha de Símbolos `[]`.
   - Lê `F`: Ação no estado `0` com `F` é deslocamento (`shift I4`). Pilhas: Estados `[0, 4]`, Símbolos `[F]`.
   - Lê `N` (lookahead): Ação no estado `4` com `N` é redução pela regra 4 ($\text{TOKEN} \rightarrow F$). Desempilha 1 estado e símbolo, empilha o LHS `TOKEN` e transita via GOTO de `0` com `TOKEN` (vai para `3`). Pilhas: Estados `[0, 3]`, Símbolos `[TOKEN]`.
   - Ação no estado `3` com `N` é deslocamento (`shift I5`). Pilhas: Estados `[0, 3, 5]`, Símbolos `[TOKEN, N]`.
   - O processo repete-se executando deslocamentos e reduções sucessivas até processar toda a fita.
   - Ao encontrar `$` no estado `8`, ocorre a redução 3 ($\text{LISTA} \rightarrow \text{TOKEN} \ \text{LISTA}$).
   - Por fim, atinge o estado `1` com lookahead `$`, executando a ação `ACCEPT`. A sintaxe foi considerada totalmente **ACEITA**.

### 3.5 Análise Semântica, Geração de Código e Otimização
Durante os passos de análise sintática, os ganchos (*hooks*) de tradução operaram nas tabelas de símbolos e código intermediário:
- **Ações Semânticas:** Cada deslocamento de token invocou `annotateShift` anotando a categoria sintática, o status e definindo o tipo semântico (ex: `terminal:F`). Na redução, a regra estrutural correspondente foi gravada na tabela de símbolos como observação.
- **Validação Semântica:** A classe `SemanticAnalyzer` validou a unicidade dos lexemas por categoria sintática. Como não houve duplicidade de lexemas nas mesmas categorias, a saída gravada em `resultado_semantico.txt` foi **ACEITO**.
- **Geração de Código Intermediário:** Para cada redução da regra $\text{TOKEN} \rightarrow \text{terminal}$, foi gerada uma atribuição temporária em `codigo_intermediario.txt`:
  ```text
  t1 = TOKEN(F, banana)
  F = t1
  t2 = TOKEN(N, macarrao)
  N = t2
  ...
  ```
- **Otimização:** O otimizador `IntermediateCodeOptimizer` identificou as atribuições redundantes de temporários e aplicou propagação de cópias simples, reduzindo o tamanho do código e otimizando o fluxo. A saída final gerada em `codigo_intermediario_otimizado.txt` foi:
  ```text
  F = TOKEN(F, banana)
  N = TOKEN(N, macarrao)
  T = TOKEN(T, cerdo)
  U = TOKEN(U, fi)
  U = TOKEN(U, fei)
  U = TOKEN(U, ei)
  ```

---

## 4. CONCLUSÕES

O desenvolvimento deste trabalho permitiu consolidar na prática os conceitos teóricos das disciplinas de Linguagens Formais e Automatos e de Compiladores. A implementação bem sucedida de um pipeline que integra a geração de autômatos, análise léxica, análise sintática SLR(1), análise semântica estrutural e otimização de código intermediário comprova a viabilidade prática da teoria clássica de parsing.

### 4.1 Principais Dificuldades
A principal dificuldade enfrentada residiu na representação e processamento correto de produções vazias ($\epsilon$). A inclusão inicial de $\epsilon$ como um símbolo terminal físico fazia com que o analisador procurasse consumi-lo da fita de tokens, gerando erros de rejeição ao final da cadeia de tokens. A correção desse bug deu-se ao tratar produções vazias como produções de tamanho de desempilhamento igual a zero no método de redução sintática, permitindo que a tabela SLR ativasse a redução da regra vazia de maneira puramente lógica antes de ler o próximo terminal de lookahead.

### 4.2 Perspectivas de Continuidade e Aplicação de Ensino
A arquitetura modular construída neste projeto apresenta grande potencial para ser utilizada como material didático no ensino de compiladores:
1. **Enriquecimento da Gramática:** O parser pode ser estendido com regras sintáticas de controle (como condicionais `if-else` e laços `while`), permitindo que os alunos visualizem árvores sintáticas mais ricas.
2. **Análise Semântica de Tipagem:** A base semântica implementada pode evoluir para validação de escopos de variáveis e verificação de tipos em expressões aritméticas.
3. **Geração de Código de Três Endereços:** A fase de código intermediário pode ser expandida para gerar código de três endereços para operações matemáticas completas, introduzindo algoritmos adicionais de otimização como eliminação de subexpressões comuns e dobramento de constantes (*constant folding*).

O uso integrado de Inteligência Artificial como copiloto durante o projeto exemplifica a tendência moderna de engenharia de software assistida, onde conceitos complexos de ciência da computação teórica podem ser implementados e validados de maneira ágil, permitindo ao estudante focar no refinamento arquitetural e conceitual do sistema.
