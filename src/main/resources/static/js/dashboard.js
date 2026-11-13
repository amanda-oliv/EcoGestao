// Dashboard (index.html): filtros, cards e gráficos com Chart.js
// gráficos de resíduos por tipo e destinação são somados aqui

const CORES = {
  agua: '#1e88e5',
  energia: '#f9a825',
  residuos: '#6d4c41',
  reciclado: '#43a047',
  naoReciclado: '#bdbdbd',
  periodoA: '#2e7d32',
  periodoB: '#8e24aa'
};

// cores dos gráficos de tipo e destinação
const PALETA = ['#43a047', '#1e88e5', '#f9a825', '#8e24aa', '#e53935', '#00897b', '#6d4c41', '#757575'];

const UNIDADES = { agua: 'litros', energia: 'kWh', residuos: 'kg' };

const filtroSetor = document.getElementById('filtro-setor');
const filtroInicio = document.getElementById('filtro-inicio');
const filtroFim = document.getElementById('filtro-fim');
const formFiltros = document.getElementById('form-filtros');

// gráficos criados, pra dar destroy() antes de desenhar de novo
// (sem isso o Chart.js deixava o gráfico antigo por baixo quando trocava o filtro)
const graficos = {};

// apoio

function caixaDoGrafico(id) {
  return document.getElementById(id).parentElement;
}

// estado: carregando, vazio, erro, aguardando ou '' (mostra o gráfico)
function definirEstado(id, estado, mensagem = '') {
  const caixa = caixaDoGrafico(id);
  caixa.dataset.estado = estado;
  const msg = caixa.querySelector('.grafico-msg');
  if (estado === 'carregando') {
    msg.innerHTML = '<div class="spinner-border spinner-border-sm text-success me-2"></div> Carregando...';
  } else {
    msg.textContent = mensagem;
  }
}

function desenhar(id, config) {
  if (graficos[id]) graficos[id].destroy();
  graficos[id] = new Chart(document.getElementById(id), config);
  definirEstado(id, '');
}

function limparGrafico(id) {
  if (graficos[id]) {
    graficos[id].destroy();
    delete graficos[id];
  }
}

function semDados(id) {
  limparGrafico(id);
  definirEstado(id, 'vazio', 'Sem dados no período');
}

function comErro(id, erro) {
  limparGrafico(id);
  definirEstado(id, 'erro', 'Erro ao carregar os dados');
  mostrarAlerta(erro.message, 'danger');
}

// números no formato brasileiro no eixo e no tooltip
function eixoNumerico() {
  return { beginAtZero: true, ticks: { callback: v => Number(v).toLocaleString('pt-BR') } };
}

function dicaComUnidade(unidade) {
  return {
    callbacks: {
      label: ctx => {
        const valor = ctx.parsed.y ?? ctx.parsed;
        // na rosca usa o nome da fatia
        const nome = ctx.chart.config.type === 'doughnut' ? ctx.label : (ctx.dataset.label || ctx.label);
        return ` ${nome}: ${formatarNumero(valor)} ${unidade}`;
      }
    }
  };
}

// soma a quantidade agrupando por um campo (tipo ou destinacao)
function somarPor(registros, campo) {
  const totais = {};
  registros.forEach(r => {
    totais[r[campo]] = (totais[r[campo]] || 0) + Number(r.quantidade);
  });
  return totais; // { Papel: 20, Plástico: 15, ... }
}

function formatarPeriodo(inicio, fim) {
  return `${formatarData(inicio)} a ${formatarData(fim)}`;
}

// gráficos

function graficoMensal(id, dados, nome, cor, unidade, tipo = 'bar') {
  if (!dados.length) return semDados(id);

  desenhar(id, {
    type: tipo,
    data: {
      labels: dados.map(d => d.rotulo),
      datasets: [{
        label: nome,
        data: dados.map(d => d.total),
        backgroundColor: tipo === 'line' ? cor + '33' : cor,
        borderColor: cor,
        borderWidth: 2,
        fill: tipo === 'line',
        tension: 0.3,
        borderRadius: 4
      }]
    },
    options: {
      maintainAspectRatio: false,
      plugins: { legend: { display: false }, tooltip: dicaComUnidade(unidade) },
      scales: { y: eixoNumerico() }
    }
  });
}

function graficoReciclagem(dados) {
  const id = 'grafico-reciclagem';
  if (!Number(dados.totalGerado)) return semDados(id);

  desenhar(id, {
    type: 'doughnut',
    data: {
      labels: ['Reciclado', 'Não reciclado'],
      datasets: [{
        data: [dados.totalReciclado, dados.totalNaoReciclado],
        backgroundColor: [CORES.reciclado, CORES.naoReciclado]
      }]
    },
    options: {
      maintainAspectRatio: false,
      cutout: '60%',
      plugins: { legend: { position: 'bottom' }, tooltip: dicaComUnidade('kg') }
    }
  });
}

function graficoSetores(dados) {
  const id = 'grafico-setores';
  const temValor = dados.some(s => Number(s.agua) || Number(s.energia) || Number(s.residuos));
  if (!dados.length || !temValor) return semDados(id);

  desenhar(id, {
    type: 'bar',
    data: {
      labels: dados.map(s => s.setor),
      datasets: [
        { label: 'Água (litros)', data: dados.map(s => s.agua), backgroundColor: CORES.agua, borderRadius: 4 },
        { label: 'Energia (kWh)', data: dados.map(s => s.energia), backgroundColor: CORES.energia, borderRadius: 4 },
        { label: 'Resíduos (kg)', data: dados.map(s => s.residuos), backgroundColor: CORES.residuos, borderRadius: 4 }
      ]
    },
    options: {
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'bottom' },
        tooltip: { callbacks: { label: ctx => ` ${ctx.dataset.label}: ${formatarNumero(ctx.parsed.y)}` } }
      },
      scales: { y: eixoNumerico() }
    }
  });
}

function graficoAgrupado(id, totais, tipo) {
  const nomes = Object.keys(totais);
  if (!nomes.length) return semDados(id);

  // maior primeiro
  nomes.sort((a, b) => totais[b] - totais[a]);

  const config = {
    type: tipo,
    data: {
      labels: nomes,
      datasets: [{
        label: 'Quantidade',
        data: nomes.map(n => totais[n]),
        backgroundColor: nomes.map((_, i) => PALETA[i % PALETA.length]),
        borderRadius: tipo === 'bar' ? 4 : 0
      }]
    },
    options: {
      maintainAspectRatio: false,
      plugins: {
        legend: { display: tipo !== 'bar', position: 'bottom' },
        tooltip: dicaComUnidade('kg')
      }
    }
  };
  if (tipo === 'bar') {
    config.options.indexAxis = 'y'; // barra deitada, os nomes ficam mais legíveis
    config.options.scales = { x: eixoNumerico() };
    config.options.plugins.tooltip = {
      callbacks: { label: ctx => ` ${formatarNumero(ctx.parsed.x)} kg` }
    };
  }
  desenhar(id, config);
}

// cards

function preencherCard(id, texto) {
  document.getElementById(id).textContent = texto;
}

function somaMensal(dados) {
  return dados.reduce((acc, d) => acc + Number(d.total), 0);
}

// carrega tudo

const IDS_GRAFICOS = [
  'grafico-agua', 'grafico-energia', 'grafico-residuos', 'grafico-reciclagem',
  'grafico-setores', 'grafico-tipo', 'grafico-destinacao'
];

async function carregarDashboard() {
  const filtros = {
    setorId: filtroSetor.value,
    dataInicio: filtroInicio.value,
    dataFim: filtroFim.value
  };
  // consumo-por-setor não aceita setorId
  const soPeriodo = { dataInicio: filtros.dataInicio, dataFim: filtros.dataFim };

  IDS_GRAFICOS.forEach(id => definirEstado(id, 'carregando'));
  ['total-agua', 'total-energia', 'total-residuos', 'percentual-reciclagem']
    .forEach(id => preencherCard(id, '...'));

  // um catch por chamada - se uma falhar o resto aparece
  const tarefas = [
    api.dashboard('agua-mensal', filtros).then(dados => {
      graficoMensal('grafico-agua', dados, 'Água', CORES.agua, 'litros');
      preencherCard('total-agua', `${formatarNumero(somaMensal(dados))} L`);
    }).catch(erro => { comErro('grafico-agua', erro); preencherCard('total-agua', '—'); }),

    api.dashboard('energia-mensal', filtros).then(dados => {
      graficoMensal('grafico-energia', dados, 'Energia', CORES.energia, 'kWh', 'line');
      preencherCard('total-energia', `${formatarNumero(somaMensal(dados))} kWh`);
    }).catch(erro => { comErro('grafico-energia', erro); preencherCard('total-energia', '—'); }),

    api.dashboard('residuos-mensal', filtros).then(dados => {
      graficoMensal('grafico-residuos', dados, 'Resíduos', CORES.residuos, 'kg');
    }).catch(erro => comErro('grafico-residuos', erro)),

    api.dashboard('reciclagem', filtros).then(dados => {
      graficoReciclagem(dados);
      preencherCard('total-residuos', `${formatarNumero(dados.totalGerado)} kg`);
      preencherCard('percentual-reciclagem', Number(dados.totalGerado)
        ? `${formatarNumero(dados.percentualReciclagem)}%`
        : '—');
    }).catch(erro => {
      comErro('grafico-reciclagem', erro);
      preencherCard('total-residuos', '—');
      preencherCard('percentual-reciclagem', '—');
    }),

    api.dashboard('consumo-por-setor', soPeriodo)
      .then(graficoSetores)
      .catch(erro => comErro('grafico-setores', erro)),

    api.listar('residuos', filtros).then(registros => {
      graficoAgrupado('grafico-tipo', somarPor(registros, 'tipo'), 'bar');
      graficoAgrupado('grafico-destinacao', somarPor(registros, 'destinacao'), 'doughnut');
    }).catch(erro => {
      comErro('grafico-tipo', erro);
      comErro('grafico-destinacao', erro);
    })
  ];

  await Promise.all(tarefas);
}

// comparação entre períodos

const formComparacao = document.getElementById('form-comparacao');
const ID_COMPARACAO = 'grafico-comparacao';

async function compararPeriodos() {
  const modulo = document.getElementById('comp-modulo').value;
  const a = { inicio: document.getElementById('comp-a-inicio').value, fim: document.getElementById('comp-a-fim').value };
  const b = { inicio: document.getElementById('comp-b-inicio').value, fim: document.getElementById('comp-b-fim').value };

  if (!a.inicio || !a.fim || !b.inicio || !b.fim) {
    mostrarAlerta('Preencha o início e o fim dos dois períodos.', 'warning');
    return;
  }
  const erroA = validarPeriodo(a.inicio, a.fim);
  const erroB = validarPeriodo(b.inicio, b.fim);
  if (erroA || erroB) {
    mostrarAlerta(erroA ? `Período A: ${erroA}` : `Período B: ${erroB}`, 'warning');
    return;
  }

  definirEstado(ID_COMPARACAO, 'carregando');
  const setorId = filtroSetor.value;

  try {
    const [dadosA, dadosB] = await Promise.all([
      api.dashboard(`${modulo}-mensal`, { setorId, dataInicio: a.inicio, dataFim: a.fim }),
      api.dashboard(`${modulo}-mensal`, { setorId, dataInicio: b.inicio, dataFim: b.fim })
    ]);

    if (!dadosA.length && !dadosB.length) return semDados(ID_COMPARACAO);

    // os meses dos dois períodos são diferentes, então alinha pela posição
    // (1º mês de A com 1º mês de B...). O nome real do mês aparece no tooltip
    const qtdMeses = Math.max(dadosA.length, dadosB.length);
    const rotulos = Array.from({ length: qtdMeses }, (_, i) => `Mês ${i + 1}`);
    const unidade = UNIDADES[modulo];

    const serie = (nome, dados, cor) => ({
      label: nome,
      data: dados.map(d => d.total),
      rotulosReais: dados.map(d => d.rotulo),
      backgroundColor: cor,
      borderRadius: 4
    });

    desenhar(ID_COMPARACAO, {
      type: 'bar',
      data: {
        labels: rotulos,
        datasets: [
          serie(`Período A (${formatarPeriodo(a.inicio, a.fim)})`, dadosA, CORES.periodoA),
          serie(`Período B (${formatarPeriodo(b.inicio, b.fim)})`, dadosB, CORES.periodoB)
        ]
      },
      options: {
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom' },
          tooltip: {
            callbacks: {
              title: itens => itens.map(i => i.dataset.rotulosReais[i.dataIndex]).join(' / '),
              label: ctx => ` ${ctx.dataset.label.split(' (')[0]}: ${formatarNumero(ctx.parsed.y)} ${unidade}`
            }
          }
        },
        scales: { y: eixoNumerico() }
      }
    });
  } catch (erro) {
    comErro(ID_COMPARACAO, erro);
  }
}

// eventos

formFiltros.addEventListener('submit', (evento) => {
  evento.preventDefault();
  const erro = validarPeriodo(filtroInicio.value, filtroFim.value);
  if (erro) {
    mostrarAlerta(erro, 'warning');
    return;
  }
  carregarDashboard();
});

document.getElementById('btn-limpar').addEventListener('click', () => {
  formFiltros.reset();
  carregarDashboard();
});

formComparacao.addEventListener('submit', (evento) => {
  evento.preventDefault();
  compararPeriodos();
});

if (typeof Chart === 'undefined') {
  mostrarAlerta('Não foi possível carregar a biblioteca de gráficos. Verifique sua internet.', 'danger');
} else {
  Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
  Chart.defaults.color = '#555';
  definirEstado(ID_COMPARACAO, 'aguardando', 'Escolha os dois períodos e clique em Comparar.');
  preencherSelectSetores(filtroSetor, true);
  carregarDashboard();
}
