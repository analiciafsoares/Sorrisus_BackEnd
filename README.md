# Sorrisus - Sistema Odontológico

## Sobre o projeto

O **Sorrisus** é um sistema odontológico desenvolvido como projeto acadêmico no IFPB, com o objetivo de auxiliar na gestão de clínicas odontológicas. Ele centraliza o cadastro de **pacientes**, **dentistas** e **recepcionistas**, a **agenda**, as **consultas**, a **ficha clínica** (anamnese, prontuário e odontograma), o catálogo de **serviços** e o acompanhamento de **retornos pendentes**.

Este repositório contém a **API REST (back-end)**. A interface fica no repositório `Sorrisus_FrontEnd`.

O sistema está sendo desenvolvido em **Java com Spring Boot**, utilizando **MySQL** como banco de dados relacional, e segue uma **arquitetura em camadas** (Controller → Service → Repository → Model).

Com a evolução do projeto, o backend agora possui **autenticação completa com JWT**, **rotas protegidas**, **controle de acesso por papéis (roles)** e **middleware de segurança** usando Spring Security.


## 🛡️ Autenticação JWT

O sistema implementa um fluxo completo de autenticação baseado em JWT (**JSON Web Token**).

### Como funciona?

1. O usuário faz login enviando **email + senha** para `/api/auth/login`.
2. O backend valida as credenciais.
3. Se válidas, gera um **token JWT**, assinado com chave secreta.
4. O token precisa ser enviado em todas as requisições protegidas no header:

```
Authorization: Bearer <token>
```

O token expira após o tempo definido em `JWT_EXPIRATION_MS`. A resposta do login traz `accessToken`, `userId`, `email` e `role`.

### Rotas públicas (sem autenticação)

- `POST /api/auth/login`
- `POST /api/usuarios/cadastro`
- `POST /api/dentistas/cadastro`
- `POST /api/recepcionistas/cadastro`

Todas as demais rotas **exigem autenticação**.

> **O paciente não se autocadastra.** O cadastro de pacientes (`POST /api/pacientes/cadastro`) exige login de dentista ou recepcionista.
>
> As rotas de cadastro de usuários, dentistas e recepcionistas ainda são públicas. Pelas regras do sistema, o cadastro de profissionais deve ser feito apenas por um dentista; essa restrição está pendente.

### Permissões por perfil

Além de exigir login, alguns endpoints são restritos por perfil (`@PreAuthorize`). Quando o perfil não tem permissão, a API responde **403**.

| Ação | Perfis permitidos |
| :--- | :--- |
| Criar, editar, confirmar, cancelar e excluir agendamentos | Dentista, Recepcionista |
| Cadastrar pacientes | Dentista, Recepcionista |
| Criar, editar e desativar serviços | Dentista, Recepcionista |
| Registrar não comparecimento | Dentista, Recepcionista |
| Finalizar atendimento | Dentista |
| Salvar e excluir ficha clínica (prontuário/anamnese) | Dentista |
| Histórico de consultas da clínica (`/consultas/historico/recepcao`) | Recepcionista |
| Demais consultas (listagens, buscas por ID, retornos pendentes) | Qualquer usuário autenticado |

Os perfis existentes são `ROLE_DENTISTA`, `ROLE_RECEPCIONISTA` e `ROLE_PACIENTE`. O paciente não cria nem altera agendamentos: ele apenas consulta os próprios dados.

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
```

Credenciais habilitadas (para envio de token).

## Funcionalidades atuais

Atualmente, o backend do sistema conta com:

* **Entidades modeladas:**
  * `Usuario` (base) → `Paciente`, `Dentista`, `Recepcionista`
  * `Role` (enum de papéis de usuário)
  * `Agendamento` (status `PENDENTE`, `CONFIRMADO`, `CANCELADO`)
  * `Consulta` (status `PENDENTE`, `CONFIRMADA`, `REALIZADA`, `CANCELADA`, `NAO_COMPARECEU`)
  * `Prontuario` / `Anamnese`
  * `Servico`

* **CRUD** de usuários, pacientes, dentistas, recepcionistas e serviços (a exclusão de serviço apenas o desativa).
* **Agenda** com validação de conflito de horário por dentista e fluxo de status (confirmar, cancelar, finalizar, não comparecimento), sem exclusão de histórico.
* **Ficha clínica** (anamnese + prontuário + odontograma) salva em uma única requisição.
* **Retornos pendentes:** pacientes cuja última consulta realizada tem mais de 6 meses e que não possuem agendamento futuro pendente.
* **Mensagens por WhatsApp (Z-API):** aviso ao paciente quando um agendamento é criado e lembrete de retorno, executado na inicialização e diariamente (agendamento configurável).
* **Autenticação JWT** com filtro, validação e controle de acesso por perfil.
* **Tratamento global de exceções** (404, 400, 401, 403 e erros de validação com mensagens em português).
* **Banco MySQL** com criação automática das tabelas via JPA/Hibernate (`ddl-auto=update`).
* **Variáveis de ambiente** para dados sensíveis.
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
| **Consulta** | Listar | `GET /api/consultas/paciente/{id}` | Histórico de atendimentos do paciente. |
| **Consulta** | Listar | `GET /api/consultas/dentista/{id}` | Consultas do dentista. |
| **Consulta** | Histórico | `GET /api/consultas/historico/recepcao` | Consultas de toda a clínica (recepção). |
| **Consulta** | Retornos | `GET /api/consultas/retornos-pendentes` | Pacientes com retorno pendente (também por dentista: `.../dentista/{id}`). |
| **Prontuário** | Salvar Ficha | `POST /api/prontuarios?consultaId={id}` | Salva Anamnese e Prontuário juntos. |
| **Prontuário** | Buscar | `GET .../consulta/{id}` | Recupera ficha completa da visita. |
| **Agendamento** | Agenda | `GET /api/agendamentos/dentista/{id}` | Agenda de um dentista (usada também pela recepção). |
| **Serviço** | CRUD | `/api/servicos` | Catálogo de serviços (`DELETE` desativa). |

A lista completa, com exemplos de requisição, está na collection do Postman (seção **Testes**).

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

O sistema lê as seguintes variáveis de ambiente:

| Variável | Obrigatória | Descrição |
| :--- | :--- | :--- |
| `DB_URL` | Sim | URL JDBC do MySQL |
| `DB_USER` | Sim | Usuário do banco |
| `DB_PASS` | Sim | Senha do banco |
| `JWT_SECRET` | Sim | Chave secreta para assinar os tokens |
| `JWT_EXPIRATION_MS` | Sim | Validade do token em milissegundos (ex.: `3600000` = 1 hora) |
| `ZAPI_INSTANCE` | Sim* | Instância da Z-API (WhatsApp) |
| `ZAPI_TOKEN` | Sim* | Token da instância Z-API |
| `ZAPI_CLIENT_TOKEN` | Sim* | Client token da conta Z-API |
| `RETORNO_LEMBRETE_ENABLED` | Sim | `true` para enviar lembretes de retorno; `false` para desativar |
| `RETORNO_LEMBRETE_CRON` | Sim | Agendamento do lembrete (ex.: `0 0 9 * * *` = todo dia às 9h) |
| `RETORNO_LEMBRETE_ZONE` | Sim | Fuso horário do agendamento (ex.: `America/Fortaleza`) |

\* As variáveis da Z-API precisam existir para a aplicação iniciar, mas podem ficar vazias em desenvolvimento. Sem Z-API configurada, o envio de mensagens falha: a criação de agendamento salva o registro, mas pode responder com erro 500. Nunca versione valores reais dessas variáveis.

### Exemplos

#### **Windows (CMD)**

```cmd
set DB_URL=jdbc:mysql://localhost:3306/sorrisus_db?useSSL=false&serverTimezone=UTC
set DB_USER=seu_usuario
set DB_PASS=sua_senha
set JWT_SECRET=sua_chave_secreta_muito_segura
set JWT_EXPIRATION_MS=3600000
set ZAPI_INSTANCE=
set ZAPI_TOKEN=
set ZAPI_CLIENT_TOKEN=
set RETORNO_LEMBRETE_ENABLED=false
set RETORNO_LEMBRETE_CRON=0 0 9 * * *
set RETORNO_LEMBRETE_ZONE=America/Fortaleza
```

#### **Linux/Mac (bash)**

```bash
export DB_URL="jdbc:mysql://localhost:3306/sorrisus_db?useSSL=false&serverTimezone=UTC"
export DB_USER=seu_usuario
export DB_PASS=sua_senha
export JWT_SECRET=sua_chave_secreta_muito_segura
export JWT_EXPIRATION_MS=3600000
export ZAPI_INSTANCE=
export ZAPI_TOKEN=
export ZAPI_CLIENT_TOKEN=
export RETORNO_LEMBRETE_ENABLED=false
export RETORNO_LEMBRETE_CRON="0 0 9 * * *"
export RETORNO_LEMBRETE_ZONE=America/Fortaleza
```

> No bash, coloque `DB_URL` entre aspas se ela contiver `&`.

### 3. Executar

```bash
./mvnw spring-boot:run
```

A API fica disponível em `http://localhost:8080/api`.

## Como subir via Docker

### 1\. Criar arquivo `.env` na raiz

```
MYSQL_ROOT_PASSWORD=seu_mysql_root_password
DB_USER=seu_usuario_app
DB_PASS=sua_senha_app
DB_URL=jdbc:mysql://db_sorrisus:3306/sorrisus_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true

JWT_SECRET=sua_chave_super_secreta
JWT_EXPIRATION_MS=3600000

ZAPI_INSTANCE=
ZAPI_TOKEN=
ZAPI_CLIENT_TOKEN=

# Opcionais no Docker (valores padrão: true, "0 0 9 * * *", America/Fortaleza)
RETORNO_LEMBRETE_ENABLED=false
```

O arquivo `.env` contém segredos e não deve ser versionado.

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

  * `model/` → entidades JPA e enums de status
  * `dto/` → objetos de entrada e saída da API
  * `repository/` → persistência
  * `service/` → regras de negócio (inclui o agendador de lembretes de retorno)
  * `controller/` → endpoints REST
  * `security/` → autenticação e JWT
  * `config/` → configs globais (CORS, Security)
  * `exception/` → exceções de negócio e tratamento global de erros
  * `resources/application.properties` → configurações

## Testes

O sistema pode ser testado via **Postman**, utilizando a collection disponível em:

```
collection/Sorrisus_API_Collection.json
```

Como usar:

1. Importe o arquivo no Postman (**Import**).
2. Confira a variável `base_url` da collection (padrão `http://localhost:8080`).
3. Execute **🔐 Auth → Login** com o e-mail e a senha de um usuário do perfil desejado. O token é salvo automaticamente na variável `token` e enviado em todas as requisições.
4. As requisições de criação salvam o ID gerado (`pacienteId`, `agendamentoId`, `consultaId`...). Para usar registros já existentes, edite essas variáveis.

A descrição de cada requisição indica o perfil exigido. Fluxo sugerido para testar a agenda: criar agendamento → confirmar → listar consultas do dentista (para obter o `consultaId`) → salvar ficha → finalizar atendimento.

## Testes unitários

Os testes podem ser executados com:

```
./mvnw test
```

Incluem testes das regras de negócio dos serviços (agendamentos, consultas, serviços, retornos e lembretes) e testes de autenticação JWT. O teste `contextLoads` sobe a aplicação completa e, por isso, precisa de um MySQL acessível e das variáveis de ambiente configuradas.

## Próximos passos

  * Restringir o cadastro de profissionais ao perfil de dentista
  * Emissão de receituário
  * Relatórios gerenciais
  * Recuperação de senha
  * Bloqueio de login após tentativas inválidas
  * Lembrete de consulta 24 horas antes

## 👥 Contribuidores

<table>
<tr>
<td align="center">
<a href="https://github.com/estertrvs" title="GitHub">
<img src="https://avatars.githubusercontent.com/u/141650957?v=4" width="100px;" alt="Foto de Ester"/><br>
<sub>
<b>Ester Trevisan</b>
</sub>
</a>
</td>
<td align="center">
<a href="https://github.com/analiciafsoares" title="GitHub">
<img src="https://avatars.githubusercontent.com/u/144076062?v=4" width="100px;" alt="Foto de Ana"/><br>
<sub>
<b>Ana Licia Soares</b>
</sub>
</a>
</td>
<td align="center">
<a href="https://github.com/Joaopaulomedeirosdesouza" title="GitHub">
<img src="https://avatars.githubusercontent.com/u/148402008?v=4" width="100px;" alt="Foto de João Paulo"/><br>
<sub>
<b>João Paulo Medeiros</b>
</sub>
</a>
</td>
<td align="center">
<a href="https://github.com/KesleyWilie" title="GitHub">
<img src="https://avatars.githubusercontent.com/u/144160126?v=4" width="100px;" alt="Foto de Kesley"/><br>
<sub>
<b>Kesley Wilie</b>
</sub>
</a>
</td>
<td align="center">
<a href="https://github.com/GeorgeAdOliveira" title="GitHub">
<img src="https://avatars.githubusercontent.com/u/143577407?v=4" width="100px;" alt="Foto de George"/><br>
<sub>
<b>George Oliveira</b>
</sub>
</a>
</td>
</tr>
</table>

---
**Instituto Federal da Paraíba (IFPB) — Campus Monteiro.** Projeto iniciado nas disciplinas de **Projeto I** e **Projeto II** e continuado como **Trabalho de Conclusão de Curso**.