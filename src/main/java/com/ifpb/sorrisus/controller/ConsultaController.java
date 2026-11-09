package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.dto.ConsultaDTO;
import com.ifpb.sorrisus.dto.ConsultaHistoricoRecepcaoDTO;
import com.ifpb.sorrisus.dto.ProntuarioDTO;
import com.ifpb.sorrisus.dto.RetornoPendenteDTO;
import com.ifpb.sorrisus.model.*;
import com.ifpb.sorrisus.service.ConsultaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {

    private final ConsultaService service;

    public ConsultaController(ConsultaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ConsultaDTO> criar(@Valid @RequestBody ConsultaDTO dto) {
        Consulta c = new Consulta();
        c.setDataHora(dto.getDataHora());
        c.setObservacao(dto.getObservacao());

        if (dto.getStatus() != null) {
            c.setStatus(StatusConsulta.valueOf(dto.getStatus()));
        } else {
            c.setStatus(StatusConsulta.CONFIRMADA);
        }

        Paciente p = new Paciente();
        p.setId(dto.getPacienteId());
        Dentista d = new Dentista();
        d.setId(dto.getDentistaId());
        c.setPaciente(p);
        c.setDentista(d);

        if (dto.getProntuario() != null) {
            Prontuario prontuario = new Prontuario();
            prontuario.setObservacoes(dto.getProntuario().getObservacoes());
            c.setProntuario(prontuario);
        }

        Consulta salvo = service.criar(c);

        return ResponseEntity.created(URI.create("/api/consultas/" + salvo.getId())).body(toDTO(salvo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsultaDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ConsultaDTO dto) {
        Consulta c = new Consulta();
        c.setDataHora(dto.getDataHora());
        c.setObservacao(dto.getObservacao());

        if (dto.getStatus() != null)
            c.setStatus(StatusConsulta.valueOf(dto.getStatus()));

        if (dto.getProntuario() != null) {
            Prontuario p = new Prontuario();
            p.setObservacoes(dto.getProntuario().getObservacoes());
            c.setProntuario(p);
        }

        Consulta atualizado = service.atualizar(id, c);
        return ResponseEntity.ok(toDTO(atualizado));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ConsultaDTO> alterarStatus(@PathVariable Long id, @RequestParam String status) {
        Consulta c = new Consulta();
        c.setStatus(StatusConsulta.valueOf(status));
        Consulta salvo = service.atualizar(id, c);
        return ResponseEntity.ok(toDTO(salvo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        service.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultaDTO> buscarPorId(@PathVariable Long id) {
        Consulta c = service.buscarPorId(id);
        return ResponseEntity.ok(toDTO(c));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<ConsultaDTO>> listarPorPaciente(@PathVariable Long pacienteId) {
        List<ConsultaDTO> list = service.listarPorPaciente(pacienteId).stream()
                .map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
  
    @GetMapping("/dentista/{dentistaId}")
    public ResponseEntity<List<ConsultaDTO>> listarPorDentista(@PathVariable Long dentistaId) {

        List<ConsultaDTO> list = service.listarPorDentista(dentistaId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/historico/recepcao")
    @PreAuthorize("hasAuthority('ROLE_RECEPCIONISTA')")
    public ResponseEntity<List<ConsultaHistoricoRecepcaoDTO>> listarHistoricoParaRecepcao() {
        List<ConsultaHistoricoRecepcaoDTO> list = service.listarParaRecepcao().stream()
                .map(this::toHistoricoRecepcaoDTO)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/retornos-pendentes")
    public ResponseEntity<List<RetornoPendenteDTO>> listarRetornosPendentes() {
        List<RetornoPendenteDTO> retornos = service.verificarRetornosPendentes()
                .stream()
                .map(this::toRetornoDTO)
                .toList();

        return ResponseEntity.ok(retornos);
    }

    @GetMapping("/retornos-pendentes/dentista/{dentistaId}")
    public ResponseEntity<List<RetornoPendenteDTO>> listarRetornosPendentesPorDentista(
            @PathVariable Long dentistaId) {

        List<RetornoPendenteDTO> retornos = service.verificarRetornosPendentesPorDentista(dentistaId)
                .stream()
                .map(this::toRetornoDTO)
                .toList();

        return ResponseEntity.ok(retornos);
    }


    private RetornoPendenteDTO toRetornoDTO(RetornoPendente r) {
        RetornoPendenteDTO dto = new RetornoPendenteDTO();
        dto.setPacienteId(r.getPaciente().getId());
        dto.setPacienteNome(r.getPaciente().getNome());
        dto.setPacienteEmail(r.getPaciente().getEmail());
        dto.setPacienteTelefone(r.getPaciente().getTelefone());
        dto.setDataUltimaConsulta(r.getDataUltimaConsulta());
        dto.setDiasDesdeUltimaConsulta((long) r.getDiasDesdeUltimaConsulta());
        dto.setDentistaId(r.getUltimaConsulta().getDentista().getId());
        dto.setDentistaNome(r.getUltimaConsulta().getDentista().getNome());
        dto.setPrioridade(r.getPrioridade());
        return dto;
    }

    private ConsultaDTO toDTO(Consulta c) {
        ConsultaDTO dto = new ConsultaDTO();
        dto.setId(c.getId());
        dto.setDataHora(c.getDataHora());

        dto.setDentistaId(c.getDentista().getId());
        dto.setPacienteId(c.getPaciente().getId());

        if (c.getPaciente() != null) {
            dto.setNomePaciente(c.getPaciente().getNome());
        }
        if (c.getDentista() != null) {
            dto.setNomeDentista(c.getDentista().getNome());
        }

        dto.setStatus(c.getStatus().name());
        dto.setObservacao(c.getObservacao());

        if (c.getProntuario() != null) {
            ProntuarioDTO p = new ProntuarioDTO();
            p.setId(c.getProntuario().getId());
            p.setObservacoes(c.getProntuario().getObservacoes());
            dto.setProntuario(p);
        }
        return dto;
    }

    private ConsultaHistoricoRecepcaoDTO toHistoricoRecepcaoDTO(Consulta c) {
        ConsultaHistoricoRecepcaoDTO dto = new ConsultaHistoricoRecepcaoDTO();
        dto.setId(c.getId());
        dto.setDataHora(c.getDataHora());
        dto.setNomePaciente(c.getPaciente() != null ? c.getPaciente().getNome() : null);
        dto.setNomeDentista(c.getDentista() != null ? c.getDentista().getNome() : null);
        dto.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
        dto.setObservacao(c.getObservacao());
        return dto;
    }
}
