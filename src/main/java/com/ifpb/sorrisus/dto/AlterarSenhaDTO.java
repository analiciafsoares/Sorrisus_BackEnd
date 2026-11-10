package com.ifpb.sorrisus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AlterarSenhaDTO {

    @NotBlank(message = "Informe a senha atual.")
    private String senhaAtual;

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 6, message = "A nova senha deve ter pelo menos 6 caracteres.")
    private String novaSenha;
}
