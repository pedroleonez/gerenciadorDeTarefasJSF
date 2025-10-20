package pedroleonez.gerenciadordetarefasjsf.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pedroleonez.gerenciadordetarefasjsf.model.Tarefa;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Garante que o repositório opere corretamente contra o banco em memória H2. */
class TarefaRepositoryTest {

    private TarefaRepository repository;

    @BeforeAll
    static void configurarPersistenceUnit() {
        System.setProperty("tarefas.persistence.unit", "tarefasPU-test");
    }

    @AfterAll
    static void limparPersistenceUnit() {
        System.clearProperty("tarefas.persistence.unit");
    }

    @BeforeEach
    void setUp() {
        repository = new TarefaRepository();
        limparBanco();
    }

    @Test
    void salvarDevePersistirComTodosOsDados() {
        Tarefa tarefa = novaTarefa("Implementar funcionalidade", "Criar endpoint", "João",
                Tarefa.Prioridade.ALTA, Tarefa.Situacao.EM_ANDAMENTO, LocalDate.now().plusDays(3));

        repository.salvar(tarefa);

        List<Tarefa> tarefas = repository.listarTodas();
        assertEquals(1, tarefas.size(), "Deve existir exatamente uma tarefa persistida");

        Tarefa armazenada = tarefas.get(0);
        assertNotNull(armazenada.getId(), "A tarefa deve receber um ID após persistência");
        assertEquals("Implementar funcionalidade", armazenada.getTitulo());
        assertEquals("Criar endpoint", armazenada.getDescricao());
        assertEquals("João", armazenada.getResponsavel());
        assertEquals(Tarefa.Prioridade.ALTA, armazenada.getPrioridade());
        assertEquals(Tarefa.Situacao.EM_ANDAMENTO, armazenada.getSituacao());
    }

    @Test
    void atualizarDeveAplicarAlteracoesPersistidas() {
        Tarefa tarefa = novaTarefa("Revisar layout", "Ajustar CSS", "Maria",
                Tarefa.Prioridade.MEDIA, Tarefa.Situacao.EM_ANDAMENTO, LocalDate.now().plusDays(5));
        repository.salvar(tarefa);

        tarefa.setTitulo("Revisar layout principal");
        tarefa.setDescricao("Ajustar CSS e componentes");
        tarefa.setSituacao(Tarefa.Situacao.CONCLUIDA);
        repository.atualizar(tarefa);

        Tarefa atualizada = repository.buscarPorId(tarefa.getId());
        assertNotNull(atualizada, "A tarefa deve ser encontrada pelo ID");
        assertEquals("Revisar layout principal", atualizada.getTitulo());
        assertEquals("Ajustar CSS e componentes", atualizada.getDescricao());
        assertEquals(Tarefa.Situacao.CONCLUIDA, atualizada.getSituacao());
    }

    @Test
    void removerDeveExcluirRegistro() {
        Tarefa tarefa = novaTarefa("Remover tarefa", "Verificar exclusão", "Carlos",
                Tarefa.Prioridade.BAIXA, Tarefa.Situacao.EM_ANDAMENTO, LocalDate.now().plusDays(2));
        repository.salvar(tarefa);

        repository.remover(tarefa.getId());

        assertTrue(repository.listarTodas().isEmpty(), "A lista deve ficar vazia após remover a tarefa");
    }

    @Test
    void filtrarDeveRespeitarParametros() {
        Tarefa tarefaA = novaTarefa("Planejar sprint", "Criar backlog inicial", "Ana",
                Tarefa.Prioridade.ALTA, Tarefa.Situacao.EM_ANDAMENTO, LocalDate.now().plusDays(4));
        Tarefa tarefaB = novaTarefa("Deploy produção", "Executar checklist completo", "carlos",
                Tarefa.Prioridade.MEDIA, Tarefa.Situacao.CONCLUIDA, LocalDate.now().plusDays(7));
        Tarefa tarefaC = novaTarefa("Refatorar módulo", "Melhorar performance", "Joana",
                Tarefa.Prioridade.BAIXA, Tarefa.Situacao.EM_ANDAMENTO, LocalDate.now().plusDays(10));

        repository.salvar(tarefaA);
        repository.salvar(tarefaB);
        repository.salvar(tarefaC);

        List<Tarefa> porTitulo = repository.filtrar(null, "sprint", null, null, null);
        assertEquals(1, porTitulo.size(), "Deve localizar tarefa pelo título");
        assertEquals("Planejar sprint", porTitulo.get(0).getTitulo());

        List<Tarefa> porDescricao = repository.filtrar(null, "checklist", null, null, null);
        assertEquals(1, porDescricao.size(), "Deve localizar tarefa pela descrição");
        assertEquals("Deploy produção", porDescricao.get(0).getTitulo());

        List<Tarefa> porResponsavel = repository.filtrar(null, null, "CARLOS", null, null);
        assertEquals(1, porResponsavel.size(), "Busca por responsável deve ser case-insensitive");
        assertEquals("Deploy produção", porResponsavel.get(0).getTitulo());

        List<Tarefa> porPrioridadeSituacao = repository.filtrar(null, null, null, Tarefa.Prioridade.BAIXA, Tarefa.Situacao.EM_ANDAMENTO);
        assertEquals(1, porPrioridadeSituacao.size(), "Filtro combinado deve retornar apenas correspondências exatas");
        assertEquals("Refatorar módulo", porPrioridadeSituacao.get(0).getTitulo());

        List<Tarefa> porId = repository.filtrar(tarefaA.getId(), null, null, null, null);
        assertEquals(1, porId.size(), "Busca por ID deve retornar a tarefa correspondente");
        assertEquals(tarefaA.getId(), porId.get(0).getId());
    }

    /** Remove todos os registros para manter os testes independentes. */
    private void limparBanco() {
        repository.listarTodas().forEach(tarefa -> repository.remover(tarefa.getId()));
    }

    private Tarefa novaTarefa(String titulo, String descricao, String responsavel,
                              Tarefa.Prioridade prioridade, Tarefa.Situacao situacao, LocalDate deadline) {
        Tarefa tarefa = new Tarefa();
        tarefa.setTitulo(titulo);
        tarefa.setDescricao(descricao);
        tarefa.setResponsavel(responsavel);
        tarefa.setPrioridade(prioridade);
        tarefa.setSituacao(situacao);
        tarefa.setDeadline(deadline);
        return tarefa;
    }
}
