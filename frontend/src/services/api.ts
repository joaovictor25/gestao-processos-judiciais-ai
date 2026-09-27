const baseUrl = import.meta.env.DEV ? '' : (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');
export const SESSION_EXPIRED_EVENT = 'axion:session-expired';

export class ApiError extends Error {
  readonly status: number;
  constructor(message: string, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
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
    if (/^\/api\/dados\/comunicacoes\/\d+\/analise-ia$/.test(path) && [422, 429, 502, 503].includes(response.status)) {
      const erro: unknown = await response.json().catch(() => null);
      const mensagens: Record<string, string> = {
        SEM_TEXTOS: 'Não há textos de comunicações salvas para analisar neste processo.',
        EXCESSO_DE_TEXTO: 'Os textos salvos excedem o limite desta análise.',
        CONFIGURACAO_AUSENTE: 'Configure GEMINI_API_KEY no backend para habilitar esta análise.',
        LIMITE_EXTERNO: 'O serviço de IA limitou as solicitações. Aguarde antes de tentar novamente.',
        RESPOSTA_INCOMPLETA: 'A IA interrompeu a resposta. Selecione menos categorias e tente novamente.',
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
        CNJ_CONSULTA_LIMITADA: 'O DataJud limitou as consultas. Aguarde antes de tentar novamente ou continue o cadastro manualmente.',
        CNJ_CONSULTA_INDISPONIVEL: 'Não foi possível consultar o DataJud. Tente novamente mais tarde ou continue o cadastro manualmente.',
      };
      if (erro && typeof erro === 'object' && 'codigo' in erro && typeof erro.codigo === 'string' && Object.hasOwn(mensagensCnj, erro.codigo)) {
        throw new ApiError(mensagensCnj[erro.codigo], response.status);
      }
    }
    if (response.status === 502 && /^\/api\/dados\/analisar\/\d+$/.test(path)) {
      // Apenas códigos conhecidos viram mensagens; nunca exibe detalhes internos recebidos.
      const erro: unknown = await response.json().catch(() => null);
      const integracoes: Record<string, string> = {
        CONSULTA_DATAJUD_FALHOU: 'Não foi possível consultar o DataJud. A análise não foi salva; os resultados anteriores foram preservados. Tente novamente.',
        CONSULTA_DJEN_FALHOU: 'A consulta de comunicações (DJEN) falhou. A análise completa, incluindo o DataJud, não foi salva; os resultados anteriores foram preservados. Tente novamente.',
        CONSULTA_DATAJUD_LIMITADA: 'O DataJud atingiu o limite de consultas (HTTP 429). Aguarde antes de solicitar outra análise. Nenhum resultado novo foi salvo; os anteriores foram preservados.',
        CONSULTA_DJEN_LIMITADA: 'O serviço de comunicações (DJEN) atingiu o limite de consultas (HTTP 429). Aguarde antes de tentar novamente. A análise completa não foi salva; os resultados anteriores foram preservados.',
      };
      if (erro && typeof erro === 'object' && 'codigo' in erro && typeof erro.codigo === 'string' && Object.hasOwn(integracoes, erro.codigo)) {
        throw new ApiError(integracoes[erro.codigo], response.status);
      }
    }
    const messages: Record<number, string> = {
      400: 'Confira os dados informados e tente novamente.',
      401: token ? 'Sua sessão expirou. Entre novamente.' : 'E-mail ou senha incorretos.',
      403: 'Você não tem permissão para realizar esta ação.',
      404: 'Não foi possível encontrar esta informação.',
      409: path === '/api/auth/cadastro' ? 'Este e-mail já está cadastrado. Faça login ou utilize outro e-mail.' : 'Não foi possível salvar devido a um conflito nos dados. Revise as informações e tente novamente.',
      429: 'Muitas tentativas. Aguarde um momento antes de tentar novamente.',
    };
    throw new ApiError(messages[response.status] ?? 'O serviço está indisponível no momento. Tente novamente.', response.status);
  }
  if (response.status === 204) return undefined as T;
  if (responseType === 'text') return await response.text() as T;
  try { return await response.json() as T; }
  catch { throw new ApiError('Não foi possível ler a resposta do servidor. Tente novamente.', response.status); }
}

export function mensagemErro(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Não foi possível concluir a ação. Tente novamente.';
}
