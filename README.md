# Desafio Votação

API REST para gerenciar as votações das assembleias de uma cooperativa.
Cada decisão a ser tomada é uma *pauta*. Para uma pauta ser votada, é aberta uma *sessão de votação* com tempo limitado.
Enquanto a sessão estiver aberta, os associados votam *SIM* ou *NAO*. Cada associado, identificado pelo CPF, só pode votar uma vez em cada pauta.
No final, a API conta os votos e indica se a pauta foi aprovada, reprovada ou empatou.

Tecnologias: Java 21, Spring Boot 3.5, Spring Data JPA, H2, Bean Validation, Lombok e Springdoc OpenAPI (Swagger UI).

---

# Requisitos

- **Java 21** (JDK). Para conferir a versão: `java -version`.

Não é preciso instalar o Maven: o projeto inclui o Maven Wrapper (`mvnw` / `mvnw.cmd`), que baixa a versão certa na primeira execução.

# Como rodar a aplicação

Na pasta do projeto, execute o comando do seu sistema:

Linux / macOS: ./mvnw spring-boot:run

Windows: mvnw.cmd spring-boot:run


Os dados ficam salvos em `./data/desafio-votacao.mv.db` e são mantidos entre reinicializações. Para começar com o banco vazio, pare a aplicação e apague a pasta `data/`.

# Rodando os testes

./mvnw test


Os testes de integração usam um banco H2 em memória e não alteram os dados da pasta `data/`.

> Se a IDE tiver compilado o projeto na pasta `target/` e os testes falharem com `Unresolved compilation problem`, execute `./mvnw clean test` para recompilar tudo com o Maven.

O teste de desempenho (veja [Performance](#performance-bônus-2)) fica fora do build normal, por ser mais demorado. Para rodá-lo:

./mvnw clean test -Pperformance

# Swagger UI

Com a aplicação rodando, abra no navegador:

http://localhost:8080/swagger-ui.html

O Swagger UI mostra todos os endpoints, com descrições, exemplos e os códigos de resposta possíveis, e permite testá-los direto no navegador. A especificação OpenAPI em JSON fica em `http://localhost:8080/v3/api-docs`.

Para consultar os dados direto no banco de dados, use o console do H2 em `http://localhost:8080/h2-console`:
- JDBC URL: `jdbc:h2:file:./data/desafio-votacao`
- usuário: `sa`
- senha: vazia


### Exemplo completo com curl

```bash
# 1. Criar a pauta (a resposta traz o id, por exemplo 1)
curl -X POST http://localhost:8080/api/v1/pautas \
  -H "Content-Type: application/json" -d '{"titulo": "Compra da nova sede"}'

# 2. Abrir a sessão por 5 minutos
curl -X POST http://localhost:8080/api/v1/pautas/1/sessao \
  -H "Content-Type: application/json" -d '{"duracaoMinutos": 5}'

# 3. Votar
curl -X POST http://localhost:8080/api/v1/pautas/1/votos \
  -H "Content-Type: application/json" -d '{"cpf": "12345678901", "escolha": "SIM"}'

# 4. Consultar o resultado
curl http://localhost:8080/api/v1/pautas/1/resultado
```

> O validador de CPF é um *fake* com resultado aleatório (veja abaixo), então o passo 3 pode retornar 404 ou 403 mesmo com um CPF no formato correto. Nesse caso, tente novamente com outro CPF.

---

## Decisões de design

### H2 em modo arquivo para persistência

O desafio pede que os dados não se percam quando a aplicação é reiniciada.
O H2 em modo arquivo (`jdbc:h2:file:./data/desafio-votacao`) atende a isso sem exigir a instalação ou a configuração de um banco de dados externo: basta ter o Java para rodar o projeto. O esquema é criado e atualizado pelo Hibernate (`ddl-auto=update`), o que é suficiente para um projeto deste tamanho.

Para produção, o caminho natural é trocar por um banco como o PostgreSQL, com o esquema gerenciado por migrations versionadas (Liquibase). Como todo o acesso a dados passa pelo Spring Data JPA, essa troca afeta principalmente a configuração, e não o código.

### Arquitetura em camadas

O código está organizado em camadas com responsabilidades separadas:

| Pacote | Responsabilidade |
|---|---|
| `controller` | Recebe as requisições HTTP, valida o formato da entrada (`@Valid`) e retorna as respostas. Não contém regras de negócio. |
| `service` | Regras de negócio: validações, cálculo de datas, contagem de votos e logs de negócio. |
| `repository` | Acesso a dados com Spring Data JPA. |
| `domain` | Entidades JPA e enums do domínio. |
| `dto` | Objetos de entrada e saída da API. As entidades nunca são expostas diretamente. |
| `client` | Integração com sistemas externos (validação de CPF). |
| `exception` | Exceções de negócio, convertidas em respostas HTTP por um único `GlobalExceptionHandler`. |

Os serviços lançam exceções com significado de negócio (`RecursoNaoEncontradoException`, `ConflitoException`, `RegraNegocioException`, `AssociadoNaoAptoException`), e só o `GlobalExceptionHandler` decide o código HTTP de cada uma. Assim, os serviços não dependem da camada web e são fáceis de testar.

A hora atual vem de um `Clock` injetado. Com isso, os testes fixam o horário e verificam com precisão a abertura, o encerramento e o limite da sessão.

### Voto único com restrição de unicidade no banco de dados

A regra "cada CPF vota uma única vez por pauta" é garantida em dois níveis:

1. **No serviço:** antes de salvar, `existsByPautaIdAndCpfAssociado` verifica se o CPF já votou. Se já votou, a resposta é 409 com uma mensagem clara, sem nem chamar o validador externo.
2. **No banco de dados:** a tabela `voto` tem a restrição única `uk_voto_pauta_cpf` sobre `(pauta_id, cpf_associado)`.

A verificação no serviço sozinha não basta: se duas requisições do mesmo CPF chegarem ao mesmo tempo, as duas podem passar pela verificação antes que alguma seja salva. A restrição no banco de dados é a garantia definitiva. O voto é salvo com `saveAndFlush`, para que uma violação seja detectada na hora e convertida no mesmo 409, em vez de aparecer como erro 500.

A regra "uma sessão por pauta" usa a mesma estratégia, com a restrição `uk_sessao_votacao_pauta` sobre `pauta_id`.

### Many-to-one em vez de one-to-one

Cada pauta tem no máximo uma sessão, mas a relação `SessaoVotacao → Pauta` está mapeada como `@ManyToOne`, com uma restrição única em `pauta_id`. No banco de dados, o resultado é o mesmo de um one-to-one: uma chave estrangeira que não pode se repetir. Os motivos dessa escolha são:

- **A regra fica explícita e com nome:** a restrição `uk_sessao_votacao_pauta` está declarada na entidade, ao lado da `uk_voto_pauta_cpf`, e seu nome aparece nos erros. Com `@OneToOne`, a unicidade seria gerada implicitamente pelo Hibernate.
- **Carregamento lazy previsível:** o `@ManyToOne(fetch = LAZY)` funciona de forma consistente. As associações `@OneToOne` têm limitações conhecidas de carregamento lazy, principalmente no lado inverso.
- **Facilidade de mudança:** se no futuro uma pauta puder ter várias sessões (por exemplo, um segundo turno), basta remover a restrição. O mapeamento e o código que o usa continuam iguais.
- **Consistência:** `Voto → Pauta` também é `@ManyToOne`, e as duas relações seguem o mesmo padrão.

### Cliente fake de validação de CPF (bônus 1)

A validação do CPF fica atrás de uma interface, `ValidadorCpfClient`, que funciona como fachada para o sistema externo. A implementação atual, `ValidadorCpfClientFake`, simula esse sistema de forma aleatória:

- em 50% das consultas o CPF é inválido e é lançada uma `CpfInvalidoException`, que a API retorna como **404**;
- nos outros 50%, retorna `ABLE_TO_VOTE` ou `UNABLE_TO_VOTE`, também com 50% de chance cada. O `UNABLE_TO_VOTE` é retornado como **403**.

Como o `VotoService` só conhece a interface, o fake pode ser substituído por um cliente HTTP real sem alterar o serviço. O gerador de números aleatórios é injetado pelo construtor, o que permite testar o fake de forma determinística. Nos testes de integração do voto, o cliente é substituído por um mock, para que cada cenário seja reproduzível.

A chamada ao validador é a última verificação antes de salvar o voto. Assim, uma requisição que já seria recusada por outro motivo (pauta inexistente, sessão encerrada ou voto duplicado) nunca chega a gerar uma chamada ao sistema externo.

### Não expor o CPF nos logs

O CPF é um dado pessoal. A aplicação nunca o grava nos logs nem nas mensagens de erro:

- **Logs de negócio:** registram os ids e o motivo, nunca o CPF. Por exemplo, `Voto registado: votoId=12, pautaId=3, escolha=SIM` ou `Voto recusado: pautaId=3, motivo=sessão encerrada`. Os eventos normais usam o nível INFO e as recusas, o nível WARN.
- **Mensagens de erro:** são genéricas, como "CPF inválido", "Este CPF já votou na pauta 3" ou "O associado não está apto a votar". As exceções nem recebem o CPF, para que não seja possível incluí-lo por engano.
- **Logs do Hibernate:** quando uma restrição é violada, o `SqlExceptionHelper` do Hibernate registra os valores da linha rejeitada, o que incluiria o CPF. Esse log está desligado no `application.properties`, mas a exceção continua sendo tratada normalmente. O `show-sql` mostra apenas `?` no lugar dos valores, então não expõe dados.

Existem testes que garantem essa regra: eles capturam a saída da aplicação durante votos aceitos e recusados, e durante uma violação real da restrição única, e falham se o CPF aparecer.

O CPF só aparece na resposta de um voto aceito, retornada ao próprio cliente que o enviou.

### Performance (bônus 2)

A aplicação foi pensada para cenários com centenas de milhares de votos:

- **Contagem no banco:** o resultado é calculado com uma única consulta `count ... group by escolha`, que retorna no máximo duas linhas (SIM e NAO). Os votos nunca são carregados para a memória, então o custo de memória é o mesmo com 10 ou 1 milhão de votos.
- **Índices certos:** a restrição única `(pauta_id, cpf_associado)` cria um índice que atende tanto a verificação de voto duplicado quanto a contagem por pauta.
- **Poucas consultas por voto:** registrar um voto faz três leituras simples por chave (pauta, sessão e voto duplicado) e um insert.
- **Sem SQL no console:** o `spring.jpa.show-sql` fica desligado, porque imprimir cada comando SQL pesa muito com alto volume. Para depurar, dá para ligar com `-Dspring.jpa.show-sql=true`.

O teste `VotacaoPerformanceTest` pré-carrega **300 mil votos** em uma pauta e mede, pela API completa:

| Cenário | Resultado medido |
|---|---|
| Consulta do resultado com 300 mil votos | ~5 ms (p95 ~6 ms) |
| Registro de um voto com 300 mil já gravados | ~2,5 ms (p95 ~6 ms) |
| 4.000 votos simultâneos em 16 threads | ~5.600 votos/s, todos aceitos |
| Mesmo CPF em 50 requisições simultâneas | 1 aceita, 49 recusadas com 409 |
| Contagem final | confere exatamente com todos os votos gravados |

Os números são de uma máquina de desenvolvimento, com H2 em memória e sem rede, então servem como ordem de grandeza. Os limites verificados no teste são folgados de propósito: o objetivo é detectar regressões graves, como a contagem passar a carregar os votos para a memória, e não comparar máquinas.

Duas decisões vieram das medições:
- **Desligar o `show-sql`:** com 16 threads, os votos por segundo subiram de cerca de 1.100 para cerca de 5.600.
- **Não criar um índice extra:** testei um índice em `(pauta_id, escolha)` para a contagem, e ele não trouxe ganho mensurável, porque o índice da restrição única já atende a consulta. Como todo índice deixa as gravações mais caras, ele ficou de fora.

O teste também confirma, sob concorrência real, a estratégia de voto único: com 50 requisições simultâneas do mesmo CPF, a restrição do banco garante que só um voto é gravado.

Em produção, os próximos passos seriam trocar o H2 por um banco como o PostgreSQL e, se a consulta de resultado fosse muito frequente durante a votação, guardar o resultado em cache por alguns segundos.

---

## Estrutura do projeto

```
src/main/java/com/caioribasregis/desafiovotacao/
├── client/       # Fachada e implementação fake do validador de CPF
├── config/       # Clock e configuração do OpenAPI
├── controller/   # Endpoints REST e tratamento global de erros
├── domain/       # Entidades JPA e enums
├── dto/          # Objetos de entrada e saída da API
├── exception/    # Exceções de negócio
├── repository/   # Repositórios Spring Data JPA
└── service/      # Regras de negócio
```

Os testes, em `src/test`, cobrem cada camada:
- testes unitários dos serviços, com Mockito e relógio fixo;
- testes dos controllers com `@WebMvcTest`;
- testes de repositório com `@DataJpaTest`;
- testes de integração de ponta a ponta com `@SpringBootTest` e H2 em memória.
