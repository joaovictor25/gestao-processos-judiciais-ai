import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { Input } from '../../components/forms/Input';
import { focarPrimeiroErro, validarEmail, validarSenha, type FormErrors } from '../../components/forms/validation';
import { Button } from '../../components/ui/Button';
import { Icon } from '../../components/ui/Icon';
import { mensagemErro } from '../../services/api';
import './Login.css';

export function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const state: unknown = location.state;
  const cadastrado = typeof state === 'object' && state !== null && 'cadastroConcluido' in state && state.cadastroConcluido === true;
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validation = { email: validarEmail(email), senha: validarSenha(senha) };
    setErrors(validation);
    setErro('');
    if (Object.values(validation).some(Boolean)) { focarPrimeiroErro(validation); return; }
    setLoading(true);
    try {
      await login({ email: email.trim().toLowerCase(), senha });
      navigate('/dashboard', { replace: true });
    } catch (error) { setErro(mensagemErro(error)); }
    finally { setLoading(false); }
  }

  return <div className="auth-form login-form">
    <header className="auth-heading"><h1>Bem-vindo de volta</h1><p>Acesse sua conta para continuar.</p></header>
    {cadastrado && <p className="notice notice--success" role="status"><Icon name="check" />Conta criada! Entre com seu e-mail e senha.</p>}
    <form onSubmit={submit} noValidate aria-busy={loading}>
      <Input id="email" label="E-mail" type="email" autoComplete="email" required
        placeholder="seu@email.com" value={email} maxLength={254} disabled={loading} error={errors.email}
        onChange={event => { setEmail(event.target.value); setErrors(current => ({ ...current, email: undefined })); }} />
      <Input id="senha" label="Senha" type="password" autoComplete="current-password" required
        placeholder="Digite sua senha" value={senha} disabled={loading} error={errors.senha}
        onChange={event => { setSenha(event.target.value); setErrors(current => ({ ...current, senha: undefined })); }} />
      {erro && <p className="notice notice--error" role="alert">{erro}</p>}
      <Button type="submit" className="login-submit" loading={loading}>{loading ? 'Entrando…' : <>Entrar<Icon name="arrowRight" /></>}</Button>
    </form>
    <footer className="auth-footer">Ainda não tem conta? <Link to="/cadastro">Criar conta</Link></footer>
  </div>;
}
