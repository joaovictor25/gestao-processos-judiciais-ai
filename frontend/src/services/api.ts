const baseUrl = import.meta.env.DEV ? '' : (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');
export const SESSION_EXPIRED_EVENT = 'axion:session-expired';

export class ApiError extends Error {
  readonly status: number;
  readonly retryAfterMs?: number;
  constructor(message: string, status = 0, retryAfterMs?: number) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.retryAfterMs = retryAfterMs;
  }
}

function retryAfterMs(response: Response): number | undefined {
  const valor = response.headers.get('Retry-After')?.trim();
  if (!valor) return undefined;
  const espera = /^\d+$/.test(valor) ? Number(valor) * 1000 : Date.parse(valor) - Date.now();
  return Number.isFinite(espera) ? Math.max(0, espera) : undefined;
}

interface ApiOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  token?: string | null;
  responseType?: 'json' | 'text';
}

// Não exibe mensagens internas da API, corpos HTML ou exceções Java ao usuário.
export async function apiRequest<T>(path: string, { body, token, responseType = 'json', ...options }: ApiOptions = {}): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Accept', responseType === 'text' ? 'text/plain, application/json' : 'application/json');
  if (body !== undefined) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options, headers, body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError('Não foi possível conectar ao servidor. Tente novamente.');
  }

  if (!response.ok) {
    if (response.status === 401 && token) window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
    if (/^\/api\/dados\/estatisticas\/\d+\/sintese-ia$/.test(path)) {
      const mensagensSintese: Record<number, string> = {
        409: 'Esta síntese foi atualizada durante a geração. Recarregue a aba DataJud para consultar o resultado salvo.',
        422: 'Gere estatísticas com processos na amostra antes de solicitar a síntese.',
        429: 'A IA atingiu o limite de solicitações. Aguarde antes de gerar a síntese novamente.',
        502: 'A IA não conseguiu concluir uma síntese válida. Os dados salvos foram preservados. Tente novamente.',
        503: 'A IA está indisponível ou ainda não foi configurada para gerar a síntese.',
      };
      if (mensagensSintese[response.status]) throw new ApiError(mensagensSintese[response.status], response.status);
    }
    if (/^\/api\/dados\/comunicacoes\/\d+\/(analise-ia|sintese-ia|pesquisar)$/.test(path) && [409, 422, 429, 502, 503].includes(response.status)) {
      const erro: unknown = await response.json().catch(() => null);
      const mensagens: Record<string, string> = {
        SEM_TEXTOS: 'Não há textos de publicações para analisar. Faça outra pesquisa.',
        EXCESSO_DE_TEXTO: 'Os textos excedem o limite desta análise. Refine o texto ou o período da pesquisa.',
        PESQUISA_DESATUALIZADA: 'As publicações foram atualizadas. Pesquise novamente antes de analisar.',
        CONSULTA_DJEN_FALHOU: 'Não foi possível pesquisar publicações no DJEN. Tente novamente mais tarde.',
        CONSULTA_DJEN_LIMITADA: 'O DJEN limitou as consultas de publicações. Aguarde antes de tentar novamente.',
        CONFIGURACAO_AUSENTE: 'Configure GEMINI_API_KEY no backend para habilitar esta análise.',
        LIMITE_EXTERNO: 'O serviço de IA limitou as solicitações. Aguarde antes de tentar novamente.',
        RESPOSTA_INCOMPLETA: 'A IA interrompeu a resposta. Tente novamente.',
        RESPOSTA_INVALIDA: 'A IA não retornou evidências verificáveis. Tente novamente.',
        SERVICO_INDISPONIVEL: 'Não foi possível consultar o serviço de IA. Tente novamente mais tarde.',
      };
      if (erro && typeof erro === 'object' && 'codigo' in erro && typeof erro.codigo === 'string' && Object.hasOwn(mensagens, erro.codigo)) {
        throw new ApiError(mensagens[erro.codigo], response.status);
      }
    }
    if (path.startsWith('/api/processo/consultar-datajud?') && [400, 404, 429, 502].includes(response.status)) {
      const erro: unknown = await response.json().catch(() => null);
      const mensagensCnj: Record<string, string> = {
        CNJ_INVALIDO: 'Confira os 20 dígitos do CNJ e seus dígitos verificadores.',
        CNJ_TRIBUNAL_NAO_SUPORTADO: 'O tribunal de origem desse CNJ não está disponível para consulta automática. Continue o cadastro manualmente.',
        CNJ_NAO_ENCONTRADO: 'Nenhum registro público desse CNJ foi encontrado no tribunal de origem. Você pode continuar o cadastro manualmente.',
        CNJ_CONSULTA_LIMITADA: 'O DataJud recusou temporariamente a consulta. Aguarde antes de tentar novamente ou continue o cadastro manualmente.',
        CNJ_CONSULTA_AGUARDANDO: 'A consulta ao DataJud está em espera. Aguarde antes de tentar novamente ou continue o cadastro manualmente.',
        CNJ_CONSULTA_INDISPONIVEL: 'Não foi possível consultar o DataJud. Tente novamente mais tarde ou continue o cadastro manualmente.',
      };
      if (erro && typeof erro === 'object' && 'codigo' in erro && typeof erro.codigo === 'string' && Object.hasOwn(mensagensCnj, erro.codigo)) {
        throw new ApiError(mensagensCnj[erro.codigo], response.status, retryAfterMs(response));
      }
    }
    if ([422, 502].includes(response.status) && /^\/api\/dados\/(analisar\/\d+|estatisticas\/\d+\/(gerar|solicitacoes))$/.test(path)) {
      // Apenas códigos conhecidos viram mensagens; nunca exibe detalhes internos recebidos.
      const erro: unknown = await response.json().catch(() => null);
      const integracoes: Record<string, string> = {
        DATAJUD_SEM_PROCESSOS: 'Nenhum processo foi encontrado no DataJud com os filtros informados. Revise os dados do processo ou amplie o período. As estatísticas anteriores foram preservadas.',
        CONSULTA_DATAJUD_FALHOU: 'Não foi possível consultar o DataJud. A análise não foi salva; os resultados anteriores foram preservados. Tente novamente.',
        CONSULTA_DJEN_FALHOU: 'A consulta de comunicações (DJEN) falhou. A análise completa, incluindo o DataJud, não foi salva; os resultados anteriores foram preservados. Tente novamente.',
        CONSULTA_DATAJUD_LIMITADA: 'O DataJud recusou temporariamente a consulta (HTTP 429). Aguarde antes de solicitar outra análise. Nenhum resultado novo foi salvo; os anteriores foram preservados.',
        CONSULTA_DJEN_LIMITADA: 'O serviço de comunicações (DJEN) atingiu o limite de consultas (HTTP 429). Aguarde antes de tentar novamente. A análise completa não foi salva; os resultados anteriores foram preservados.',
      };
      if (erro && typeof erro === 'object' && 'codigo' in erro && typeof erro.codigo === 'string' && Object.hasOwn(integracoes, erro.codigo)) {
        throw new ApiError(integracoes[erro.codigo], response.status);
      }
    }
    if (response.status === 409 && path.endsWith('/solicitacoes'))
      throw new ApiError('Já existe uma geração em andamento para este processo. Reabra a aba para acompanhar.', 409);
    if (response.status === 429 && path.endsWith('/solicitacoes'))
      throw new ApiError('A fila de consultas está ocupada. Aguarde a conclusão das solicitações em andamento.', 429);
    const messages: Record<number, string> = {
      400: 'Confira os dados informados e tente novamente.',
      401: token ? 'Sua sessão expirou. Entre novamente.' : 'E-mail ou senha incorretos.',
      403: 'Você não tem permissão para realizar esta ação.',
      404: 'Não foi possível encontrar esta informação.',
      409: path === '/api/auth/cadastro' ? 'Este e-mail já está cadastrado. Faça login ou utilize outro e-mail.' : 'Não foi possível salvar devido a um conflito nos dados. Revise as informações e tente novamente.',
      429: 'Muitas tentativas. Aguarde um momento antes de tentar novamente.',
    };
    throw new ApiError(messages[response.status] ?? 'O serviço está indisponível no momento. Tente novamente.', response.status, retryAfterMs(response));
  }
  if (response.status === 204) return undefined as T;
  if (responseType === 'text') return await response.text() as T;
  try { return await response.json() as T; }
  catch { throw new ApiError('Não foi possível ler a resposta do servidor. Tente novamente.', response.status); }
}

export function mensagemErro(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Não foi possível concluir a ação. Tente novamente.';
}
