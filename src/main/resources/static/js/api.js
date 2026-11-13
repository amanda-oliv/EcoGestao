// Chamadas para a API (backend)
// se mudar o endereço da API, trocar
const BASE_URL = 'http://localhost:8080';

async function requisitar(caminho, opcoes = {}) {
  // só manda Content-Type quando tem corpo (POST/PUT)
  if (opcoes.body) {
    opcoes.headers = { 'Content-Type': 'application/json' };
  }

  let resposta;
  try {
    resposta = await fetch(BASE_URL + caminho, opcoes);
  } catch (e) {
    // API fora do ar ou bloqueio de CORS (trava de segurança)
    throw new Error('Não foi possível conectar à API. Ela está rodando em ' + BASE_URL + '?');
  }

  // DELETE devolve 204 sem corpo
  if (resposta.status === 204) return null;

  const corpo = await resposta.json().catch(() => null);

  if (!resposta.ok) {
    // erro { timestamp, status, erro, mensagem }
    throw new Error(corpo?.mensagem || 'Erro ao comunicar com a API (status ' + resposta.status + ')');
  }

  return corpo;
}

// monta ?setorId=1&dataInicio=... ignorando os campos vazios
function montarQuery(filtros) {
  const params = new URLSearchParams();
  for (const [chave, valor] of Object.entries(filtros)) {
    if (valor) params.append(chave, valor);
  }
  const q = params.toString();
  return q ? '?' + q : '';
}

// modulos - 'agua', 'energia' ou 'residuos'
const api = {
  listar: (modulo, filtros = {}) => requisitar(`/api/${modulo}${montarQuery(filtros)}`),
  buscar: (modulo, id) => requisitar(`/api/${modulo}/${id}`),
  criar: (modulo, dados) => requisitar(`/api/${modulo}`, { method: 'POST', body: JSON.stringify(dados) }),
  atualizar: (modulo, id, dados) => requisitar(`/api/${modulo}/${id}`, { method: 'PUT', body: JSON.stringify(dados) }),
  excluir: (modulo, id) => requisitar(`/api/${modulo}/${id}`, { method: 'DELETE' }),

  // setor não tem PUT nem DELETE na API
  setores: () => requisitar('/api/setores'),
  criarSetor: (dados) => requisitar('/api/setores', { method: 'POST', body: JSON.stringify(dados) }),

  // recurso: agua-mensal, energia-mensal, residuos-mensal, reciclagem, consumo-por-setor
  dashboard: (recurso, filtros = {}) => requisitar(`/api/dashboard/${recurso}${montarQuery(filtros)}`)
};
