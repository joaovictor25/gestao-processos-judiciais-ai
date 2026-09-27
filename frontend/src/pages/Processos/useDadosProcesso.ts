import { useEffect, useState } from 'react';
import { ApiError, mensagemErro } from '../../services/api';

// Compartilha somente o ciclo de leitura/cancelamento entre processo e suas abas.
export function useDadosProcesso<T>(id: number, carregar: (id: number, signal?: AbortSignal) => Promise<T>, versao = 0) {
  const [dados, setDados] = useState<T>();
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState('');
  const [status, setStatus] = useState<number>();
  const [tentativa, setTentativa] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setCarregando(true); setErro(''); setStatus(undefined);
    void carregar(id, controller.signal).then(result => {
      if (!controller.signal.aborted) setDados(result);
    }).catch((error: unknown) => {
      if (!controller.signal.aborted) { setErro(mensagemErro(error)); setStatus(error instanceof ApiError ? error.status : undefined); }
    }).finally(() => { if (!controller.signal.aborted) setCarregando(false); });
    return () => controller.abort();
  }, [id, carregar, versao, tentativa]);
  return { dados, carregando, erro, status, tentarNovamente: () => setTentativa(current => current + 1) };
}
