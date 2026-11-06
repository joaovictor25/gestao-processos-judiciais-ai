package br.com.axionweb.backend.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("E-mail já cadastrado");
    }
}
