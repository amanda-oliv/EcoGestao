// Tela de setores, só lista e cadastra 

const tabela = document.getElementById('tabela-setores');
const form = document.getElementById('form-setor');
const btnSalvar = document.getElementById('btn-salvar');

async function carregarSetores() {
  tabela.innerHTML = '<tr><td colspan="3" class="estado-vazio">Carregando...</td></tr>';
  try {
    const setores = await api.setores();

    if (setores.length === 0) {
      tabela.innerHTML = '<tr><td colspan="3" class="estado-vazio">Nenhum setor cadastrado ainda.</td></tr>';
      return;
    }

    tabela.innerHTML = setores.map(s => `
      <tr>
        <td>${s.id}</td>
        <td>${escaparHtml(s.nome)}</td>
        <td>${escaparHtml(s.descricao || '—')}</td>
      </tr>
    `).join('');
  } catch (erro) {
    tabela.innerHTML = '<tr><td colspan="3" class="estado-vazio text-danger">Erro ao carregar setores.</td></tr>';
    mostrarAlerta(erro.message, 'danger');
  }
}

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();

  const nome = document.getElementById('nome').value.trim();
  const descricao = document.getElementById('descricao').value.trim();

  if (!nome) {
    mostrarAlerta('Informe o nome do setor.', 'warning');
    return;
  }

  btnSalvar.disabled = true;
  btnSalvar.textContent = 'Salvando...';

  try {
    await api.criarSetor({ nome, descricao });
    mostrarAlerta('Setor cadastrado com sucesso!', 'success');
    form.reset();
    carregarSetores();
  } catch (erro) {
    mostrarAlerta(erro.message, 'danger');
  } finally {
    btnSalvar.disabled = false;
    btnSalvar.textContent = 'Cadastrar';
  }
});

carregarSetores();
