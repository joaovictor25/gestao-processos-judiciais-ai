package br.com.axionweb.backend.exception;

public class ProcessoNaoEncontradoException extends RuntimeException {
    public ProcessoNaoEncontradoException() {
        super("Processo não encontrado");
    }
}
