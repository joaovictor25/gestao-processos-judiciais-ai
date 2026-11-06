// Contrato de ComunicacaoSalvaDTO. idOrgao é um código, não um nome de órgão.
export interface Comunicacao {
  id: number;
  processoId: number;
  codigoClasse: string | null;
  dataDisponibilizacao: string | null;
  dataDisponibilizacaoFormatada: string | null;
  idOrgao: number | null;
  meio: string | null;
  meioCompleto: string | null;
  siglaTribunal: string | null;
  status: string | null;
  texto: string | null;
  tipoComunicacao: string | null;
  tipoDocumento: string | null;
}
