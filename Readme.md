# 🎓 Gerador de Autômato Finito Determinístico (AFD)

> Projeto de Linguagens Formais e Autômatos - UFFS Chapecó

## 📌 Visão Geral

Aplicação Java que gera **Autômatos Finitos Determinísticos (AFD)** a partir de:
- **Tokens**: palavras reservadas, operadores, símbolos especiais
- **Gramáticas Regulares**: em formato BNF (Notação de Backus-Naur)

## 🎯 Funcionalidades

✅ **Leitura** de tokens e gramáticas de arquivo  
✅ **Geração** de AFND (Autômato Finito Não-Determinístico)  
✅ **Determinização** usando algoritmo de subconjuntos  
❌ **Minimização** com remoção de estados inalcançáveis e mortos (nao foi solicitada implementaçao)
✅ **Adição** de estado de erro para transições não mapeadas  
✅ **Formatação** em tabelas(csv)

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
- ✅ Tokens lidos do arquivo `entrada.txt`
- ✅ Gramáticas lidas do arquivo `entrada.txt`
- ✅ **TABELA DO AFND** (Autômato Finito Não-Determinístico)
- ✅ **TABELA DO AFD** (Autômato Finito Determinístico com estado de erro)
- ✅ Mensagem confirmando criação dos CSVs

**Arquivos Gerados (raiz do projeto):**
- 📄 `AFND.csv` - Tabela do autômato não-determinístico em CSV
- 📄 `AFD.csv` - Tabela do autômato determinístico com estado de erro em CSV

## 📦 Estrutura

```
geradorAFD/
├── src/main/java/org/acme/afd/
│   ├── Main.java                    # Entrada
│   ├── model/                       # Modelos (State, Transition, Automaton)
│   ├── parser/                      # Parser de entrada
│   ├── generator/                   # Gerador de AFND
│   ├── determinizer/                # Algoritmo de determinização com error
│   ├── printer/                     # Formatação de saída
│   └── controller/                  # Orquestrador
├── pom.xml                          # Configuração Maven
├── entrada.txt                      # Arquivo de teste
├── exemplo_entrada.txt              # Arquivo de exemplo de teste
└── Readme.md]                       # arquivo de documentação
``` 

## 🔄 Fluxo

```
Entrada (tokens + gramáticas)
    ↓
InputParser (lê entrada)
    ↓
AFNDGenerator (cria AFND)
    ↓
Determinizer (AFND → AFD + error)
    ↓
AutomatonPrinter (formata saída)
    ↓
Saída (tabelas, HTML, JSON)
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
