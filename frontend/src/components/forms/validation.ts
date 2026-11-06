export type FormErrors = Partial<Record<'nome' | 'email' | 'senha' | 'confirmacao', string>>;

export function validarEmail(email: string): string | undefined {
  if (!email.trim()) return 'Informe seu e-mail.';
  if (email.trim().length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) return 'Informe um e-mail válido.';
}

export function validarSenha(senha: string, cadastro = false): string | undefined {
  if (!senha.trim()) return 'Informe sua senha.';
  if (cadastro && senha.length < 6) return 'A senha deve ter pelo menos 6 caracteres.';
  if (new TextEncoder().encode(senha).length > 72) return 'A senha excede o limite de 72 bytes. Utilize uma senha menor.';
}

export function focarPrimeiroErro(errors: FormErrors) {
  const field = Object.keys(errors).find(key => errors[key as keyof FormErrors]);
  if (field) document.getElementById(field)?.focus();
}
