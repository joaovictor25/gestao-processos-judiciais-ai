import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Input } from '../../components/forms/Input';
import { focarPrimeiroErro, validarEmail, validarSenha, type FormErrors } from '../../components/forms/validation';
import { Button } from '../../components/ui/Button';
import { Icon } from '../../components/ui/Icon';
import { authService } from '../../services/authService';
import { ApiError, mensagemErro } from '../../services/api';
import './Cadastro.css';

export function Cadastro() {
  const navigate = useNavigate();
  const [dados, setDados] = useState({ nome: '', email: '', senha: '', confirmacao: '' });
  const [errors, setErrors] = useState<FormErrors>({});
  const [erro, setErro] = useState('');
  const [loading, setLoading] = useState(false);

  function atualizar(field: keyof typeof dados, value: string) {
    setDados(current => ({ ...current, [field]: value }));
    setErrors(current => ({ ...current, [field]: undefined }));
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validation: FormErrors = {
      nome: !dados.nome.trim() ? 'Informe seu nome completo.' : dados.nome.trim().length > 255 ? 'Utilize até 255 caracteres.' : undefined,
      email: validarEmail(dados.email), senha: validarSenha(dados.senha, true),
      confirmacao: !dados.confirmacao ? 'Confirme sua senha.' : dados.confirmacao !== dados.senha ? 'As senhas não coincidem.' : undefined,
    };
    setErrors(validation);
    setErro('');
    if (Object.values(validation).some(Boolean)) { focarPrimeiroErro(validation); return; }
    setLoading(true);
    try {
      await authService.cadastrar({ nome: dados.nome.trim(), email: dados.email.trim().toLowerCase(), senha: dados.senha });
      navigate('/login', { replace: true, state: { cadastroConcluido: true } });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) setErrors({ email: error.message });
      else setErro(mensagemErro(error));
    } finally { setLoading(false); }
  }

  return <div className="auth-form signup-form">
    <header className="auth-heading"><h1>Criar sua conta</h1><p>Preencha os dados abaixo para começar a usar a plataforma.</p></header>
    <form onSubmit={submit} noValidate aria-busy={loading}>
      <Input id="nome" label="Nome completo" icon="user" autoComplete="name" required
        placeholder="Digite seu nome completo" value={dados.nome} disabled={loading} maxLength={255}
        error={errors.nome} onChange={event => atualizar('nome', event.target.value)} />
      <Input id="email" label="E-mail" icon="mail" type="email" autoComplete="email" required
        placeholder="Digite seu e-mail" value={dados.email} disabled={loading} maxLength={254}
        error={errors.email} onChange={event => atualizar('email', event.target.value)} />
      <Input id="senha" label="Senha" icon="lock" type="password" autoComplete="new-password" required
        placeholder="Crie uma senha" hint="Use pelo menos 6 caracteres." value={dados.senha} disabled={loading}
        error={errors.senha} onChange={event => atualizar('senha', event.target.value)} />
      <Input id="confirmacao" label="Confirmar senha" icon="lock" type="password" autoComplete="new-password" required
        placeholder="Confirme sua senha" value={dados.confirmacao} disabled={loading}
        error={errors.confirmacao} onChange={event => atualizar('confirmacao', event.target.value)} />
      <div className="oab-fields" aria-describedby="oab-hint">
        <Input id="oab" label="Número da OAB" icon="badge" placeholder="Ex.: 123.456" disabled />
        <div className="field"><label htmlFor="oab-uf">UF da OAB</label><div className="input-wrap">
          <Icon name="pin" className="input-icon" /><select id="oab-uf" className="input--icon" disabled defaultValue="">
            <option value="">Selecione</option>
          </select><Icon name="down" className="select-icon" />
        </div></div>
      </div>
      <p className="field-hint oab-hint" id="oab-hint">Em breve, você poderá adicionar sua OAB ao perfil.</p>
      {erro && <p className="notice notice--error" role="alert">{erro}</p>}
      <Button type="submit" className="signup-submit" loading={loading}>{loading ? 'Criando conta…' : 'Criar conta'}</Button>
    </form>
    <footer className="auth-footer">Já tem uma conta? <Link to="/login">Fazer login</Link></footer>
  </div>;
}
