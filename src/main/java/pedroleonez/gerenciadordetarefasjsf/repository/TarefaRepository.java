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
                    System.out.println("✅ Detectado ambiente Heroku/Render. Inicializando com banco gerenciado.");
                    this.emf = Persistence.createEntityManagerFactory("tarefasPU", cloudProps);
                } else {
                    System.out.println("✅ Ambiente local detectado. Utilizando configurações de persistence.xml.");
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
            String jdbcUrlVar = trimToNull(System.getenv("JDBC_DATABASE_URL"));
            String databaseUrlVar = trimToNull(System.getenv("DATABASE_URL"));
            String jdbcUserVar = trimToNull(System.getenv("JDBC_DATABASE_USERNAME"));
            String jdbcPassVar = trimToNull(System.getenv("JDBC_DATABASE_PASSWORD"));

            DatabaseCredentials credentials = null;

            if (jdbcUrlVar != null) {
                credentials = parseDatabaseUrl(jdbcUrlVar);
            } else if (databaseUrlVar != null) {
                credentials = parseDatabaseUrl(databaseUrlVar);
            }

            if (credentials == null) {
                return null;
            }

            String username = jdbcUserVar != null ? jdbcUserVar : credentials.username;
            String password = jdbcPassVar != null ? jdbcPassVar : credentials.password;

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
