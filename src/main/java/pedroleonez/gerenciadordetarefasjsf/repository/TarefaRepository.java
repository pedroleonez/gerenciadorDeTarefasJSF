package pedroleonez.gerenciadordetarefasjsf.repository;

import pedroleonez.gerenciadordetarefasjsf.model.Tarefa;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Repositório JPA com operações CRUD e filtros dinâmicos para {@link Tarefa}.
 */
@ApplicationScoped
public class TarefaRepository {

    @PersistenceUnit(unitName = "tarefasPU")
    private EntityManagerFactory emf;

    public TarefaRepository() {}

    private EntityManager getEntityManager() {
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
