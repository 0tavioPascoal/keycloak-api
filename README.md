API Spring Boot com Keycloak

Projeto de estudo criado para compreender, na prática, autenticação e autorização com Spring Boot, Spring Security, OAuth 2.0, OpenID Connect, JWT, Keycloak e Docker.

Atualmente, a aplicação possui endpoints públicos e protegidos, valida os Access Tokens emitidos pelo Keycloak e controla o acesso por meio das roles USER e ADMIN.

Este projeto está em evolução. Os próximos passos incluem um CRUD protegido, persistência de dados, testes automatizados e integração com um frontend usando Authorization Code com PKCE.

Tecnologias

Java 25

Spring Boot

Spring Web

Spring Security

OAuth2 Resource Server

Bean Validation

Spring Boot Actuator

Keycloak 26.7.4

PostgreSQL 17

Docker e Docker Compose

Maven Wrapper

Arquitetura

Visão dos componentes

flowchart LR
C["Cliente<br/>Terminal ou frontend"]
K["Keycloak<br/>Identity Provider"]
A["API Spring<br/>Resource Server"]
D["PostgreSQL<br/>do Keycloak"]

    C -->|"Login"| K
    K -->|"Access Token JWT"| C
    C -->|"Bearer Token"| A
    K -->|"Usuários, roles e sessões"| D
    A -.->|"Descobre chaves públicas"| K

O cliente autentica-se no Keycloak e utiliza o Access Token para acessar a API. A API não recebe a senha do usuário e não consulta o banco do Keycloak diretamente.

Fluxo de autenticação

sequenceDiagram
participant C as Cliente
participant K as Keycloak
participant A as API Spring

    C->>K: Solicita autenticação
    K-->>C: Retorna Access Token JWT
    C->>A: Authorization: Bearer token
    A->>A: Valida assinatura, emissor e expiração
    A->>A: Verifica as roles do usuário
    A-->>C: Retorna 200, 401 ou 403

Responsabilidades

O Keycloak é responsável por:

autenticar usuários;

armazenar e gerenciar credenciais;

emitir e renovar tokens;

gerenciar usuários, sessões e roles.

A API Spring é responsável por:

validar o JWT recebido;

extrair a identidade autenticada;

converter as roles do Keycloak em authorities do Spring Security;

autorizar ou bloquear o acesso aos endpoints;

executar as regras de negócio.

Requisitos

Antes de executar o projeto, instale:

Java 25;

Docker;

Docker Compose.

Confira o ambiente:

java -version
docker --version
docker compose version

Executando a infraestrutura

Na raiz do projeto, inicie o Keycloak e o PostgreSQL:

docker compose up -d

Confira os serviços:

docker compose ps

O ambiente utiliza as seguintes portas:

Serviço

Endereço

API Spring

http://localhost:8080

Keycloak

http://localhost:8081

PostgreSQL

disponível apenas na rede do Docker

Para acompanhar os logs do Keycloak:

docker compose logs -f keycloak

Configurando o Keycloak

Acesse o console administrativo:

http://localhost:8081

As credenciais administrativas locais estão definidas no compose.yaml. Elas são destinadas exclusivamente ao ambiente de desenvolvimento.

Organização utilizada

flowchart TD
K["Keycloak"] --> M["Realm master<br/>Administração do servidor"]
K --> R["Realm keycloak-spring<br/>Aplicações e usuários"]
R --> C["Client spring-api"]
R --> RO["Roles USER e ADMIN"]
R --> U["Usuários da aplicação"]

O realm master administra o servidor. O realm keycloak-spring mantém isolados os clients, usuários e permissões deste projeto.

1. Realm

Crie um realm:

keycloak-spring

O realm master deve ser usado apenas para administrar o Keycloak. Os usuários da aplicação ficam no realm keycloak-spring.

2. Client

Crie um client OpenID Connect:

Client ID: spring-api
Client authentication: ON
Authorization: OFF
Standard flow: OFF
Direct access grants: ON
Service accounts roles: OFF

O uso de Direct access grants neste projeto é estritamente didático, para permitir a obtenção de tokens pelo terminal. Uma aplicação real com frontend deve utilizar Authorization Code com PKCE.

3. Realm roles

Crie as roles:

USER
ADMIN

4. Usuários do laboratório

Crie dois usuários:

Usuário

Roles

usuario

USER

administrador

USER, ADMIN

Para os testes pelo terminal:

habilite os usuários;

deixe Required user actions vazio;

defina Email verified como ativo;

defina uma senha não temporária.

Não utilize as senhas de exemplo deste README em ambientes reais.

Configuração da API

O application.yml aponta para o emissor dos tokens:

spring:
application:
name: keycloak-api

security:
oauth2:
resourceserver:
jwt:
issuer-uri: http://localhost:8081/realms/keycloak-spring

server:
port: 8080

management:
endpoints:
web:
exposure:
include: health,info

O Spring consulta a configuração OpenID Connect do realm e obtém automaticamente as chaves públicas usadas para validar os tokens.

Como a API valida o JWT

flowchart TD
R["Requisição com Bearer Token"] --> E{"Token existe e<br/>tem formato JWT?"}
E -->|"Não"| U["401 Unauthorized"]
E -->|"Sim"| S{"Assinatura válida?"}
S -->|"Não"| U
S -->|"Sim"| I{"Emissor e validade<br/>estão corretos?"}
I -->|"Não"| U
I -->|"Sim"| P["Identidade autenticada"]
P --> O["Verificação das roles"]

O Spring realiza essas verificações antes de encaminhar a requisição ao controller. Portanto, o controller recebe uma identidade que já foi autenticada.

Executando a API

Com a infraestrutura em funcionamento:

./mvnw spring-boot:run

Verifique a saúde da aplicação:

curl -i http://localhost:8080/actuator/health

Endpoints

Método

Endpoint

Regra

Resultado esperado

GET

/api/publico

Acesso livre

200 OK

GET

/api/usuario

Role USER ou ADMIN

200, 401 ou 403

GET

/api/admin

Role ADMIN

200, 401 ou 403

Diferença entre 401 e 403

401 Unauthorized: a requisição não apresentou uma autenticação válida;

403 Forbidden: o usuário está autenticado, mas não possui a permissão necessária.

flowchart TD
R["Requisição para endpoint protegido"] --> T{"Access Token válido?"}
T -->|"Não"| E401["401 Unauthorized"]
T -->|"Sim"| P{"Possui a role exigida?"}
P -->|"Não"| E403["403 Forbidden"]
P -->|"Sim"| E200["200 OK"]

Solicitando um Access Token

Copie o Client Secret em:

Keycloak → Clients → spring-api → Credentials

No Zsh, carregue-o sem exibi-lo nem gravá-lo diretamente no comando:

read -rs "KC_CLIENT_SECRET?Client secret: "
echo

Solicite um token para o usuário comum:

TOKEN_RESPONSE="$(
curl --silent --request POST \
--url "http://localhost:8081/realms/keycloak-spring/protocol/openid-connect/token" \
--header "Content-Type: application/x-www-form-urlencoded" \
--data-urlencode "grant_type=password" \
--data-urlencode "client_id=spring-api" \
--data-urlencode "client_secret=${KC_CLIENT_SECRET}" \
--data-urlencode "username=usuario" \
--data-urlencode "password=usuario123"
)"

USER_TOKEN="$(
echo "$TOKEN_RESPONSE" |
jq --exit-status --raw-output '
if .access_token then
.access_token
else
error(.error_description // "Token não retornado")
end
'
)"

Não publique o Access Token, o Refresh Token ou o Client Secret.

Testes manuais

Endpoint público

curl -i http://localhost:8080/api/publico

Resultado esperado:

200 OK

Endpoint protegido sem token

curl -i http://localhost:8080/api/usuario

Resultado esperado:

401 Unauthorized

Endpoint de usuário com token válido

curl -i \
--header "Authorization: Bearer ${USER_TOKEN}" \
http://localhost:8080/api/usuario

Resultado esperado:

200 OK

Usuário comum acessando endpoint administrativo

curl -i \
--header "Authorization: Bearer ${USER_TOKEN}" \
http://localhost:8080/api/admin

Resultado esperado:

403 Forbidden

Para testar o acesso permitido, solicite um token para administrador e envie-o ao mesmo endpoint. O resultado esperado será 200 OK.

Mapeamento das roles

O Keycloak envia as realm roles dentro do JWT:

{
"realm_access": {
"roles": [
"USER",
"ADMIN"
]
}
}

A aplicação converte essas roles para o padrão do Spring Security:

USER  → ROLE_USER
ADMIN → ROLE_ADMIN

Isso permite configurar regras como:

.requestMatchers("/api/admin").hasRole("ADMIN")
.requestMatchers("/api/usuario").hasAnyRole("USER", "ADMIN")

O fluxo de autorização fica assim:

flowchart LR
J["JWT do Keycloak"] --> RA["realm_access.roles"]
RA --> U["USER"]
RA --> A["ADMIN"]
U --> RU["ROLE_USER"]
A --> RD["ROLE_ADMIN"]
RU --> SS["Spring Security"]
RD --> SS

Segurança

Este projeto utiliza configurações voltadas ao desenvolvimento local. Antes de utilizar uma arquitetura semelhante em produção, é necessário:

utilizar HTTPS;

remover credenciais fixas do compose.yaml;

armazenar segredos em variáveis ou em um gerenciador de segredos;

substituir o password grant por Authorization Code com PKCE;

configurar e validar uma audiência própria para a API;

configurar CORS apenas para as origens necessárias;

definir políticas de senha, sessão e MFA;

automatizar backup e atualização do Keycloak;

implementar testes automatizados de autenticação e autorização.

Comandos úteis

Subir os contêineres:

docker compose up -d

Parar os contêineres sem remover os dados:

docker compose down

Visualizar logs:

docker compose logs -f

docker compose down -v também remove os volumes e apaga os dados locais do PostgreSQL. Utilize esse comando somente quando quiser reiniciar todo o laboratório.

Roadmap

Subir Keycloak e PostgreSQL com Docker Compose

Criar realm, client, roles e usuários

Emitir Access Token e Refresh Token

Configurar Spring como OAuth2 Resource Server

Proteger endpoints por autenticação

Proteger endpoints com USER e ADMIN

Criar testes automatizados do Spring Security

Automatizar a importação do realm

Configurar e validar audience

Criar CRUD protegido por roles

Adicionar PostgreSQL próprio para os dados da API

Documentar a API com OpenAPI/Swagger

Integrar um frontend usando Authorization Code com PKCE

Implementar logout e renovação de sessão

Criar configuração adequada para produção

Próxima evolução sugerida

Um bom próximo passo é implementar um CRUD de produtos:

GET    /api/produtos       → USER ou ADMIN
GET    /api/produtos/{id}  → USER ou ADMIN
POST   /api/produtos       → ADMIN
PUT    /api/produtos/{id}  → ADMIN
DELETE /api/produtos/{id}  → ADMIN

Essa evolução permitirá estudar em conjunto:

Spring Data JPA;

PostgreSQL da aplicação;

migrations com Flyway;

DTOs e Bean Validation;

tratamento global de erros;

testes unitários e de integração;

autorização por operação.

Fluxo esperado do CRUD

flowchart TD
C["Cliente autenticado"] --> API["API de produtos"]
API --> R{"Operação solicitada"}
R -->|"Consultar"| V{"USER ou ADMIN?"}
R -->|"Criar, alterar ou excluir"| G{"ADMIN?"}
V -->|"Sim"| DB["PostgreSQL da aplicação"]
G -->|"Sim"| DB
V -->|"Não"| F["403 Forbidden"]
G -->|"Não"| F

Esse banco será independente do PostgreSQL utilizado internamente pelo Keycloak. O Keycloak continuará responsável pela identidade, enquanto a API será responsável pelos dados de negócio.

Objetivo educacional

O objetivo deste repositório não é apenas disponibilizar uma configuração pronta. Ele registra a evolução do aprendizado sobre identidade, autenticação, autorização e proteção de APIs com padrões amplamente utilizados no mercado.