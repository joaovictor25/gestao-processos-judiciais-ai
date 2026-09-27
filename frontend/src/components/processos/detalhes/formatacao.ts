export const numero = (value: number | null | undefined) => value == null || !Number.isFinite(value) ? '—' : new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(value);

export function data(value: string | null | undefined): string {
  if (!value) return 'Não informado';
  const iso = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  if (iso) return `${iso[3]}/${iso[2]}/${iso[1]}`;
  return value;
}

export function moeda(value: number | null | undefined): string {
  return value == null ? 'Não informado' : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

export function posicao(value: string | null): string {
  if (value?.toUpperCase() === 'AUTOR') return 'Autor';
  if (['REU', 'RÉU'].includes(value?.toUpperCase() ?? '')) return 'Réu';
  return value || 'Não informado';
}

// A saída é sempre texto, nunca HTML inserido na página.
export function textoComunicacao(value: string | null): string {
  if (!value) return '';
  return value.replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, '')
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, '')
    .replace(/<\/?(?:p|div|br|li)\b[^>]*>/gi, '\n').replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/gi, ' ').replace(/&lt;/gi, '<').replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"').replace(/&#39;|&apos;/gi, "'").replace(/&amp;/gi, '&')
    .replace(/\n{3,}/g, '\n\n').trim();
}
