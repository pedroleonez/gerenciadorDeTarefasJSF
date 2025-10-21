package pedroleonez.gerenciadordetarefasjsf.repository;

import pedroleonez.gerenciadordetarefasjsf.model.Tarefa;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Repositório JPA com operações CRUD e filtros dinâmicos para {@link Tarefa}.
 */
@ApplicationScoped
public class TarefaRepository {

    private EntityManagerFactory emf;

    public TarefaRepository() {}

    @PostConstruct
    public void init() {
        try {
            if (this.emf == null) {
                System.out.println("⚙️ Inicializando EntityManagerFactory via TarefaRepository.init()...");

                Map<String, Object> cloudProps = resolveCloudDatabaseProperties();

                if (cloudProps != null) {
                    System.out.println("✅ Detectado ambiente gerenciado. Inicializando com configurações provenientes das variáveis de ambiente.");
                    this.emf = Persistence.createEntityManagerFactory("tarefasPU", cloudProps);
                } else {
                    System.out.println("✅ Ambiente local detectado. Utilizando configurações declaradas em persistence.xml.");
                    this.emf = Persistence.createEntityManagerFactory("tarefasPU");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Erro ao inicializar EntityManagerFactory: " + e.getMessage(), e);
        }
    }

    private EntityManager getEntityManager() {
        if (emf == null) {
            throw new IllegalStateException("❌ EntityManagerFactory não foi inicializado!");
        }
        return emf.createEntityManager();
    }

    public void salvar(Tarefa tarefa) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(tarefa);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public void atualizar(Tarefa tarefa) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(tarefa);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public void remover(Long id) {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Tarefa tarefa = em.find(Tarefa.class, id);
            if (tarefa != null) {
                em.remove(tarefa);
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    public Tarefa buscarPorId(Long id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Tarefa.class, id);
        } finally {
            em.close();
        }
    }

    public List<Tarefa> listarTodas() {
        EntityManager em = getEntityManager();
        try {
            TypedQuery<Tarefa> query = em.createQuery("SELECT t FROM Tarefa t", Tarefa.class);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Monta uma consulta JPQL adaptando os filtros informados sem exigir todos os parâmetros.
     */
    public List<Tarefa> filtrar(Long id, String tituloOuDescricao, String responsavel,
                                Tarefa.Prioridade prioridade, Tarefa.Situacao situacao) {

        EntityManager em = getEntityManager();

        try {
            StringBuilder jpql = new StringBuilder("SELECT t FROM Tarefa t WHERE 1=1");

            if (id != null) {
                jpql.append(" AND t.id = :id");
            }
            if (tituloOuDescricao != null && !tituloOuDescricao.isEmpty()) {
                jpql.append(" AND (LOWER(t.titulo) LIKE LOWER(:titulo) OR LOWER(t.descricao) LIKE LOWER(:titulo))");
            }
            if (responsavel != null && !responsavel.isEmpty()) {
                jpql.append(" AND LOWER(t.responsavel) = LOWER(:responsavel)");
            }
            if (prioridade != null) {
                jpql.append(" AND t.prioridade = :prioridade");
            }
            if (situacao != null) {
                jpql.append(" AND t.situacao = :situacao");
            }

            TypedQuery<Tarefa> query = em.createQuery(jpql.toString(), Tarefa.class);

            if (id != null) query.setParameter("id", id);
            if (tituloOuDescricao != null && !tituloOuDescricao.isEmpty())
                query.setParameter("titulo", "%" + tituloOuDescricao + "%");
            if (responsavel != null && !responsavel.isEmpty())
                query.setParameter("responsavel", responsavel);
            if (prioridade != null) query.setParameter("prioridade", prioridade);
            if (situacao != null) query.setParameter("situacao", situacao);

            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Resolve configurações de banco para ambientes Heroku/Render baseadas em variáveis de ambiente.
     */
    private Map<String, Object> resolveCloudDatabaseProperties() {
        try {
            Map<String, String> env = System.getenv();
            String jdbcUrlVar = trimToNull(env.get("JDBC_DATABASE_URL"));
            String databaseUrlVar = trimToNull(env.get("DATABASE_URL"));
            String jdbcUserVar = trimToNull(env.get("JDBC_DATABASE_USERNAME"));
            String jdbcPassVar = trimToNull(env.get("JDBC_DATABASE_PASSWORD"));

            DatabaseCredentials credentials = null;

            if (jdbcUrlVar != null) {
                credentials = parseDatabaseUrl(jdbcUrlVar);
                System.out.println("🔍 Usando JDBC_DATABASE_URL para configuração do banco.");
            } else if (databaseUrlVar != null) {
                credentials = parseDatabaseUrl(databaseUrlVar);
                System.out.println("🔍 Usando DATABASE_URL para configuração do banco.");
            } else {
                credentials = resolveFromPgPieces(env);
            }

            if (credentials == null) {
                System.out.println("ℹ️ Variáveis de ambiente JDBC_DATABASE_URL/DATABASE_URL/PG* não encontradas.");
                return null;
            }

            String username = jdbcUserVar != null ? jdbcUserVar : credentials.username;
            String password = jdbcPassVar != null ? jdbcPassVar : credentials.password;

            if (username == null) {
                username = trimToNull(env.get("PGUSER"));
            }
            if (password == null) {
                password = trimToNull(env.get("PGPASSWORD"));
            }

            Map<String, Object> props = new HashMap<>();
            props.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
            props.put("javax.persistence.jdbc.url", credentials.jdbcUrl);

            if (username != null) {
                props.put("javax.persistence.jdbc.user", username);
            }
            if (password != null) {
                props.put("javax.persistence.jdbc.password", password);
            }

            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("hibernate.hbm2ddl.auto", "update");
            props.put("hibernate.show_sql", "false");
            props.put("hibernate.format_sql", "false");

            return props;
        } catch (URISyntaxException ex) {
            System.err.println("❌ Não foi possível interpretar as variáveis de ambiente do banco: " + ex.getMessage());
            return null;
        }
    }

    private DatabaseCredentials parseDatabaseUrl(String rawUrl) throws URISyntaxException {
        String sanitized = trimToNull(rawUrl);
        if (sanitized == null) {
            return null;
        }

        if (sanitized.startsWith("jdbc:")) {
            sanitized = sanitized.substring("jdbc:".length());
        }
        if (sanitized.startsWith("postgres://")) {
            sanitized = "postgresql://" + sanitized.substring("postgres://".length());
        }

        URI uri = new URI(sanitized);

        StringBuilder jdbcBuilder = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost());

        if (uri.getPort() != -1) {
            jdbcBuilder.append(":").append(uri.getPort());
        }

        if (uri.getPath() != null) {
            jdbcBuilder.append(uri.getPath());
        }

        if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
            jdbcBuilder.append("?").append(uri.getQuery());
        }

        String jdbcUrl = jdbcBuilder.toString();
        if (!jdbcUrl.contains("sslmode=")) {
            jdbcUrl = jdbcUrl + (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require";
        }

        String username = null;
        String password = null;

        if (uri.getUserInfo() != null) {
            String[] parts = uri.getUserInfo().split(":", 2);
            username = parts[0];
            if (parts.length > 1) {
                password = parts[1];
            }
        }

        return new DatabaseCredentials(jdbcUrl, username, password);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DatabaseCredentials resolveFromPgPieces(Map<String, String> env) {
        String host = trimToNull(env.get("PGHOST"));
        String db = trimToNull(env.get("PGDATABASE"));

        if (host == null || db == null) {
            return null;
        }

        String port = trimToNull(env.get("PGPORT"));

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
        if (port != null) {
            jdbc.append(":").append(port);
        }
        jdbc.append("/").append(db);

        String extraOptions = trimToNull(env.get("PGSSLMODE"));
        if (extraOptions != null) {
            jdbc.append("?sslmode=").append(extraOptions);
        } else {
            jdbc.append("?sslmode=require");
        }

        System.out.println("🔍 Montando JDBC a partir das variáveis PGHOST/PGDATABASE.");
        return new DatabaseCredentials(jdbc.toString(), null, null);
    }

    private static class DatabaseCredentials {
        private final String jdbcUrl;
        private final String username;
        private final String password;

        private DatabaseCredentials(String jdbcUrl, String username, String password) {
            this.jdbcUrl = jdbcUrl;
            this.username = username;
            this.password = password;
        }
    }
}
