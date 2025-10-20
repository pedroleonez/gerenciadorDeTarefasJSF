package pedroleonez.gerenciadordetarefasjsf.repository;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import pedroleonez.gerenciadordetarefasjsf.model.Tarefa;

import java.util.List;

/** Repositório JPA com operações CRUD e filtros dinâmicos para {@link Tarefa}. */
public class TarefaRepository {
    private static final EntityManagerFactory emf = buildEntityManagerFactory();

    public TarefaRepository() {}

    /** Cria o `EntityManagerFactory`, permitindo sobrescrever a PU em testes via system property. */
    private static EntityManagerFactory buildEntityManagerFactory() {
        String persistenceUnit = System.getProperty("tarefas.persistence.unit", "tarefasPU");
        return Persistence.createEntityManagerFactory(persistenceUnit);
    }

    public void salvar(Tarefa tarefa) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.persist(tarefa);
        em.getTransaction().commit();
        em.close();
    }

    public void atualizar(Tarefa tarefa) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.merge(tarefa);
        em.getTransaction().commit();
        em.close();
    }

    public void remover(Long id) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Tarefa tarefa = em.find(Tarefa.class, id);
        if (tarefa != null) {
            em.remove(tarefa);
        }
        em.getTransaction().commit();
        em.close();
    }

    public Tarefa buscarPorId(Long id) {
        EntityManager em = emf.createEntityManager();
        Tarefa tarefa = em.find(Tarefa.class, id);
        em.close();
        return tarefa;
    }

    public List<Tarefa> listarTodas() {
        EntityManager em = emf.createEntityManager();
        TypedQuery<Tarefa> query = em.createQuery("SELECT t FROM Tarefa t", Tarefa.class);
        List<Tarefa> tarefas = query.getResultList();
        em.close();
        return tarefas;
    }

    /** Monta uma consulta JPQL adaptando os filtros informados sem exigir todos os parâmetros. */
    public List<Tarefa> filtrar(Long id, String tituloOuDescricao, String responsavel,
                                Tarefa.Prioridade prioridade, Tarefa.Situacao situacao) {
        EntityManager em = emf.createEntityManager();

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
        if (tituloOuDescricao != null && !tituloOuDescricao.isEmpty()) query.setParameter("titulo", "%" + tituloOuDescricao + "%");
        if (responsavel != null && !responsavel.isEmpty()) query.setParameter("responsavel", responsavel);
        if (prioridade != null) query.setParameter("prioridade", prioridade);
        if (situacao != null) query.setParameter("situacao", situacao);

        List<Tarefa> tarefas = query.getResultList();
        em.close();
        return tarefas;
    }
}
