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

    // Inicializa o EntityManagerFactory logo após o CDI criar o bean.
    @PostConstruct
    public void init() {
        try {
            if (this.emf == null) {
                System.out.println("[DB] Inicializando EntityManagerFactory (TarefaRepository.init).");

                Map<String, Object> cloudProps = resolveCloudDatabaseProperties();

                if (cloudProps != null) {
                    System.out.println("[DB] Ambiente gerenciado detectado. Utilizando variáveis de ambiente para configurar o banco.");
                    this.emf = Persistence.createEntityManagerFactory("tarefasPU", cloudProps);
                } else {
                    System.out.println("[DB] Variáveis de ambiente específicas não encontradas. Utilizando persistence.xml (ambiente local).");
                    this.emf = Persistence.createEntityManagerFactory("tarefasPU");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Erro ao inicializar EntityManagerFactory: " + e.getMessage(), e);
        }
    }

    // Garante acesso ao EntityManager recriando o factory caso ele ainda não exista.
    private EntityManager getEntityManager() {
        if (emf == null) {
            System.out.println("[DB] EntityManagerFactory não inicializado. Tentando reinicializar...");
            init();
            if (emf == null) {
                throw new IllegalStateException("EntityManagerFactory não foi inicializado.");
            }
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

            // Preferência: URL já em formato JDBC -> URL estilo Heroku -> peças PGHOST/PGDATABASE.
            if (jdbcUrlVar != null) {
                credentials = parseDatabaseUrl(jdbcUrlVar, true);
                System.out.println("[DB] Usando JDBC_DATABASE_URL.");
            } else if (databaseUrlVar != null) {
                credentials = parseDatabaseUrl(databaseUrlVar, false);
                System.out.println("[DB] Usando DATABASE_URL.");
            } else {
                credentials = resolveFromPgPieces(env);
                if (credentials != null) {
                    System.out.println("[DB] Usando variáveis PGHOST/PGDATABASE.");
                }
            }

            if (credentials == null) {
                System.out.println("[DB] Variáveis JDBC_DATABASE_URL/DATABASE_URL/PG* não encontradas.");
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
            // Completa o conjunto de propriedades enviadas ao Hibernate quando estivermos na nuvem.
            props.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
            props.put("javax.persistence.jdbc.url", credentials.jdbcUrl);

            if (username != null) {
                props.put("javax.persistence.jdbc.user", username);
            }
            if (password != null) {
                props.put("javax.persistence.jdbc.password", password);
            }

            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("hibernate.hbm2ddl.auto", "create");
            props.put("hibernate.show_sql", "false");
            props.put("hibernate.format_sql", "false");

            return props;
        } catch (URISyntaxException ex) {
            System.err.println("[DB] Não foi possível interpretar as variáveis de ambiente do banco: " + ex.getMessage());
            return null;
        }
    }

    // Converte as URLs suportadas (jdbc, postgres://, postgresql://) para um JDBC completo + sslmode.
    private DatabaseCredentials parseDatabaseUrl(String rawUrl, boolean alreadyJdbc) throws URISyntaxException {
        String sanitized = trimToNull(rawUrl);
        if (sanitized == null) {
            return null;
        }

        String jdbcCandidate = sanitized;
        if (!alreadyJdbc) {
            if (jdbcCandidate.startsWith("postgres://")) {
                jdbcCandidate = "jdbc:postgresql://" + jdbcCandidate.substring("postgres://".length());
            } else if (jdbcCandidate.startsWith("postgresql://")) {
                jdbcCandidate = "jdbc:" + jdbcCandidate;
            }
        }

        URI uri = new URI(stripJdbcPrefix(jdbcCandidate));

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

        System.out.println("[DB] JDBC final montado: " + jdbcUrl);
        return new DatabaseCredentials(jdbcUrl, username, password);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String stripJdbcPrefix(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring("jdbc:".length()) : jdbcUrl;
    }

    // Fallback para plataformas que exportam variáveis PGHOST/PGDATABASE/PGPORT em vez da URL completa.
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
        jdbc.append("?sslmode=").append(extraOptions != null ? extraOptions : "require");

        System.out.println("[DB] Montando JDBC a partir de PGHOST/PGDATABASE.");
        return new DatabaseCredentials(jdbc.toString(), null, null);
    }

    // Estrutura simples para transporte das credenciais derivadas das variáveis de ambiente.
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
