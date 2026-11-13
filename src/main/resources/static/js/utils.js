// funções usadas em várias páginas

// 2026-10-01 -> 01/10/2026
function formatarData(dataIso) {
  if (!dataIso) return '';
  const [ano, mes, dia] = dataIso.split('-');
  return `${dia}/${mes}/${ano}`;
}

// 1500.5 -> 1.500,50
function formatarNumero(valor) {
  return Number(valor).toLocaleString('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

// data de hoje em yyyy-MM-dd (formato da API)
// não usei toISOString porque ela converte pra UTC e a noite vira pro dia seguinte
function hojeIso() {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}

// alerta do bootstrap no canto da tela (div #alertas)
// tipo: success, danger, warning ou info
function mostrarAlerta(mensagem, tipo = 'success') {
  const area = document.getElementById('alertas');
  if (!area) return alert(mensagem);

  // se a mesma mensagem já está aparecendo não mostra de novo
  // (no dashboard, com a API fora, vinham 7 alertas iguais)
  const repetido = [...area.children].some(a => a.dataset.mensagem === mensagem);
  if (repetido) return;

  const div = document.createElement('div');
  div.className = `alert alert-${tipo} alert-dismissible fade show`;
  div.role = 'alert';
  div.dataset.mensagem = mensagem;
  div.innerHTML = `
    ${escaparHtml(mensagem)}
    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Fechar"></button>
  `;
  area.appendChild(div);

  // some depois de 5s
  setTimeout(() => {
    div.classList.remove('show');
    setTimeout(() => div.remove(), 300);
  }, 5000);
}

// pra não quebrar o HTML com texto que vem da API
function escaparHtml(texto) {
  const div = document.createElement('div');
  div.textContent = texto ?? '';
  return div.innerHTML;
}

// preenche um select com os setores da API
// incluirTodos = true nos filtros (opção "Todos os setores")
async function preencherSelectSetores(select, incluirTodos = false) {
  select.innerHTML = '<option value="">Carregando...</option>';
  try {
    const setores = await api.setores();
    select.innerHTML = incluirTodos
      ? '<option value="">Todos os setores</option>'
      : '<option value="">Selecione um setor</option>';
    setores.forEach(s => {
      const opt = document.createElement('option');
      opt.value = s.id;
      opt.textContent = s.nome;
      select.appendChild(opt);
    });
  } catch (erro) {
    select.innerHTML = '<option value="">Erro ao carregar setores</option>';
    mostrarAlerta(erro.message, 'danger');
  }
}

// a API dá 400 se vier só uma das datas ou se o início for depois do fim -  confere antes de chamar
function validarPeriodo(dataInicio, dataFim) {
  if ((dataInicio && !dataFim) || (!dataInicio && dataFim)) {
    return 'Preencha as duas datas do período, ou deixe as duas em branco.';
  }
  if (dataInicio && dataFim && dataInicio > dataFim) {
    return 'A data inicial não pode ser depois da data final.';
  }
  return null;
}

// deixa marcado na navbar o link da página atual
document.addEventListener('DOMContentLoaded', () => {
  const pagina = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.navbar .nav-link').forEach(link => {
    if (link.getAttribute('href') === pagina) link.classList.add('active');
  });
});
