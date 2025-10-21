# Gerenciador de Tarefas (JSF + JPA)

Aplicação web para controle de tarefas construída com Javax, PrimeFaces e Hibernate. Permite cadastrar, editar, filtrar e concluir tarefas mantendo foco nas atividades em andamento.

## Principais recursos
- Cadastro completo de tarefas com validação (título, descrição, responsável, prioridade, prazo e situação).
- Modal para criação/edição com feedback de validação amigável.
- Filtro dinâmico por número, texto, responsável, prioridade e situação.
- Persistência em banco relacional via JPA/Hibernate.
- Testes de repositório utilizando banco em memória H2.

## Stack e dependências
- Java 11
- Maven (mvnw incluso)
- JSF (Mojarra 2.3)
- PrimeFaces 8
- CDI/Weld
- Hibernate ORM + Bean Validation
- PostgreSQL (produção) / H2 (testes)

## Estrutura do projeto
```
src/
├── main
│   ├── java/pedroleonez/javaweb2   # Controllers JSF, modelo JPA e repositórios
│   ├── resources/META-INF          # persistence.xml (produção)
│   └── webapp                      # Páginas JSF, recursos e includes
└── test
    ├── java                        # Testes JUnit
    └── resources/META-INF          # persistence.xml (H2 para testes)
```

## Pré-requisitos
- JDK 11 instalado e configurado.
- PostgreSQL acessível com um banco criado (por padrão `jdbc:postgresql://localhost:5432/db_todo`).
- Opcional: Apache Tomcat 9 (ou compatível) para deploy do `.war`.

## Clonar o projeto
```bash
git clone https://github.com/pedroleonez/gerenciadorDeTarefasJSF.git
```

## Configuração do banco
1. Crie o banco e um usuário com permissão de escrita:
   ```sql
   CREATE DATABASE db_todo;
   CREATE USER pedrol WITH PASSWORD '1234';
   GRANT ALL PRIVILEGES ON DATABASE db_todo TO pedrol;
   ```
2. Ajuste as propriedades em `src/main/resources/META-INF/persistence.xml` se desejar usar credenciais ou URL diferentes.

## Build e deploy
```bash
./mvnw clean package
```

O artefato `target/gerenciadorDeTarefasJSF.war` pode ser implantado em um container compatível com Servlet 4 (ex.: Tomcat 9). Após o deploy, acesse `http://localhost:8080/gerenciadorDeTarefasJSF`.

### Deploy no Heroku
O projeto inclui um `Procfile` que executa o Tomcat embarcado via `webapp-runner`. Para publicar no Heroku:

1. Realize o build (`./mvnw clean package`) e faça o push do código para o app.
2. Configure as variáveis de ambiente com as credenciais do banco gerenciado (Render ou outro PostgreSQL):
   ```bash
   heroku config:set \
     JDBC_DATABASE_URL="jdbc:postgresql://<host>/<db>?sslmode=require" \
     JDBC_DATABASE_USERNAME="<usuario>" \
     JDBC_DATABASE_PASSWORD="<senha>" \
     --app gerenciador-de-tarefas-jsf
   ```
3. Reinicie a dyno (`heroku restart --app gerenciador-de-tarefas-jsf`) e acompanhe os logs (`heroku logs --tail`) para verificar a conexão (`[DB] Usando JDBC_DATABASE_URL.`).

Quando essas variáveis não estão presentes, a aplicação usa automaticamente o banco local configurado em `persistence.xml`.

## Testes
```bash
./mvnw -Prun-tests test
```

Os testes configuram automaticamente a unidade de persistência `tarefasPU-test`, utilizando um banco H2 em memória (não requer configuração externa).

## Configurações adicionais
- Para alternar a unidade de persistência em runtime (ex.: em testes integrados), defina a system property `tarefas.persistence.unit`.
- O bean JSF trabalha com um filtro padrão que exibe apenas tarefas em andamento; ajuste em `TarefaBean` conforme necessidade.
