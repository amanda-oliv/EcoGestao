// telas de Água, Energia e Resíduos.
// Resíduos tem os campos tipo e destinação a mais, que serão usados se existirem na página

const MODULO = document.body.dataset.modulo;
const UNIDADE = document.body.dataset.unidade;

// mesmos valores que a API aceita (ResiduoValores.java no backend)
const TIPOS_RESIDUO = ['Papel', 'Plástico', 'Vidro', 'Metal', 'Orgânico', 'Eletrônico', 'Outros'];
const DESTINACOES = ['Reciclagem', 'Compostagem', 'Aterro', 'Empresa especializada'];

const tabela = document.getElementById('tabela-registros');
const totalRegistros = document.getElementById('total-registros');

const filtroSetor = document.getElementById('filtro-setor');
const filtroInicio = document.getElementById('filtro-inicio');
const filtroFim = document.getElementById('filtro-fim');
const formFiltros = document.getElementById('form-filtros');
const btnLimpar = document.getElementById('btn-limpar');

const modalForm = new bootstrap.Modal(document.getElementById('modal-registro'));
const tituloModal = document.getElementById('titulo-modal');
const form = document.getElementById('form-registro');
const campoId = document.getElementById('registro-id');
const campoSetor = document.getElementById('registro-setor');
const campoData = document.getElementById('registro-data');
const campoQuantidade = document.getElementById('registro-quantidade');
const btnSalvar = document.getElementById('btn-salvar');

// esses dois só existem em residuos.html
const campoTipo = document.getElementById('registro-tipo');
const campoDestinacao = document.getElementById('registro-destinacao');
const TEM_TIPO = Boolean(campoTipo && campoDestinacao);
const COLUNAS = TEM_TIPO ? 6 : 4;

const modalExcluir = new bootstrap.Modal(document.getElementById('modal-excluir'));
const textoExcluir = document.getElementById('texto-excluir');
const btnConfirmarExcluir = document.getElementById('btn-confirmar-excluir');

let registros = []; // guarda a última lista pra usar no editar/excluir
let idParaExcluir = null;

// listagem

function filtrosAtuais() {
  return {
    setorId: filtroSetor.value,
    dataInicio: filtroInicio.value,
    dataFim: filtroFim.value
  };
}

function linhaMensagem(texto, classeExtra = '') {
  return `<tr><td colspan="${COLUNAS}" class="estado-vazio ${classeExtra}">${texto}</td></tr>`;
}

function linhaRegistro(r) {
  const colunasTipo = TEM_TIPO
    ? `<td>${escaparHtml(r.tipo)}</td><td>${escaparHtml(r.destinacao)}</td>`
    : '';
  return `
    <tr>
      <td>${formatarData(r.data)}</td>
      <td>${escaparHtml(r.setor?.nome || '—')}</td>
      ${colunasTipo}
      <td class="text-end">${formatarNumero(r.quantidade)} ${UNIDADE}</td>
      <td class="text-end text-nowrap">
        <button class="btn btn-sm btn-outline-secondary" onclick="abrirEdicao(${r.id})">Editar</button>
        <button class="btn btn-sm btn-outline-danger" onclick="pedirExclusao(${r.id})">Excluir</button>
      </td>
    </tr>
  `;
}

async function carregarRegistros() {
  tabela.innerHTML = linhaMensagem('Carregando...');
  totalRegistros.textContent = '';

  try {
    registros = await api.listar(MODULO, filtrosAtuais());

    // mais recentes em cima
    registros.sort((a, b) => b.data.localeCompare(a.data) || b.id - a.id);

    if (registros.length === 0) {
      tabela.innerHTML = linhaMensagem('Nenhum registro encontrado.');
      return;
    }

    const soma = registros.reduce((acc, r) => acc + Number(r.quantidade), 0);
    totalRegistros.textContent =
      `${registros.length} registro(s), total de ${formatarNumero(soma)} ${UNIDADE}`;

    tabela.innerHTML = registros.map(linhaRegistro).join('');
  } catch (erro) {
    tabela.innerHTML = linhaMensagem('Erro ao carregar os registros.', 'text-danger');
    mostrarAlerta(erro.message, 'danger');
  }
}

// filtros

formFiltros.addEventListener('submit', (evento) => {
  evento.preventDefault();
  const erro = validarPeriodo(filtroInicio.value, filtroFim.value);
  if (erro) {
    mostrarAlerta(erro, 'warning');
    return;
  }
  carregarRegistros();
});

btnLimpar.addEventListener('click', () => {
  formFiltros.reset();
  carregarRegistros();
});

// cadastro e edição 

function preencherSelectFixo(select, valores, textoInicial) {
  select.innerHTML = `<option value="">${textoInicial}</option>` +
    valores.map(v => `<option value="${v}">${v}</option>`).join('');
}

function abrirCadastro() {
  form.reset();
  form.classList.remove('was-validated');
  campoId.value = '';
  campoData.value = hojeIso();
  tituloModal.textContent = 'Novo registro';
  modalForm.show();
}

function abrirEdicao(id) {
  const r = registros.find(item => item.id === id);
  if (!r) return;

  form.classList.remove('was-validated');
  campoId.value = r.id;
  campoSetor.value = r.setor?.id ?? '';
  campoData.value = r.data;
  campoQuantidade.value = r.quantidade;
  if (TEM_TIPO) {
    campoTipo.value = r.tipo;
    campoDestinacao.value = r.destinacao;
  }
  tituloModal.textContent = 'Editar registro';
  modalForm.show();
}

form.addEventListener('submit', async (evento) => {
  evento.preventDefault();

  // mesmas regras da API: obrigatório, data até hoje e quantidade > 0
  campoData.max = hojeIso();
  if (!form.checkValidity()) {
    form.classList.add('was-validated');
    return;
  }

  const dados = {
    setor: { id: Number(campoSetor.value) },
    data: campoData.value,
    quantidade: Number(campoQuantidade.value)
  };
  if (TEM_TIPO) {
    dados.tipo = campoTipo.value;
    dados.destinacao = campoDestinacao.value;
  }

  btnSalvar.disabled = true;
  btnSalvar.textContent = 'Salvando...';

  try {
    if (campoId.value) {
      await api.atualizar(MODULO, campoId.value, dados);
      mostrarAlerta('Registro atualizado com sucesso!');
    } else {
      await api.criar(MODULO, dados);
      mostrarAlerta('Registro cadastrado com sucesso!');
    }
    modalForm.hide();
    carregarRegistros();
  } catch (erro) {
    mostrarAlerta(erro.message, 'danger');
  } finally {
    btnSalvar.disabled = false;
    btnSalvar.textContent = 'Salvar';
  }
});

// exclusão

function pedirExclusao(id) {
  const r = registros.find(item => item.id === id);
  if (!r) return;
  idParaExcluir = id;

  const partes = [formatarData(r.data), r.setor?.nome || ''];
  if (TEM_TIPO) partes.push(`${r.tipo} (${r.destinacao})`);
  partes.push(`${formatarNumero(r.quantidade)} ${UNIDADE}`);
  textoExcluir.textContent = partes.join(' - ');

  modalExcluir.show();
}

btnConfirmarExcluir.addEventListener('click', async () => {
  btnConfirmarExcluir.disabled = true;
  try {
    await api.excluir(MODULO, idParaExcluir);
    mostrarAlerta('Registro excluído.');
    modalExcluir.hide();
    carregarRegistros();
  } catch (erro) {
    mostrarAlerta(erro.message, 'danger');
  } finally {
    btnConfirmarExcluir.disabled = false;
    idParaExcluir = null;
  }
});

// abertura da página

document.getElementById('btn-novo').addEventListener('click', abrirCadastro);
campoData.max = hojeIso();

if (TEM_TIPO) {
  preencherSelectFixo(campoTipo, TIPOS_RESIDUO, 'Selecione o tipo');
  preencherSelectFixo(campoDestinacao, DESTINACOES, 'Selecione a destinação');
}

preencherSelectSetores(filtroSetor, true);
preencherSelectSetores(campoSetor, false);
carregarRegistros();
