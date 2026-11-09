package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.dto.AgendamentoDTO;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.*;
import com.ifpb.sorrisus.service.AgendamentoService;
import com.ifpb.sorrisus.repository.DentistaRepository;
import com.ifpb.sorrisus.repository.PacienteRepository;
import com.ifpb.sorrisus.service.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;
    private final PacienteRepository pacienteRepository;
    private final DentistaRepository dentistaRepository;
    private final WhatsAppService whatsAppService;

    public AgendamentoController(AgendamentoService service,
                                 PacienteRepository pacienteRepository,
                                 DentistaRepository dentistaRepository,
                                 WhatsAppService whatsAppService) {
        this.service = service;
        this.pacienteRepository = pacienteRepository;
        this.dentistaRepository = dentistaRepository;
        this.whatsAppService = whatsAppService;
    }

    @PostMapping
    public ResponseEntity<AgendamentoDTO> criar(@Valid @RequestBody AgendamentoDTO dto) {
        Agendamento ag = new Agendamento();
        ag.setDataHora(dto.getDataHora());
        ag.setObservacao(dto.getObservacao());
        ag.setServicoId(dto.getServicoId());

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId()).orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));
        Dentista dentista = dentistaRepository.findById(dto.getDentistaId()).orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));

        ag.setPaciente(paciente);
        ag.setDentista(dentista);

        Agendamento salvo = service.criar(ag);

        AgendamentoDTO resp = toDTO(salvo);

        // Envio de Agendamento para o paciente
        if (paciente.getTelefone() != null) {
            String mensagem = "Olá " + paciente.getNome() + ",\n\n"
                    + "Seu agendamento foi realizado com sucesso.\n"
                    + "🗓 Data e Horário: " + ag.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")) + "\n"
                    + "📍 Local: Rua Francisco de Melo, 304, centro. Sumé-PB " + "\n\n"
                    + "Por favor, chegue com 10 minutos de antecedência e traga seus documentos pessoais.\n\n"
                    + "Atenciosamente,\n"
                    + "Equipe Sorrisus";

            whatsAppService.sendMessage(paciente.getTelefone(), mensagem);
        }

        return ResponseEntity.created(URI.create("/api/agendamentos/" + salvo.getId())).body(resp);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody AgendamentoDTO dto) {
        Agendamento ag = new Agendamento();
        ag.setDataHora(dto.getDataHora());
        ag.setObservacao(dto.getObservacao());
        ag.setServicoId(dto.getServicoId());

        Paciente paciente = pacienteRepository.findById(dto.getPacienteId()).orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));;
        Dentista dentista = dentistaRepository.findById(dto.getDentistaId()).orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));;

        ag.setPaciente(paciente);
        ag.setDentista(dentista);

        Agendamento atualizado = service.atualizar(id, ag);
        return ResponseEntity.ok(toDTO(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<AgendamentoDTO> confirmar(@PathVariable Long id) {
        Agendamento confirmado = service.confirmar(id);
        return ResponseEntity.ok(toDTO(confirmado));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyAuthority('ROLE_DENTISTA', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<AgendamentoDTO> cancelar(@PathVariable Long id) {
        Agendamento cancelado = service.cancelar(id);
        return ResponseEntity.ok(toDTO(cancelado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoDTO> buscarPorId(@PathVariable Long id) {
        Agendamento a = service.buscarPorId(id);
        return ResponseEntity.ok(toDTO(a));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<AgendamentoDTO>> listarPorPaciente(@PathVariable Long pacienteId) {
        List<AgendamentoDTO> list = service.listarPorPaciente(pacienteId).stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/dentista/{dentistaId}")
    public ResponseEntity<List<AgendamentoDTO>> listarPorDentista(@PathVariable Long dentistaId) {
        List<AgendamentoDTO> list = service.listarPorDentista(dentistaId).stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private AgendamentoDTO toDTO(Agendamento a) {
        AgendamentoDTO dto = new AgendamentoDTO();
        dto.setId(a.getId());
        dto.setDataHora(a.getDataHora());
        dto.setPacienteId(a.getPaciente().getId());
        dto.setDentistaId(a.getDentista().getId());
        dto.setServicoId(a.getServicoId());
        dto.setConfirmado(a.isConfirmado());
        dto.setStatus(a.getStatus() != null ? a.getStatus().name() : null);
        dto.setObservacao(a.getObservacao());
        if (a.getCriadoPor() != null) dto.setRecepcionistaId(a.getCriadoPor().getId());
        return dto;
    }
}
