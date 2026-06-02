# Guia de Teste Manual — PolySecure

## Pré-requisito: subir os bancos

### Opção A — Docker Desktop (recomendado)

1. Instale o [Docker Desktop para Windows](https://www.docker.com/products/docker-desktop/)
2. Dentro de `polysecure/`, execute:

```powershell
docker-compose up -d
```

Isso sobe PostgreSQL (5432), MongoDB (27017) e Neo4j (7474/7687).

Verificar se estão rodando:
```powershell
docker-compose ps
```

### Opção B — Bancos já instalados localmente

Se você já tiver PostgreSQL e/ou MongoDB instalados, ajuste os valores de `host`, `port`,
`username` e `password` nas chamadas de registro abaixo.

---

## Iniciar o PolySecure

```powershell
cd polysecure
mvn spring-boot:run
```

O servidor sobe em `http://localhost:8080`.

---

## Roteiro de testes

Use **PowerShell** (comandos abaixo) ou **Postman/Insomnia** apontando para `http://localhost:8080`.

---

### 1. Registrar os stores

```powershell
# PostgreSQL
Invoke-RestMethod -Method POST http://localhost:8080/stores `
  -ContentType "application/json" `
  -Body '{"name":"pg","type":"POSTGRES","host":"localhost","port":5432,"database":"polysecure","username":"poly","password":"poly123"}'

# MongoDB
Invoke-RestMethod -Method POST http://localhost:8080/stores `
  -ContentType "application/json" `
  -Body '{"name":"mg","type":"MONGODB","host":"localhost","port":27017,"database":"polysecure","username":null,"password":null}'

# Neo4j
Invoke-RestMethod -Method POST http://localhost:8080/stores `
  -ContentType "application/json" `
  -Body '{"name":"neo","type":"NEO4J","host":"localhost","port":7687,"database":"neo4j","username":null,"password":null}'
```

Verificar stores registrados:
```powershell
Invoke-RestMethod http://localhost:8080/stores
```

---

### 2. DDL — Criar tabela polystore

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{
    "sql": "CREATE POLYSTORE TABLE pedidos (id INT STORE pg PRIMARY KEY, detalhes DOCUMENT STORE mg, conexoes GRAPH STORE neo)"
  }'
```

**Resposta esperada:**
```json
{"operation":"CREATE","table":"pedidos","message":"Table ''pedidos'' created in stores: pg, mg, neo"}
```

---

### 3. DML — Inserir dados

```powershell
# Inserir pedido 1
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{
    "sql": "INSERT INTO POLYSTORE pedidos pg (id, cliente, total) VALUES (1, ''Alice'', 150.0) mg (id, itens) VALUES (1, ''notebook'')"
  }'

# Inserir pedido 2
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{
    "sql": "INSERT INTO POLYSTORE pedidos pg (id, cliente, total) VALUES (2, ''Bob'', 89.9) mg (id, itens) VALUES (2, ''mouse'')"
  }'
```

**Verificar no PostgreSQL** (via psql ou DBeaver):
```sql
SELECT * FROM pedidos;
-- id | cliente | total
-- 1  | Alice   | 150.0
-- 2  | Bob     | 89.9
```

**Verificar no MongoDB** (via Compass ou mongosh):
```javascript
db.pedidos.find()
// [{ id: 1, itens: "notebook" }, { id: 2, itens: "mouse" }]
```

---

### 4. SELECT — Query num único store

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "SELECT * FROM pg.pedidos"}'
```

**Com filtro WHERE:**
```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "SELECT * FROM pg.pedidos WHERE total > 100.0"}'
```

---

### 5. SELECT — Query cross-store (JOIN)

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{
    "sql": "SELECT p.cliente, p.total, m.itens FROM pg.pedidos p JOIN mg.pedidos m ON p.id = m.id"
  }'
```

**Resposta esperada:**
```json
{
  "count": 2,
  "rows": [
    {"cliente": "Alice", "total": 150.0, "itens": "notebook"},
    {"cliente": "Bob",   "total": 89.9,  "itens": "mouse"}
  ]
}
```

---

### 6. UPDATE — Atualizar em múltiplos stores

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{
    "sql": "UPDATE POLYSTORE pedidos SET pg.total = 199.9, mg.itens = ''notebook pro'' WHERE id = 1"
  }'
```

**Verificar a atualização:**
```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "SELECT p.cliente, p.total, m.itens FROM pg.pedidos p JOIN mg.pedidos m ON p.id = m.id WHERE p.id = 1"}'
```

---

### 7. DELETE — Remover de todos os stores

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "DELETE FROM POLYSTORE pedidos WHERE id = 2"}'
```

**Verificar que foi removido do PostgreSQL e MongoDB:**
```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "SELECT * FROM pg.pedidos"}'
# Deve retornar só o pedido 1
```

---

### 8. DDL — Remover a tabela

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "DROP POLYSTORE TABLE pedidos"}'
```

---

### 9. Erro de sintaxe (comportamento esperado)

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "SELECT FROM pg.pedidos"}'
```

**Resposta esperada:**
```json
{"error": "PARSE_ERROR", "message": "Syntax error at line 1:7 — ..."}
```

---

### 10. Store não registrado (comportamento esperado)

```powershell
Invoke-RestMethod -Method POST http://localhost:8080/query `
  -ContentType "application/json" `
  -Body '{"sql": "SELECT * FROM oracle.tabela"}'
```

**Resposta esperada:**
```json
{"error": "INVALID_ARGUMENT", "message": "Store not registered: 'oracle'"}
```

---

## Usando Postman / Insomnia

Importe esta coleção rapidamente:
- **URL base:** `http://localhost:8080`
- **Endpoint único:** `POST /query` com body `{ "sql": "..." }`
- **Gestão de stores:** `GET /stores`, `POST /stores`, `DELETE /stores/{name}`

---

## Verificar logs

Os logs do PolySecure mostram cada query executada:

```
DEBUG c.p.engine.QueryEngine - Executing: SELECT * FROM pg.pedidos
DEBUG c.p.adapter.postgres.PostgresAdapter - SQL: SELECT * FROM pedidos
```

---

## Parar os containers

```powershell
docker-compose down          # para e remove containers
docker-compose down -v       # também remove os volumes (dados)
```
