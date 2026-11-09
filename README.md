# Sorrisus - Sistema Odontológico

## Sobre o projeto

O **Sorrisus** é um sistema odontológico desenvolvido como parte de um projeto acadêmico, com o objetivo de auxiliar na gestão de clínicas odontológicas. Ele permite o gerenciamento de informações relacionadas a **usuários**, **pacientes**, **dentistas** e **recepcionistas**, centralizando os dados em um único sistema.

O sistema está sendo desenvolvido em **Java com Spring Boot**, utilizando **MySQL** como banco de dados relacional, e segue uma **arquitetura em camadas** (Controller → Service → Repository → Model).

Com a evolução do projeto, o backend agora possui **autenticação completa com JWT**, **rotas protegidas**, **controle de acesso por papéis (roles)** e **middleware de segurança** usando Spring Security.


## 🛡️ Autenticação JWT (nova funcionalidade)

O sistema agora implementa um fluxo completo de autenticação baseado em JWT (**JSON Web Token**).

### Como funciona?

1. O usuário faz login enviando **email + senha** para `/api/auth/login`.
2. O backend valida as credenciais.
3. Se válidas, gera um **token JWT**, assinado com chave secreta.
4. O token precisa ser enviado em todas as requisições protegidas no header:

```

Authorization: Bearer \<token\>

```

### Rotas públicas (sem autenticação)

- `POST /api/auth/login`
- `POST /api/usuarios/cadastro`
- `POST /api/dentistas/cadastro`
- `POST /api/pacientes/cadastro`
- `POST /api/recepcionistas/cadastro`

Todas as demais rotas agora **exigem autenticação**.

### Middleware de segurança

- Filtro JWT (`JwtAuthenticationFilter`) validando tokens a cada request.
- Classe de usuários customizada (`CustomUserDetailsService`).
- Configuração de segurança centralizada (`SecurityConfig`).
- Controle de sessão 100% stateless.

## 🌐 CORS configurado para comunicação com o front-end

O CORS foi configurado globalmente para permitir que o front-end (porta 5173, Vite + React) possa consumir a API:

Origem permitida:
```

http://localhost:5173

```

Métodos liberados:
```

GET, POST, PUT, DELETE, OPTIONS

```

Headers permitidos:
```

Authorization, Content-Type

````

Credenciais habilitadas (para envio de token).

## Funcionalidades atuais

Atualmente, o backend do sistema conta com:

* **Entidades modeladas:**
  * `Usuario`
  * `Paciente`
  * `Dentista`
  * `Recepcionista`
  * `Role` (enum de papéis de usuário)
  * `Agendamento` / `Consulta`
  * `Prontuario` / `Anamnese`

* **CRUD completo** (Create, Read, Update, Delete) para todas as entidades.
* **Autenticação JWT** com filtro, validação e roles.
* **Rotas protegidas por segurança** via Spring Security.
* **Tratamento global de exceções**.
* **Banco MySQL configurado** com criação automática via JPA/Hibernate.
* **Uso de variáveis de ambiente** para dados sensíveis.
* **Projeto configurado com Lombok** e **Spring DevTools**.

## Fluxo de Agendamento, Consulta e Prontuário

O sistema implementa as regras de negócio para **Manter Agenda (UC06)**, **Manter Prontuário (UC10)** e **Preencher Anamnese (UC11)**.

Devido à arquitetura aprimorada, o sistema agora separa logicamente os dados de saúde do paciente (Anamnese) dos dados da visita atual (Prontuário), mas facilita a operação para o Frontend através de um único endpoint.

### 1. Criar um Agendamento
O processo inicia com a reserva de um horário na agenda. O sistema valida se o horário está disponível para o dentista selecionado.

* **Endpoint:** `POST /api/agendamentos`
* **Ação:** Cria uma reserva provisória com status `PENDENTE`.

### 2. Confirmar o Agendamento
Para que o agendamento se torne efetivo, ele deve ser confirmado. Ao confirmar, o sistema **automaticamente gera uma Consulta**.

* **Endpoint:** `POST /api/agendamentos/{id}/confirmar`
* **Regra de Negócio:** O sistema valida conflitos finais, muda o status do agendamento para `CONFIRMADO` e cria um registro na tabela `Consulta` aguardando atendimento.

### 3. Realizar Atendimento (Anamnese + Prontuário)
No momento do atendimento, o dentista preenche a ficha clínica. O backend utiliza um serviço orquestrador (`ProntuarioService`) que recebe um DTO completo e distribui os dados:
* **Dados de Saúde (Anamnese):** (Ex: Alergias, medicamentos) são salvos na entidade `Anamnese` vinculada ao **Paciente**.
* **Dados da Visita (Prontuário):** (Ex: Procedimentos, Odontograma) são salvos na entidade `Prontuario` vinculada à **Consulta**.

* **Endpoint:** `POST /api/prontuarios?consultaId={id}`
* **Payload Unificado (Exemplo):**
    ```json
    {
      "alergiaResposta": "Sim", 
      "alergiaNotas": "Dipirona",
      "problemaSaudeResposta": "Não",
      "observacoes": "Realizada restauração no dente 16.",
      "odontogramaJson": "{ ... dados do desenho ... }"
    }
    ```
* **Vantagem:** O frontend envia apenas uma requisição. Se o paciente já possuir anamnese de consultas anteriores, o sistema apenas a atualiza; caso contrário, cria uma nova.

### 4. Encerrar a Consulta
Nenhum registro é excluído: o encerramento é feito pela mudança de status.

* **Finalizar atendimento** (somente dentista): `POST /api/consultas/{id}/finalizar` → `REALIZADA`. Só é aceito a partir do horário marcado.
* **Não comparecimento** (dentista ou recepcionista): `POST /api/consultas/{id}/nao-compareceu` → `NAO_COMPARECEU`. Só é aceito depois do horário marcado.
* **Cancelamento** (dentista ou recepcionista): `POST /api/agendamentos/{id}/cancelar` → agendamento `CANCELADO`; se ele já estava confirmado, a consulta gerada passa para `CANCELADA`. Horários cancelados ficam livres para novos agendamentos.

Somente consultas `REALIZADA` contam para o cálculo de retornos pendentes.

```
Agendamento: PENDENTE ──confirmar──> CONFIRMADO
                 └──────cancelar──────> CANCELADO <──cancelar── CONFIRMADO

Consulta (criada na confirmação): CONFIRMADA ──finalizar──────> REALIZADA
                                             ──não compareceu─> NAO_COMPARECEU
                                             ──cancelar───────> CANCELADA
```

### Resumo dos Endpoints Principais

| Entidade | Ação | Endpoint | Descrição |
| :--- | :--- | :--- | :--- |
| **Agendamento** | Agendar | `POST /api/agendamentos` | Reserva o horário. |
| **Agendamento** | Confirmar | `POST .../{id}/confirmar` | Valida e cria a Consulta. |
| **Agendamento** | Cancelar | `POST .../{id}/cancelar` | Cancela sem excluir (e cancela a consulta gerada). |
| **Consulta** | Finalizar | `POST /api/consultas/{id}/finalizar` | Marca como realizada (dentista). |
| **Consulta** | Não compareceu | `POST /api/consultas/{id}/nao-compareceu` | Registra a falta do paciente. |
| **Consulta** | Listar | `GET /api/consultas/paciente/{id}` | Histórico de atendimentos. |
| **Prontuário** | Salvar Ficha | `POST /api/prontuarios` | Salva Anamnese e Prontuário juntos. |
| **Prontuário** | Buscar | `GET .../consulta/{id}` | Recupera ficha completa da visita. |

## Tecnologias utilizadas

* Java 17
* Spring Boot 3.5.6
* Spring Security
* JWT (jjwt-api / jjwt-impl / jjwt-jackson)
* Spring Data JPA
* MySQL
* Lombok
* Maven

## Configuração do Projeto

### 1. Criar banco de dados MySQL

```sql
CREATE DATABASE sorrisus_db;
````

#### Atualização de bancos criados antes do fluxo de status

O projeto usa `ddl-auto=update`, que cria colunas novas mas **não altera colunas existentes**. Em bancos criados antes desta versão, a coluna `consultas.status` foi gerada como `ENUM` do MySQL e não aceita o status `NAO_COMPARECEU`. Execute uma vez:

```sql
ALTER TABLE consultas MODIFY status VARCHAR(20) NOT NULL;
-- Somente se a coluna agendamentos.status já existir como ENUM:
ALTER TABLE agendamentos MODIFY status VARCHAR(20) NULL;
```

-----

### 2\. Configurar variáveis de ambiente

Agora o sistema utiliza **4 variáveis principais**:

```
DB_URL=
DB_USER=
DB_PASS=
JWT_SECRET=
JWT_EXPIRATION_MS=
```

### Exemplos

#### **Windows (CMD)**

```cmd
set DB_URL=jdbc:mysql://localhost:3306/sorrisus_db?useSSL=false&serverTimezone=UTC
set DB_USER=seu_usuario
set DB_PASS=sua_senha
set JWT_SECRET=sua_chave_secreta_muito_segura
set JWT_EXPIRATION_MS=3600000
```

#### **Linux/Mac (bash)**

```bash
export DB_URL=jdbc:mysql://localhost:3306/sorrisus_db?useSSL=false&serverTimezone=UTC
export DB_USER=seu_usuario
export DB_PASS=sua_senha
export JWT_SECRET=sua_chave_secreta_muito_segura
export JWT_EXPIRATION_MS=3600000
```

## Como subir via Docker

### 1\. Criar arquivo `.env` na raiz

```
MYSQL_ROOT_PASSWORD=seu_mysql_root_password
DB_USER=seu_usuario_app
DB_PASS=sua_senha_app
DB_URL=jdbc:mysql://db_sorrisus:3306/sorrisus_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true

JWT_SECRET=sua_chave_super_secreta
JWT_EXPIRATION_MS=3600000
```

-----

### 2\. Subir containers

```
docker compose up --build
```

Em background:

```
docker compose up -d --build
```

-----

### 3\. Verificar status

```
docker compose ps
```

Logs:

```
docker compose logs -f sorrisus_app
docker compose logs -f db_sorrisus
```

-----

### 4\. Parar containers

```
docker compose stop
```

Remover:

```
docker compose down -v
```

## Estrutura do projeto

  * `model/` → entidades JPA
  * `repository/` → persistência
  * `service/` → regras de negócio
  * `controller/` → endpoints REST
  * `security/` → autenticação e JWT
  * `config/` → configs globais (CORS, Security)
  * `resources/application.yml` → configurações

## Testes

O sistema pode ser testado via **Postman**, utilizando a collection disponível em:

```
collection/Sorrisus_API_Collection.json
```

Cada entidade possui operações:

  * Criar (`POST`)
  * Listar (`GET`)
  * Buscar por ID (`GET`)
  * Atualizar (`PUT`)
  * Deletar (`DELETE`)

## Testes unitários

Os testes podem ser executados com:

```
mvn test
```

Incluem testes de serviços e agora também testes de autenticação JWT.

## Próximos passos

  * Criar relacionamento entre entidades (ex: Paciente ↔ Dentista)
  * Criar dashboard no front-end
  * Implementar auditoria de ações

## 👥 Contribuidores

\<table\>
\<tr\>
\<td align="center"\>
\<a href="https://github.com/estertrvs" title="GitHub"\>
\<img src="https://avatars.githubusercontent.com/u/141650957?v=4" width="100px;" alt="Foto de Ester"/\><br>
\<sub\>
\<b\>Ester Trevisan\</b\>
\</sub\>
\</a\>
\</td\>
\<td align="center"\>
\<a href="https://github.com/analiciafsoares" title="GitHub"\>
\<img src="https://avatars.githubusercontent.com/u/144076062?v=4" width="100px;" alt="Foto de Ana"/\><br>
\<sub\>
\<b\>Ana Licia Soares\</b\>
\</sub\>
\</a\>
\</td\>
\<td align="center"\>
\<a href="https://github.com/Joaopaulomedeirosdesouza" title="GitHub"\>
\<img src="https://avatars.githubusercontent.com/u/148402008?v=4" width="100px;" alt="Foto de João Paulo"/\><br>
\<sub\>
\<b\>João Paulo Medeiros\</b\>
\</sub\>
\</a\>
\</td\>
\<td align="center"\>
\<a href="https://github.com/KesleyWilie" title="GitHub"\>
\<img src="https://avatars.githubusercontent.com/u/144160126?v=4" width="100px;" alt="Foto de Kesley"/\><br>
\<sub\>
\<b\>Kesley Wilie\</b\>
\</sub\>
\</a\>
\</td\>
\<td align="center"\>
\<a href="https://github.com/GeorgeAdOliveira" title="GitHub"\>
\<img src="https://avatars.githubusercontent.com/u/143577407?v=4" width="100px;" alt="Foto de George"/\><br>
\<sub\>
\<b\>George Oliveira\</b\>
\</sub\>
\</a\>
\</td\>
\</tr\>
\</table\>

---
**Instituto Federal da Paraíba** — Disciplina de **Projeto II**.