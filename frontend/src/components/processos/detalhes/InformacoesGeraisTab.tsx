import type { ReactNode } from 'react';
import type { Processo } from '../../../types/processo';
import { Button } from '../../ui/Button';
import { Icon, type IconName } from '../../ui/Icon';
import { ProcessStatusBadge } from '../ProcessStatusBadge';
import { data, moeda, posicao } from './formatacao';

function Card({ titulo, icon, children }: { titulo: string; icon: IconName; children: ReactNode }) {
  return <section className="detail-card"><h2><Icon name={icon} />{titulo}</h2>{children}</section>;
}
function Dado({ label, children, destaque = false }: { label: string; children: ReactNode; destaque?: boolean }) {
  return <div><dt>{label}</dt><dd className={destaque ? 'detail-value-strong' : undefined}>{children || 'Não informado'}</dd></div>;
}

export function InformacoesGeraisTab({ processo, analisando, solicitar }: { processo: Processo; analisando: boolean; solicitar: () => void }) {
  const grau = ({ G1: 'G1 — Primeiro grau', G2: 'G2 — Segundo grau', JE: 'JE — Juizado especial', TR: 'TR — Turma recursal', SUP: 'SUP — Superior' } as Record<string, string>)[processo.grau ?? ''] ?? processo.grau;
  return <div className="general-details-grid"><div className="general-details-main">
    <Card titulo="Identificação do processo" icon="file"><dl className="detail-fields detail-fields--four">
      <Dado label="Número do processo" destaque>{processo.numeroProcesso}</Dado><Dado label="Cliente">{processo.clienteVinculado}</Dado>
      <Dado label="Posição do cliente">{posicao(processo.posicaoCliente)}</Dado><Dado label="Status"><ProcessStatusBadge status={processo.status} /></Dado>
    </dl></Card>
    <Card titulo="Dados processuais" icon="scales"><dl className="detail-fields detail-fields--three">
      <Dado label="Tribunal" destaque>{processo.tribunal}</Dado><Dado label="Grau">{grau}</Dado><Dado label="Órgão julgador">{processo.orgaoJulgador?.nome}</Dado>
      <Dado label="Código do órgão">{processo.orgaoJulgador?.codigo}</Dado><Dado label="Comarca">{processo.comarca}</Dado><Dado label="UF">{processo.uf}</Dado>
    </dl></Card>
    <Card titulo="Classificação jurídica" icon="file"><dl className="detail-fields detail-fields--three">
      <Dado label="Classe processual" destaque>{processo.classeProcessual?.nome || 'Não informado'}<small>Código {processo.classeProcessual?.codigo || 'não informado'}</small></Dado>
      <Dado label="Assunto" destaque>{processo.assunto?.nome || 'Não informado'}<small>Código {processo.assunto?.codigo || 'não informado'}</small></Dado><Dado label="Área de atuação">{processo.areaAtuacao}</Dado>
    </dl></Card>
    <Card titulo="Informações complementares" icon="info"><dl className="detail-fields detail-fields--four">
      <Dado label="Valor da causa" destaque>{moeda(processo.valorCausa)}</Dado><Dado label="Magistrado">{processo.nomeMagistrado}</Dado>
      <Dado label="Data de distribuição">{data(processo.dataInicio)}</Dado><Dado label="Data final do período">{data(processo.dataFim)}</Dado>
    </dl></Card>
  </div><aside className="detail-card detail-summary" aria-label="Resumo do processo"><h2><Icon name="file" />Resumo</h2>
    <dl><Dado label="Status"><ProcessStatusBadge status={processo.status} /></Dado><Dado label="Tribunal">{processo.tribunal}</Dado><Dado label="Grau">{processo.grau}</Dado><Dado label="Cliente">{processo.clienteVinculado}</Dado><Dado label="Última análise">Data não informada</Dado></dl>
    <Button loading={analisando} onClick={solicitar}>{!analisando && <Icon name="sparkles" />}{analisando ? 'Analisando…' : 'Solicitar análise'}</Button>
  </aside></div>;
}
