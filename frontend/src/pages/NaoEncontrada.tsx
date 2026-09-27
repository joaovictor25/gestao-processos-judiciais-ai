import { Link } from 'react-router-dom';
import { Icon } from '../components/ui/Icon';

export function NaoEncontrada() {
  return <main className="session-screen"><Icon name="folder" /><h1>Página não encontrada</h1><p>O endereço pode ter mudado ou não estar disponível.</p><Link className="button button--primary" to="/dashboard">Voltar ao início</Link></main>;
}
