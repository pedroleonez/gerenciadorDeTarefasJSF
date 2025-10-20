package pedroleonez.gerenciadordetarefasjsf.controller;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import pedroleonez.gerenciadordetarefasjsf.model.Tarefa;
import pedroleonez.gerenciadordetarefasjsf.repository.TarefaRepository;

/** Bean de sessão que coordena o CRUD de tarefas e a interação com a tela JSF. */
@Named
@SessionScoped
public class TarefaBean implements Serializable {
    private static final long serialVersionUID = 1L;

    // Tarefa manipulada pelos botões principais da tela
    private Tarefa tarefa = new Tarefa();

    // Buffer utilizado pelo diálogo modal de criação/edição
    private Tarefa novaTarefa = new Tarefa();

    // Resultado atualmente exibido na tabela
    private List<Tarefa> tarefas;

    private final TarefaRepository repository = new TarefaRepository();

    @Inject
    private transient Validator validator;

    // Parâmetros de filtragem aplicados na tabela principal
    private Long filtroId;
    private String filtroTitulo;
    private String filtroResponsavel;
    private Tarefa.Prioridade filtroPrioridade;
    private Tarefa.Situacao filtroSituacao = Tarefa.Situacao.EM_ANDAMENTO; // padrão

    // Campo de data associado ao diálogo modal
    private LocalDate data;

    // Listas auxiliares para combos e seletores
    private final List<Tarefa.Prioridade> prioridades = List.of(Tarefa.Prioridade.values());
    private final List<Tarefa.Situacao> situacoes = List.of(Tarefa.Situacao.values());
    private final List<String> responsaveis = List.of("João", "Maria", "Carlos", "Ana"); // exemplo

    /** Carrega a lista inicial exibindo apenas tarefas em andamento. */
    @PostConstruct
    public void init() {
        listarTarefas();
    }

    // ===========================
    // Métodos de manipulação
    // ===========================

    public void criarTarefa() {
        tarefa.setSituacao(Tarefa.Situacao.EM_ANDAMENTO);
        tarefa.setDeadline(data);
        repository.salvar(tarefa);
        tarefa = new Tarefa();
        data = null;
        listarTarefas();
    }

    public void atualizarTarefa() {
        tarefa.setDeadline(data);
        repository.atualizar(tarefa);
        tarefa = new Tarefa();
        data = null;
        listarTarefas();
    }

    public void removerTarefa(Long id) {
        repository.remover(id);
        listarTarefas();
    }

    /** Marca uma tarefa como concluída e atualiza a lista (ocultando-a) */
    public void concluirTarefa(Long id) {
        Tarefa t = repository.buscarPorId(id);
        if (t != null && t.getSituacao() == Tarefa.Situacao.EM_ANDAMENTO) {
            t.setSituacao(Tarefa.Situacao.CONCLUIDA);
            repository.atualizar(t);
        }
        // Após concluir, mostra apenas as tarefas ainda em andamento
        listarTarefas();
    }

    /** Lista apenas tarefas em andamento (filtro padrão) */
    public void listarTarefas() {
        List<Tarefa> todas = repository.listarTodas();
        // Exibir apenas tarefas em andamento por padrão
        tarefas = todas.stream()
                .filter(t -> t.getSituacao() == Tarefa.Situacao.EM_ANDAMENTO)
                .collect(Collectors.toList());
    }

    public void filtrarTarefas() {
        tarefas = repository.filtrar(filtroId, filtroTitulo, filtroResponsavel, filtroPrioridade, filtroSituacao);
    }

    // ===========================
    // Métodos para o diálogo (Nova / Editar)
    // ===========================

    /** Prepara o diálogo para criar uma nova tarefa (situação = EM_ANDAMENTO) */
    public void prepararNovaTarefa() {
        this.novaTarefa = new Tarefa();
        this.novaTarefa.setSituacao(Tarefa.Situacao.EM_ANDAMENTO);
        this.data = null;
    }

    /** Prepara o diálogo para editar a tarefa selecionada criando uma cópia independente. */
    public void prepararEdicao(Tarefa t) {
        Tarefa copia = new Tarefa();
        copia.setId(t.getId());
        copia.setTitulo(t.getTitulo());
        copia.setDescricao(t.getDescricao());
        copia.setResponsavel(t.getResponsavel());
        copia.setPrioridade(t.getPrioridade());
        copia.setSituacao(t.getSituacao());
        copia.setDeadline(t.getDeadline());
        this.novaTarefa = copia;
        this.data = t.getDeadline();
    }

    /**
     * Salva ou atualiza a tarefa do diálogo.
     * O bean executa a validação manualmente para consolidar mensagens sem duplicidades antes de persistir.
     */
    public void salvarTarefa() {
        novaTarefa.setDeadline(data);

        FacesContext context = FacesContext.getCurrentInstance();
        Set<ConstraintViolation<Tarefa>> violations = getValidator().validate(novaTarefa);

        if (!violations.isEmpty()) {
            Set<String> mensagensExistentes = context.getMessageList().stream()
                    .map(FacesMessage::getSummary)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());

            violations.forEach(violation -> {
                String mensagem = violation.getMessage();
                if (mensagensExistentes.add(mensagem)) {
                    context.addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, mensagem, null));
                }
            });
            context.validationFailed();
            return;
        }

        if (novaTarefa.getId() == null) {
            novaTarefa.setSituacao(Tarefa.Situacao.EM_ANDAMENTO);
            repository.salvar(novaTarefa);
        } else {
            repository.atualizar(novaTarefa);
        }

        novaTarefa = new Tarefa();
        data = null;
        listarTarefas();
    }

    // ===========================
    // Getters e Setters
    // ===========================

    public Tarefa getTarefa() { return tarefa; }
    public void setTarefa(Tarefa tarefa) { this.tarefa = tarefa; }

    public Tarefa getNovaTarefa() { return novaTarefa; }
    public void setNovaTarefa(Tarefa novaTarefa) { this.novaTarefa = novaTarefa; }

    public List<Tarefa> getTarefas() { return tarefas; }
    public void setTarefas(List<Tarefa> tarefas) { this.tarefas = tarefas; }

    public Long getFiltroId() { return filtroId; }
    public void setFiltroId(Long filtroId) { this.filtroId = filtroId; }

    public String getFiltroTitulo() { return filtroTitulo; }
    public void setFiltroTitulo(String filtroTitulo) { this.filtroTitulo = filtroTitulo; }

    public String getFiltroResponsavel() { return filtroResponsavel; }
    public void setFiltroResponsavel(String filtroResponsavel) { this.filtroResponsavel = filtroResponsavel; }

    public Tarefa.Prioridade getFiltroPrioridade() { return filtroPrioridade; }
    public void setFiltroPrioridade(Tarefa.Prioridade filtroPrioridade) { this.filtroPrioridade = filtroPrioridade; }

    public Tarefa.Situacao getFiltroSituacao() { return filtroSituacao; }
    public void setFiltroSituacao(Tarefa.Situacao filtroSituacao) { this.filtroSituacao = filtroSituacao; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalDate getToday() { return LocalDate.now(); }

    public List<Tarefa.Prioridade> getPrioridades() { return prioridades; }
    public List<Tarefa.Situacao> getSituacoes() { return situacoes; }
    public List<String> getResponsaveis() { return responsaveis; }

    private Validator getValidator() {
        if (validator == null) {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }
        return validator;
    }
}
