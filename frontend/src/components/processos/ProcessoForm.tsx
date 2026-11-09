import { useCallback, useReducer, useRef, useState, type ChangeEvent, type FormEvent, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import type { Processo, ProcessoRequest, RegistroDataJudCadastro, ClassificacaoProcessual } from '../../types/processo';
import { Input } from '../forms/Input';
import { Select } from '../forms/Select';
import { Button } from '../ui/Button';
import { mensagemErro } from '../../services/api';
import { areas, dadosDoProcesso, graus, mascararMoeda, statusProcesso, tribunais, ufs, validarProcesso, type ProcessoCampos, type ProcessoErros } from './processoFormModel';
import { ConsultaCnj } from './ConsultaCnj';
import { iniciarFormulario, reduzirFormulario } from './preenchimentoCnj';

function FormSection({ numero, titulo, descricao, children }: { numero: number; titulo: string; descricao?: string; children: ReactNode }) {
  return <section className="process-form-section" aria-labelledby={`section-${numero}`}>
    <div className="process-section-heading"><span>{numero}</span><div><h2 id={`section-${numero}`}>{titulo}</h2>{descricao && <p>{descricao}</p>}</div></div>
    <div className="process-section-fields">{children}</div>
  </section>;
}

export function ProcessoForm({ processo, modo, salvar }: {
  processo?: Processo; modo: 'create' | 'edit'; salvar: (dados: ProcessoRequest) => Promise<void>;
}) {
  const navigate = useNavigate();
  const [{ campos }, dispatch] = useReducer(reduzirFormulario, processo, iniciarFormulario);
  const [errors, setErrors] = useState<ProcessoErros>({});
  const [erro, setErro] = useState('');
  const [salvando, setSalvando] = useState(false);
  const enviando = useRef(false);
  const preencher = useCallback((numero: string, registro: RegistroDataJudCadastro) => {
    if (enviando.current) return;
    dispatch({ tipo: 'preencher', numero, registro }); setErrors({});
  }, []);
  const escolherAssunto = useCallback((numero: string, assunto: ClassificacaoProcessual | null) => {
    if (enviando.current) return;
    dispatch({ tipo: 'assunto', numero, assunto }); setErrors(current => ({ ...current, assuntoNome: undefined, assuntoCodigo: undefined }));
  }, []);

  function atualizar(field: keyof ProcessoCampos, value: string) {
    dispatch({ tipo: 'editar', campo: field, valor: value });
    setErrors(current => ({ ...current, [field]: undefined }));
  }
  function campo(field: keyof ProcessoCampos) {
    return { id: field, name: field, value: campos[field], error: errors[field], onChange: (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => atualizar(field, event.target.value) };
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (enviando.current) return;
    const validation = validarProcesso(campos);
    setErrors(validation);
    setErro('');
    const firstError = Object.keys(validation)[0];
    if (firstError) { document.getElementById(firstError)?.focus(); return; }
    enviando.current = true;
    setSalvando(true);
    try { await salvar(dadosDoProcesso(campos)); }
    catch (error) { setErro(mensagemErro(error)); }
    finally { enviando.current = false; setSalvando(false); }
  }

  return <form className="process-form" onSubmit={submit} noValidate aria-busy={salvando}>
    <fieldset className="process-fields" disabled={salvando}>
      <legend className="sr-only">Informações do processo</legend>
      <FormSection numero={1} titulo="Identificação" descricao="Informações utilizadas para identificar o processo e vinculá-lo ao cliente.">
        <div className="process-field-full"><Input {...campo('numeroProcesso')} label="Número do processo (CNJ)" required placeholder="0000000-00.0000.0.00.0000" maxLength={255} />
          {modo === 'create' && <ConsultaCnj numero={campos.numeroProcesso} desabilitada={salvando} preencher={preencher} escolherAssunto={escolherAssunto} />}
        </div>
        <Input {...campo('clienteVinculado')} label="Cliente" required placeholder="Nome do cliente" maxLength={255} />
        <Select {...campo('posicaoCliente')} label="Posição do cliente" required options={['AUTOR', 'RÉU']} placeholder="Selecione a posição" />
        <div className="process-field-full"><Select {...campo('status')} label="Status do processo" required options={statusProcesso} placeholder="Selecione o status" /></div>
      </FormSection>
      <FormSection numero={2} titulo="Dados processuais" descricao="Informações de classificação e localização do processo.">
        <Select {...campo('tribunal')} label="Tribunal" required options={tribunais} placeholder="Selecione o tribunal" />
        <Select {...campo('grau')} label="Grau" required options={graus} placeholder="Selecione o grau" />
        <Input {...campo('orgaoNome')} label="Órgão julgador" placeholder="Ex.: 3ª Turma Recursal dos Juizados Especiais" maxLength={255} />
        <Input {...campo('orgaoCodigo')} label="Código do órgão" placeholder="Ex.: 72197" maxLength={255} />
        <Input {...campo('comarca')} label="Comarca" placeholder="Ex.: Curitiba" maxLength={255} />
        <Select {...campo('uf')} label="UF" options={ufs} />
      </FormSection>
      <FormSection numero={3} titulo="Classificação jurídica" descricao="Informações utilizadas para identificar processos semelhantes durante as análises.">
        <Input {...campo('classeNome')} label="Classe processual" required placeholder="Ex.: Recurso Inominado Cível" maxLength={255} />
        <Input {...campo('classeCodigo')} label="Código da classe" placeholder="Ex.: 460" maxLength={255} />
        <Input {...campo('assuntoNome')} label="Assunto" required placeholder="Ex.: Indenização por Dano Material" maxLength={255} />
        <Input {...campo('assuntoCodigo')} label="Código do assunto" placeholder="Ex.: 10439" maxLength={255} />
        <div className="process-field-full"><Select {...campo('areaAtuacao')} label="Área de atuação" required options={areas} placeholder="Selecione a área de atuação" /></div>
      </FormSection>
      {modo === 'edit' && <FormSection numero={4} titulo="Informações complementares" descricao="Dados adicionais para organização e acompanhamento do processo.">
          <Input {...campo('valorCausa')} label="Valor da causa" placeholder="R$ 0,00" inputMode="numeric" maxLength={30}
            onChange={event => atualizar('valorCausa', mascararMoeda(event.target.value))} />
          <div className="process-field-full"><Input {...campo('nomeMagistrado')} label="Magistrado" placeholder="Nome do magistrado responsável" maxLength={255} /></div>
      </FormSection>}
      <FormSection numero={modo === 'edit' ? 5 : 4} titulo="Período">
        <div className="process-field-full"><Input {...campo('dataInicio')} label="Data de distribuição" type="date" required max="9999-12-31" /></div>
      </FormSection>
    </fieldset>
    {erro && <p className="notice notice--error process-save-error" role="alert">{erro} Seus dados continuam no formulário.</p>}
    <footer className="process-form-actions">
      <Button variant="secondary" disabled={salvando} onClick={() => navigate('/processos')}>Cancelar</Button>
      <Button type="submit" loading={salvando}>{salvando ? 'Salvando…' : modo === 'edit' ? 'Salvar alterações' : 'Cadastrar processo'}</Button>
    </footer>
  </form>;
}
