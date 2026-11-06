package br.com.axionweb.backend.exception;

import br.com.axionweb.backend.dto.erro.ErroResponseDTO;
import br.com.axionweb.backend.dto.erro.ErroIntegracaoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponseDTO> emailDuplicado(EmailJaCadastradoException exception) {
        return erro(409, exception.getMessage());
    }

    @ExceptionHandler(ProcessoNaoEncontradoException.class)
    public ResponseEntity<ErroResponseDTO> processoNaoEncontrado(ProcessoNaoEncontradoException exception) {
        return erro(404, exception.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponseDTO> autenticacao() {
        return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer")
                .body(new ErroResponseDTO(401, "Credenciais inválidas ou autenticação necessária"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponseDTO> acessoNegado() {
        return erro(403, "Acesso negado");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponseDTO> validacao(MethodArgumentNotValidException exception) {
        Map<String, String> campos = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(field -> campos.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResponseDTO(400, "Dados inválidos", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponseDTO> jsonInvalido() {
        return erro(400, "Corpo da requisição inválido");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErroResponseDTO> parametrosInvalidos() {
        return erro(400, "Parâmetros da requisição inválidos");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponseDTO> metodoInvalido() {
        return erro(405, "Método HTTP não permitido");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponseDTO> formatoInvalido() {
        return erro(415, "Formato da requisição não suportado");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResponseDTO> status(ResponseStatusException exception) {
        return erro(exception.getStatusCode().value(), exception.getReason());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponseDTO> recursoNaoEncontrado() {
        return erro(404, "Recurso não encontrado");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponseDTO> integridade() {
        return erro(409, "A operação conflita com os dados cadastrados");
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErroResponseDTO> sinteseConcorrente() {
        return erro(409, "Esta execução foi atualizada durante a geração. Recarregue a síntese salva antes de tentar novamente.");
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErroResponseDTO> integracao() {
        return erro(502, "Não foi possível consultar o serviço externo");
    }

    @ExceptionHandler(ConsultaCnjException.class)
    public ResponseEntity<ErroIntegracaoDTO> consultaCnj(ConsultaCnjException exception) {
        var resposta = ResponseEntity.status(exception.getStatus());
        if (exception.getTentarEm() != null) {
            long restante = Math.max(0, exception.getTentarEm() - System.currentTimeMillis());
            resposta.header("Retry-After", Long.toString(Math.max(1, (restante + 999) / 1000)));
        }
        return resposta.body(new ErroIntegracaoDTO(
                exception.getStatus().value(), exception.getCodigo(), exception.getMessage()));
    }

    @ExceptionHandler(AnaliseComunicacaoException.class)
    public ResponseEntity<ErroIntegracaoDTO> analiseComunicacao(AnaliseComunicacaoException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ErroIntegracaoDTO(
                exception.getStatus().value(), exception.getCodigo(), exception.getMessage()));
    }

    @ExceptionHandler(ConsultaExternaException.class)
    public ResponseEntity<ErroIntegracaoDTO> consultaExterna(ConsultaExternaException exception) {
        var causa = exception.getCause();
        var http = causa instanceof RestClientResponseException resposta ? resposta.getStatusCode().value() : null;
        if (causa instanceof DataJudAguardarException aguardar) http = aguardar.getStatusExterno();
        // Não registra corpo da resposta externa, credenciais ou conteúdo de processos.
        log.warn("Análise cancelada: serviço={}, tipo={}, httpExterno={}",
                exception.getServico(), causa.getClass().getSimpleName(), http);
        String servico = exception.getServico() == ConsultaExternaException.Servico.DATAJUD ? "DataJud" : "DJEN (comunicações)";
        if (Integer.valueOf(429).equals(http)) {
            return ResponseEntity.status(502).body(new ErroIntegracaoDTO(502,
                    "CONSULTA_" + exception.getServico() + "_LIMITADA",
                    "O " + servico + " limitou as consultas. Aguarde antes de tentar novamente. A análise não foi salva; os resultados anteriores foram preservados."));
        }
        return ResponseEntity.status(502).body(new ErroIntegracaoDTO(502,
                "CONSULTA_" + exception.getServico() + "_FALHOU",
                "Não foi possível consultar o " + servico + ". A análise não foi salva; os resultados anteriores foram preservados."));
    }

    @ExceptionHandler(EstatisticasSemProcessosException.class)
    public ResponseEntity<ErroIntegracaoDTO> estatisticasSemProcessos(EstatisticasSemProcessosException exception) {
        return ResponseEntity.status(422).body(new ErroIntegracaoDTO(422,
                "DATAJUD_SEM_PROCESSOS", exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> inesperado(Exception exception) {
        log.error("Erro inesperado ao processar requisição", exception);
        return erro(500, "Não foi possível concluir a operação");
    }

    private ResponseEntity<ErroResponseDTO> erro(int status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponseDTO(status, mensagem));
    }
}
