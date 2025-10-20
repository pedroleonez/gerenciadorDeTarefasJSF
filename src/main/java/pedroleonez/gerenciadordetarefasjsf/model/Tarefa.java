package pedroleonez.gerenciadordetarefasjsf.model;

import javax.persistence.*;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/** Entidade JPA que representa uma tarefa gerenciada pela aplicação. */
@Entity
@Table(name = "tarefas")
public class Tarefa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o título da tarefa.")
    @Size(max = 120, message = "O título deve ter no máximo 120 caracteres.")
    private String titulo;

    @NotBlank(message = "Informe a descrição da tarefa.")
    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
    private String descricao;

    @NotBlank(message = "Informe o responsável pela tarefa.")
    @Size(max = 80, message = "O responsável deve ter no máximo 80 caracteres.")
    private String responsavel;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Selecione uma prioridade.")
    private Prioridade prioridade;

    @NotNull(message = "Informe a data limite.")
    @FutureOrPresent(message = "A data limite não pode estar no passado.")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Informe a situação da tarefa.")
    private Situacao situacao;

    /** Níveis de prioridade exibidos para o usuário e salvos como texto. */
    public enum Prioridade {
        ALTA("Alta"),
        MEDIA("Média"),
        BAIXA("Baixa");

        private final String label;

        Prioridade(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Situações possíveis durante o ciclo de vida da tarefa. */
    public enum Situacao {
        EM_ANDAMENTO("Em andamento"),
        CONCLUIDA("Concluída");

        private final String label;

        Situacao(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public Prioridade getPrioridade() { return prioridade; }
    public void setPrioridade(Prioridade prioridade) { this.prioridade = prioridade; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public Situacao getSituacao() { return situacao; }
    public void setSituacao(Situacao situacao) { this.situacao = situacao; }

}
