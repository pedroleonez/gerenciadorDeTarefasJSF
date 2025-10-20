package pedroleonez.gerenciadordetarefasjsf.repository;

import pedroleonez.gerenciadordetarefasjsf.model.Tarefa;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.*;
import java.util.*;

/**
 * Repositório JPA com operações CRUD e filtros dinâmicos para {@link Tarefa}.
 */
@ApplicationScoped
public class TarefaRepository {

    @PersistenceUnit(unitName = "tarefasPU")
    private EntityManagerFactory emf;

    public TarefaRepository() {
        // fallback: inicializa manualmente se @PersistenceUnit falhar (Heroku/Tomcat)
        if (this.emf == null) {
            System.out.println("⚙️ Inicializando EntityManagerFactory manualmente...");
            this.emf = buildEntityManagerFactory();
        }
    }

    /**
     * Cria o EntityManagerFactory dinamicamente.
     * Detecta DATABASE_URL (Heroku) e converte para formato JDBC PostgreSQL.
     */
    private EntityManagerFactory buildEntityManagerFactory() {
        try {
            String databaseUrl = System.getenv("DATABASE_URL");
            Map<String, Object> props = new HashMap<>();

            if (databaseUrl != null && databaseUrl.startsWith("postgres://")) {
                // converte "postgres://user:pass@host:port/db" → "jdbc:postgresql://host:port/db"
                String jdbcUrl = "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
                props.put("javax.persistence.jdbc.url", jdbcUrl);
                props.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
                props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
                props.put("hibernate.hbm2ddl.auto", "update");
                props.put("hibernate.show_sql", "true");
                props.put("hibernate.format_sql", "true");
                props.put("hibernate.connection.sslmode", "require");

                System.out.println("✅ Conectando ao PostgreSQL do Heroku: " + jdbcUrl);
            } else {
                // fallback local (H2)
                System.err.println("⚠️ DATABASE_URL não encontrada. Usando H2 em memória.");
                props.put("javax.persistence.jdbc.driver", "org.h2.Driver");
                props.put("javax.persistence.jdbc.url", "jdbc:h2:mem:tarefas;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
                props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
                props.put("hibernate.hbm2ddl.auto", "update");
                props.put("hibernate.show_sql", "true");
            }

            return Persistence.createEntityManagerFactory("tarefasPU", props);
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
}
