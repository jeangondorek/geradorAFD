# 🎓 Gerador de Autômato Finito Determinístico (AFD)

> Projeto de Linguagens Formais e Autômatos - UFFS Chapecó

## 📌 Visão Geral

Aplicação Java que gera **Autômatos Finitos Determinísticos (AFD)** a partir de:
- **Tokens**: palavras reservadas, operadores, símbolos especiais
- **Gramáticas Regulares**: em formato BNF (Notação de Backus-Naur)

## ⚙️ Contexto de Compiladores (Análise Léxica)

Neste projeto, avançamos dos conceitos da disciplina de Linguagens Formais e Autômatos direto para a primeira fase da construção de um Compilador: a **Análise Léxica**.

- **O Analisador Léxico:** É o componente que varre o código-fonte caractere por caractere e os agrupa em "Tokens" (lexemas válidos).
- **A Lógica na Prática:** Ele utiliza o **Autômato Finito Determinístico (AFD)** gerado para validar cada palavra. O sistema define `EC = S` (Estado Corrente = Estado Inicial) e então transita `EC = AF[EC, Sb]` para cada símbolo/letra inserida.
- **Estado de Erro (Sumidouro / Lixo):** Se, durante o processamento de uma palavra, não houver transição de estado mapeada no autômato ou a palavra finalizar em um estado de não-aceitação (estado com `final = false`), o analisador joga a palavra para o estado de Erro (Apelidado de `X`). Isso denuncia um **Erro Léxico** no código-fonte.

### 📤 Saídas do Analisador Léxico
Após processar o código com o AFD, a aplicação exporta duas estruturas vitais para um compilador passar para sua próxima fase (a Análise Sintática):

1. **A Fita de Saída (`fita.csv`):** Uma lista linear contendo sequencialmente as classes (Estados) reconhecidas pela leitura do arquivo, sendo finalizada formalmente por `$`, que denota o EOF (End Of File). O Analisador Sintático usa essa fita para ver se o sequenciamento faz sentido na gramática (Ex: `B E H $`).
2. **Tabela de Símbolos (`tabela_simbolos.csv`):** Atua como o "banco de dados" da compilação. Ela rastreia e armazena metadados de cada token lido, tais como a sua **Linha** (onde ocorreu), o seu **Identificador** (Estado onde terminou, ex: `B`) e o seu **Rótulo/Label** (a palavra real escrita no código-fonte, ex: `se` ou `senao`).

## 🎯 Funcionalidades

✅ **Leitura** de tokens e gramáticas de arquivo  
✅ **Geração** de AFND (Autômato Finito Não-Determinístico)  
✅ **Determinização** usando algoritmo de subconjuntos  
❌ **Minimização** com remoção de estados inalcançáveis e mortos (nao foi solicitada implementaçao)
✅ **Adição** de estado de erro (`X`) para transições não mapeadas e fluxo léxico  
✅ **Análise Léxica** de código-fonte customizável utilizando o AFD  
✅ **Geração de Fita de Saída e Tabela de Símbolos**  
✅ **Formatação** em tabelas (.csv)

## 🚀 Início Rápido

### 1. Compilar
```bash
mvn clean compile
```

### 2. Executar
```bash
mvn exec:java -Dexec.mainClass="org.acme.afd.Main"
```

### 3. Resultado Esperado

**No Terminal (saída na tela):**
- ✅ Tokens e Gramáticas lidos do arquivo de configuração
- ✅ **TABELA DO AFND** (Autômato Finito Não-Determinístico)
- ✅ **TABELA DO AFD** (Autômato Finito Determinístico com estado de erro)
- ✅ Processamento sequencial do analisador léxico sobre o arquivo de código-fonte
- ✅ **FITA DE SAÍDA** (Ex: `B E H $`) no console
- ✅ **TABELA DE SÍMBOLOS** impressa

**Arquivos Gerados (raiz do projeto):**
- 📄 `afnd.csv` - Tabela do autômato não-determinístico (AFND) em CSV
- 📄 `afd.csv` - Tabela do autômato determinístico (com estado de erro `X`) em CSV
- 📄 `fita.csv` - O arquivo linear sequencial consumido pelo léxico que o sintático consumiria
- 📄 `tabela_simbolos.csv` - Banco de dados das sentenças que foram reconhecidas e lidas do programa

## 📦 Estrutura

```
geradorAFD/
├── src/main/java/org/acme/afd/
│   ├── Main.java                    # Entrada principal (inicializador do analisador)
│   ├── model/                       # Modelos (State, Transition, Automaton, SymbolTable)
│   ├── parser/                      # Analisador/Parser do arquivo de linguagens
│   ├── generator/                   # Responsável por montar a matriz AFND e caminhos vazios (Epsilon)
│   ├── determinizer/                # Algoritmo de subconjuntos e criação do estado 'X' / Lixo
│   ├── analisadorlexico/            # O Reconhecedor / Lexer do código-fonte utilizando o AFD
│   └── controller/                  # Orquestrador dos fluxos e exportação AFD
├── pom.xml                          # Dependências Maven + Quarkus
├── entradacompi.txt                 # Definição e vocabulário da linguagem 
├── codigo_fonte.txt                 # O seu código que passará pelo Scanner/Lexer
└── Readme.md                        # Documentação
``` 

## 🔄 Fluxo

```
Linguagem Formais (Tokens + Gramáticas)
    ↓
InputParser (lê expressões base)
    ↓
AFNDGenerator (cria as transições AFND baseadas na entrada)
    ↓
Determinizer (AFND → AFD com transições amarradas e estado sumidouro X)
    ↓
Lexical / Reconhecedor (Consome o arquivo 'codigo_fonte.txt' caractere por caractere simulando o AFD)
    ↓
Saídas 
  - Fita Léxica gerada ($)
  - Tabela de Símbolos Gerada (CSV)
```

## 🎯 Exemplo de Entrada

```
se
entao
senao
<S> ::= a<A> | e<A> | i<A> | o<A> | u<A>
<A> ::= a<A> | e<A> | i<A> | o<A> | u<A> | ε
```

## 📊 Exemplo de Saída (Tabela)

```
================================================================================
TABELA DE TRANSIÇÕES - AFD MINIMIZADO
================================================================================
Estado  │  a  │  e  │  i  │  o  │  u  │ (Final)
→ q0    │ q1  │ q2  │ q3  │ q4  │ q5  │ 
q1      │ q6  │ -   │ -   │ -   │ -   │ (se)
q2      │ q7  │ -   │ -   │ -   │ -   │ (entao)
...
ERROR   │ ERR │ ERR │ ERR │ ERR │ ERR │
================================================================================
```

## 📄 Especificações

### Arquivo de Entrada: `entrada.txt`
**Localização:** Raiz do projeto  
**Formato:**
- **Tokens**: uma por linha (ex: `se`, `entao`, `senao`)
- **Gramáticas**: formato BNF `<símbolo> ::= alternativas`
- **Separador**: `|` para alternativas
- **Epsilon**: `ε` para produção vazia

**Exemplo:**
```
se
entao
senao
<S> ::= a<A> | e<A> | i<A> | o<A> | u<A>
<A> ::= a<A> | e<A> | i<A> | o<A> | u<A> | ε
```

---

## 🧠 Projeto 2 — Reconhecedor Sintático SLR

O projeto foi estendido para incluir um **analisador sintático SLR(1)** completo, com análise semântica, geração de código intermediário e otimização.

### 📋 Etapa 1 — Reconhecimento Sintático (70%)

| Componente | Classe | Artefato gerado |
|:---|:---|:---|
| Construção da GLC | `GrammarLoader.java` | `producoes.csv` |
| Eliminação de inúteis + fatoração | `GrammarNormalizer.java` | (gramática normalizada internamente) |
| Cálculo FIRST/FOLLOW | `FirstFollowCalculator.java` | `first_follow.csv` |
| Itens LR(0) e transições | `SLRTableBuilder.java` | `itens_lr0.csv`, `transicoes_lr0.csv` |
| Tabela SLR (ACTION/GOTO) | `SLRTableBuilder.java` | `tabela_slr.csv`, `conflitos_slr.csv` |
| Algoritmo shift/reduce | `SyntacticAnalyzer.java` | aceite ou erros sintáticos |
| Tabela de símbolos estendida | `SymbolTableEntry.java` | `tabela_simbolos.csv` |

**Entrada:** `fita.csv` (fita de saída do léxico)  
**Saída:** aceite sintático ou mensagem(ns) de erro  

### 🔍 Etapa 2 — Análise Semântica (10%)

- **Característica semântica:** unicidade de lexema por categoria sintática + presença de nome e tipo
- **Ações semânticas:** `annotateShift()` e `annotateReduce()` em `SyntacticAnalyzer.java`
- **Validação:** `SemanticAnalyzer.java`
- **Saída:** `resultado_semantico.txt` (ACEITO ou erros semânticos)

### ⚙️ Etapa 3 — Código Intermediário (10%)

- **Geração:** em `SyntacticAnalyzer.generateIntermediateCode()` durante reduções da regra `TOKEN → terminal`
- **Saída:** `codigo_intermediario.txt`

### 🚀 Etapa 4 — Otimização (10%)

- **Estratégia:** propagação de cópias (*copy propagation*) em `IntermediateCodeOptimizer.java`
- **Entrada:** `codigo_intermediario.txt`
- **Saída:** `codigo_intermediario_otimizado.txt`

### 📂 Estrutura do pacote sintático

```
src/main/java/org/acme/afd/sintatico/
├── GrammarLoader.java               # Carrega e parseia a GLC do arquivo de entrada
├── GrammarNormalizer.java            # Remove inúteis + fatoração simples
├── FirstFollowCalculator.java        # Calcula FIRST e FOLLOW por ponto fixo
├── SLRTableBuilder.java              # Gera itens LR(0), transições e tabela SLR
├── SyntacticAnalyzer.java            # Motor shift/reduce + ações semânticas + código intermediário
├── SemanticAnalyzer.java             # Valida característica semântica
├── IntermediateCodeOptimizer.java     # Propagação de cópia
├── SyntacticService.java             # Orquestra todo o pipeline sintático
├── ParserArtifactsWriter.java        # Grava CSVs e arquivos de saída
├── TapeReader.java                   # Lê a fita léxica
├── SymbolTableCsvRepository.java     # Lê/grava tabela de símbolos em CSV
├── Grammar.java                      # Record: representação da GLC
├── Production.java                   # Record: regra de produção
├── LR0Item.java                      # Record: item LR(0)
├── SLRTable.java                     # Record: tabela SLR completa
├── ParsingAction.java                # Record: ação de parsing (shift/reduce/accept)
├── ActionType.java                   # Enum: SHIFT, REDUCE, ACCEPT
├── SyntacticResult.java              # Record: resultado do reconhecimento
└── SemanticResult.java               # Record: resultado da semântica
```

### 📖 Documentação adicional

- **`DOCUMENTACAO_PROJETO.md`** — Relatório técnico completo (Resumo, Introdução, Referencial Teórico, Implementação, Conclusões)
- **`RELATORIO_PROJETO.md`** — Relatório acadêmico formatado para entrega
- **`docs_tour.html`** — Tour visual interativo do projeto (abrir no navegador)
