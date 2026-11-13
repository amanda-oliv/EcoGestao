# EcoGestão - Frontend

Parte do frontend do EcoGestão (APS). São as telas de cadastro/consulta de água, energia,
resíduos e setores, e o dashboard com os gráficos.

O frontend não acessa o banco. Tudo passa pela API REST (repositório `amanda-oliv/ecogestao`),
com fetch e JSON.

Feito com HTML, CSS e JavaScript puro, Bootstrap 5.3 e Chart.js 4 (os dois por CDN, então precisa de internet).
Não tem build nem npm, é só abrir com um servidor local.

## Como rodar

1. Subir a API (por padrão fica em `http://localhost:8080`).
2. Abrir esta pasta no VS Code. Se aparecer o Modo Restrito, clicar em Trust.
3. Com a extensão Live Server instalada, clicar em **Go Live**. Abre em `http://127.0.0.1:5500`.

Se a API estiver em outro endereço, é só mudar a `BASE_URL` no começo do `js/api.js`.

## CORS

O site roda na porta 5500 e a API na 8080, então o navegador bloqueia as chamadas se a API não
liberar essa origem. Aparece "blocked by CORS policy" no console (F12) e na tela aparece
"Não foi possível conectar à API".

Pra resolver, a API precisa de uma classe de configuração assim:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://127.0.0.1:5500", "http://localhost:5500")
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
```

## Páginas

- `index.html` - dashboard (cards, gráficos, filtros e comparação entre períodos)
- `agua.html` - consumo de água em litros
- `energia.html` - consumo de energia em kWh
- `residuos.html` - resíduos em kg, com tipo e destinação
- `setores.html` - cadastro e lista de setores

Água, energia e resíduos usam o mesmo `js/consumo.js`. Cada página só informa o módulo e a unidade
no `<body>` (`data-modulo` e `data-unidade`).

## Arquivos JS

- `api.js` - todas as chamadas à API
- `utils.js` - formatação de data/número, alertas, select de setores, validação do período
- `consumo.js` - tabela, filtros, modal de cadastro/edição e exclusão
- `setores.js` - tela de setores
- `dashboard.js` - cards e gráficos

## Testando

Pra ter alguma coisa nos gráficos, cadastrar pelo menos 2 setores e registros em 2 meses diferentes
em água, energia e resíduos.
