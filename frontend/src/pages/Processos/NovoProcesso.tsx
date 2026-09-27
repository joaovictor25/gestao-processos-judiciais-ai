import { useNavigate } from 'react-router-dom';
import { ProcessoPageHeader } from '../../components/processos/ProcessoPageHeader';
import { ProcessoForm } from '../../components/processos/ProcessoForm';
import { processoService } from '../../services/processoService';
import type { ProcessoRequest } from '../../types/processo';
import './Processos.css';

export function NovoProcesso() {
  const navigate = useNavigate();
  async function salvar(dados: ProcessoRequest) {
    await processoService.cadastrar(dados);
    navigate('/processos', { replace: true, state: { processoSalvo: 'cadastrado' } });
  }
  return <div className="process-module">
    <ProcessoPageHeader titulo="Novo processo" descricao="Cadastre as informações do processo para adicioná-lo à sua carteira." breadcrumb="Novo processo" />
    <ProcessoForm modo="create" salvar={salvar} />
  </div>;
}
