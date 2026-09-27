import type { ClassificacaoProcessual, Processo, RegistroDataJudCadastro } from '../../types/processo';
import { camposDoProcesso, type ProcessoCampos } from './processoFormModel';

export const normalizarCnj = (valor: string) => valor.replace(/[.\-\s]/g, '');
export function cnjValido(valor: string): boolean {
  const numero = normalizarCnj(valor);
  if (!/^\d{20}$/.test(numero)) return false;
  const verificacao = numero.slice(0, 7) + numero.slice(9) + numero.slice(7, 9);
  let resto = 0;
  for (const digito of verificacao) resto = (resto * 10 + Number(digito)) % 97;
  return resto === 1;
}

type Campo = keyof ProcessoCampos;
const grupos: Campo[][] = [['tribunal'], ['grau'], ['orgaoNome', 'orgaoCodigo'], ['classeNome', 'classeCodigo'], ['assuntoNome', 'assuntoCodigo'], ['dataInicio']];
export interface FormularioProcesso {
  campos: ProcessoCampos;
  automaticos: Partial<ProcessoCampos>;
  manuais: Campo[];
}
export type AcaoFormulario = { tipo: 'editar'; campo: Campo; valor: string }
  | { tipo: 'preencher'; numero: string; registro: RegistroDataJudCadastro }
  | { tipo: 'assunto'; numero: string; assunto: ClassificacaoProcessual | null };
export const iniciarFormulario = (processo?: Processo): FormularioProcesso => ({ campos: camposDoProcesso(processo), automaticos: {}, manuais: [] });

function limparAutomaticos(estado: FormularioProcesso) {
  const campos = { ...estado.campos };
  for (const campo of Object.keys(estado.automaticos) as Campo[]) {
    if (campos[campo] === estado.automaticos[campo]) campos[campo] = '';
  }
  return campos;
}

export function reduzirFormulario(estado: FormularioProcesso, acao: AcaoFormulario): FormularioProcesso {
  if (acao.tipo === 'editar') {
    if (acao.campo === 'numeroProcesso') {
      const mudou = normalizarCnj(estado.campos.numeroProcesso) !== normalizarCnj(acao.valor);
      return { ...estado, campos: { ...(mudou ? limparAutomaticos(estado) : estado.campos), numeroProcesso: acao.valor }, automaticos: mudou ? {} : estado.automaticos };
    }
    const grupo = grupos.find(campos => campos.includes(acao.campo)) ?? [acao.campo];
    const campos = { ...estado.campos, [acao.campo]: acao.valor };
    const automaticos = { ...estado.automaticos };
    // Nome e código são um par. Não combina um novo nome manual com um código antigo importado.
    for (const campo of grupo) {
      if (campo !== acao.campo && automaticos[campo] !== undefined && campos[campo] === automaticos[campo]) campos[campo] = '';
      delete automaticos[campo];
    }
    return { campos, automaticos, manuais: [...new Set([...estado.manuais, ...grupo])] };
  }
  // Uma resposta tardia nunca preenche outro CNJ.
  if (normalizarCnj(estado.campos.numeroProcesso) !== acao.numero) return estado;
  if (acao.tipo === 'assunto') {
    const nome = acao.assunto?.nome ?? '', codigo = acao.assunto?.codigo ?? '';
    return { campos: { ...estado.campos, assuntoNome: nome, assuntoCodigo: codigo },
      automaticos: { ...estado.automaticos, assuntoNome: nome, assuntoCodigo: codigo },
      manuais: estado.manuais.filter(campo => campo !== 'assuntoNome' && campo !== 'assuntoCodigo') };
  }
  const campos = limparAutomaticos(estado);
  const automaticos: Partial<ProcessoCampos> = {};
  const registro = acao.registro;
  const assunto = registro.assuntos.length === 1 ? registro.assuntos[0] : null;
  const valores: Partial<ProcessoCampos> = {
    tribunal: registro.tribunal ?? '', grau: registro.grau ?? '', dataInicio: registro.dataInicio ?? '',
    orgaoNome: registro.orgaoJulgador?.nome ?? '', orgaoCodigo: registro.orgaoJulgador?.codigo ?? '',
    classeNome: registro.classeProcessual?.nome ?? '', classeCodigo: registro.classeProcessual?.codigo ?? '',
    assuntoNome: assunto?.nome ?? '', assuntoCodigo: assunto?.codigo ?? '',
  };
  for (const grupo of grupos) {
    if (grupo.some(campo => estado.manuais.includes(campo) || campos[campo].trim())) continue;
    for (const campo of grupo) if (valores[campo]) { campos[campo] = valores[campo]!; automaticos[campo] = valores[campo]; }
  }
  return { ...estado, campos, automaticos };
}
