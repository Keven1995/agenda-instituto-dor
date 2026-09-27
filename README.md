# Agenda Instituto Dor

PWA de agendamento para os profissionais do Instituto Dor. A aplicacao permite organizar atendimentos, visualizar a agenda por periodo e manter os compromissos sincronizados com o Google Calendar.

## Objetivo

Centralizar o controle da agenda profissional em uma interface simples, responsiva e acessivel por computador ou celular.

O sistema foi criado para que o profissional possa:

- Entrar usando sua conta Google.
- Visualizar seus atendimentos em um calendario.
- Criar atendimentos informando paciente, data, inicio e termino.
- Trabalhar com duracoes diferentes para cada atendimento.
- Impedir conflitos de horario.
- Editar ou reagendar atendimentos.
- Cancelar atendimentos.
- Sincronizar os compromissos com o Google Calendar.
- Instalar a aplicacao como PWA em dispositivos compativeis.

## Funcionalidades

### Agenda

- Visualizacao mensal responsiva.
- Selecao de um dia para consultar os atendimentos.
- Navegacao entre meses.
- Indicacao visual de dias com compromissos.
- Listagem dos horarios do dia selecionado.

### Agendamentos

- Nome do paciente obrigatorio.
- Data, horario inicial e horario final obrigatorios.
- Validacao de horario final posterior ao inicial.
- Duracao calculada automaticamente.
- Bloqueio de horarios sobrepostos para o mesmo usuario.
- Permissao para atendimentos consecutivos.
- Edicao, reagendamento e cancelamento.

### Autenticacao e seguranca

- Login via OAuth 2.0 com Google.
- Sessao mantida no backend.
- Tokens Google nao sao expostos ao frontend.
- Agendamentos isolados por usuario.
- Validacoes aplicadas no backend.
- Dados sensiveis configurados por variaveis de ambiente.

## Arquitetura

O projeto e dividido em dois modulos independentes:

```text
agenda-instituto-dor/
├── agenda-web/    # PWA e interface do usuario
├── agenda-api/    # API, regras de negocio e persistencia
└── docker-compose.yml
```

Fluxo principal:

```text
PWA React
   |
   | HTTPS / REST
   v
API Spring Boot
   |             \
   v              v
PostgreSQL    Google OAuth / Calendar
```

O PostgreSQL e a fonte principal dos agendamentos. O Google Calendar funciona como integracao externa.

## Tecnologias

### Frontend

- React 18
- TypeScript
- Vite
- React Query
- React Hook Form
- Zod
- React Router
- Vite PWA Plugin
- Service Worker
- Web App Manifest
- CSS responsivo

### Backend

- Java 21
- Spring Boot 3.4
- Spring Web
- Spring Security
- OAuth 2.0 Client
- Spring Data JPA
- Bean Validation
- PostgreSQL Driver
- Flyway
- JUnit 5
- Mockito
- AssertJ

### Infraestrutura

- Docker Compose para desenvolvimento local
- PostgreSQL 16
- Vercel, previsto para o frontend
- Render, previsto para a API
- Neon, previsto para o PostgreSQL de producao
- Google Cloud, para OAuth e Google Calendar

## Estrutura do backend

```text
agenda-api/src/main/java/br/com/institutodor/agenda/
├── appointment/    # Entidade, regras, endpoints e repositorio
├── auth/           # Informacoes da sessao autenticada
├── config/         # Seguranca e tratamento de erros
├── health/         # Endpoint de disponibilidade
└── user/           # Entidade e repositorio de usuarios
```

## API de agendamentos

Todos os endpoints de agendamento exigem autenticacao e filtram os dados pelo usuario atual.

```http
GET    /api/appointments?start=2026-09-01T00:00:00Z&end=2026-10-01T00:00:00Z
GET    /api/appointments/{id}
POST   /api/appointments
PUT    /api/appointments/{id}
DELETE /api/appointments/{id}
```

Exemplo de criacao:

```json
{
  "patientName": "Joao da Silva",
  "startAt": "2026-09-28T14:00:00Z",
  "endAt": "2026-09-28T16:00:00Z"
}
```

Endpoint publico de verificacao:

```http
GET /api/health
```

## Como executar localmente

### Pre-requisitos

- Node.js 20 ou superior
- Java 17 ou superior
- Maven 3.9 ou superior
- Docker Desktop

### Banco de dados

Na raiz do projeto:

```bash
docker compose up -d db
```

### API

Copie `agenda-api/.env.example`, configure as variaveis e inicie:

```bash
cd agenda-api
mvn spring-boot:run
```

### Frontend

```bash
cd agenda-web
npm install
npm run dev
```

O frontend fica disponivel em `http://localhost:5173`, a API em `http://localhost:8080` e o PostgreSQL deste projeto em `localhost:5433`. A porta interna do PostgreSQL continua sendo `5432`.

## Configuracao do Google OAuth

Crie um cliente OAuth no Google Cloud e cadastre o redirect URI:

```text
http://localhost:8080/login/oauth2/code/google
```

Configure na API:

```env
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google
FRONTEND_URL=http://localhost:5173
```

O login e iniciado pelo endpoint:

```text
/oauth2/authorization/google
```

## Testes e qualidade

O backend utiliza testes unitarios para validar as regras de negocio de agendamento, incluindo conflitos, periodos invalidos, isolamento por usuario e cancelamento.

Comandos principais:

```bash
# Frontend
npm run lint
npm run build

# Backend
mvn test
```

O projeto segue separacao de responsabilidades, injecao de dependencias, contratos de API, validacao no backend e isolamento da integracao externa por meio do `CalendarGateway`.

## Status

Implementados:

- PWA responsivo com calendario mensal.
- Autenticacao Google via sessao de servidor.
- Persistencia de usuarios e agendamentos.
- CRUD de agendamentos com validacao de conflitos.
- Criacao, atualizacao e cancelamento de eventos no Google Calendar.
- Testes unitarios das regras de negocio e do mapeamento de eventos.

Proximos incrementos:

- Outbox e retry para aumentar a resiliencia da sincronizacao externa.
- Testes de contrato HTTP e testes de integracao com PostgreSQL.

## Notificacoes Push no iPhone

O PWA possui notificacoes Web Push para lembrar o profissional cinco minutos antes do atendimento. A mensagem exibida e:

```text
A consulta com Nome do Paciente esta agendada para 14:00
```

Para usar no iPhone:

1. Abra o sistema pelo Safari.
2. Instale o PWA pela opcao **Adicionar a Tela de Inicio**.
3. Abra o PWA instalado.
4. Clique em **Ativar notificações**.
5. Permita as notificacoes quando o iPhone solicitar.

O PWA precisa estar publicado em HTTPS para funcionar em producao. O backend verifica os agendamentos a cada minuto e envia a notificacao quando o horario estiver a cinco minutos do atendimento.

### Chaves VAPID

Gere um par de chaves VAPID para o ambiente local ou de producao:

```bash
npx web-push generate-vapid-keys
```

Configure os valores no arquivo `agenda-api/.env`:

```env
VAPID_PUBLIC_KEY=
VAPID_PRIVATE_KEY=
VAPID_SUBJECT=mailto:admin@institutodor.com.br
```

Nunca versione a chave privada. O arquivo `.env` ja esta no `.gitignore`.
