# PolySecure

**Sistema polystore com segurança integrada, linguagem SQL-Poly e otimizador de consultas com modelo de custo aprendido.**

Desenvolvido por **Jóvio Luiz Giacomolli** como uma continuação do TCC *"Integração de Dados Heterogêneos: Uma Análise dos Sistemas Polystores"* (UFFS, 2024).

> **Nota sobre o desenvolvimento:** Este projeto utiliza **Claude Code (Anthropic)** como assistente de IA em todo o ciclo de desenvolvimento — geração de código, revisão arquitetural, implementação de testes e documentação. O uso de IA é intencional e faz parte da metodologia de desenvolvimento adotada.

---

## O que é o PolySecure

Um **polystore** é um sistema que integra múltiplos bancos de dados heterogêneos (relacionais, de documentos, de grafos, de busca) por meio de uma interface de consulta unificada. O usuário escreve uma única query e o sistema a decompõe, executa em cada banco e consolida os resultados.

O PolySecure resolve lacunas identificadas na análise de 16 sistemas polystore existentes:

| Lacuna identificada | PolySecure |
|---|---|
| Segurança e privacidade integrada (ausente em **100%** dos sistemas analisados) | ✅ JWT + RBAC + anonimização LGPD |
| DDL completo cross-store | ✅ `CREATE/DROP POLYSTORE TABLE` |
| DML completo cross-store | ✅ `INSERT/UPDATE/DELETE` multi-store |
| Otimizador com modelo de custo aprendido | ✅ Regressão linear online (inspirado no AWESOME/UCSD) |
| Adaptadores plugáveis em runtime | ✅ `POST /stores` + UI no frontend |

---

## Stack tecnológica

| Camada | Tecnologia |
|---|---|
| Backend | Spring Boot 3.2 · Java 21 · Maven |
| Parser SQL-Poly | ANTLR 4.13.1 |
| Frontend | Angular 21 · CodeMirror 6 · TypeScript |
| Bancos suportados | PostgreSQL · MongoDB · Neo4j |
| Segurança | JJWT 0.12.6 · BCrypt · Spring Security |
| Testes | JUnit 5 · Testcontainers · AssertJ |
| Infraestrutura | Docker Compose |

---

## Arquitetura

```
REST API          → QueryController · StoreController · AdminController
Parser            → SqlPoly.g4 (ANTLR 4) → SqlPolyVisitorImpl → Statement (sealed)
Engine            → QueryEngine (dispatch + join ordering)
                     └─ DdlExecutor · DmlExecutor
Optimizer         → CostEstimator → StatsRegistry (TTL cache) + LinearCostModel (gradiente online)
Cache             → MaterializedViewCache — resultados com auto-invalidação por store
Catalog           → StoreRegistry (adaptadores) + MetadataCatalog (esquemas)
Adapters          → PostgresAdapter · MongoAdapter · Neo4jAdapter
Transactions      → TransactionCoordinator — sequencial + paralelo (virtual threads Java 21)
Security          → JwtFilter → PermissionEvaluator (RBAC) → AnonymizationEngine → AuditLogger
```

---

## SQL-Poly — exemplos

```sql
-- SELECT cross-store (PostgreSQL + MongoDB)
SELECT u.name, p.bio
FROM pg.users u
JOIN mg.profiles p ON u.id = p.user_id
WHERE u.active = true;

-- DDL distribuído
CREATE POLYSTORE TABLE orders (
  id       INT      STORE pg PRIMARY KEY,
  detalhes DOCUMENT STORE mg,
  grafo    GRAPH    STORE neo
);

-- DML cross-store
INSERT INTO POLYSTORE orders
  pg (id, total)    VALUES (1, 150.0)
  mg (id, detalhes) VALUES (1, 'descricao');

UPDATE POLYSTORE orders
  SET pg.total = 199.9, mg.detalhes = 'atualizado'
  WHERE id = 1;

DELETE FROM POLYSTORE orders WHERE id = 1;
```

---

## Status de implementação

### ✅ Fase 1 — Consultas SELECT (concluída em 19/05/2026)
SELECTs single-store e cross-store, JOINs (PostgreSQL + MongoDB), WHERE, projeção de colunas, aliases. Bind-join em memória com predicate push-down por adapter.

### ✅ Fase 2 — DDL/DML completo (concluída em 19/05/2026)
`CREATE/DROP POLYSTORE TABLE`, `INSERT/UPDATE/DELETE` cross-store, adapter Neo4j, `MetadataCatalog`, `TransactionCoordinator` com rollback best-effort.

### ✅ Fase 3 — Segurança (concluída em 21/05/2026)
Autenticação JWT (JJWT 0.12.6), RBAC com permissões por store, anonimização de PII (conforme LGPD), audit logging. Endpoints: `POST /auth/login`, `POST /admin/users`, `GET /admin/audit`. Suite de 9 testes de integração.

### ✅ Fase 4 — Otimizador (concluída em 31/05/2026)
Estimativa de cardinalidade por adapter (`estimateCardinality`), `StatsRegistry` (cache TTL 60s), `LinearCostModel` (gradiente online, inspirado no AWESOME/UCSD 2021), `CostEstimator`. Reordenamento de JOINs por cardinalidade ascendente. DML paralelo com virtual threads Java 21. Endpoint `GET /admin/stats`.

### 🔄 Fase 5 — Adaptadores dinâmicos + segurança avançada (planejada)
RBAC por coluna, refresh tokens, adaptadores para MySQL/MariaDB, SQL Server, Oracle, Cassandra, Elasticsearch, ClickHouse e outros.

---

## Como executar

### Pré-requisitos
- Java 21+
- Node.js 20+
- Docker e Docker Compose

### Backend

```bash
cd polysecure/backend

# Subir os bancos de dados
docker-compose up -d

# Rodar a API (http://localhost:8080)
mvn spring-boot:run

# Rodar os testes
mvn test
```

### Frontend

```bash
cd polysecure/frontend

# Instalar dependências (apenas na primeira vez)
npm install

# Rodar o servidor de desenvolvimento (http://localhost:4200)
npm start
```
---

## Endpoints principais

| Método | Endpoint | Descrição | Auth |
|---|---|---|---|
| POST | `/auth/login` | Autenticação — retorna JWT | Não |
| POST | `/query` | Executa SQL-Poly | Sim |
| POST | `/query/standard` | Executa SQL padrão (PostgreSQL) | Sim |
| POST | `/query/translate` | Traduz SQL padrão para SQL-Poly | Sim |
| GET | `/stores` | Lista stores registradas | Sim |
| POST | `/stores` | Registra nova store | Sim |
| DELETE | `/stores/{name}` | Remove uma store | Sim |
| GET | `/catalog/stores` | Stores no catálogo | Sim |
| GET | `/catalog/tables` | Tabelas e esquemas | Sim |
| POST | `/admin/users` | Cria usuário | Sim (admin) |
| GET | `/admin/audit` | Log de auditoria | Sim (admin) |
| GET | `/admin/stats` | Pesos do modelo de custo + cardinalidades | Sim (admin) |

---

## Estrutura do repositório

```
polysecure/
├── backend/                        # Spring Boot — API e motor de consultas
│   ├── src/main/java/com/polysecure/
│   │   ├── api/                    # Controllers REST
│   │   ├── parser/                 # SqlPoly.g4 (ANTLR) + Visitor
│   │   ├── engine/                 # QueryEngine, CostEstimator, StatsRegistry, LinearCostModel
│   │   ├── adapter/                # StoreAdapter + implementações (Postgres, MongoDB, Neo4j)
│   │   ├── catalog/                # StoreRegistry, MetadataCatalog
│   │   ├── model/                  # Statement, Expr, Condition (sealed interfaces)
│   │   └── security/               # auth/, rbac/, anonymization/, audit/
│   └── src/test/                   # Testes unitários e de integração (Testcontainers)
└── frontend/                       # Angular 21 — interface web
    └── src/app/
        ├── components/             # login, navbar, sidebar, editor-panel, results-panel, help-modal
        ├── services/               # AuthService, CatalogService, QueryService
        ├── interceptors/           # authInterceptor (JWT)
        └── models/                 # types.ts

proposta_tecnica_novo_polystore.md  # Proposta técnica detalhada com roadmap
sugestoes_melhorias_polysecure.md   # Catálogo de melhorias futuras (8 categorias)
TCC_versao_final_entrega.pdf        # TCC base — análise de 16 sistemas polystore
```

---

## Base acadêmica

- **TCC:** *"Integração de Dados Heterogêneos: Uma Análise dos Sistemas Polystores"* — Jóvio Luiz Giacomolli, UFFS, 2024
- **Modelo de custo:** inspirado em *"Processing Analytical Queries in the AWESOME Polystore"* (UCSD, 2021)
- **Bind-join:** estratégia validada em *"CloudMdSQL"* (Kolev et al., IEEE Big Data 2016) — 59–273× de melhoria em baixa seletividade
- **Taxonomia:** baseada em Kiehn et al. (Hamburg/VLDB 2022) e Pereira et al. — padrão DAO Compound Mediator
- **Ausência de segurança em polystores:** documentada por Kiehn et al. e Vogt et al. (Basel) — motivação central do projeto

---

## Uso de Inteligência Artificial

O desenvolvimento do PolySecure adota **Claude Code (Anthropic)** como ferramenta central de desenvolvimento assistido por IA. O assistente participa ativamente de:

- Geração e refatoração de código (backend Java e frontend Angular)
- Revisão de decisões arquiteturais com base na literatura acadêmica
- Escrita e execução de testes de integração
- Documentação técnica e de API
- Análise de trade-offs entre abordagens de implementação

Todas as decisões de design, validação de resultados e direcionamento do projeto são de responsabilidade do autor. A IA atua como par de programação, não como substituto do julgamento técnico.

---

## Licença

Este projeto é de uso acadêmico e de pesquisa. Para outros usos, entre em contato com o autor.
**Autor:** Jóvio Luiz Giacomolli — [jovioluizg@gmail.com](mailto:jovioluizg@gmail.com)
