import type { Processo, ProcessoRequest } from '../../types/processo';

export const ufs = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'];
// Mesmas siglas aceitas por DataJudService.montarEndpoint no backend atual.
export const tribunais = [
  ...ufs.map(uf => uf === 'DF' ? 'TJDFT' : `TJ${uf}`),
  ...Array.from({ length: 6 }, (_, index) => `TRF${index + 1}`),
  ...Array.from({ length: 24 }, (_, index) => `TRT${index + 1}`),
  ...ufs.map(uf => uf === 'DF' ? 'TRE-DFT' : `TRE-${uf}`),
  'STJ', 'TST', 'TSE', 'STM', 'TJMMG', 'TJMRS', 'TJM-SP',
];
export const graus = ['G1', 'G2', 'JE', 'TR', 'SUP'];
export const areas = ['Cível', 'Trabalhista', 'Previdenciário', 'Consumidor', 'Criminal', 'Tributário', 'Família', 'Empresarial', 'Administrativo', 'Eleitoral', 'Outros'];
export const statusProcesso = ['Em andamento', 'Concluído', 'Suspenso', 'Arquivado'];

export interface ProcessoCampos {
  numeroProcesso: string; clienteVinculado: string; posicaoCliente: string;
  tribunal: string; grau: string; orgaoNome: string; orgaoCodigo: string; comarca: string; uf: string;
  classeNome: string; classeCodigo: string; assuntoNome: string; assuntoCodigo: string; areaAtuacao: string;
  status: string; valorCausa: string; nomeMagistrado: string; dataInicio: string; dataFim: string;
}
export type ProcessoErros = Partial<Record<keyof ProcessoCampos, string>>;

export function camposDoProcesso(processo?: Processo): ProcessoCampos {
  return {
    numeroProcesso: processo?.numeroProcesso ?? '', clienteVinculado: processo?.clienteVinculado ?? '',
    posicaoCliente: processo?.posicaoCliente ?? '', tribunal: processo?.tribunal ?? '', grau: processo?.grau ?? '',
    orgaoNome: processo?.orgaoJulgador?.nome ?? '', orgaoCodigo: processo?.orgaoJulgador?.codigo ?? '',
    comarca: processo?.comarca ?? '', uf: processo?.uf ?? '',
    classeNome: processo?.classeProcessual?.nome ?? '', classeCodigo: processo?.classeProcessual?.codigo ?? '',
    assuntoNome: processo?.assunto?.nome ?? '', assuntoCodigo: processo?.assunto?.codigo ?? '',
    areaAtuacao: processo?.areaAtuacao ?? '', status: processo?.status ?? '', nomeMagistrado: processo?.nomeMagistrado ?? '',
    valorCausa: processo?.valorCausa == null ? '' : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(processo.valorCausa),
    dataInicio: processo?.dataInicio ?? '', dataFim: processo?.dataFim ?? '',
  };
}

export function mascararMoeda(value: string): string {
  const digits = value.replace(/\D/g, '').replace(/^0+(?=\d)/, '');
  if (!digits) return '';
  const padded = digits.padStart(3, '0');
  return `R$ ${padded.slice(0, -2).replace(/\B(?=(\d{3})+(?!\d))/g, '.')},${padded.slice(-2)}`;
}

function dataValida(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || value.startsWith('0000')) return false;
  const date = new Date(`${value}T00:00:00Z`);
  return !Number.isNaN(date.getTime()) && date.toISOString().slice(0, 10) === value;
}

export function validarProcesso(campos: ProcessoCampos): ProcessoErros {
  const errors: ProcessoErros = {};
  const obrigatorios: (keyof ProcessoCampos)[] = ['numeroProcesso', 'clienteVinculado', 'posicaoCliente', 'tribunal', 'grau', 'classeNome', 'assuntoNome', 'areaAtuacao', 'status', 'dataInicio'];
  for (const field of obrigatorios) if (!campos[field].trim()) errors[field] = 'Campo obrigatório.';
  for (const field of Object.keys(campos) as (keyof ProcessoCampos)[]) {
    if (campos[field].trim().length > 255) errors[field] = 'Utilize até 255 caracteres.';
  }
  if (campos.dataInicio && !dataValida(campos.dataInicio)) errors.dataInicio = 'Informe uma data válida.';
  if (campos.valorCausa && !Number.isSafeInteger(Number(campos.valorCausa.replace(/\D/g, '')))) errors.valorCausa = 'O valor excede o limite suportado pelo formulário.';
  return errors;
}

export function dadosDoProcesso(campos: ProcessoCampos): ProcessoRequest {
  return {
    numeroProcesso: campos.numeroProcesso.trim(), clienteVinculado: campos.clienteVinculado.trim(),
    posicaoCliente: campos.posicaoCliente, tribunal: campos.tribunal, grau: campos.grau,
    orgaoJulgador: { nome: campos.orgaoNome.trim(), codigo: campos.orgaoCodigo.trim() },
    comarca: campos.comarca.trim(), uf: campos.uf,
    classeProcessual: { nome: campos.classeNome.trim(), codigo: campos.classeCodigo.trim() },
    assunto: { nome: campos.assuntoNome.trim(), codigo: campos.assuntoCodigo.trim() },
    areaAtuacao: campos.areaAtuacao, status: campos.status, nomeMagistrado: campos.nomeMagistrado.trim(),
    valorCausa: campos.valorCausa ? Number(campos.valorCausa.replace(/\D/g, '')) / 100 : null,
    dataInicio: campos.dataInicio, dataFim: campos.dataFim || null,
  };
}

export function normalizarTexto(value: string | null | undefined) {
  return (value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[_-]/g, ' ').trim().toLowerCase();
}

export function emAndamento(status: string | null) {
  return ['em andamento', 'andamento', 'ativo'].includes(normalizarTexto(status));
}
